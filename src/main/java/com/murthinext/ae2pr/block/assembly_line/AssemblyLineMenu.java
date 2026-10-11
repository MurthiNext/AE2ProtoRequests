package com.murthinext.ae2pr.block.assembly_line;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.SlotItemHandler;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.AssemblyLineJobPacket;

/**
 * 水晶装配线主机的容器菜单。
 */
public class AssemblyLineMenu extends AbstractContainerMenu {

    public static final MenuType<AssemblyLineMenu> TYPE = IForgeMenuType.create(AssemblyLineMenu::new);

    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;
    private static final int SLOT = 18;

    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private static final int UPGRADE_SLOTS = 4;
    private static final int UPGRADE_X = 180;
    private static final int UPGRADE_Y = 4;
    private static final int UPGRADE_STEP = 20;

    private final AssemblyLineControllerBlockEntity controller;
    private final Player owner;
    private final SimpleContainerData data = new SimpleContainerData(8);
    private final boolean clientSide;
    private ItemStack lastJobOutput = ItemStack.EMPTY;
    private boolean jobOutputSynced;

    public AssemblyLineMenu(int id, Inventory playerInventory, AssemblyLineControllerBlockEntity controller) {
        super(TYPE, id);
        this.controller = controller;
        this.owner = playerInventory.player;
        this.clientSide = playerInventory.player.level().isClientSide;

        if (controller != null) {
            var upgrades = controller.getUpgrades().toItemHandler();
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                addSlot(new SlotItemHandler(upgrades, i, UPGRADE_X, UPGRADE_Y + i * UPGRADE_STEP));
            }
        }
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                addSlot(new Slot(playerInventory, col + row * INV_COLS + INV_COLS,
                        INV_X + col * SLOT, INV_Y + row * SLOT));
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public AssemblyLineMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, (AssemblyLineControllerBlockEntity) playerInventory.player.level()
                .getBlockEntity(buffer.readBlockPos()));
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            int flags = (controller.isFormed() ? 1 : 0) | (controller.isRunning() ? 2 : 0)
                    | (controller.isPaused() ? 4 : 0);
            long power = (long) Math.min(Math.max(controller.getNetworkStoredPower(), 0), Long.MAX_VALUE);
            data.set(0, flags);
            data.set(1, controller.isEnergyConnected() ? 1 : 0);
            data.set(2, (int) (power & 0xFFFFFFFFL));
            data.set(3, (int) (power >>> 32));
            data.set(4, controller.getError().ordinal());
            data.set(5, controller.getJobElapsed());
            data.set(6, controller.getJobDuration());
            data.set(7, controller.maxParallel());

            ItemStack jobOutput = controller.getJobOutput();
            if (owner instanceof ServerPlayer serverPlayer
                    && (!jobOutputSynced || !ItemStack.matches(jobOutput, lastJobOutput))) {
                jobOutputSynced = true;
                lastJobOutput = jobOutput.copy();
                ModNetwork.sendToPlayer(serverPlayer,
                        new AssemblyLineJobPacket(controller.getBlockPos(), lastJobOutput));
            }
        }
        super.broadcastChanges();
    }

    public boolean isFormed() {
        return (data.get(0) & 1) != 0;
    }

    public boolean isRunning() {
        return (data.get(0) & 2) != 0;
    }

    public boolean isPaused() {
        return (data.get(0) & 4) != 0;
    }

    public int getErrorCode() {
        return data.get(4);
    }

    public boolean isEnergyConnected() {
        return (data.get(1) & 1) != 0;
    }

    public long getNetworkStoredPower() {
        return (data.get(2) & 0xFFFFFFFFL) | ((long) data.get(3) << 32);
    }

    public ItemStack getJobOutput() {
        return controller != null ? controller.getClientJobOutput() : ItemStack.EMPTY;
    }

    public int getJobElapsed() {
        return data.get(5);
    }

    public int getJobDuration() {
        return data.get(6);
    }

    public int getMaxParallel() {
        return data.get(7);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < UPGRADE_SLOTS) {
            if (!moveItemStackTo(stack, UPGRADE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, UPGRADE_SLOTS, false)) {
            // 玩家背包 -> 升级槽
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return controller != null && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }
}
