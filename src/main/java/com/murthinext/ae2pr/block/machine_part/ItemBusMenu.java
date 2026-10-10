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
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemHandlerHelper;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.MachinePartStackPacket;

/**
 * 机器部件物品总线的容器菜单。
 * <p>
 * 存储量超过原版槽位同步上限（数量按 byte 传输），因此不占槽位：
 * 展示由 {@link MachinePartStackPacket} 同步，操作由点击包驱动（见 {@link #handleStorageClick}）。
 */
public class ItemBusMenu extends AbstractContainerMenu {

    public static final MenuType<ItemBusMenu> TYPE = IForgeMenuType.create(ItemBusMenu::create);

    private static final int INV_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private final BlockPos pos;
    private final Player owner;
    private final boolean serverSide;
    private final ItemBusBlockEntity blockEntity;
    private List<ItemStack> lastSentStacks;
    private boolean stackSynced;
    private boolean lastAutoTransfer;
    private boolean autoTransferSynced;

    private ItemBusMenu(int id, Inventory playerInventory, BlockPos pos, @Nullable ItemBusBlockEntity blockEntity) {
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

    public ItemBusMenu(int id, Inventory playerInventory, ItemBusBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity.getBlockPos(), blockEntity);
    }

    public static ItemBusMenu create(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return new ItemBusMenu(id, playerInventory, pos,
                blockEntity instanceof ItemBusBlockEntity bus ? bus : null);
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    /** 是否是输出总线（决定客户端界面贴图）。 */
    public boolean isOutputBus() {
        return blockEntity != null && blockEntity.isOutputBus();
    }

    /** 物品槽数量。 */
    public int getSlotCount() {
        return blockEntity != null ? blockEntity.getSlotCount() : 1;
    }

    /** 指定槽位的机器存储内容（数量即储量；客户端为同步数据）。 */
    public ItemStack getStoredStack(int slot) {
        return blockEntity != null ? blockEntity.getStorage().getStackInSlot(slot) : ItemStack.EMPTY;
    }

    /** 服务端每 tick：存储内容或自动搬运开关变化时向打开界面的玩家发送同步包；首次广播强制同步一次。 */
    @Override
    public void broadcastChanges() {
        if (blockEntity != null && owner instanceof ServerPlayer serverPlayer) {
            List<ItemStack> current = blockEntity.getStorageSnapshots();
            boolean autoTransfer = blockEntity.isAutoTransfer();
            if (!stackSynced || !autoTransferSynced || autoTransfer != lastAutoTransfer
                    || !sameStacks(current, lastSentStacks)) {
                stackSynced = true;
                autoTransferSynced = true;
                lastSentStacks = current;
                lastAutoTransfer = autoTransfer;
                ModNetwork.sendToPlayer(serverPlayer,
                        new MachinePartStackPacket(pos, lastSentStacks, autoTransfer));
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

    private static boolean sameStacks(List<ItemStack> a, @Nullable List<ItemStack> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 存储区点击（由客户端点击包调用）：
     *
     * @param slot   槽位序号
     * @param button 0 = 左键，1 = 右键
     */
    public void handleStorageClick(int slot, int button, boolean shift) {
        if (!serverSide || blockEntity == null || slot < 0 || slot >= blockEntity.getSlotCount()) {
            return;
        }
        if (shift) {
            // 单次抽取最多只能取走物品的堆叠上限（64），循环取到背包放不下或该槽清空
            while (true) {
                ItemStack taken = blockEntity.getStorage().extractItem(slot, Integer.MAX_VALUE, false);
                if (taken.isEmpty()) {
                    break;
                }
                if (!moveItemStackTo(taken, 0, slots.size(), true) || !taken.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(blockEntity.getStorage(), taken, false);
                    break;
                }
            }
        } else if (!getCarried().isEmpty()) {
            if (!blockEntity.acceptsPlayerInsert()) {
                return; // 输出总线仅接受配方输出
            }
            ItemStack carried = getCarried();
            int count = button == 1 ? 1 : carried.getCount();
            ItemStack remainder = blockEntity.getStorage().insertItem(slot, carried.copyWithCount(count), false);
            carried.shrink(count - remainder.getCount());
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else {
            ItemStack taken = blockEntity.getStorage().extractItem(slot, button == 1 ? 1 : 64, false);
            if (!taken.isEmpty()) {
                setCarried(taken);
            }
        }
        broadcastChanges();
    }

    /** Shift 点击玩家背包：把该格存入总线（仅同种物品可叠加；输出总线不接收玩家存入）。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !serverSide || blockEntity == null || !blockEntity.acceptsPlayerInsert()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem().copy();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(blockEntity.getStorage(),
                slot.getItem().copy(), false);
        if (remainder.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.set(remainder);
        slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0;
    }
}
