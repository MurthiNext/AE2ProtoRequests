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
import com.murthinext.ae2pr.multiblock.structure.AssemblyLineStructure;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;

/**
 * 水晶装配线控制器方块实体。
 */
public class AssemblyLineControllerBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 20;

    public enum Error {
        NONE, POWER, OUTPUT
    }

    private int tickCounter;

    private int jobTicksLeft;
    private int jobTotalTicks;
    private boolean paused;
    private Error error = Error.NONE;
    private int retryCounter;
    @Nullable
    private Match currentMatch;

    @Nullable
    private ResourceLocation pendingRecipeId;
    private int pendingParallel;
    private int pendingTicks;
    private int pendingTotalTicks;

    private ItemStack clientJobOutput = ItemStack.EMPTY;

    private int lastSlices;
    private int lastMismatches;
    @Nullable
    private BlockPos lastMismatchPos;
    private char lastExpected = ' ';
    @Nullable
    private Block lastFound;

    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    @Nullable
    private ItemBusBlockEntity outputBus;
    private final List<FluidHatchBlockEntity> inputHatches = new ArrayList<>();
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    private ModuleSnapshot modules = ModuleSnapshot.EMPTY;
    private final List<UnitCell> unitCells = new ArrayList<>();
    private final StructureValidator structure = new StructureValidator(AssemblyLineStructure.PATTERN);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(ModItems.CRYSTAL_ASSEMBLY_LINE.get(), 4,
            this::onUpgradesChanged);

    public AssemblyLineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_ASSEMBLY_LINE.get(), pos, state);
    }

    /**
     * 服务端 tick：每 tick 推进加工作业，每 {@link #CHECK_INTERVAL} tick 重新检测结构。
     */
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

    // 加工作业

    private void tickJob() {
        if (!isFormed()) {
            cancelJob();
            return;
        }
        // 检查控制外壳/模块是否被替换
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

    private void cancelJob() {
        if (jobTicksLeft > 0 || jobTotalTicks > 0 || currentMatch != null || paused || error != Error.NONE) {
            jobTicksLeft = 0;
            jobTotalTicks = 0;
            currentMatch = null;
            clearPause();
        }
    }

    private void tryStartJob() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        // 启动前再次验证结构与模块
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

    // 配方匹配
    private record Match(CrystalAssemblyLineRecipe recipe, int parallel, List<Integer> busIndices,
                         List<Integer> hatchIndices) {
    }

    private record UnitCell(BlockPos pos, Block block) {
    }

    public int maxParallel() {
        int base = Config.assemblyBaseParallel()
                + Math.max(0, lastSlices - AssemblyLineStructure.MIN_SLICES) * Config.assemblyParallelPerSlice();
        return Math.min(base + moduleParallelBonus(), Config.assemblyMaxParallel());
    }

    private int moduleParallelBonus() {
        return modules.count(ModModules.PARALLEL) * Config.assemblyModuleParallelPerUnit();
    }

    public double getSpeedMultiplier() {
        double multiplier = 1.0 + modules.count(ModModules.SPEED) * Config.assemblyModuleSpeedPerUnit();
        return Math.min(multiplier, Config.assemblyMaxSpeedMultiplier()) * speedCardMultiplier();
    }

    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public double speedCardMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
    }

    public int getModuleLevel(ModuleType type) {
        return modules.level(type);
    }

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
            int parallel = parallelFor(recipe, busIndices, hatchIndices);
            if (parallel <= 0) {
                continue;
            }
            return new Match(recipe, parallel, busIndices, hatchIndices);
        }
        return null;
    }

    /**
     * 物品输入匹配。
     */
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

    /**
     * 各行可并行份数的较小值。
     */
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

    /**
     * 流体输入匹配。
     */
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

    // 扣料与产出

    private void consumeInputs(Match match) {
        List<CrystalAssemblyLineRecipe.ItemInput> itemInputs = match.recipe().getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
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
                ae2pr.LOGGER.warn("水晶装配线产物无法放入输出总线，已掉落 {}", remainder);
                Block.popResource(level, outputBus.getBlockPos(), remainder);
            }
        }
    }

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

    // 结构检测

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
        if (jobTicksLeft > 0 && currentMatch != null
                && !snapshot.satisfies(currentMatch.recipe().getModuleRequirements())) {
            cancelJob();
        }
        modules = snapshot;

        boolean formed = result.formed();
        boolean active = formed && jobTicksLeft > 0;

        AssemblyLineStructure.updateFormed(level, worldPosition, facing, result, active);
        refreshParts(level, result);
        applyControllerState(formed);
    }

    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        inputHatches.clear();
        energyHatches.clear();
        unitCells.clear();
        outputBus = null;
        if (!result.formed()) {
            return;
        }
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

    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(CrystalAssemblyLineBlock.FORMED));
    }

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

    public void onControllerRemoved() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        structure.invalidate();
        AssemblyLineStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(CrystalAssemblyLineBlock.FACING), StructureResult.EMPTY, false);
    }

    // 持久化

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
        // 首个服务端 tick 再解析配方
        pendingRecipeId = ResourceLocation.tryParse(tag.getString("jobRecipe"));
        pendingParallel = tag.getInt("jobParallel");
        pendingTicks = tag.getInt("jobTicks");
        pendingTotalTicks = tag.getInt("jobTotalTicks");
    }

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
            jobTicksLeft = pendingTicks;
            jobTotalTicks = pendingTotalTicks > 0 ? pendingTotalTicks : Math.max(1, lineRecipe.getDuration());
            currentMatch = new Match(lineRecipe, pendingParallel, List.of(), List.of());
        }
    }

    // 状态查询

    public boolean isRunning() {
        return jobTicksLeft > 0 && isFormed();
    }

    public boolean isPaused() {
        return paused && isFormed();
    }

    public Error getError() {
        return error;
    }

    public boolean isFormed() {
        return getBlockState().getValue(CrystalAssemblyLineBlock.FORMED);
    }

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

    public int getJobDuration() {
        return jobTotalTicks;
    }

    public int getJobElapsed() {
        return currentMatch != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    public ItemStack getClientJobOutput() {
        return clientJobOutput;
    }

    public void applyClientJobOutput(ItemStack stack) {
        this.clientJobOutput = stack;
    }

    public boolean isEnergyConnected() {
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (hatch.isGridConnected()) {
                return true;
            }
        }
        return false;
    }

    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }

    public int getLastSlices() {
        return lastSlices;
    }

    public int getLastMismatches() {
        return lastMismatches;
    }

    @Nullable
    public BlockPos getLastMismatchPos() {
        return lastMismatchPos;
    }

    public char getLastExpected() {
        return lastExpected;
    }

    @Nullable
    public Block getLastFound() {
        return lastFound;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测
            tickCounter = CHECK_INTERVAL - 1;
        }
    }
}
