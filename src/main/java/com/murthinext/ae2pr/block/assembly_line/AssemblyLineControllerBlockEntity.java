package com.murthinext.ae2pr.block.assembly_line;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.multiblock.module.ModuleSnapshot;
import com.murthinext.ae2pr.multiblock.module.ModuleType;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;

/**
 * 水晶装配线控制器方块实体：结构检测 + 配方执行。
 * <p>
 * 结构检测由 {@link StructureValidator} 完成：结构未变化时走缓存快速路径，
 * 保证有序匹配使用的部件顺序稳定。
 * <p>
 * 执行流程：匹配配方、检查输出空间、扣除所需电力与原料、加工一定 tick 后产出到输出总线。
 * <p>
 * 实际并行数 = 各输入现有份数（物品 / 流体）中的较小值，上限为
 * 基础并行 + (片数 - 最小片数) × 每片增量 + 模块加成；能量 = 每并行耗电（配方可覆盖，默认取配置）× 实际并行数。
 */
public class AssemblyLineControllerBlockEntity extends BlockEntity {

    /** 结构检测周期（tick） */
    private static final int CHECK_INTERVAL = 20;
    /** 暂停后的重试间隔（tick） */
    private static final int RETRY_INTERVAL = 20;

    /** 暂停原因（供界面报错） */
    public enum Error {
        NONE, POWER, OUTPUT
    }

    private int tickCounter;

    /** 加工剩余 tick（<= 0 表示空闲或暂停） */
    private int jobTicksLeft;
    /** 当前作业的实际总耗时（tick，含速度模块加成） */
    private int jobTotalTicks;
    /** 是否处于暂停态（电力/输出不足，黄色贴图） */
    private boolean paused;
    private Error error = Error.NONE;
    private int retryCounter;
    /** 当前作业（加工中保留，用于结算产物） */
    @Nullable
    private Match currentMatch;

    /** 待恢复的作业数据（load 时 world 可能未就绪，首个服务端 tick 再解析） */
    @Nullable
    private ResourceLocation pendingRecipeId;
    private int pendingParallel;
    private int pendingTicks;
    private int pendingTotalTicks;

    /** 客户端展示用：当前作业产物（仅由同步包写入） */
    private ItemStack clientJobOutput = ItemStack.EMPTY;

    /** 最近一次检测结果（供状态界面展示） */
    private int lastSlices;
    private int lastMismatches;
    @Nullable
    private BlockPos lastMismatchPos;
    private char lastExpected = ' ';
    @Nullable
    private Block lastFound;

    /** 结构内的输入总线（按 片 → 行 → 列 排序，从主机侧到另一侧） */
    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    /** 结构内的输出总线（最后一片） */
    @Nullable
    private ItemBusBlockEntity outputBus;
    /** 结构内的输入仓（按 片 → 行 → 列 排序） */
    private final List<FluidHatchBlockEntity> inputHatches = new ArrayList<>();
    /** 结构内的能源仓 */
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    /** 结构内模块快照（结构未变化时沿用） */
    private ModuleSnapshot modules = ModuleSnapshot.EMPTY;
    /** 结构内控制外壳单元格（含模块），用于每 tick 轻量检查 */
    private final List<UnitCell> unitCells = new ArrayList<>();
    /** 结构验证器：模式匹配 + 缓存快速路径 */
    private final StructureValidator structure = new StructureValidator(AssemblyLineStructure.PATTERN);
    /** 升级槽 */
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(ModItems.CRYSTAL_ASSEMBLY_LINE.get(), 4,
            this::onUpgradesChanged);

    public AssemblyLineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_ASSEMBLY_LINE.get(), pos, state);
    }

    /** 服务端 tick：每 tick 推进加工作业，每 {@link #CHECK_INTERVAL} tick 重新检测结构。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        restorePendingJob();
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
        tickJob();
    }

    // ---------------------------------------------------------------- 加工作业

    private void tickJob() {
        if (!isFormed()) {
            cancelJob();
            return;
        }
        // 加工中每 tick 轻量检查控制外壳/模块是否被替换，变化时立即完整验证
        if (jobTicksLeft > 0 && unitCellsChanged()) {
            validateStructure();
            if (!isFormed()) {
                cancelJob();
                return;
            }
        }
        if (jobTicksLeft > 0) {
            if (--jobTicksLeft == 0) {
                finishJob();
            }
            return;
        }
        if (retryCounter > 0) {
            retryCounter--;
            return;
        }
        retryCounter = RETRY_INTERVAL;
        tryStartJob();
    }

    /** 取消当前作业（结构损坏或模块资格失效；已投入的材料与能量不返还）。 */
    private void cancelJob() {
        if (jobTicksLeft > 0 || jobTotalTicks > 0 || currentMatch != null || paused || error != Error.NONE) {
            jobTicksLeft = 0;
            jobTotalTicks = 0;
            currentMatch = null;
            clearPause();
        }
    }

    /** 尝试开始一次加工：匹配配方 → 输出检查 → 扣电 → 扣料 → 进入运行态。 */
    private void tryStartJob() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        // 启动前再次验证结构与模块（缓存命中时开销极低），避免结构检测间隔内的短窗口
        validateStructure();
        if (!isFormed()) {
            return;
        }
        Match match = findMatch();
        if (match == null) {
            clearPause();
            return;
        }
        if (!canOutput(match.recipe(), match.parallel())) {
            pause(Error.OUTPUT);
            return;
        }
        double cost = match.recipe().getEnergyPerParallel() * match.parallel() * lossyEnergyMultiplier();
        if (extractNetworkEnergy(cost, true) < cost) {
            pause(Error.POWER);
            return;
        }
        // 扣电与扣料
        extractNetworkEnergy(cost, false);
        consumeInputs(match);

        jobTotalTicks = scaledDuration(match.recipe().getDuration());
        jobTicksLeft = jobTotalTicks;
        currentMatch = match;
        paused = false;
        error = Error.NONE;
        applyControllerState();
    }

    /** 加工完成：产物写入输出总线，并立即触发下一次配方检查以消除连续合成之间的空档。 */
    private void finishJob() {
        Match match = currentMatch;
        currentMatch = null;
        if (match != null && level != null) {
            insertOutputs(match.recipe(), match.parallel());
        }
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        applyControllerState();
        if (isFormed()) {
            tryStartJob();
        }
    }

    private void pause(Error reason) {
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        paused = true;
        error = reason;
        applyControllerState();
    }

    private void clearPause() {
        if (paused || error != Error.NONE) {
            paused = false;
            error = Error.NONE;
            applyControllerState();
        }
    }

    // ---------------------------------------------------------------- 配方匹配

    /** 一次匹配的结果 */
    private record Match(CrystalAssemblyLineRecipe recipe, int parallel, List<Integer> busIndices,
            List<Integer> hatchIndices) {
    }

    /** 控制外壳单元格 */
    private record UnitCell(BlockPos pos, Block block) {
    }

    /** 结构允许的最大并行数 */
    public int maxParallel() {
        int base = Config.assemblyBaseParallel()
                + Math.max(0, lastSlices - AssemblyLineStructure.MIN_SLICES) * Config.assemblyParallelPerSlice();
        return Math.min(base + moduleParallelBonus(), Config.assemblyMaxParallel());
    }

    /** 模块提供的并行上限加成 */
    private int moduleParallelBonus() {
        return modules.count(ModModules.PARALLEL) * Config.assemblyModuleParallelPerUnit();
    }

    /** 模块提供的速度倍率 */
    public double getSpeedMultiplier() {
        double multiplier = 1.0 + modules.count(ModModules.SPEED) * Config.assemblyModuleSpeedPerUnit();
        return Math.min(multiplier, Config.assemblyMaxSpeedMultiplier()) * speedCardMultiplier();
    }

    /** 已安装的加速卡数量 */
    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    /** 加速卡的速度倍率 */
    public double speedCardMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    /** 加速卡的耗能倍率 */
    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    /** 升级槽 */
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 升级变化回调 */
    private void onUpgradesChanged() {
        setChanged();
    }

    /** 指定功能模块的当前等级 */
    public int getModuleLevel(ModuleType type) {
        return modules.level(type);
    }

    /** 按速度模块倍率折算实际耗时 */
    private int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    @Nullable
    private Match findMatch() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        for (CrystalAssemblyLineRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CRYSTAL_ASSEMBLY_LINE_TYPE.get())) {
            // 模块要求不满足的配方直接跳过，不阻塞其他合法配方
            if (!modules.satisfies(recipe.getModuleRequirements())) {
                continue;
            }
            List<Integer> busIndices = matchItems(recipe);
            if (busIndices == null) {
                continue;
            }
            List<Integer> hatchIndices = matchFluids(recipe);
            if (hatchIndices == null) {
                continue;
            }
            // 实际并行数由现有原料份数决定，最多到结构允许的上限
            int parallel = parallelFor(recipe, busIndices, hatchIndices);
            if (parallel <= 0) {
                continue;
            }
            return new Match(recipe, parallel, busIndices, hatchIndices);
        }
        return null;
    }

    /** 物品输入匹配：有序时按总线顺序，无序时逐个选取（只校验种类，份数在并行计算时统计）。 */
    @Nullable
    private List<Integer> matchItems(CrystalAssemblyLineRecipe recipe) {
        List<CrystalAssemblyLineRecipe.ItemInput> inputs = recipe.getItemInputs();
        List<Integer> indices = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return indices;
        }
        if (Config.assemblyItemsOrdered()) {
            if (inputBuses.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                if (!hasItems(inputBuses.get(i), inputs.get(i))) {
                    return null;
                }
                indices.add(i);
            }
            return indices;
        }
        boolean[] used = new boolean[inputBuses.size()];
        for (CrystalAssemblyLineRecipe.ItemInput input : inputs) {
            int found = -1;
            for (int i = 0; i < inputBuses.size(); i++) {
                if (!used[i] && hasItems(inputBuses.get(i), input)) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            used[found] = true;
            indices.add(found);
        }
        return indices;
    }

    private static boolean hasItems(ItemBusBlockEntity bus, CrystalAssemblyLineRecipe.ItemInput input) {
        return input.test(bus.getStorage().getStackInSlot(0));
    }

    /** 各行可并行份数的较小值：物品按 count、流体按 amount 取整后向上限截断。 */
    private int parallelFor(CrystalAssemblyLineRecipe recipe, List<Integer> busIndices,
            List<Integer> hatchIndices) {
        int parallel = maxParallel();
        List<CrystalAssemblyLineRecipe.ItemInput> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            ItemStack content = inputBuses.get(busIndices.get(i)).getStorage().getStackInSlot(0);
            parallel = Math.min(parallel, content.getCount() / itemInputs.get(i).count());
        }
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            FluidStack stored = inputHatches.get(hatchIndices.get(i)).getTank().getFluid();
            parallel = Math.min(parallel, stored.getAmount() / fluidInputs.get(i).getAmount());
        }
        return parallel;
    }

    /** 流体输入匹配：有序时按仓顺序，无序时逐个选取（只校验种类，份数在并行计算时统计）。 */
    @Nullable
    private List<Integer> matchFluids(CrystalAssemblyLineRecipe recipe) {
        List<FluidStack> inputs = recipe.getFluidInputs();
        List<Integer> indices = new ArrayList<>(inputs.size());
        if (inputs.isEmpty()) {
            return indices;
        }
        if (Config.assemblyFluidsOrdered()) {
            if (inputHatches.size() < inputs.size()) {
                return null;
            }
            for (int i = 0; i < inputs.size(); i++) {
                if (!hasFluid(inputHatches.get(i), inputs.get(i))) {
                    return null;
                }
                indices.add(i);
            }
            return indices;
        }
        boolean[] used = new boolean[inputHatches.size()];
        for (FluidStack input : inputs) {
            int found = -1;
            for (int i = 0; i < inputHatches.size(); i++) {
                if (!used[i] && hasFluid(inputHatches.get(i), input)) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            used[found] = true;
            indices.add(found);
        }
        return indices;
    }

    private static boolean hasFluid(FluidHatchBlockEntity hatch, FluidStack input) {
        FluidStack stored = hatch.getTank().getFluid();
        return stored.isFluidEqual(input) && stored.getAmount() > 0;
    }

    // ---------------------------------------------------------------- 扣料与产出

    private void consumeInputs(Match match) {
        List<CrystalAssemblyLineRecipe.ItemInput> itemInputs = match.recipe().getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            // 单次抽取最多只能取走物品的堆叠上限（64），按需求量分批抽取
            int remaining = itemInputs.get(i).count() * match.parallel();
            var storage = inputBuses.get(match.busIndices().get(i)).getStorage();
            while (remaining > 0) {
                ItemStack taken = storage.extractItem(0, remaining, false);
                if (taken.isEmpty()) {
                    break;
                }
                remaining -= taken.getCount();
            }
        }
        List<FluidStack> fluidInputs = match.recipe().getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputHatches.get(match.hatchIndices().get(i)).getTank()
                    .drain(fluidInputs.get(i).getAmount() * match.parallel(),
                            IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 输出总线能否容纳全部产物（单类型，容量 32K）。 */
    private boolean canOutput(CrystalAssemblyLineRecipe recipe, int parallel) {
        if (outputBus == null) {
            return false;
        }
        ItemStack simulated = outputBus.getStorage().getStackInSlot(0).copy();
        for (ItemStack output : recipe.getItemOutputs()) {
            int total = output.getCount() * parallel;
            if (simulated.isEmpty()) {
                if (total > ItemBusBlockEntity.CAPACITY) {
                    return false;
                }
                simulated = output.copyWithCount(total);
            } else {
                if (!ItemStack.isSameItemSameTags(simulated, output)
                        || simulated.getCount() + total > ItemBusBlockEntity.CAPACITY) {
                    return false;
                }
                simulated.grow(total);
            }
        }
        return true;
    }

    private void insertOutputs(CrystalAssemblyLineRecipe recipe, int parallel) {
        Level level = this.level;
        if (level == null || outputBus == null) {
            return;
        }
        for (ItemStack output : recipe.getItemOutputs()) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputBus.getStorage(),
                    output.copyWithCount(output.getCount() * parallel), false);
            if (!remainder.isEmpty()) {
                // 启动前已检查；兜底掉落到输出总线处，避免产物丢失
                ae2pr.LOGGER.warn("水晶装配线产物无法放入输出总线，已掉落 {}", remainder);
                Block.popResource(level, outputBus.getBlockPos(), remainder);
            }
        }
    }

    /** 从结构内能源仓一次性抽取能量（AE）；simulate 时只求和可用量。 */
    private double extractNetworkEnergy(double amount, boolean simulate) {
        double remaining = amount;
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (remaining <= 0) {
                break;
            }
            remaining -= hatch.extractAEPower(remaining, simulate);
        }
        return amount - remaining;
    }

    // ---------------------------------------------------------------- 结构检测

    /** 立即检测一次结构，并按结果切换控制器与结构内方块的状态；结构未变化时走缓存快速路径。 */
    public void validateStructure() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get())) {
            return;
        }
        Direction facing = state.getValue(CrystalAssemblyLineBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                AssemblyLineStructure.maxSlices());
        if (result.fromCache()) {
            // 缓存命中：方块类型与方块实体均未变化，无需刷新外观与部件引用
            return;
        }

        lastSlices = result.slices();
        lastMismatches = result.mismatches();
        lastMismatchPos = result.mismatchPos();
        lastExpected = result.expected();
        lastFound = result.found();

        ModuleSnapshot snapshot = result.formed() ? result.modules() : ModuleSnapshot.EMPTY;
        // 模块资格失效（拆除 / 降级 / 重复安装）：取消当前作业，已投入的材料与能量不返还
        if (jobTicksLeft > 0 && currentMatch != null
                && !snapshot.satisfies(currentMatch.recipe().getModuleRequirements())) {
            cancelJob();
        }
        modules = snapshot;

        boolean formed = result.formed();
        // 运行态仅在成型后生效：控制外壳与主机的工作态贴图由作业驱动
        boolean active = formed && jobTicksLeft > 0;

        AssemblyLineStructure.updateFormed(level, worldPosition, facing, result, active);
        refreshParts(level, result);
        applyControllerState(formed);
    }

    /** 收集结构内的总线、输入仓、能源仓与控制外壳（仅在结构成型时收集）。 */
    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        inputHatches.clear();
        energyHatches.clear();
        unitCells.clear();
        outputBus = null;
        if (!result.formed()) {
            return;
        }
        // 显式按 片 → 行 → 列 排序，保证有序匹配使用的部件顺序稳定（对应 GT 的 partSorter）
        List<BlockPos> cells = new ArrayList<>(result.cells());
        cells.sort(AssemblyLineStructure.PATTERN.posComparator(
                getBlockState().getValue(CrystalAssemblyLineBlock.FACING), Direction.UP, result.mirrorSide(),
                result.mirrorFront()));
        for (BlockPos pos : cells) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBusBlockEntity bus) {
                if (bus.isOutputBus()) {
                    outputBus = bus;
                } else {
                    inputBuses.add(bus);
                }
            } else if (blockEntity instanceof FluidHatchBlockEntity hatch) {
                inputHatches.add(hatch);
            } else if (blockEntity instanceof FluixCrystalEnergyHatchBlockEntity energy) {
                energyHatches.add(energy);
            }
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof AssemblyLineUnitBlock) {
                unitCells.add(new UnitCell(pos, block));
            }
        }
    }

    /** 结构内控制外壳单元格的方块类型是否发生变化（模块被替换 / 拆除）。 */
    private boolean unitCellsChanged() {
        Level level = this.level;
        if (level == null) {
            return false;
        }
        for (UnitCell cell : unitCells) {
            if (level.getBlockState(cell.pos()).getBlock() != cell.block()) {
                return true;
            }
        }
        return false;
    }

    /** 按当前方块状态读取成型标记并刷新控制器状态。 */
    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(CrystalAssemblyLineBlock.FORMED));
    }

    /** 同步控制器的成型标记与运行/暂停贴图。 */
    private void applyControllerState(boolean formed) {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean showRunning = formed && jobTicksLeft > 0;
        boolean showPaused = formed && paused && jobTicksLeft <= 0;
        BlockState updated = state;
        if (state.getValue(CrystalAssemblyLineBlock.FORMED) != formed) {
            updated = updated.setValue(CrystalAssemblyLineBlock.FORMED, formed);
        }
        if (state.getValue(CrystalAssemblyLineBlock.RUNNING) != showRunning) {
            updated = updated.setValue(CrystalAssemblyLineBlock.RUNNING, showRunning);
        }
        if (state.getValue(CrystalAssemblyLineBlock.PAUSED) != showPaused) {
            updated = updated.setValue(CrystalAssemblyLineBlock.PAUSED, showPaused);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    /** 控制器被移除/破坏时调用：复位结构中的控制外壳与部件外观。 */
    public void onControllerRemoved() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        structure.invalidate();
        AssemblyLineStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(CrystalAssemblyLineBlock.FACING), StructureResult.EMPTY, false);
    }

    // ---------------------------------------------------------------- 持久化（加工中的作业）

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        upgrades.writeToNBT(tag, "upgrades");
        if (currentMatch != null && jobTicksLeft > 0) {
            tag.putString("jobRecipe", currentMatch.recipe().getId().toString());
            tag.putInt("jobParallel", currentMatch.parallel());
            tag.putInt("jobTicks", jobTicksLeft);
            tag.putInt("jobTotalTicks", jobTotalTicks);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        upgrades.readFromNBT(tag, "upgrades");
        if (!tag.contains("jobRecipe")) {
            return;
        }
        // world 在 load 时可能尚未就绪，先记录待恢复数据，首个服务端 tick 再解析配方
        pendingRecipeId = ResourceLocation.tryParse(tag.getString("jobRecipe"));
        pendingParallel = tag.getInt("jobParallel");
        pendingTicks = tag.getInt("jobTicks");
        pendingTotalTicks = tag.getInt("jobTotalTicks");
    }

    /** 解析待恢复的作业（配方已删除或类型不符时直接放弃恢复）。 */
    private void restorePendingJob() {
        ResourceLocation recipeId = pendingRecipeId;
        if (recipeId == null) {
            return;
        }
        pendingRecipeId = null;
        Level level = this.level;
        if (level == null) {
            return;
        }
        var recipe = level.getRecipeManager().byKey(recipeId);
        if (recipe.orElse(null) instanceof CrystalAssemblyLineRecipe lineRecipe && pendingTicks > 0) {
            // 恢复加工进度；输入下标只在启动时使用，恢复后无需重算
            jobTicksLeft = pendingTicks;
            // 旧存档缺少实际总耗时字段：按配方时长兼容
            jobTotalTicks = pendingTotalTicks > 0 ? pendingTotalTicks : Math.max(1, lineRecipe.getDuration());
            currentMatch = new Match(lineRecipe, pendingParallel, List.of(), List.of());
        }
    }

    // ---------------------------------------------------------------- 状态查询

    /** 是否正在加工。 */
    public boolean isRunning() {
        return jobTicksLeft > 0 && isFormed();
    }

    /** 是否因电力/输出不足暂停。 */
    public boolean isPaused() {
        return paused && isFormed();
    }

    /** 暂停原因（NONE 表示未暂停）。 */
    public Error getError() {
        return error;
    }

    /** 是否已成型。 */
    public boolean isFormed() {
        return getBlockState().getValue(CrystalAssemblyLineBlock.FORMED);
    }

    /** 当前作业的展示产物（数量已乘并行数）；空闲返回空。 */
    public ItemStack getJobOutput() {
        if (currentMatch == null || jobTicksLeft <= 0) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> outputs = currentMatch.recipe().getItemOutputs();
        if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack main = outputs.get(0);
        long total = (long) main.getCount() * currentMatch.parallel();
        return main.copyWithCount((int) Math.min(total, Integer.MAX_VALUE));
    }

    /** 当前作业的实际总耗时（tick，含速度模块加成）；空闲为 0。 */
    public int getJobDuration() {
        return jobTotalTicks;
    }

    /** 当前作业已进行的时间（tick）；空闲为 0。 */
    public int getJobElapsed() {
        return currentMatch != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    /** 客户端展示的当前作业产物（仅由同步包写入）。 */
    public ItemStack getClientJobOutput() {
        return clientJobOutput;
    }

    /** 客户端同步入口：覆盖本地作业产物（仅由同步包调用）。 */
    public void applyClientJobOutput(ItemStack stack) {
        this.clientJobOutput = stack;
    }

    /** 结构内是否有能源仓接入 ME 网络。 */
    public boolean isEnergyConnected() {
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (hatch.isGridConnected()) {
                return true;
            }
        }
        return false;
    }

    /** 结构内能源仓所接 ME 网络的可用能量合计（AE；按网络去重）。 */
    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }

    /** 最近一次检测到的片数（未成型为 0）。 */
    public int getLastSlices() {
        return lastSlices;
    }

    /** 最近一次检测的不符方块数量。 */
    public int getLastMismatches() {
        return lastMismatches;
    }

    /** 最近一次检测的首个不符位置（可能为空）。 */
    @Nullable
    public BlockPos getLastMismatchPos() {
        return lastMismatchPos;
    }

    /** 首个不符位置期望的字符（可能为空字符）。 */
    public char getLastExpected() {
        return lastExpected;
    }

    /** 首个不符位置的实际方块（可能为空）。 */
    @Nullable
    public Block getLastFound() {
        return lastFound;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测（不持久化），交给首次 tick 完成，避免加载期修改世界
            tickCounter = CHECK_INTERVAL - 1;
        }
    }
}
