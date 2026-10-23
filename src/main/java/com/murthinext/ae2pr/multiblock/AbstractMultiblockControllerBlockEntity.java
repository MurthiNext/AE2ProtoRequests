package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.HighEnergyCoreBlock;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

/**
 * 多方块控制器方块实体的公共基底：结构检测、作业状态机、能量与升级卡。
 */
public abstract class AbstractMultiblockControllerBlockEntity extends BlockEntity {

    public enum Error {
        NONE, POWER, OUTPUT, DURABILITY
    }

    private static final int CHECK_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 20;
    private static final int UPGRADE_SLOTS = 4;

    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String MIRROR_SIDE_ID = "mirrorSide";
    private static final String MIRROR_FRONT_ID = "mirrorFront";
    private static final String FORMED_CELLS_ID = "formedCells";
    private static final String TAG_UPGRADES = "upgrades";

    protected final StructureValidator structure;
    private final AbstractStructureFacade structureFacade;
    private final IUpgradeInventory upgrades;

    protected final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    protected final List<ItemBusBlockEntity> outputBuses = new ArrayList<>();
    protected final List<FluidHatchBlockEntity> inputHatches = new ArrayList<>();
    protected final List<FluidHatchBlockEntity> outputHatches = new ArrayList<>();
    protected final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();

    protected boolean mirrorSide;
    protected boolean mirrorFront;

    // 上一次成型时覆盖的方块
    private List<BlockPos> formedCells = List.of();

    private int tickCounter;
    private int retryTicks;
    private boolean validating;

    protected int jobTicksLeft;
    protected int jobTotalTicks;
    protected int jobParallel;
    private boolean paused;
    private Error error = Error.NONE;
    @Nullable
    private ResourceLocation jobRecipeId;

    protected AbstractMultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            AbstractStructureFacade structureFacade, net.minecraft.world.level.ItemLike machine) {
        super(type, pos, state);
        this.structureFacade = structureFacade;
        this.structure = new StructureValidator(structureFacade.pattern());
        this.upgrades = UpgradeInventories.forMachine(machine, UPGRADE_SLOTS, this::setChanged);
    }

    /**
     * 机器标识，用于日志与界面翻译键。
     */
    public abstract String machineKey();

    protected abstract RecipeType<? extends MultiblockRecipe> recipeType();

    /**
     * 最大并行数。
     */
    public abstract int maxParallel();

    /**
     * 机器类型专属的配方匹配。
     */
    @Nullable
    protected abstract Match findMatch();

    /**
     * 启动前对并行数的额外上限，返回 0 时按 {@link #parallelShortageError()} 暂停。
     */
    protected int capParallel(MultiblockRecipe recipe, int parallel) {
        return parallel;
    }

    /**
     * 并行数被压到 0 时的暂停原因。
     */
    protected Error parallelShortageError() {
        return Error.OUTPUT;
    }

    /**
     * 作业启动时的额外消耗，例如配方耐久。
     */
    protected void onJobStarted(Match match) {
    }

    /**
     * 产物入库。
     */
    protected void outputProducts(MultiblockRecipe recipe, int parallel) {
        if (level == null) {
            return;
        }
        for (ItemStack output : recipe.getItemOutputs()) {
            ItemStack remaining = output.copyWithCount(clampCount((long) output.getCount() * parallel));
            for (ItemBusBlockEntity bus : outputBuses) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = ItemHandlerHelper.insertItemStacked(bus.getStorage(), remaining, false);
            }
            if (!remaining.isEmpty()) {
                // 启动前已检查，兜底掉落避免产物丢失
                ae2pr.LOGGER.warn("{} output could not be inserted into output buses, dropped {}", machineKey(),
                        remaining);
                Block.popResource(level, worldPosition, remaining);
            }
        }
        for (FluidStack output : recipe.getFluidOutputs()) {
            FluidStack remaining = output.copy();
            remaining.setAmount(clampCount((long) output.getAmount() * parallel));
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
                ae2pr.LOGGER.warn("{} output could not be stored in output hatches, discarded {}", machineKey(),
                        remaining);
            }
        }
    }

    private static int clampCount(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    /**
     * 服务端 tick：结构检测与作业推进。
     */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
        tickMachine();
        tickJob();
    }

    /**
     * 机器专属的每 tick 逻辑。
     */
    protected void tickMachine() {
    }

    // 作业状态机

    private void tickJob() {
        if (!isFormed()) {
            cancelJob();
            return;
        }
        if (jobTicksLeft > 0) {
            // 配方被移除或不再适用时立即中止
            if (currentRecipe() == null) {
                cancelJob();
                return;
            }
            if (--jobTicksLeft == 0) {
                finishJob();
            }
            return;
        }
        if (retryTicks > 0) {
            retryTicks--;
            return;
        }
        retryTicks = RETRY_INTERVAL;
        tryStartJob();
    }

    private void tryStartJob() {
        if (level == null) {
            return;
        }
        Match match = findMatch();
        if (match == null) {
            clearPause();
            return;
        }
        // 机器专属的并行上限，例如熔岩冶炼炉的配方耐久
        int capped = capParallel(match.recipe(), match.parallel());
        if (capped <= 0) {
            pause(parallelShortageError());
            return;
        }
        match = new Match(match.recipe(), capped);
        // 输入够但产物放不下时单独提示，避免与“没有可用配方”混淆
        int parallel = outputParallel(match.recipe(), match.parallel());
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        match = new Match(match.recipe(), parallel);
        // 能量按并行数与耗能倍率从能源仓网络一次性扣除
        double cost = energyCost(match);
        if (cost > 0) {
            if (extractNetworkEnergy(cost, true) < cost) {
                pause(Error.POWER);
                return;
            }
            extractNetworkEnergy(cost, false);
        }
        if (!consumeInputs(match)) {
            pause(Error.OUTPUT);
            return;
        }
        onJobStarted(match);

        jobRecipeId = match.recipe().getId();
        jobParallel = match.parallel();
        jobTotalTicks = scaledDuration(match.recipe().getDuration());
        jobTicksLeft = jobTotalTicks;
        paused = false;
        error = Error.NONE;
        // 作业内容通过方块实体数据同步到客户端
        markStateChanged();
        applyControllerState();
    }

    /**
     * 扣除本次作业的输入；默认按全部输入总线与输入仓的最大流分配。
     */
    protected boolean consumeInputs(Match match) {
        PartInventory inputs = new PartInventory(inputBuses, inputHatches);
        if (!inputs.consume(match.recipe(), match.parallel())) {
            return false;
        }
        inputs.commit();
        return true;
    }

    /**
     * 一次作业消耗的网络能量。
     */
    protected double energyCost(Match match) {
        return match.recipe().getEnergyPerParallel() * match.parallel() * lossyEnergyMultiplier();
    }

    private void finishJob() {
        MultiblockRecipe recipe = currentRecipe();
        int parallel = jobParallel;
        clearJobFields();
        if (recipe != null) {
            outputProducts(recipe, parallel);
        }
        // 先清空客户端的作业内容，再尝试开始下一个
        markStateChanged();
        applyControllerState();
        if (isFormed()) {
            tryStartJob();
        }
    }

    protected void cancelJob() {
        if (jobTicksLeft > 0 || jobTotalTicks > 0 || jobRecipeId != null || paused || error != Error.NONE) {
            clearJobFields();
            clearPause();
        }
    }

    private void clearJobFields() {
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        jobParallel = 0;
        jobRecipeId = null;
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

    protected void clearPauseState() {
        clearPause();
    }

    /**
     * 当前作业对应的配方。
     */
    @Nullable
    protected MultiblockRecipe currentRecipe() {
        if (level == null || jobRecipeId == null) {
            return null;
        }
        Recipe<?> recipe = level.getRecipeManager().byKey(jobRecipeId).orElse(null);
        return recipe instanceof MultiblockRecipe multiblock && isRecipeApplicable(multiblock) ? multiblock : null;
    }

    /**
     * 配方是否仍适用于当前机器状态。
     */
    protected boolean isRecipeApplicable(MultiblockRecipe recipe) {
        return true;
    }

    /**
     * 当前机器可用的配方，已按 {@link #compareRecipePriority} 从高到低排序。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<MultiblockRecipe> recipes() {
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        List<MultiblockRecipe> sorted = new ArrayList<>((List<MultiblockRecipe>) (List<?>) manager
                .getAllRecipesFor((RecipeType) recipeType()));
        // 原料种类多的配方更具体，优先尝试可避免泛用配方在满仓时抢料
        sorted.sort(AbstractMultiblockControllerBlockEntity::compareRecipePriority);
        return sorted;
    }

    /**
     * 配方竞争优先级：原料种类多者优先，其次单次物品总量、流体总量，最后按 ID 稳定排序。
     */
    private static int compareRecipePriority(MultiblockRecipe left, MultiblockRecipe right) {
        int result = Integer.compare(inputKinds(right), inputKinds(left));
        if (result != 0) {
            return result;
        }
        result = Long.compare(itemInputAmount(right), itemInputAmount(left));
        if (result != 0) {
            return result;
        }
        result = Long.compare(fluidInputAmount(right), fluidInputAmount(left));
        if (result != 0) {
            return result;
        }
        return left.getId().compareTo(right.getId());
    }

    private static int inputKinds(MultiblockRecipe recipe) {
        return recipe.getItemInputs().size() + recipe.getFluidInputs().size();
    }

    private static long itemInputAmount(MultiblockRecipe recipe) {
        long total = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            total += input.count();
        }
        return total;
    }

    private static long fluidInputAmount(MultiblockRecipe recipe) {
        long total = 0;
        for (FluidStack input : recipe.getFluidInputs()) {
            total += input.getAmount();
        }
        return total;
    }

    /**
     * 输入允许的执行份数，常用于 {@link #findMatch()}。
     */
    protected int inputParallel(MultiblockRecipe recipe, int maximum) {
        int low = 0;
        int high = outputCountBound(recipe, maximum);
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (new PartInventory(inputBuses, inputHatches).consume(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /**
     * 产物容量允许的执行份数。
     */
    protected int outputParallel(MultiblockRecipe recipe, int maximum) {
        int low = 0;
        int high = outputCountBound(recipe, maximum);
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (new PartInventory(outputBuses, outputHatches).insert(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    // 单次作业的产物数量不得超过 int 上限
    private static int outputCountBound(MultiblockRecipe recipe, int maximum) {
        int high = maximum;
        for (ItemStack output : recipe.getItemOutputs()) {
            high = Math.min(high, Integer.MAX_VALUE / Math.max(1, output.getCount()));
        }
        for (FluidStack output : recipe.getFluidOutputs()) {
            high = Math.min(high, Integer.MAX_VALUE / Math.max(1, output.getAmount()));
        }
        return Math.max(0, high);
    }

    /**
     * 配方匹配结果。
     */
    public record Match(MultiblockRecipe recipe, int parallel) {
    }

    // 加速卡与电力

    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    protected int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    protected double extractNetworkEnergy(double amount, boolean simulate) {
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

    protected int maxSlices() {
        return structureFacade.maxSlices();
    }

    protected void onStructureChanged(StructureResult result) {
    }

    public void validateStructure() {
        if (level == null || level.isClientSide || validating) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MultiblockControllerBlock block)) {
            return;
        }
        Direction facing = state.getValue(block.facingProperty());
        validating = true;
        try {
            validateStructure(facing);
        } finally {
            validating = false;
        }
    }

    private void validateStructure(Direction facing) {
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP, maxSlices());
        if (result.fromCache()) {
            // 结构未变化，但相邻控制器可能重置过部件外观，此处补回
            if (formedCells.isEmpty()) {
                formedCells = structureFacade.updateFormed(level, worldPosition, facing, result, formedCells,
                        jobTicksLeft > 0);
            } else {
                structureFacade.reapply(level, formedCells, jobTicksLeft > 0);
            }
            return;
        }
        onStructureChanged(result);
        boolean mirrorChanged = mirrorSide != result.mirrorSide() || mirrorFront != result.mirrorFront();
        if (result.formed()) {
            mirrorSide = result.mirrorSide();
            mirrorFront = result.mirrorFront();
        }
        boolean active = result.formed() && jobTicksLeft > 0;
        formedCells = structureFacade.updateFormed(level, worldPosition, facing, result, formedCells, active);
        refreshParts(result);
        applyControllerState(result.formed());
        // 镜像状态变化时通知客户端更新渲染基准
        if (mirrorChanged) {
            markStateChanged();
        }
    }

    private void refreshParts(StructureResult result) {
        inputBuses.clear();
        outputBuses.clear();
        inputHatches.clear();
        outputHatches.clear();
        energyHatches.clear();
        onPartsCleared();
        if (!result.formed() || level == null) {
            return;
        }
        // 按 片 → 行 → 列 排序，保证部件顺序稳定
        List<BlockPos> cells = new ArrayList<>(result.cells());
        cells.sort(structureFacade.pattern().posComparator(getFacing(), Direction.UP,
                result.mirrorSide(), result.mirrorFront()));
        for (BlockPos pos : cells) {
            BlockEntity part = level.getBlockEntity(pos);
            if (part instanceof ItemBusBlockEntity bus) {
                (bus.isOutputBus() ? outputBuses : inputBuses).add(bus);
            } else if (part instanceof FluidHatchBlockEntity hatch) {
                (hatch.isOutputHatch() ? outputHatches : inputHatches).add(hatch);
            } else if (part instanceof FluixCrystalEnergyHatchBlockEntity hatch) {
                energyHatches.add(hatch);
            }
            collectExtraPart(pos, part);
        }
    }

    protected void collectExtraPart(BlockPos pos, @Nullable BlockEntity blockEntity) {
    }

    protected void onPartsCleared() {
    }

    protected Direction getFacing() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MultiblockControllerBlock block) {
            return state.getValue(block.facingProperty());
        }
        return Direction.NORTH;
    }

    /**
     * 核心方块位置，没有核心的机器返回 null。
     */
    @Nullable
    public BlockPos getCorePos() {
        return null;
    }

    protected BlockPos corePos() {
        return getCorePos();
    }

    protected void applyControllerState() {
        applyControllerState(isFormed());
    }

    protected void applyControllerState(boolean formed) {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MultiblockControllerBlock block)) {
            return;
        }
        boolean showRunning = formed && jobTicksLeft > 0;
        boolean showPaused = formed && paused;
        BlockState updated = state;
        if (state.getValue(block.formedProperty()) != formed) {
            updated = updated.setValue(block.formedProperty(), formed);
        }
        if (state.getValue(block.runningProperty()) != showRunning) {
            updated = updated.setValue(block.runningProperty(), showRunning);
        }
        if (state.getValue(block.pausedProperty()) != showPaused) {
            updated = updated.setValue(block.pausedProperty(), showPaused);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
        updateCoreLight(showRunning);
        onControllerStateChanged(formed, showRunning);
    }

    /**
     * 机器专属的方块状态联动。
     */
    protected void onControllerStateChanged(boolean formed, boolean running) {
    }

    protected void updateCoreLight(boolean lit) {
        BlockPos core = corePos();
        if (level == null || core == null) {
            return;
        }
        BlockState state = level.getBlockState(core);
        if (state.is(ModBlocks.HIGH_ENERGY_CORE.get()) && state.getValue(HighEnergyCoreBlock.LIT) != lit) {
            level.setBlock(core, state.setValue(HighEnergyCoreBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void onControllerRemoved() {
        if (level == null || level.isClientSide) {
            return;
        }
        structure.invalidate();
        updateCoreLight(false);
        structureFacade.reset(level, formedCells);
        formedCells = List.of();
        for (ItemStack upgrade : upgrades) {
            Block.popResource(level, worldPosition, upgrade);
        }
        onControllerRemovedHook();
    }

    protected void onControllerRemovedHook() {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测
            tickCounter = CHECK_INTERVAL - 1;
        }
    }

    // 客户端同步

    protected void markStateChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        // 部件位置仅服务端撤销外观时使用，不同步到客户端
        tag.remove(FORMED_CELLS_ID);
        tag.putBoolean(MIRROR_SIDE_ID, mirrorSide);
        tag.putBoolean(MIRROR_FRONT_ID, mirrorFront);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    // 持久化

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(MIRROR_SIDE_ID, mirrorSide);
        tag.putBoolean(MIRROR_FRONT_ID, mirrorFront);
        tag.putLongArray(FORMED_CELLS_ID, formedCells.stream().mapToLong(BlockPos::asLong).toArray());
        upgrades.writeToNBT(tag, TAG_UPGRADES);
        if (jobRecipeId != null && jobTicksLeft > 0) {
            tag.putString(JOB_RECIPE_ID, jobRecipeId.toString());
            tag.putInt(JOB_TICKS_ID, jobTicksLeft);
            tag.putInt(JOB_TOTAL_TICKS_ID, jobTotalTicks);
            tag.putInt(JOB_PARALLEL_ID, jobParallel);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mirrorSide = tag.getBoolean(MIRROR_SIDE_ID);
        mirrorFront = tag.getBoolean(MIRROR_FRONT_ID);
        long[] cells = tag.getLongArray(FORMED_CELLS_ID);
        List<BlockPos> loadedCells = new ArrayList<>(cells.length);
        for (long packed : cells) {
            loadedCells.add(BlockPos.of(packed));
        }
        formedCells = loadedCells.isEmpty() ? List.of() : List.copyOf(loadedCells);
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        jobRecipeId = tag.contains(JOB_RECIPE_ID) ? ResourceLocation.tryParse(tag.getString(JOB_RECIPE_ID)) : null;
        jobParallel = Math.max(1, tag.getInt(JOB_PARALLEL_ID));
        jobTicksLeft = Math.max(0, tag.getInt(JOB_TICKS_ID));
        jobTotalTicks = Math.max(1, tag.getInt(JOB_TOTAL_TICKS_ID));
        error = Error.NONE;
    }

    // 状态查询

    public boolean isFormed() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MultiblockControllerBlock block)) {
            return false;
        }
        BooleanProperty formed = block.formedProperty();
        return state.hasProperty(formed) && state.getValue(formed);
    }

    public boolean isBusy() {
        return jobRecipeId != null;
    }

    public boolean isRunning() {
        return isFormed() && jobTicksLeft > 0;
    }

    public boolean isPaused() {
        return isFormed() && paused;
    }

    public Error getError() {
        return error;
    }

    public int getJobElapsed() {
        return jobTicksLeft > 0 ? jobTotalTicks - jobTicksLeft : 0;
    }

    public int getJobDuration() {
        return jobTicksLeft > 0 ? jobTotalTicks : 0;
    }

    public ItemStack getJobItem() {
        MultiblockRecipe recipe = currentRecipe();
        if (recipe == null || jobTicksLeft <= 0 || recipe.getItemOutputs().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack output = recipe.getItemOutputs().get(0);
        return output.copyWithCount(clampCount((long) output.getCount() * Math.max(1, jobParallel)));
    }

    public FluidStack getJobFluid() {
        MultiblockRecipe recipe = currentRecipe();
        if (recipe == null || jobTicksLeft <= 0 || recipe.getFluidOutputs().isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack output = recipe.getFluidOutputs().get(0).copy();
        output.setAmount(clampCount((long) output.getAmount() * Math.max(1, jobParallel)));
        return output;
    }

    public boolean isMirrorSide() {
        return mirrorSide;
    }

    public boolean isMirrorFront() {
        return mirrorFront;
    }

    /**
     * 当前作业产物的客户端视图。
     */
    public ItemStack getClientJobItem() {
        return getJobItem();
    }

    public FluidStack getClientJobFluid() {
        return getJobFluid();
    }
}


