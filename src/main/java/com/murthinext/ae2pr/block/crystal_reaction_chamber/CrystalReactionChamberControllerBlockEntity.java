package com.murthinext.ae2pr.block.crystal_reaction_chamber;

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
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemHandlerHelper;

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
import com.murthinext.ae2pr.multiblock.structure.CrystalReactionChamberStructure;
import com.murthinext.ae2pr.recipe.CrystalReactionChamberRecipe;

/**
 * 水晶反应仓主机方块实体。
 */
public class CrystalReactionChamberControllerBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 20;

    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String TAG_UPGRADES = "upgrades";

    /**
     * 暂停原因。
     */
    public enum Error {
        NONE, POWER, OUTPUT
    }

    private int tickCounter;
    private int retryCounter;

    private int jobTicksLeft;
    private int jobTotalTicks;
    private int jobParallel;
    private boolean paused;
    private Error error = Error.NONE;
    @Nullable
    private CrystalReactionChamberRecipe currentRecipe;

    private ItemStack clientJobItem = ItemStack.EMPTY;
    private FluidStack clientJobFluid = FluidStack.EMPTY;

    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    private final List<ItemBusBlockEntity> outputBuses = new ArrayList<>();
    private final List<FluidHatchBlockEntity> inputHatches = new ArrayList<>();
    private final List<FluidHatchBlockEntity> outputHatches = new ArrayList<>();
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    private final StructureValidator structure = new StructureValidator(CrystalReactionChamberStructure.PATTERN);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.CRYSTAL_REACTION_CHAMBER.get(), 4, this::onUpgradesChanged);

    public CrystalReactionChamberControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_REACTION_CHAMBER.get(), pos, state);
    }

    /**
     * 服务端 tick：结构检测与加工作业。
     */
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

    // 加工作业

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

    private void tryStartJob() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        Match match = findMatch();
        if (match == null) {
            clearPause();
            return;
        }
        if (!fitsOutputs(match.recipe(), match.parallel())) {
            pause(Error.OUTPUT);
            return;
        }
        // 能量按并行数与耗能倍率从能源仓网络一次性扣除
        double cost = match.recipe().getEnergyPerParallel() * match.parallel() * lossyEnergyMultiplier();
        if (extractNetworkEnergy(cost, true) < cost) {
            pause(Error.POWER);
            return;
        }
        // 扣电与扣料
        extractNetworkEnergy(cost, false);
        consumeItems(match.recipe(), match.parallel());
        consumeFluids(match.recipe(), match.parallel());

        jobTotalTicks = scaledDuration(match.recipe().getDuration());
        jobTicksLeft = jobTotalTicks;
        jobParallel = match.parallel();
        currentRecipe = match.recipe();
        paused = false;
        error = Error.NONE;
        setChanged();
        applyControllerState();
    }

    private void finishJob() {
        CrystalReactionChamberRecipe recipe = currentRecipe;
        int parallel = jobParallel;
        currentRecipe = null;
        jobParallel = 0;
        if (recipe != null && level != null) {
            insertItems(recipe, parallel);
            insertFluids(recipe, parallel);
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

    private record Match(CrystalReactionChamberRecipe recipe, int parallel) {
    }

    @Nullable
    private Match findMatch() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        for (CrystalReactionChamberRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CRYSTAL_REACTION_CHAMBER_TYPE.get())) {
            int parallel = parallelFor(recipe);
            if (parallel > 0) {
                return new Match(recipe, parallel);
            }
        }
        return null;
    }

    /**
     * 输入份数、并行上限与产物容量中的较小值。
     */
    private int parallelFor(CrystalReactionChamberRecipe recipe) {
        int parallel = maxParallel();
        for (CrystalReactionChamberRecipe.ItemInput input : recipe.getItemInputs()) {
            parallel = Math.min(parallel, available(input) / input.count());
        }
        for (FluidStack input : recipe.getFluidInputs()) {
            parallel = Math.min(parallel, availableFluid(input) / input.getAmount());
        }
        if (parallel <= 0) {
            return 0;
        }
        return outputParallel(recipe, parallel);
    }

    private int available(CrystalReactionChamberRecipe.ItemInput input) {
        int total = 0;
        for (ItemBusBlockEntity bus : inputBuses) {
            for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                ItemStack stack = bus.getStorage().getStackInSlot(slot);
                if (!stack.isEmpty() && input.ingredient().test(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    private int availableFluid(FluidStack input) {
        long total = 0;
        for (FluidHatchBlockEntity hatch : inputHatches) {
            for (int tank = 0; tank < hatch.getTankCount(); tank++) {
                FluidStack stored = hatch.getTank(tank).getFluid();
                if (stored.isFluidEqual(input)) {
                    total += stored.getAmount();
                }
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /**
     * 产物完全放得下的最大并行数。
     */
    private int outputParallel(CrystalReactionChamberRecipe recipe, int maximum) {
        int low = 0;
        int high = maximum;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (fitsOutputs(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /**
     * 输出总线槽位的虚拟快照，用于容量计算。
     */
    private static final class OutputSlot {

        private ItemStack stack;
        private final int limit;

        private OutputSlot(ItemStack stack, int limit) {
            this.stack = stack;
            this.limit = limit;
        }
    }

    private boolean fitsOutputs(CrystalReactionChamberRecipe recipe, int parallel) {
        List<OutputSlot> slots = new ArrayList<>();
        for (ItemBusBlockEntity bus : outputBuses) {
            for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                slots.add(new OutputSlot(bus.getStorage().getStackInSlot(slot).copy(), bus.getCapacityPerSlot()));
            }
        }
        for (ItemStack output : recipe.getItemOutputs()) {
            long remaining = (long) output.getCount() * parallel;
            remaining = distributeItems(slots, output, remaining, true);
            remaining = distributeItems(slots, output, remaining, false);
            if (remaining > 0) {
                return false;
            }
        }
        for (FluidStack output : recipe.getFluidOutputs()) {
            if (!fitsFluid(output, parallel)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 先在同类物品的槽位中堆叠，再占用空槽位。
     */
    private static long distributeItems(List<OutputSlot> slots, ItemStack output, long remaining, boolean sameItem) {
        for (OutputSlot slot : slots) {
            if (remaining <= 0) {
                break;
            }
            boolean match = sameItem
                    ? ItemStack.isSameItemSameTags(slot.stack, output)
                    : slot.stack.isEmpty();
            if (!match) {
                continue;
            }
            int free = slot.limit - slot.stack.getCount();
            if (free <= 0) {
                continue;
            }
            int moved = (int) Math.min(free, remaining);
            if (slot.stack.isEmpty()) {
                slot.stack = output.copyWithCount(moved);
            } else {
                slot.stack.grow(moved);
            }
            remaining -= moved;
        }
        return remaining;
    }

    private boolean fitsFluid(FluidStack output, int parallel) {
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

    // 扣料与产出

    private void consumeItems(CrystalReactionChamberRecipe recipe, int parallel) {
        for (CrystalReactionChamberRecipe.ItemInput input : recipe.getItemInputs()) {
            long remaining = (long) input.count() * parallel;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount() && remaining > 0; slot++) {
                    // 单次抽取上限为物品堆叠上限，按需求量分批抽取
                    while (remaining > 0) {
                        ItemStack current = bus.getStorage().getStackInSlot(slot);
                        if (current.isEmpty() || !input.ingredient().test(current)) {
                            break;
                        }
                        ItemStack taken = bus.getStorage().extractItem(slot,
                                (int) Math.min(remaining, Integer.MAX_VALUE), false);
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

    private void consumeFluids(CrystalReactionChamberRecipe recipe, int parallel) {
        for (FluidStack input : recipe.getFluidInputs()) {
            long remaining = (long) input.getAmount() * parallel;
            for (FluidHatchBlockEntity hatch : inputHatches) {
                for (int tank = 0; tank < hatch.getTankCount() && remaining > 0; tank++) {
                    FluidStack stored = hatch.getTank(tank).getFluid();
                    if (!stored.isFluidEqual(input)) {
                        continue;
                    }
                    FluidStack drained = hatch.getTank(tank).drain((int) Math.min(remaining, Integer.MAX_VALUE),
                            IFluidHandler.FluidAction.EXECUTE);
                    remaining -= drained.getAmount();
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    private void insertItems(CrystalReactionChamberRecipe recipe, int parallel) {
        Level level = this.level;
        if (level == null) {
            return;
        }
        for (ItemStack output : recipe.getItemOutputs()) {
            long total = (long) output.getCount() * parallel;
            ItemStack remaining = output.copyWithCount((int) Math.min(total, Integer.MAX_VALUE));
            for (ItemBusBlockEntity bus : outputBuses) {
                remaining = ItemHandlerHelper.insertItemStacked(bus.getStorage(), remaining, false);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                // 启动前已检查，兜底记录避免产物丢失
                ae2pr.LOGGER.warn("水晶反应仓产物无法放入输出总线，已掉落 {}", remaining);
                BlockPos pos = outputBuses.isEmpty() ? worldPosition : outputBuses.get(0).getBlockPos();
                Block.popResource(level, pos, remaining);
            }
        }
    }

    private void insertFluids(CrystalReactionChamberRecipe recipe, int parallel) {
        for (FluidStack output : recipe.getFluidOutputs()) {
            long total = (long) output.getAmount() * parallel;
            FluidStack remaining = output.copy();
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
                // 启动前已检查，兜底记录避免产物丢失
                ae2pr.LOGGER.warn("水晶反应仓产物无法存入输出仓，已丢弃 {}", remaining);
            }
        }
    }

    // 加速卡与电力

    public int maxParallel() {
        return Math.max(1, Config.crystalReactionParallel());
    }

    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    private int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
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

    // 结构检测

    public void validateStructure() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.CRYSTAL_REACTION_CHAMBER.get())) {
            return;
        }
        Direction facing = state.getValue(CrystalReactionChamberBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                CrystalReactionChamberStructure.PATTERN.minSlices());
        if (result.fromCache()) {
            // 缓存命中：结构未变化，无需刷新外观与部件引用
            return;
        }
        CrystalReactionChamberStructure.updateFormed(level, worldPosition, facing, result);
        refreshParts(level, result);
        applyControllerState(result.formed());
    }

    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        outputBuses.clear();
        inputHatches.clear();
        outputHatches.clear();
        energyHatches.clear();
        if (!result.formed()) {
            return;
        }
        // 按 片 → 行 → 列 排序，保证部件顺序稳定
        List<BlockPos> cells = new ArrayList<>(result.cells());
        cells.sort(CrystalReactionChamberStructure.PATTERN.posComparator(
                getBlockState().getValue(CrystalReactionChamberBlock.FACING), Direction.UP,
                result.mirrorSide(), result.mirrorFront()));
        for (BlockPos pos : cells) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBusBlockEntity bus) {
                if (bus.isOutputBus()) {
                    outputBuses.add(bus);
                } else {
                    inputBuses.add(bus);
                }
            } else if (blockEntity instanceof FluidHatchBlockEntity hatch) {
                if (hatch.isOutputHatch()) {
                    outputHatches.add(hatch);
                } else {
                    inputHatches.add(hatch);
                }
            } else if (blockEntity instanceof FluixCrystalEnergyHatchBlockEntity energyHatch) {
                energyHatches.add(energyHatch);
            }
        }
    }

    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(CrystalReactionChamberBlock.FORMED));
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
        if (state.getValue(CrystalReactionChamberBlock.FORMED) != formed) {
            updated = updated.setValue(CrystalReactionChamberBlock.FORMED, formed);
        }
        if (state.getValue(CrystalReactionChamberBlock.RUNNING) != showRunning) {
            updated = updated.setValue(CrystalReactionChamberBlock.RUNNING, showRunning);
        }
        if (state.getValue(CrystalReactionChamberBlock.PAUSED) != showPaused) {
            updated = updated.setValue(CrystalReactionChamberBlock.PAUSED, showPaused);
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
        CrystalReactionChamberStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(CrystalReactionChamberBlock.FACING), StructureResult.EMPTY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测
            tickCounter = CHECK_INTERVAL - 1;
        }
    }

    // 持久化

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
        if (recipe.orElse(null) instanceof CrystalReactionChamberRecipe chamberRecipe) {
            // 恢复加工进度
            jobTicksLeft = tag.getInt(JOB_TICKS_ID);
            jobTotalTicks = tag.contains(JOB_TOTAL_TICKS_ID)
                    ? tag.getInt(JOB_TOTAL_TICKS_ID)
                    : Math.max(1, chamberRecipe.getDuration());
            jobParallel = Math.max(1, tag.getInt(JOB_PARALLEL_ID));
            currentRecipe = chamberRecipe;
        }
    }

    // 状态查询

    public boolean isFormed() {
        return getBlockState().getValue(CrystalReactionChamberBlock.FORMED);
    }

    public boolean isRunning() {
        return isFormed() && jobTicksLeft > 0;
    }

    public boolean isPaused() {
        return isFormed() && paused && jobTicksLeft <= 0;
    }

    public Error getError() {
        return error;
    }

    public ItemStack getJobItem() {
        if (currentRecipe == null || jobTicksLeft <= 0 || currentRecipe.getItemOutputs().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack output = currentRecipe.getItemOutputs().get(0);
        long total = (long) output.getCount() * jobParallel;
        return output.copyWithCount((int) Math.min(total, Integer.MAX_VALUE));
    }

    public FluidStack getJobFluid() {
        if (currentRecipe == null || jobTicksLeft <= 0 || currentRecipe.getFluidOutputs().isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack output = currentRecipe.getFluidOutputs().get(0).copy();
        long total = (long) output.getAmount() * jobParallel;
        output.setAmount((int) Math.min(total, Integer.MAX_VALUE));
        return output;
    }

    public int getJobDuration() {
        return currentRecipe != null && jobTicksLeft > 0 ? jobTotalTicks : 0;
    }

    public int getJobElapsed() {
        return currentRecipe != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    public ItemStack getClientJobItem() {
        return clientJobItem;
    }

    public FluidStack getClientJobFluid() {
        return clientJobFluid;
    }

    /**
     * 仅由同步包调用。
     */
    public void applyClientJob(ItemStack item, FluidStack fluid) {
        this.clientJobItem = item.isEmpty() ? ItemStack.EMPTY : item.copy();
        this.clientJobFluid = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }
}
