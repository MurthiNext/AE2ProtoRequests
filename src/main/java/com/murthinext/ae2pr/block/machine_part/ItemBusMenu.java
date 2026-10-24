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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.client.DisplaySlot;
import com.murthinext.ae2pr.network.MachinePartFluidPacket;
import com.murthinext.ae2pr.network.MachinePartStackPacket;

/**
 * 机器部件物品总线的容器菜单：存储经只读槽位展示，数量由界面层绘制。
 */
public class ItemBusMenu extends AbstractContainerMenu {

    public static final MenuType<ItemBusMenu> TYPE = IForgeMenuType.create(ItemBusMenu::create);

    private static final int INV_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;
    // 单次点击搬运上限
    private static final int MAX_CLICK_TRANSFER = 64;

    private final BlockPos pos;
    private final Player owner;
    private final boolean serverSide;
    private final ItemBusBlockEntity blockEntity;
    private final int playerSlotFirst;
    private List<ItemStack> lastSentStacks;
    private List<FluidStack> lastSentFluids;
    private boolean stackSynced;
    private boolean lastAutoTransfer;
    private boolean autoTransferSynced;

    private ItemBusMenu(int id, Inventory playerInventory, BlockPos pos, @Nullable ItemBusBlockEntity blockEntity) {
        super(TYPE, id);
        this.pos = pos;
        this.owner = playerInventory.player;
        this.serverSide = !playerInventory.player.level().isClientSide;
        this.blockEntity = blockEntity;
        this.playerSlotFirst = getSlotCount();
        boolean dual = getFluidStorage() != null;
        for (int slot = 0; slot < playerSlotFirst; slot++) {
            int index = slot;
            // 槽位只画单个图标，数量由界面绘制
            addSlot(new DisplaySlot(() -> getStoredStack(index),
                    MachinePartLayout.itemSlotX(dual, playerSlotFirst, slot),
                    MachinePartLayout.itemSlotY(dual, slot)));
        }
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

    public boolean isOutputBus() {
        return blockEntity != null && blockEntity.isOutputBus();
    }

    public int getSlotCount() {
        return blockEntity != null ? blockEntity.getSlotCount() : 1;
    }

    @Nullable
    public MachinePartFluidStorage getFluidStorage() {
        return blockEntity instanceof DualHatchBlockEntity dual ? dual.getFluidStorage() : null;
    }

    public void handleTankClick(int tank, int button) {
        MachinePartFluidStorage storage = getFluidStorage();
        if (serverSide && storage != null) {
            FluidHatchMenu.handleTankClick(this, owner, storage, tank, button);
        }
    }

    public ItemStack getStoredStack(int slot) {
        return blockEntity != null ? blockEntity.getStorage().getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public int getStorageSlotIndex(int menuSlot) {
        return menuSlot >= 0 && menuSlot < playerSlotFirst ? menuSlot : -1;
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && owner instanceof ServerPlayer serverPlayer) {
            List<ItemStack> current = blockEntity.getStorageSnapshots();
            boolean autoTransfer = blockEntity.isAutoTransfer();
            MachinePartFluidStorage storage = getFluidStorage();
            if (storage != null) {
                List<FluidStack> fluids = storage.getFluidSnapshots();
                if (!autoTransferSynced || autoTransfer != lastAutoTransfer
                        || !FluidHatchMenu.sameFluids(fluids, lastSentFluids)) {
                    lastSentFluids = fluids;
                    ModNetwork.sendToPlayer(serverPlayer,
                            new MachinePartFluidPacket(pos, fluids, autoTransfer));
                }
            }
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

    /**
     * 工具栏按钮 id 0 = 切换自动搬运。
     */
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

    // 单次点击可搬运的数量
    private static int clickTransferLimit(ItemStack stack) {
        return Math.min(MAX_CLICK_TRANSFER, stack.getMaxStackSize());
    }

    public void handleStorageClick(int slot, int button, boolean shift) {
        if (!serverSide || blockEntity == null || slot < 0 || slot >= blockEntity.getSlotCount()) {
            return;
        }
        if (shift) {
            ItemStack stored = getStoredStack(slot);
            ItemStack taken = blockEntity.getStorage().extractItem(slot,
                    stored.isEmpty() ? MAX_CLICK_TRANSFER : clickTransferLimit(stored), false);
            if (!taken.isEmpty()) {
                ItemStack remainder = taken.copy();
                moveItemStackTo(remainder, playerSlotFirst, slots.size(), true);
                if (!remainder.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(blockEntity.getStorage(), remainder, false);
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
            ItemStack stored = getStoredStack(slot);
            int amount = button == 1 ? 1 : clickTransferLimit(stored);
            ItemStack taken = blockEntity.getStorage().extractItem(slot, amount, false);
            if (!taken.isEmpty()) {
                setCarried(taken);
            }
        }
        broadcastChanges();
    }

    public void handleCreativeClone(int slot, Player player) {
        if (!serverSide || blockEntity == null || !player.getAbilities().instabuild
                || !player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        ItemStack stored = getStoredStack(slot);
        if (!stored.isEmpty()) {
            setCarried(stored.copyWithCount(clickTransferLimit(stored)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < playerSlotFirst) {
            return ItemStack.EMPTY;
        }
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
