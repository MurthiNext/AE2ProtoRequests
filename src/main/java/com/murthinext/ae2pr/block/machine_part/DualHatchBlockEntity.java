package com.murthinext.ae2pr.block.machine_part;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 同时提供物品总线与流体仓功能的总成。
 */
public class DualHatchBlockEntity extends ItemBusBlockEntity {

    private final MachinePartFluidStorage fluidStorage;

    public DualHatchBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        fluidStorage = new MachinePartFluidStorage(this);
    }

    public MachinePartFluidStorage getFluidStorage() {
        return fluidStorage;
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

    @Override
    public void serverTick() {
        super.serverTick();
        if (isAutoTransfer()) {
            fluidStorage.serverTick();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        fluidStorage.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluidStorage.load(tag);
    }
}
