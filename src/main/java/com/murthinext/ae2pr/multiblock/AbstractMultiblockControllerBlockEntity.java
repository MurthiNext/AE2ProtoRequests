package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.murthinext.ae2pr.block.machine_part.DualHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartFluidStorage;
import com.murthinext.ae2pr.recipe.ChancedItemStack;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

public abstract class AbstractMultiblockControllerBlockEntity extends BlockEntity {

    public enum Error {
        NONE, POWER, OUTPUT, DURABILITY, NETWORK
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
    protected final List<MachinePartFluidStorage> inputHatches = new ArrayList<>();
    protected final List<MachinePartFluidStorage> outputHatches = new ArrayList<>();
    private final List<MultiblockChannelConsumer> channelConsumers = new ArrayList<>();
    @Nullable
    private MENetworkAccessHatchBlockEntity networkAccessHatch;

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

    public abstract String machineKey();

    protected abstract RecipeType<?> recipeType();

    public abstract int maxParallel();

    @Nullable
    protected abstract Match findMatch();

    protected int capParallel(MultiblockRecipe recipe, int parallel) {
        return parallel;
    }

    protected Error parallelShortageError() {
        return Error.OUTPUT;
    }

    protected void onJobStarted(Match match) {
    }

    protected void outputProducts(MultiblockRecipe recipe, int parallel) {
        if (level == null) {
            return;
        }
        for (ItemStack output : rolledItemOutputs(recipe, parallel)) {
            ItemStack remaining = output.copy();
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
            for (MachinePartFluidStorage hatch : outputHatches) {
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

    protected List<ItemStack> rolledItemOutputs(MultiblockRecipe recipe, int parallel) {
        if (level == null) {
            return List.of();
        }
        List<ItemStack> produced = new ArrayList<>();
        for (ChancedItemStack output : recipe.getChancedItemOutputs()) {
            if (output.certain()) {
                produced.add(output.stack().copyWithCount(clampCount((long) output.stack().getCount() * parallel)));
                continue;
            }
            int rolled = 0;
            for (int i = 0; i < parallel; i++) {
                if (output.roll(level.random)) {
                    rolled++;
                }
            }
            if (rolled > 0) {
                produced.add(output.stack().copyWithCount(clampCount((long) output.stack().getCount() * rolled)));
            }
        }
        return produced;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
        updateNetworkAccess();
        tickMachine();
        tickJob();
    }

    protected void tickMachine() {
    }

    // 作业状态机

    private void tickJob() {
        if (!isFormed()) {
            cancelJob();
            return;
        }
        if (!isNetworkConnected()) {
            if (!paused || error != Error.NETWORK) {
                paused = true;
                error = Error.NETWORK;
                applyControllerState();
            }
            return;
        }
        if (error == Error.NETWORK) {
            clearPause();
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
        if (!isNetworkConnected()) {
            pause(Error.NETWORK);
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
        match = new Match(match.recipe(), capped, match.group());
        // 输入够但产物放不下时单独提示，避免与“没有可用配方”混淆
        int parallel = outputParallel(match.recipe(), match.parallel());
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        match = new Match(match.recipe(), parallel, match.group());
        // 能量按并行数与耗能倍率从接入仓网络一次性扣除
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

    protected boolean consumeInputs(Match match) {
        PartInventory inputs = match.group().inventory();
        if (!inputs.consume(match.recipe(), match.parallel())) {
            return false;
        }
        inputs.commit();
        return true;
    }

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

    @Nullable
    protected MultiblockRecipe currentRecipe() {
        if (level == null || jobRecipeId == null) {
            return null;
        }
        MultiblockRecipe recipe = resolveRecipe(jobRecipeId);
        return recipe != null && isRecipeApplicable(recipe) ? recipe : null;
    }

    // 作业配方解析入口，包装配方在此适配为 MultiblockRecipe
    @Nullable
    protected MultiblockRecipe resolveRecipe(ResourceLocation id) {
        if (level == null) {
            return null;
        }
        Recipe<?> recipe = level.getRecipeManager().byKey(id).orElse(null);
        return recipe instanceof MultiblockRecipe multiblock ? multiblock : null;
    }

    protected boolean isRecipeApplicable(MultiblockRecipe recipe) {
        return true;
    }

    // 逐个过滤为多方块配方，避免对非本模组配方类型做强转
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<MultiblockRecipe> recipes() {
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        List<? extends Recipe<?>> found = (List<? extends Recipe<?>>) (List<?>) manager
                .getAllRecipesFor((RecipeType) recipeType());
        List<MultiblockRecipe> sorted = new ArrayList<>();
        for (Recipe<?> recipe : found) {
            if (recipe instanceof MultiblockRecipe multiblock) {
                sorted.add(multiblock);
            }
        }
        // 原料种类多的配方更具体，优先尝试可避免泛用配方在满仓时抢料
        sorted.sort(AbstractMultiblockControllerBlockEntity::compareRecipePriority);
        return sorted;
    }

    protected static int compareRecipePriority(MultiblockRecipe left, MultiblockRecipe right) {
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
        return recipe.getItemInputs().size() + recipe.getFluidInputs().size() + recipe.getItemCatalysts().size();
    }

    private static long itemInputAmount(MultiblockRecipe recipe) {
        long total = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            total += input.count();
        }
        for (CountedIngredient catalyst : recipe.getItemCatalysts()) {
            total += catalyst.count();
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

    protected int inputParallel(InputGroup group, MultiblockRecipe recipe, int maximum) {
        int low = 0;
        int high = outputCountBound(recipe, maximum);
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (group.inventory().consume(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    // 逐组试算，组间取最大并行而不相加
    @Nullable
    protected Match matchGroups(MultiblockRecipe recipe) {
        InputGroup best = null;
        int bestParallel = 0;
        for (InputGroup group : inputGroups()) {
            int parallel = inputParallel(group, recipe, maxParallel());
            if (parallel > bestParallel) {
                best = group;
                bestParallel = parallel;
            }
        }
        return best == null ? null : new Match(recipe, bestParallel, best);
    }

    // 隔离部件各自成组，未隔离部件组成共享池并排在最后
    protected List<InputGroup> inputGroups() {
        Map<BlockPos, InputGroup> isolated = new LinkedHashMap<>();
        InputGroup shared = new InputGroup();
        for (ItemBusBlockEntity bus : inputBuses) {
            if (bus.isRecipeIsolated()) {
                isolatedGroup(isolated, bus.getBlockPos()).addBus(bus);
            } else {
                shared.addBus(bus);
            }
        }
        for (MachinePartFluidStorage hatch : inputHatches) {
            // 双仓的物品与流体归属同一个组
            if (isIsolatedHatch(hatch)) {
                isolatedGroup(isolated, hatch.getBlockPos()).addHatch(hatch);
            } else {
                shared.addHatch(hatch);
            }
        }
        List<InputGroup> groups = new ArrayList<>(isolated.values());
        // 共享池始终存在，保持无输入部件时零输入配方的原有行为
        groups.add(shared);
        return groups;
    }

    private static InputGroup isolatedGroup(Map<BlockPos, InputGroup> groups, BlockPos owner) {
        return groups.computeIfAbsent(owner, key -> new InputGroup());
    }

    private static boolean isIsolatedHatch(MachinePartFluidStorage hatch) {
        BlockEntity owner = hatch.getOwner();
        return owner instanceof ItemBusBlockEntity bus ? bus.isRecipeIsolated()
                : owner instanceof FluidHatchBlockEntity fluid && fluid.isRecipeIsolated();
    }

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

    public record Match(MultiblockRecipe recipe, int parallel, InputGroup group) {
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
        return isNetworkConnected() ? networkAccessHatch.extractAEPower(amount, simulate) : 0;
    }

    protected int getControllerChannelUsage() {
        return 1;
    }

    public int getRequiredChannels() {
        long required = Math.max(1, getControllerChannelUsage());
        for (MultiblockChannelConsumer consumer : channelConsumers) {
            required += Math.max(0, consumer.getRequiredChannels());
        }
        return (int) Math.min(Integer.MAX_VALUE, required);
    }

    public int getAllocatedChannels() {
        return isFormed() && networkAccessHatch != null && !networkAccessHatch.isRemoved()
                ? networkAccessHatch.getAllocatedChannels() : 0;
    }

    public boolean isNetworkConnected() {
        return isFormed() && networkAccessHatch != null && !networkAccessHatch.isRemoved()
                && networkAccessHatch.isNetworkConnected(this);
    }

    public double getNetworkStoredPower() {
        return isNetworkConnected() ? networkAccessHatch.getAvailableAEPower() : 0;
    }

    private void updateNetworkAccess() {
        if (isFormed() && networkAccessHatch != null && !networkAccessHatch.isRemoved()) {
            networkAccessHatch.updateController(this);
        }
    }

    private void detachNetworkAccess() {
        if (networkAccessHatch != null) {
            networkAccessHatch.detachController(this);
            networkAccessHatch = null;
        }
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
                        isRunning());
            } else {
                structureFacade.reapply(level, formedCells, isRunning());
            }
            refreshParts(result);
            return;
        }
        onStructureChanged(result);
        boolean mirrorChanged = mirrorSide != result.mirrorSide() || mirrorFront != result.mirrorFront();
        if (result.formed()) {
            mirrorSide = result.mirrorSide();
            mirrorFront = result.mirrorFront();
        }
        boolean active = result.formed() && jobTicksLeft > 0 && !paused;
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
        MENetworkAccessHatchBlockEntity previousAccessHatch = networkAccessHatch;
        networkAccessHatch = null;
        channelConsumers.clear();
        onPartsCleared();
        if (!result.formed() || level == null) {
            if (previousAccessHatch != null) {
                previousAccessHatch.detachController(this);
            }
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
            }
            MachinePartFluidStorage fluidStorage = part instanceof DualHatchBlockEntity dual
                    ? dual.getFluidStorage()
                    : part instanceof FluidHatchBlockEntity hatch ? hatch.getFluidStorage() : null;
            if (fluidStorage != null) {
                (fluidStorage.isOutputHatch() ? outputHatches : inputHatches).add(fluidStorage);
            }
            if (part instanceof MENetworkAccessHatchBlockEntity hatch) {
                networkAccessHatch = hatch;
            }
            if (part != this && part instanceof MultiblockChannelConsumer consumer) {
                channelConsumers.add(consumer);
            }
            collectExtraPart(pos, part);
        }
        if (previousAccessHatch != null && previousAccessHatch != networkAccessHatch) {
            previousAccessHatch.detachController(this);
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

    // 核心方块位置，没有核心的机器返回 null
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
        boolean showRunning = formed && jobTicksLeft > 0 && !paused;
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
        detachNetworkAccess();
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
    public void setRemoved() {
        detachNetworkAccess();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        detachNetworkAccess();
        super.onChunkUnloaded();
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
        return isFormed() && jobTicksLeft > 0 && !paused;
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

    public ItemStack getClientJobItem() {
        return getJobItem();
    }

    public FluidStack getClientJobFluid() {
        return getJobFluid();
    }
}
