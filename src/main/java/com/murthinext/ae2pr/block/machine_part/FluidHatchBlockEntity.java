package com.murthinext.ae2pr.block.machine_part;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;

public class FluidHatchBlockEntity extends BlockEntity {

    public static final int CAPACITY = 16 * 1024 * 1000;
    public static final int AEV_CAPACITY = 1024 * 1000;

    private static final String AUTO_TRANSFER_ID = "autoTransfer";
    private static final String RECIPE_ISOLATED_ID = "recipeIsolated";

    private final MachinePartFluidStorage fluidStorage;
    private boolean autoTransfer = true;
    private boolean recipeIsolated;

    public FluidHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERTUS_QUARTZ_CRYSTAL_MACHINE_PART.get(), pos, state);
        fluidStorage = new MachinePartFluidStorage(this);
    }

    public MachinePartFluidStorage getFluidStorage() {
        return fluidStorage;
    }

    public int getTankCount() {
        return fluidStorage.getTankCount();
    }

    public FluidTank getTank(int tank) {
        return fluidStorage.getTank(tank);
    }

    public int getCapacityPerTank() {
        return fluidStorage.getCapacityPerTank();
    }

    public List<FluidStack> getFluidSnapshots() {
        return fluidStorage.getFluidSnapshots();
    }

    public boolean isOutputHatch() {
        return isOutputHatch(getBlockState());
    }

    public static boolean isOutputHatch(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get())
                || state.is(ModBlocks.AEV_OUTPUT_HATCH.get())
                || state.is(ModBlocks.AEV_DUAL_OUTPUT_HATCH.get());
    }

    public boolean acceptsPlayerInsert() {
        return !isOutputHatch();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidStorage.getCapability().cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidStorage.invalidateCaps();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidStorage.reviveCaps();
    }

    public boolean isAutoTransfer() {
        return autoTransfer;
    }

    public void setAutoTransfer(boolean autoTransfer) {
        this.autoTransfer = autoTransfer;
        setChanged();
    }

    public boolean isRecipeIsolated() {
        return recipeIsolated;
    }

    public void setRecipeIsolated(boolean recipeIsolated) {
        this.recipeIsolated = recipeIsolated;
        setChanged();
    }

    public void serverTick() {
        if (autoTransfer) {
            fluidStorage.serverTick();
        }
    }

    public void applyClientFluid(int tank, FluidStack fluid) {
        fluidStorage.applyClientFluid(tank, fluid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        fluidStorage.save(tag);
        tag.putBoolean(AUTO_TRANSFER_ID, autoTransfer);
        tag.putBoolean(RECIPE_ISOLATED_ID, recipeIsolated);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluidStorage.load(tag);
        if (tag.contains(AUTO_TRANSFER_ID)) {
            autoTransfer = tag.getBoolean(AUTO_TRANSFER_ID);
        }
        recipeIsolated = tag.getBoolean(RECIPE_ISOLATED_ID);
    }
}
