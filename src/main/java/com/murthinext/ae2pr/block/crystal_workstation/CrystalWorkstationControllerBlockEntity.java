package com.murthinext.ae2pr.block.crystal_workstation;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.block.HighEnergyCoreBlock;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;
import com.murthinext.ae2pr.recipe.CrystalWorkstationRecipe;

public class CrystalWorkstationControllerBlockEntity extends BlockEntity {

    public enum Error {
        NONE, POWER, OUTPUT
    }

    private static final int CHECK_INTERVAL = 20;
    private final StructureValidator structure = new StructureValidator(CrystalWorkstationStructure.PATTERN);
    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    private final List<ItemBusBlockEntity> outputBuses = new ArrayList<>();
    private final List<FluidHatchBlockEntity> inputHatches = new ArrayList<>();
    private final List<FluidHatchBlockEntity> outputHatches = new ArrayList<>();
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.CRYSTAL_WORKSTATION.get(), 4, this::setChanged);

    private CrystalWorkstationMode mode = CrystalWorkstationMode.ETCHING;
    private Error error = Error.NONE;
    private int checkTicks;
    private int retryTicks;
    private boolean mirrorFront;
    @Nullable
    private ResourceLocation jobRecipeId;
    private int jobTicksLeft;
    private int jobTotalTicks;
    private int jobParallel;

    public CrystalWorkstationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_WORKSTATION.get(), pos, state);
    }

    public CrystalWorkstationMode getMode() {
        return mode;
    }

    public boolean switchMode() {
        if (level == null || level.isClientSide || isBusy()) {
            return false;
        }
        mode = mode.next();
        error = Error.NONE;
        retryTicks = 0;
        syncState();
        return true;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++checkTicks >= CHECK_INTERVAL) {
            checkTicks = 0;
            validateStructure();
        }
        if (isBusy() && currentRecipe() == null) {
            clearJob();
        }
        if (!isFormed()) {
            return;
        }
        if (isBusy()) {
            CrystalWorkstationRecipe recipe = currentRecipe();
            if (recipe == null) {
                clearJob();
                return;
            }
            if (jobTicksLeft > 0) {
                jobTicksLeft--;
                setChanged();
            }
            if (jobTicksLeft == 0) {
                WorkstationInventory outputs = new WorkstationInventory(outputBuses, outputHatches);
                if (outputs.insert(recipe, jobParallel)) {
                    outputs.commit();
                    clearJob();
                    retryTicks = CHECK_INTERVAL;
                } else if (error != Error.OUTPUT) {
                    error = Error.OUTPUT;
                    syncState();
                }
            }
        } else if (retryTicks > 0) {
            retryTicks--;
        } else {
            retryTicks = CHECK_INTERVAL;
            tryStartJob();
        }
    }

    private void tryStartJob() {
        Error nextError = Error.NONE;
        // 只向配方管理器查询当前模式，包含失败重试与切换后的首次匹配。
        for (CrystalWorkstationRecipe recipe : level.getRecipeManager().getAllRecipesFor(mode.recipeType())) {
            if (!new WorkstationInventory(inputBuses, inputHatches).consume(recipe, 1)) {
                continue;
            }
            int low = 0;
            int high = maxParallel();
            for (ItemStack output : recipe.getItemOutputs()) {
                high = Math.min(high, Integer.MAX_VALUE / output.getCount());
            }
            for (FluidStack output : recipe.getFluidOutputs()) {
                high = Math.min(high, Integer.MAX_VALUE / output.getAmount());
            }
            while (low < high) {
                int middle = low + (high - low + 1) / 2;
                if (new WorkstationInventory(inputBuses, inputHatches).consume(recipe, middle)
                        && new WorkstationInventory(outputBuses, outputHatches).insert(recipe, middle)) {
                    low = middle;
                } else {
                    high = middle - 1;
                }
            }
            if (low == 0) {
                nextError = Error.OUTPUT;
                continue;
            }
            double cost = recipe.getEnergyPerParallel() * low
                    * Math.pow(Config.lossyEnergyMultiplierPerCard(), upgrades.getInstalledUpgrades(AEItems.SPEED_CARD));
            if (!Double.isFinite(cost) || extractEnergy(cost, true) < cost) {
                nextError = Error.POWER;
                continue;
            }
            WorkstationInventory inputs = new WorkstationInventory(inputBuses, inputHatches);
            if (!inputs.consume(recipe, low)) {
                continue;
            }
            extractEnergy(cost, false);
            inputs.commit();
            jobRecipeId = recipe.getId();
            jobParallel = low;
            double speed = 1.0 + upgrades.getInstalledUpgrades(AEItems.SPEED_CARD) * Config.lossySpeedPerCard();
            jobTotalTicks = Math.max(1, (int) Math.ceil(recipe.getDuration() / speed));
            jobTicksLeft = jobTotalTicks;
            error = Error.NONE;
            syncState();
            return;
        }
        if (error != nextError) {
            error = nextError;
            syncState();
        }
    }

    @Nullable
    private CrystalWorkstationRecipe currentRecipe() {
        if (level == null || jobRecipeId == null) {
            return null;
        }
        var recipe = level.getRecipeManager().byKey(jobRecipeId).orElse(null);
        return recipe instanceof CrystalWorkstationRecipe workstationRecipe && workstationRecipe.getMode() == mode
                && workstationRecipe.getType() == mode.recipeType() ? workstationRecipe : null;
    }

    private void clearJob() {
        jobRecipeId = null;
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        jobParallel = 0;
        error = Error.NONE;
        syncState();
    }

    private double extractEnergy(double amount, boolean simulate) {
        double remaining = amount;
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (remaining <= 0) {
                break;
            }
            remaining -= hatch.extractAEPower(remaining, simulate);
        }
        return amount - remaining;
    }

    public void validateStructure() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(CrystalWorkstationBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                CrystalWorkstationStructure.PATTERN.minSlices());
        if (result.fromCache()) {
            return;
        }
        if (result.formed()) {
            if (mirrorFront != result.mirrorFront()) {
                updateCoreLight(false);
            }
            mirrorFront = result.mirrorFront();
        }
        inputBuses.clear();
        outputBuses.clear();
        inputHatches.clear();
        outputHatches.clear();
        energyHatches.clear();
        if (result.formed()) {
            List<BlockPos> cells = new ArrayList<>(result.cells());
            cells.sort(CrystalWorkstationStructure.PATTERN.posComparator(facing, Direction.UP,
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
            }
        }
        CrystalWorkstationStructure.updateFormed(level, worldPosition, facing, result);
        BlockState state = getBlockState();
        if (state.getValue(CrystalWorkstationBlock.FORMED) != result.formed()) {
            level.setBlock(worldPosition, state.setValue(CrystalWorkstationBlock.FORMED, result.formed()), Block.UPDATE_ALL);
        }
        syncState();
    }

    private void syncState() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState updated = state.setValue(CrystalWorkstationBlock.RUNNING, isFormed() && isBusy() && jobTicksLeft > 0)
                .setValue(CrystalWorkstationBlock.PAUSED, isFormed() && error != Error.NONE);
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
        updateCoreLight(updated.getValue(CrystalWorkstationBlock.RUNNING));
        level.sendBlockUpdated(worldPosition, state, updated, Block.UPDATE_CLIENTS);
    }

    private void updateCoreLight(boolean lit) {
        BlockPos pos = CrystalWorkstationStructure.corePos(worldPosition,
                getBlockState().getValue(CrystalWorkstationBlock.FACING), mirrorFront);
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.HIGH_ENERGY_CORE.get()) && state.getValue(HighEnergyCoreBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(HighEnergyCoreBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void onControllerRemoved() {
        structure.invalidate();
        updateCoreLight(false);
        CrystalWorkstationStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(CrystalWorkstationBlock.FACING), StructureResult.EMPTY);
        for (ItemStack upgrade : upgrades) {
            Block.popResource(level, worldPosition, upgrade);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        checkTicks = CHECK_INTERVAL - 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("mode", mode.id());
        tag.putBoolean("mirrorFront", mirrorFront);
        upgrades.writeToNBT(tag, "upgrades");
        if (jobRecipeId != null) {
            tag.putString("jobRecipe", jobRecipeId.toString());
            tag.putInt("jobTicks", jobTicksLeft);
            tag.putInt("jobTotalTicks", jobTotalTicks);
            tag.putInt("jobParallel", jobParallel);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = CrystalWorkstationMode.byId(tag.getString("mode"));
        mirrorFront = tag.getBoolean("mirrorFront");
        upgrades.readFromNBT(tag, "upgrades");
        jobRecipeId = tag.contains("jobRecipe") ? ResourceLocation.tryParse(tag.getString("jobRecipe")) : null;
        jobTotalTicks = Math.max(1, tag.getInt("jobTotalTicks"));
        jobTicksLeft = Math.min(jobTotalTicks, Math.max(0, tag.getInt("jobTicks")));
        jobParallel = Math.max(1, tag.getInt("jobParallel"));
        error = Error.NONE;
        retryTicks = 0;
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
        if (packet.getTag() != null) {
            load(packet.getTag());
        }
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public int maxParallel() {
        return Config.crystalWorkstationParallel();
    }

    public boolean isFormed() {
        return getBlockState().getValue(CrystalWorkstationBlock.FORMED);
    }

    public boolean isBusy() {
        return jobRecipeId != null;
    }

    public boolean isRunning() {
        return isFormed() && isBusy() && jobTicksLeft > 0;
    }

    public boolean isPaused() {
        return isFormed() && error != Error.NONE;
    }

    public Error getError() {
        return error;
    }

    public int getJobElapsed() {
        return isBusy() ? jobTotalTicks - jobTicksLeft : 0;
    }

    public int getJobDuration() {
        return isBusy() ? jobTotalTicks : 0;
    }

    public ItemStack getJobItem() {
        CrystalWorkstationRecipe recipe = currentRecipe();
        return recipe == null || recipe.getItemOutputs().isEmpty() ? ItemStack.EMPTY
                : recipe.getItemOutputs().get(0).copyWithCount(recipe.getItemOutputs().get(0).getCount() * jobParallel);
    }

    public FluidStack getJobFluid() {
        CrystalWorkstationRecipe recipe = currentRecipe();
        if (recipe == null || recipe.getFluidOutputs().isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack fluid = recipe.getFluidOutputs().get(0).copy();
        fluid.setAmount(fluid.getAmount() * jobParallel);
        return fluid;
    }

    public boolean isEnergyConnected() {
        return energyHatches.stream().anyMatch(FluixCrystalEnergyHatchBlockEntity::isGridConnected);
    }

    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }
}
