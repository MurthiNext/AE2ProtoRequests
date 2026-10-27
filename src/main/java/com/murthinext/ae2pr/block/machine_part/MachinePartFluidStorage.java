package com.murthinext.ae2pr.block.machine_part;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class MachinePartFluidStorage {

    private static final String TANKS_ID = "tanks";

    private final BlockEntity owner;
    private final FluidTank[] tanks;

    private final IFluidHandler tanksView;
    private LazyOptional<IFluidHandler> tankCapability;

    public MachinePartFluidStorage(BlockEntity owner) {
        this.owner = owner;
        BlockState state = owner.getBlockState();
        int capacity = MachinePartBlock.fluidCapacity(state);
        this.tanks = new FluidTank[MachinePartBlock.fluidTankCount(state)];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = new FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    owner.setChanged();
                }
            };
        }
        this.tanksView = new TanksView(!isOutputHatch());
        this.tankCapability = LazyOptional.of(() -> tanksView);
    }

    // 多罐视图：按槽顺序注入/抽出
    private final class TanksView implements IFluidHandler {

        private final boolean allowFill;

        private TanksView(boolean allowFill) {
            this.allowFill = allowFill;
        }

        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks[tank].getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return allowFill && tanks[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill || resource.isEmpty()) {
                return 0;
            }
            int filled = 0;
            for (boolean sameFluid : new boolean[] { true, false }) {
                for (FluidTank tank : tanks) {
                    if (sameFluid ? !tank.getFluid().isFluidEqual(resource) : !tank.isEmpty()) {
                        continue;
                    }
                    filled += tank.fill(new FluidStack(resource, resource.getAmount() - filled), action);
                    if (filled >= resource.getAmount()) {
                        return filled;
                    }
                }
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int drained = 0;
            FluidStack moved = FluidStack.EMPTY;
            for (FluidTank tank : tanks) {
                FluidStack part = tank.drain(new FluidStack(resource, resource.getAmount() - drained), action);
                if (!part.isEmpty()) {
                    drained += part.getAmount();
                    moved = part;
                }
                if (drained >= resource.getAmount()) {
                    break;
                }
            }
            return moved.isEmpty() ? FluidStack.EMPTY : new FluidStack(moved, drained);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (FluidTank tank : tanks) {
                if (!tank.isEmpty()) {
                    return drain(new FluidStack(tank.getFluid(), maxDrain), action);
                }
            }
            return FluidStack.EMPTY;
        }
    }

    public int getTankCount() {
        return tanks.length;
    }

    // 机器内部读写不受对外视图限制
    public FluidTank getTank(int tank) {
        return tanks[tank];
    }

    public FluidTank getTank() {
        return tanks[0];
    }

    public int getCapacityPerTank() {
        return tanks[0].getTankCapacity(0);
    }

    public List<FluidStack> getFluidSnapshots() {
        List<FluidStack> fluids = new ArrayList<>(tanks.length);
        for (FluidTank tank : tanks) {
            fluids.add(tank.getFluid().copy());
        }
        return fluids;
    }

    public boolean isOutputHatch() {
        return FluidHatchBlockEntity.isOutputHatch(owner.getBlockState());
    }

    public boolean acceptsPlayerInsert() {
        return !isOutputHatch();
    }

    public LazyOptional<IFluidHandler> getCapability() {
        return tankCapability;
    }

    public void invalidateCaps() {
        tankCapability.invalidate();
    }

    public void reviveCaps() {
        tankCapability = LazyOptional.of(() -> tanksView);
    }

    public BlockPos getBlockPos() {
        return owner.getBlockPos();
    }

    public void setChanged() {
        owner.setChanged();
    }

    // 与朝向面的外部流体容器搬运流体
    public void serverTick() {
        var level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = owner.getBlockState().getValue(MachinePartBlock.FACING);
        BlockEntity target = level.getBlockEntity(getBlockPos().relative(facing));
        if (target == null) {
            return;
        }
        IFluidHandler handler = target.getCapability(ForgeCapabilities.FLUID_HANDLER, facing.getOpposite())
                .orElse(null);
        if (handler == null) {
            return;
        }
        if (isOutputHatch()) {
            pushFluid(handler);
        } else {
            pullFluid(handler);
        }
    }

    private void pullFluid(IFluidHandler source) {
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack available = source.getFluidInTank(tank);
            if (available.isEmpty()) {
                continue;
            }
            FluidStack drained = source.drain(available.copy(), IFluidHandler.FluidAction.SIMULATE);
            int accepted = tanksView.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack moved = source.drain(new FluidStack(drained, accepted), IFluidHandler.FluidAction.EXECUTE);
                tanksView.fill(moved, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private void pushFluid(IFluidHandler target) {
        for (FluidTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored.isEmpty()) {
                continue;
            }
            int accepted = target.fill(stored.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack moved = tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            target.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // 仅由同步包调用，直接覆盖本地罐内容
    public void applyClientFluid(int tank, FluidStack fluid) {
        tanks[tank].setFluid(fluid.copy());
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (FluidTank tank : tanks) {
            list.add(tank.writeToNBT(new CompoundTag()));
        }
        tag.put(TANKS_ID, list);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(TANKS_ID, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TANKS_ID, Tag.TAG_COMPOUND);
            for (int i = 0; i < tanks.length && i < list.size(); i++) {
                tanks[i].readFromNBT(list.getCompound(i));
            }
        }
    }
}
