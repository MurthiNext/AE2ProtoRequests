package com.murthinext.ae2pr.block.machine_part;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.MachinePartFluidPacket;

/**
 * 机器部件流体仓的容器菜单。
 */
public class FluidHatchMenu extends AbstractContainerMenu {

    public static final MenuType<FluidHatchMenu> TYPE = IForgeMenuType.create(FluidHatchMenu::create);

    private static final int INV_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private final BlockPos pos;
    private final Player owner;
    private final boolean serverSide;
    private final FluidHatchBlockEntity blockEntity;
    private List<FluidStack> lastSentFluids;
    private boolean fluidSynced;
    private boolean lastAutoTransfer;
    private boolean autoTransferSynced;

    private FluidHatchMenu(int id, Inventory playerInventory, BlockPos pos, @Nullable FluidHatchBlockEntity blockEntity) {
        super(TYPE, id);
        this.pos = pos;
        this.owner = playerInventory.player;
        this.serverSide = !playerInventory.player.level().isClientSide;
        this.blockEntity = blockEntity;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COLS + INV_COLS,
                        INV_X + col * SLOT_SIZE, INV_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    public FluidHatchMenu(int id, Inventory playerInventory, FluidHatchBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity.getBlockPos(), blockEntity);
    }

    public static FluidHatchMenu create(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return new FluidHatchMenu(id, playerInventory, pos,
                blockEntity instanceof FluidHatchBlockEntity hatch ? hatch : null);
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    /** 是否是输出仓（决定客户端界面贴图与自动搬运方向）。 */
    public boolean isOutputHatch() {
        return blockEntity != null && blockEntity.isOutputHatch();
    }

    /** 流体槽数量。 */
    public int getTankCount() {
        return blockEntity != null ? blockEntity.getTankCount() : 1;
    }

    /** 单槽容量（mB），供界面按占比绘制流体。 */
    public int getCapacityPerTank() {
        return blockEntity != null ? blockEntity.getCapacityPerTank() : FluidHatchBlockEntity.CAPACITY;
    }

    /** 服务端每 tick：罐内流体或自动搬运开关变化时向打开界面的玩家发送同步包；首次广播强制同步一次。 */
    @Override
    public void broadcastChanges() {
        if (blockEntity != null && owner instanceof ServerPlayer serverPlayer) {
            List<FluidStack> current = blockEntity.getFluidSnapshots();
            boolean autoTransfer = blockEntity.isAutoTransfer();
            if (!fluidSynced || !autoTransferSynced || autoTransfer != lastAutoTransfer
                    || !sameFluids(current, lastSentFluids)) {
                fluidSynced = true;
                autoTransferSynced = true;
                lastSentFluids = current;
                lastAutoTransfer = autoTransfer;
                ModNetwork.sendToPlayer(serverPlayer,
                        new MachinePartFluidPacket(pos, lastSentFluids, autoTransfer));
            }
        }
        super.broadcastChanges();
    }

    /** 工具栏按钮点击：id 0 = 切换自动搬运。 */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && serverSide && blockEntity != null) {
            blockEntity.setAutoTransfer(!blockEntity.isAutoTransfer());
            return true;
        }
        return false;
    }

    private static boolean sameFluids(List<FluidStack> a, @Nullable List<FluidStack> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!sameFluid(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameFluid(FluidStack a, FluidStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.getAmount() == b.getAmount() && a.isFluidEqual(b);
    }

    /**
     * 流体槽点击（由客户端点击包调用）：
     *
     * @param tank   罐序号（石英仓只有一个）
     * @param button 0 = 左键（从罐内取出），1 = 右键（存入罐内）
     */
    public void handleTankClick(int tank, int button) {
        if (!serverSide || blockEntity == null || tank < 0 || tank >= blockEntity.getTankCount()) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }
        if (button == 0) {
            takeFluid(carried, tank);
        } else if (button == 1) {
            if (!blockEntity.acceptsPlayerInsert()) {
                return; // 输出仓仅接受机器内部产出
            }
            storeFluid(carried, tank);
        }
        broadcastChanges();
    }

    /** 右键：用光标上的流体容器向罐内存入流体。 */
    private void storeFluid(ItemStack carried, int tank) {
        ItemStack single = carried.copyWithCount(1);
        IFluidHandlerItem handler = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) {
            return;
        }
        FluidStack drained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return;
        }
        int accepted = blockEntity.getTank(tank).fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack moved = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        blockEntity.getTank(tank).fill(moved, IFluidHandler.FluidAction.EXECUTE);
        replaceCarried(carried, handler.getContainer());
    }

    /** 左键：把罐内流体装入光标上的流体容器。 */
    private void takeFluid(ItemStack carried, int tank) {
        ItemStack single = carried.copyWithCount(1);
        IFluidHandlerItem handler = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) {
            return;
        }
        FluidStack stored = blockEntity.getTank(tank).getFluid();
        if (stored.isEmpty()) {
            return;
        }
        int filled = handler.fill(stored.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) {
            return;
        }
        handler.fill(stored.copy(), IFluidHandler.FluidAction.EXECUTE);
        blockEntity.getTank(tank).drain(filled, IFluidHandler.FluidAction.EXECUTE);
        replaceCarried(carried, handler.getContainer());
    }

    /** 用转移后的容器替换光标物品；整堆时把结果塞进玩家背包，放不下则丢到脚边。 */
    private void replaceCarried(ItemStack carried, ItemStack result) {
        if (carried.getCount() <= 1) {
            setCarried(result);
            return;
        }
        carried.shrink(1);
        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                new PlayerMainInvWrapper(owner.getInventory()), result, false);
        if (!remainder.isEmpty()) {
            owner.drop(remainder, false);
        }
    }

    /** 界面无机器槽位，Shift 点击不参与搬运。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0;
    }
}
