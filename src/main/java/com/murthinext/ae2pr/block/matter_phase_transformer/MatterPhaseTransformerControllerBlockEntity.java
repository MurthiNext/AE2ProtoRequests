package com.murthinext.ae2pr.block.matter_phase_transformer;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.multiblock.structure.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MatterPhaseRecipe;

/**
 * 物质相变仪主机方块实体：结构检测 + 物质相变配方执行。
 */
public class MatterPhaseTransformerControllerBlockEntity extends BlockEntity {

    /** 结构检测周期（tick） */
    private static final int CHECK_INTERVAL = 20;
    /** 暂停后的重试间隔（tick） */
    private static final int RETRY_INTERVAL = 20;

    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String TAG_UPGRADES = "upgrades";

    /** 暂停原因（供界面与 Jade 报错） */
    public enum Error {
        NONE, POWER, OUTPUT
    }

    private int tickCounter;
    private int retryCounter;

    /** 加工剩余 tick（<= 0 表示空闲或暂停） */
    private int jobTicksLeft;
    /** 当前作业的实际总耗时（tick，含加速卡加成） */
    private int jobTotalTicks;
    /** 当前作业的并行数 */
    private int jobParallel;
    /** 是否处于暂停态（电力/输出不足，黄色暂停贴图） */
    private boolean paused;
    private Error error = Error.NONE;
    /** 当前作业（加工中保留，用于结算产物） */
    @Nullable
    private MatterPhaseRecipe currentRecipe;

    /** 客户端展示用：当前作业产出的流体（仅由同步包写入） */
    private FluidStack clientJobFluid = FluidStack.EMPTY;

    /** 结构内的输入总线（按 片 → 行 → 列 排序） */
    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    /** 结构内的输出仓（流体产物出口） */
    private final List<FluidHatchBlockEntity> outputHatches = new ArrayList<>();
    /** 结构内的能源仓 */
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    /** 结构验证器：模式匹配 + 缓存快速路径 */
    private final StructureValidator structure = new StructureValidator(MatterPhaseTransformerStructure.PATTERN);
    /** 升级槽 */
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.MATTER_PHASE_TRANSFORMER.get(), 4, this::onUpgradesChanged);

    public MatterPhaseTransformerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PHASE_TRANSFORMER.get(), pos, state);
    }

    /** 服务端 tick：结构检测与加工作业。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
        tickJob();
    }

    // ---------------------------------------------------------------- 加工作业

    private void tickJob() {
        if (!isFormed()) {
            if (jobTicksLeft > 0 || paused || error != Error.NONE || currentRecipe != null) {
                jobTicksLeft = 0;
                jobTotalTicks = 0;
                jobParallel = 0;
                currentRecipe = null;
                clearPause();
            }
            return;
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

    /** 尝试开始一次相变：匹配配方 → 输出检查 → 扣电 → 扣料 → 进入运行态。 */
    private void tryStartJob() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        MatterPhaseRecipe recipe = findMatch();
        if (recipe == null) {
            clearPause();
            return;
        }
        // 实际并行数 = 输入份数与输出仓容量的较小值
        int parallel = parallelFor(recipe);
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        // 能量按并行数与加速卡耗能倍率从结构内能源仓所接 ME 网络一次性扣除
        double cost = recipe.getEnergyPerParallel() * parallel * lossyEnergyMultiplier();
        if (extractNetworkEnergy(cost, true) < cost) {
            pause(Error.POWER);
            return;
        }
        // 扣电与扣料
        extractNetworkEnergy(cost, false);
        consumeInputs(recipe, parallel);

        jobTotalTicks = scaledDuration(recipe.getDuration());
        jobTicksLeft = jobTotalTicks;
        jobParallel = parallel;
        currentRecipe = recipe;
        paused = false;
        error = Error.NONE;
        setChanged();
        applyControllerState();
    }

    /** 加工完成：产物注入输出仓，并立即触发下一次配方检查以消除连续加工之间的空档。 */
    private void finishJob() {
        MatterPhaseRecipe recipe = currentRecipe;
        int parallel = jobParallel;
        currentRecipe = null;
        jobParallel = 0;
        if (recipe != null && level != null) {
            insertFluid(recipe, parallel);
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

    /** 找到首个输入总线可满足的配方。 */
    @Nullable
    private MatterPhaseRecipe findMatch() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        for (MatterPhaseRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MATTER_PHASE_TYPE.get())) {
            if (hasIngredients(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    /** 输入总线内是否有满足配方的原料（统计所有槽位，只校验种类与总数量）。 */
    private boolean hasIngredients(MatterPhaseRecipe recipe) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            if (available(ingredient) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    /** 输入总线内某种原料的总数量。 */
    private int available(CountedIngredient ingredient) {
        int total = 0;
        for (ItemBusBlockEntity bus : inputBuses) {
            for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                var stack = bus.getStorage().getStackInSlot(slot);
                if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    private void consumeInputs(MatterPhaseRecipe recipe, int parallel) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int remaining = ingredient.count() * parallel;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount() && remaining > 0; slot++) {
                    // 单次抽取最多只能取走物品的堆叠上限（64），按需求量分批抽取
                    while (remaining > 0) {
                        var current = bus.getStorage().getStackInSlot(slot);
                        if (current.isEmpty() || !ingredient.ingredient().test(current)) {
                            break;
                        }
                        var taken = bus.getStorage().extractItem(slot, remaining, false);
                        if (taken.isEmpty()) {
                            break;
                        }
                        remaining -= taken.getCount();
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * 实际并行数：输入份数与输出仓容量的较小值，上限为配置的最大并行数；
     * 每种原料按总数量除以单份需求计算可并行份数。
     */
    private int parallelFor(MatterPhaseRecipe recipe) {
        int parallel = maxParallel();
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            parallel = Math.min(parallel, available(ingredient) / ingredient.count());
        }
        return outputParallel(recipe, parallel);
    }

    /** 在输入允许的并行范围内，二分求出全部产物流体都能存入输出仓的最大并行数。 */
    private int outputParallel(MatterPhaseRecipe recipe, int maximum) {
        int low = 0;
        int high = maximum;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (canFitFluid(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /** 输出仓的剩余容量是否装得下指定并行数的产物流体。 */
    private boolean canFitFluid(MatterPhaseRecipe recipe, int parallel) {
        FluidStack output = recipe.getFluidOutput();
        long remaining = (long) output.getAmount() * parallel;
        for (FluidHatchBlockEntity hatch : outputHatches) {
            for (int tank = 0; tank < hatch.getTankCount() && remaining > 0; tank++) {
                FluidTank target = hatch.getTank(tank);
                FluidStack stored = target.getFluid();
                if (stored.isEmpty()) {
                    remaining -= target.getCapacity();
                } else if (output.isFluidEqual(stored)) {
                    remaining -= Math.max(0, target.getCapacity() - stored.getAmount());
                }
            }
        }
        return remaining <= 0;
    }

    private void insertFluid(MatterPhaseRecipe recipe, int parallel) {
        FluidStack remaining = recipe.getFluidOutput();
        long total = (long) remaining.getAmount() * parallel;
        remaining.setAmount((int) Math.min(total, Integer.MAX_VALUE));
        for (FluidHatchBlockEntity hatch : outputHatches) {
            for (int tank = 0; tank < hatch.getTankCount() && !remaining.isEmpty(); tank++) {
                int filled = hatch.getTank(tank).fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    remaining.shrink(filled);
                }
            }
        }
        if (!remaining.isEmpty()) {
            // 启动前已检查；兜底记录，避免产物静默丢失
            ae2pr.LOGGER.warn("物质相变仪产物无法存入输出仓，已丢弃 {}", remaining);
        }
    }

    // ---------------------------------------------------------------- 加速卡与电力

    /** 结构允许的最大并行数 */
    public int maxParallel() {
        return Math.max(1, Config.matterPhaseParallel());
    }

    /** 已安装的加速卡数量 */
    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    /** 加速卡的速度倍率 */
    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    /** 加速卡的耗能倍率 */
    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    /** 按速度倍率折算实际耗时 */
    private int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    /** 升级槽 */
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 升级变化回调 */
    private void onUpgradesChanged() {
        setChanged();
    }

    /** 从结构内能源仓一次性抽取能量 */
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

    /** 结构内是否有能源仓接入 ME 网络 */
    public boolean isEnergyConnected() {
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (hatch.isGridConnected()) {
                return true;
            }
        }
        return false;
    }

    /** 结构内能源仓所接 ME 网络的可用能量合计 */
    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }

    // ---------------------------------------------------------------- 结构检测

    /** 立即检测一次结构，并按结果切换主机与结构内部件的状态；结构未变化时走缓存快速路径。 */
    public void validateStructure() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.MATTER_PHASE_TRANSFORMER.get())) {
            return;
        }
        Direction facing = state.getValue(MatterPhaseTransformerBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                MatterPhaseTransformerStructure.PATTERN.minSlices());
        if (result.fromCache()) {
            // 缓存命中：结构未变化，无需刷新外观与部件引用
            return;
        }
        MatterPhaseTransformerStructure.updateFormed(level, worldPosition, facing, result);
        refreshParts(level, result);
        applyControllerState(result.formed());    }

    /** 收集结构内的输入总线、输出仓与能源仓（仅在结构成型时收集）。 */
    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        outputHatches.clear();
        energyHatches.clear();
        if (!result.formed()) {
            return;
        }
        // 显式按 片 → 行 → 列 排序，保证部件顺序稳定
        List<BlockPos> cells = new ArrayList<>(result.cells());
        cells.sort(MatterPhaseTransformerStructure.PATTERN.posComparator(
                getBlockState().getValue(MatterPhaseTransformerBlock.FACING), Direction.UP,
                result.mirrorSide(), result.mirrorFront()));
        for (BlockPos pos : cells) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBusBlockEntity bus) {
                if (!bus.isOutputBus()) {
                    inputBuses.add(bus);
                }
            } else if (blockEntity instanceof FluidHatchBlockEntity hatch) {
                if (hatch.isOutputHatch()) {
                    outputHatches.add(hatch);
                }
            } else if (blockEntity instanceof FluixCrystalEnergyHatchBlockEntity energyHatch) {
                energyHatches.add(energyHatch);
            }
        }
    }

    /** 按当前方块状态读取成型标记并刷新主机状态。 */
    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(MatterPhaseTransformerBlock.FORMED));
    }

    /** 同步成型标记与运行/暂停贴图。 */
    private void applyControllerState(boolean formed) {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean showRunning = formed && jobTicksLeft > 0;
        boolean showPaused = formed && paused && jobTicksLeft <= 0;
        BlockState updated = state;
        if (state.getValue(MatterPhaseTransformerBlock.FORMED) != formed) {
            updated = updated.setValue(MatterPhaseTransformerBlock.FORMED, formed);
        }
        if (state.getValue(MatterPhaseTransformerBlock.RUNNING) != showRunning) {
            updated = updated.setValue(MatterPhaseTransformerBlock.RUNNING, showRunning);
        }
        if (state.getValue(MatterPhaseTransformerBlock.PAUSED) != showPaused) {
            updated = updated.setValue(MatterPhaseTransformerBlock.PAUSED, showPaused);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    /** 控制器被移除 / 破坏时调用：复位结构中的部件外观并清空结构缓存。 */
    public void onControllerRemoved() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        structure.invalidate();
        MatterPhaseTransformerStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(MatterPhaseTransformerBlock.FACING), StructureResult.EMPTY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测（不持久化），交给首次 tick 完成，避免加载期修改世界
            tickCounter = CHECK_INTERVAL - 1;
        }
    }

    // ---------------------------------------------------------------- 持久化（升级 / 加工中的作业）

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        upgrades.writeToNBT(tag, TAG_UPGRADES);
        if (currentRecipe != null && jobTicksLeft > 0) {
            tag.putString(JOB_RECIPE_ID, currentRecipe.getId().toString());
            tag.putInt(JOB_TICKS_ID, jobTicksLeft);
            tag.putInt(JOB_TOTAL_TICKS_ID, jobTotalTicks);
            tag.putInt(JOB_PARALLEL_ID, jobParallel);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        Level level = this.level;
        if (level == null || level.isClientSide || !tag.contains(JOB_RECIPE_ID)) {
            return;
        }
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse(tag.getString(JOB_RECIPE_ID)));
        if (recipe.orElse(null) instanceof MatterPhaseRecipe matterPhaseRecipe) {
            // 恢复加工进度
            jobTicksLeft = tag.getInt(JOB_TICKS_ID);
            jobTotalTicks = tag.contains(JOB_TOTAL_TICKS_ID)
                    ? tag.getInt(JOB_TOTAL_TICKS_ID)
                    : Math.max(1, matterPhaseRecipe.getDuration());
            jobParallel = Math.max(1, tag.getInt(JOB_PARALLEL_ID));
            currentRecipe = matterPhaseRecipe;
        }
    }

    // ---------------------------------------------------------------- 状态查询

    /** 是否已成型。 */
    public boolean isFormed() {
        return getBlockState().getValue(MatterPhaseTransformerBlock.FORMED);
    }

    /** 是否正在加工。 */
    public boolean isRunning() {
        return isFormed() && getBlockState().getValue(MatterPhaseTransformerBlock.RUNNING);
    }

    /** 是否因电力/输出不足暂停。 */
    public boolean isPaused() {
        return isFormed() && getBlockState().getValue(MatterPhaseTransformerBlock.PAUSED);
    }

    /** 暂停原因（NONE 表示未暂停）。 */
    public Error getError() {
        return error;
    }

    /** 当前作业的展示产物（流体数量已乘并行数）；空闲返回空。 */
    public FluidStack getJobFluid() {
        if (currentRecipe == null || jobTicksLeft <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack output = currentRecipe.getFluidOutput();
        long total = (long) output.getAmount() * jobParallel;
        output.setAmount((int) Math.min(total, Integer.MAX_VALUE));
        return output;
    }

    /** 当前作业的实际总耗时（tick，含加速卡加成）；空闲为 0。 */
    public int getJobDuration() {
        return currentRecipe != null && jobTicksLeft > 0 ? jobTotalTicks : 0;
    }

    /** 当前作业已进行的时间（tick）；空闲为 0。 */
    public int getJobElapsed() {
        return currentRecipe != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    /** 客户端展示的当前作业产物（仅由同步包写入）。 */
    public FluidStack getClientJobFluid() {
        return clientJobFluid;
    }

    /** 客户端同步入口：覆盖本地作业产物（仅由同步包调用）。 */
    public void applyClientJobFluid(FluidStack stack) {
        this.clientJobFluid = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
    }
}
