package com.murthinext.ae2pr.block.assembly_line;

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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;

/**
 * 赛特斯石英水晶输入/输出仓方块实体：单类流体存储，上限 16K 桶（16,384,000 mB）。
 */
public class FluidHatchBlockEntity extends BlockEntity {

    /** 存储上限：16K 桶 */
    public static final int CAPACITY = 16 * 1024 * 1000;
    /** 可存储的类型数（预留多种类扩展） */
    public static final int TYPE_CAPACITY = 1;

    private static final String TANK_ID = "tank";
    private static final String AUTO_TRANSFER_ID = "autoTransfer";

    /** 自动搬运开关：输入仓拉取、输出仓推出，仅作用于朝向面，默认启用。 */
    private boolean autoTransfer = true;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** 输出仓对外视图：拒绝外部注入，仅允许抽出。 */
    private final IFluidHandler externalTank = new IFluidHandler() {
        @Override
        public int getTanks() {
            return tank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tank.getFluidInTank(tankIndex);
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tank.getTankCapacity(tankIndex);
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    };

    /** 对外暴露的罐体：输入仓双向可交互，输出仓只出不进。 */
    private final IFluidHandler exposedTank;
    private final LazyOptional<IFluidHandler> tankCapability;

    public FluidHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERTUS_QUARTZ_CRYSTAL_MACHINE_PART.get(), pos, state);
        this.exposedTank = isOutputHatch() ? externalTank : tank;
        this.tankCapability = LazyOptional.of(() -> exposedTank);
    }

    /** 内部罐体（机器内部读写不受对外视图限制）。 */
    public FluidTank getTank() {
        return tank;
    }

    /** 是否为输出仓。 */
    public boolean isOutputHatch() {
        return isOutputHatch(getBlockState());
    }

    /** 静态判定：是否为输出仓方块。 */
    public static boolean isOutputHatch(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get());
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
        Direction facing = getBlockState().getValue(CertusQuartzCrystalMachinePartBlock.FACING);
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
        int accepted = tank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack moved = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        tank.fill(moved, IFluidHandler.FluidAction.EXECUTE);
    }

    /** 向外部流体容器推出流体，直到罐空或外部容器存满。 */
    private void pushFluid(IFluidHandler target) {
        FluidStack stored = tank.getFluid();
        if (stored.isEmpty()) {
            return;
        }
        int accepted = target.fill(stored.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack moved = tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        target.fill(moved, IFluidHandler.FluidAction.EXECUTE);
    }

    /** 客户端展示同步：直接覆盖本地罐内容（仅由同步包调用）。 */
    public void applyClientFluid(FluidStack fluid) {
        tank.setFluid(fluid.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(TANK_ID, tank.writeToNBT(new CompoundTag()));
        tag.putBoolean(AUTO_TRANSFER_ID, autoTransfer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TANK_ID)) {
            tank.readFromNBT(tag.getCompound(TANK_ID));
        }
        if (tag.contains(AUTO_TRANSFER_ID)) {
            autoTransfer = tag.getBoolean(AUTO_TRANSFER_ID);
        }
    }
}
