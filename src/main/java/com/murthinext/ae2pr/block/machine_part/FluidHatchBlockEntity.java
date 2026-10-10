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
 * 机器部件流体仓方块实体。
 * <p>
 * 输出仓只接收机器内部产出，禁止玩家/外部注入。
 */
public class FluidHatchBlockEntity extends BlockEntity {

    /** 石英仓单槽容量：16K 桶 */
    public static final int CAPACITY = 16 * 1024 * 1000;
    /** AEV 仓单槽容量：1024 桶 */
    public static final int AEV_CAPACITY = 1024 * 1000;

    private static final String TANKS_ID = "tanks";
    /** 旧版单罐存档键 */
    private static final String TANK_ID = "tank";
    private static final String AUTO_TRANSFER_ID = "autoTransfer";

    /** 自动搬运开关：输入仓拉取、输出仓推出，仅作用于朝向面，默认启用。 */
    private boolean autoTransfer = true;

    private final FluidTank[] tanks;

    /** 统一的多罐视图：输入仓可注入，输出仓只出不进。 */
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

    /** 多罐视图：按槽顺序注入/抽出，行为与单罐一致。 */
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

    /** 流体槽数量（石英 1、AEV 2）。 */
    public int getTankCount() {
        return tanks.length;
    }

    /** 按槽读取罐体（机器内部读写不受对外视图限制）。 */
    public FluidTank getTank(int tank) {
        return tanks[tank];
    }

    /** 首个罐体（单槽部件的兼容入口）。 */
    public FluidTank getTank() {
        return tanks[0];
    }

    /** 单槽容量（mB）。 */
    public int getCapacityPerTank() {
        return tanks[0].getTankCapacity(0);
    }

    /** 当前罐内流体的只读快照，用于界面同步。 */
    public List<FluidStack> getFluidSnapshots() {
        List<FluidStack> fluids = new ArrayList<>(tanks.length);
        for (FluidTank tank : tanks) {
            fluids.add(tank.getFluid().copy());
        }
        return fluids;
    }

    /** 是否为输出仓。 */
    public boolean isOutputHatch() {
        return isOutputHatch(getBlockState());
    }

    /** 静态判定：是否为输出仓方块（含 AEV）。 */
    public static boolean isOutputHatch(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get())
                || state.is(ModBlocks.AEV_OUTPUT_HATCH.get());
    }

    /** 是否允许玩家存入（输出仓仅接受机器内部产出）。 */
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

    /** 服务端 tick：与朝向面的外部流体容器搬运流体（输入拉取 / 输出推出），直到装满或清空。 */
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

    /** 从外部流体容器拉取流体，直到罐满或外部无可取之物。 */
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

    /** 向外部流体容器推出流体，直到罐空或外部容器存满。 */
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

    /** 客户端展示同步：直接覆盖本地指定罐内容（仅由同步包调用）。 */
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
