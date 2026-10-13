package com.murthinext.ae2pr.block.matter_phase_transformer;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import com.murthinext.ae2pr.block.HighEnergyCoreBlock;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.multiblock.structure.MatterPhaseTransformerStructure;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MatterPhaseTransformerRecipe;

/**
 * 物质相变仪主机方块实体。
 */
public class MatterPhaseTransformerControllerBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 20;

    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String MIRROR_FRONT_ID = "mirrorFront";

    /**
     * 暂停原因。
     */
    public enum Error {
        NONE, POWER, OUTPUT
    }

    private int tickCounter;
    private int retryCounter;
    private boolean mirrorFront;
    private final MatterPhaseTransformerAnimation animation = new MatterPhaseTransformerAnimation();

    private int jobTicksLeft;
    private int jobTotalTicks;
    private int jobParallel;
    private boolean paused;
    private Error error = Error.NONE;
    @Nullable
    private MatterPhaseTransformerRecipe currentRecipe;

    private FluidStack clientJobFluid = FluidStack.EMPTY;

    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    private final List<FluidHatchBlockEntity> outputHatches = new ArrayList<>();
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    private final StructureValidator structure = new StructureValidator(MatterPhaseTransformerStructure.PATTERN);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.MATTER_PHASE_TRANSFORMER.get(), 4, this::onUpgradesChanged);

    public MatterPhaseTransformerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PHASE_TRANSFORMER.get(), pos, state);
    }

    public void clientTick() {
        animation.tick(isFormed(), isRunning());
    }

    public double getParticleTime(float partialTick) {
        return animation.getParticleTime(partialTick);
    }

    public float getEffectActivity(float partialTick) {
        return animation.getActivity(partialTick);
    }

    public float getBeamStrength(float partialTick) {
        return animation.getBeamStrength(partialTick);
    }

    public BlockPos getCorePos() {
        return MatterPhaseTransformerStructure.corePos(worldPosition,
                getBlockState().getValue(MatterPhaseTransformerBlock.FACING), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        return new AABB(core.getX(), core.getY() - 3, core.getZ(),
                core.getX() + 1, core.getY() + 4, core.getZ() + 1);
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
        MatterPhaseTransformerRecipe recipe = findMatch();
        if (recipe == null) {
            clearPause();
            return;
        }
        int parallel = parallelFor(recipe);
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        // 能量按并行数与耗能倍率从能源仓网络一次性扣除
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

    private void finishJob() {
        MatterPhaseTransformerRecipe recipe = currentRecipe;
        int parallel = jobParallel;
        currentRecipe = null;
        jobParallel = 0;
        if (recipe != null && level != null) {
            insertFluid(recipe, parallel);
        }
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        if (isFormed()) {
            tryStartJob();
        }
        applyControllerState();
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

    @Nullable
    private MatterPhaseTransformerRecipe findMatch() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        for (MatterPhaseTransformerRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.MATTER_PHASE_TRANSFORMER_TYPE.get())) {
            if (hasIngredients(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean hasIngredients(MatterPhaseTransformerRecipe recipe) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            if (available(ingredient) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

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

    private void consumeInputs(MatterPhaseTransformerRecipe recipe, int parallel) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int remaining = ingredient.count() * parallel;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount() && remaining > 0; slot++) {
                    // 单次抽取上限为物品堆叠上限，按需求量分批抽取
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
     * 输入份数与输出仓容量中的较小值。
     */
    private int parallelFor(MatterPhaseTransformerRecipe recipe) {
        int parallel = maxParallel();
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            parallel = Math.min(parallel, available(ingredient) / ingredient.count());
        }
        return outputParallel(recipe, parallel);
    }

    private int outputParallel(MatterPhaseTransformerRecipe recipe, int maximum) {
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

    private boolean canFitFluid(MatterPhaseTransformerRecipe recipe, int parallel) {
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

    private void insertFluid(MatterPhaseTransformerRecipe recipe, int parallel) {
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
            // 启动前已检查，兜底记录避免产物丢失
            ae2pr.LOGGER.warn("物质相变仪产物无法存入输出仓，已丢弃 {}", remaining);
        }
    }

    // 加速卡与电力

    public int maxParallel() {
        return Math.max(1, Config.matterPhaseTransformerParallel());
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
        if (result.formed()) {
            if (mirrorFront != result.mirrorFront()) {
                updateCoreLight(false);
            }
            mirrorFront = result.mirrorFront();
        }
        MatterPhaseTransformerStructure.updateFormed(level, worldPosition, facing, result);
        refreshParts(level, result);
        applyControllerState(result.formed());
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        outputHatches.clear();
        energyHatches.clear();
        if (!result.formed()) {
            return;
        }
        // 按 片 → 行 → 列 排序，保证部件顺序稳定
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

    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(MatterPhaseTransformerBlock.FORMED));
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
        updateCoreLight(showRunning);
    }

    private void updateCoreLight(boolean lit) {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos core = getCorePos();
        BlockState state = level.getBlockState(core);
        if (state.is(ModBlocks.HIGH_ENERGY_CORE.get()) && state.getValue(HighEnergyCoreBlock.LIT) != lit) {
            level.setBlock(core, state.setValue(HighEnergyCoreBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void onControllerRemoved() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        structure.invalidate();
        updateCoreLight(false);
        MatterPhaseTransformerStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(MatterPhaseTransformerBlock.FACING), StructureResult.EMPTY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测
            tickCounter = CHECK_INTERVAL - 1;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

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
        tag.putBoolean(MIRROR_FRONT_ID, mirrorFront);
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
        mirrorFront = tag.getBoolean(MIRROR_FRONT_ID);
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        Level level = this.level;
        if (level == null || level.isClientSide || !tag.contains(JOB_RECIPE_ID)) {
            return;
        }
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse(tag.getString(JOB_RECIPE_ID)));
        if (recipe.orElse(null) instanceof MatterPhaseTransformerRecipe matterPhaseTransformerRecipe) {
            // 恢复加工进度
            jobTicksLeft = tag.getInt(JOB_TICKS_ID);
            jobTotalTicks = tag.contains(JOB_TOTAL_TICKS_ID)
                    ? tag.getInt(JOB_TOTAL_TICKS_ID)
                    : Math.max(1, matterPhaseTransformerRecipe.getDuration());
            jobParallel = Math.max(1, tag.getInt(JOB_PARALLEL_ID));
            currentRecipe = matterPhaseTransformerRecipe;
        }
    }

    // 状态查询

    public boolean isFormed() {
        return getBlockState().getValue(MatterPhaseTransformerBlock.FORMED);
    }

    public boolean isRunning() {
        return isFormed() && getBlockState().getValue(MatterPhaseTransformerBlock.RUNNING);
    }

    public boolean isPaused() {
        return isFormed() && getBlockState().getValue(MatterPhaseTransformerBlock.PAUSED);
    }

    public Error getError() {
        return error;
    }

    public FluidStack getJobFluid() {
        if (currentRecipe == null || jobTicksLeft <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack output = currentRecipe.getFluidOutput();
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

    public FluidStack getClientJobFluid() {
        return clientJobFluid;
    }

    public void applyClientJobFluid(FluidStack stack) {
        this.clientJobFluid = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
    }
}
