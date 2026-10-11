package com.murthinext.ae2pr.block.machine_part;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;

/**
 * 机器部件流体仓方块实体，输出仓禁止外部注入。
 */
public class FluidHatchBlockEntity extends BlockEntity {

    public static final int CAPACITY = 16 * 1024 * 1000;
    public static final int AEV_CAPACITY = 1024 * 1000;

    private static final String TANKS_ID = "tanks";
    private static final String TANK_ID = "tank";
    private static final String AUTO_TRANSFER_ID = "autoTransfer";

    private boolean autoTransfer = true;

    private final FluidTank[] tanks;

    private final IFluidHandler tanksView;
    private final LazyOptional<IFluidHandler> tankCapability;

    public FluidHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERTUS_QUARTZ_CRYSTAL_MACHINE_PART.get(), pos, state);
        int capacity = MachinePartBlock.fluidCapacity(state);
        this.tanks = new FluidTank[MachinePartBlock.fluidTankCount(state)];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = new FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };
        }
        this.tanksView = new TanksView(!isOutputHatch());
        this.tankCapability = LazyOptional.of(() -> tanksView);
    }

    /**
     * 多罐视图：按槽顺序注入/抽出。
     */
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
            for (FluidTank tank : tanks) {
                filled += tank.fill(new FluidStack(resource, resource.getAmount() - filled), action);
                if (filled >= resource.getAmount()) {
                    break;
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
            int drained = 0;
            FluidStack moved = FluidStack.EMPTY;
            for (FluidTank tank : tanks) {
                FluidStack part = tank.drain(maxDrain - drained, action);
                if (!part.isEmpty()) {
                    drained += part.getAmount();
                    moved = part;
                }
                if (drained >= maxDrain) {
                    break;
                }
            }
            return moved.isEmpty() ? FluidStack.EMPTY : new FluidStack(moved, drained);
        }
    }

    public int getTankCount() {
        return tanks.length;
    }

    /**
     * 机器内部读写不受对外视图限制。
     */
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
        return isOutputHatch(getBlockState());
    }

    public static boolean isOutputHatch(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get())
                || state.is(ModBlocks.AEV_OUTPUT_HATCH.get());
    }

    public boolean acceptsPlayerInsert() {
        return !isOutputHatch();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return tankCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankCapability.invalidate();
    }

    public boolean isAutoTransfer() {
        return autoTransfer;
    }

    public void setAutoTransfer(boolean autoTransfer) {
        this.autoTransfer = autoTransfer;
        setChanged();
    }

    /**
     * 与朝向面的外部流体容器搬运流体。
     */
    public void serverTick() {
        if (!autoTransfer || level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(MachinePartBlock.FACING);
        BlockEntity target = level.getBlockEntity(worldPosition.relative(facing));
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
        FluidStack drained = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return;
        }
        int accepted = tanksView.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack moved = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        tanksView.fill(moved, IFluidHandler.FluidAction.EXECUTE);
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

    /**
     * 仅由同步包调用，直接覆盖本地罐内容。
     */
    public void applyClientFluid(int tank, FluidStack fluid) {
        tanks[tank].setFluid(fluid.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (FluidTank tank : tanks) {
            list.add(tank.writeToNBT(new CompoundTag()));
        }
        tag.put(TANKS_ID, list);
        tag.putBoolean(AUTO_TRANSFER_ID, autoTransfer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TANKS_ID, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TANKS_ID, Tag.TAG_COMPOUND);
            for (int i = 0; i < tanks.length && i < list.size(); i++) {
                tanks[i].readFromNBT(list.getCompound(i));
            }
        } else if (tag.contains(TANK_ID)) {
            // 旧版单罐存档
            tanks[0].readFromNBT(tag.getCompound(TANK_ID));
        }
        if (tag.contains(AUTO_TRANSFER_ID)) {
            autoTransfer = tag.getBoolean(AUTO_TRANSFER_ID);
        }
    }
}
