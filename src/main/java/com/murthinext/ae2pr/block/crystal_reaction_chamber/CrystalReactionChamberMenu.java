package com.murthinext.ae2pr.block.crystal_reaction_chamber;

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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;

import appeng.api.upgrades.Upgrades;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.CrystalReactionChamberJobPacket;

/**
 * 水晶反应仓主机的容器菜单。
 */
public class CrystalReactionChamberMenu extends AbstractContainerMenu {

    public static final MenuType<CrystalReactionChamberMenu> TYPE = IForgeMenuType
            .create(CrystalReactionChamberMenu::new);

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
    private static final int PLAYER_SLOT_FIRST = UPGRADE_SLOTS;

    private static final int FLAG_FORMED = 1;
    private static final int FLAG_RUNNING = 2;
    private static final int FLAG_PAUSED = 4;

    private static final int DATA_FLAGS = 0;
    private static final int DATA_ERROR = 1;
    private static final int DATA_ELAPSED = 2;
    private static final int DATA_DURATION = 3;
    private static final int DATA_ENERGY_CONNECTED = 4;
    private static final int DATA_POWER_LO = 5;
    private static final int DATA_POWER_HI = 6;
    private static final int DATA_PARALLEL = 7;

    private final CrystalReactionChamberControllerBlockEntity controller;
    private final Player owner;
    private final SimpleContainerData data = new SimpleContainerData(8);
    private final boolean clientSide;
    private ItemStack lastJobItem = ItemStack.EMPTY;
    private FluidStack lastJobFluid = FluidStack.EMPTY;
    private boolean jobSynced;

    public CrystalReactionChamberMenu(int id, Inventory playerInventory,
            CrystalReactionChamberControllerBlockEntity controller) {
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

    public CrystalReactionChamberMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, (CrystalReactionChamberControllerBlockEntity) playerInventory.player.level()
                .getBlockEntity(buffer.readBlockPos()));
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            int flags = (controller.isFormed() ? FLAG_FORMED : 0)
                    | (controller.isRunning() ? FLAG_RUNNING : 0)
                    | (controller.isPaused() ? FLAG_PAUSED : 0);
            data.set(DATA_FLAGS, flags);
            data.set(DATA_ERROR, controller.getError().ordinal());
            data.set(DATA_ELAPSED, controller.getJobElapsed());
            data.set(DATA_DURATION, controller.getJobDuration());
            long power = (long) Math.min(Math.max(controller.getNetworkStoredPower(), 0), Long.MAX_VALUE);
            data.set(DATA_ENERGY_CONNECTED, controller.isEnergyConnected() ? 1 : 0);
            data.set(DATA_POWER_LO, (int) (power & 0xFFFFFFFFL));
            data.set(DATA_POWER_HI, (int) (power >>> 32));
            data.set(DATA_PARALLEL, controller.maxParallel());

            ItemStack jobItem = controller.getJobItem();
            FluidStack jobFluid = controller.getJobFluid();
            if (owner instanceof ServerPlayer serverPlayer
                    && (!jobSynced || !ItemStack.matches(jobItem, lastJobItem) || !sameFluid(jobFluid, lastJobFluid))) {
                jobSynced = true;
                lastJobItem = jobItem.copy();
                lastJobFluid = jobFluid.copy();
                ModNetwork.sendToPlayer(serverPlayer,
                        new CrystalReactionChamberJobPacket(controller.getBlockPos(), lastJobItem, lastJobFluid));
            }
        }
        super.broadcastChanges();
    }

    private static boolean sameFluid(FluidStack a, FluidStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.isFluidEqual(b) && a.getAmount() == b.getAmount();
    }

    public boolean isFormed() {
        return (data.get(DATA_FLAGS) & FLAG_FORMED) != 0;
    }

    public boolean isRunning() {
        return (data.get(DATA_FLAGS) & FLAG_RUNNING) != 0;
    }

    public boolean isPaused() {
        return (data.get(DATA_FLAGS) & FLAG_PAUSED) != 0;
    }

    public int getErrorCode() {
        return data.get(DATA_ERROR);
    }

    public int getJobElapsed() {
        return data.get(DATA_ELAPSED);
    }

    public int getJobDuration() {
        return data.get(DATA_DURATION);
    }

    public ItemStack getJobItem() {
        return controller != null ? controller.getClientJobItem() : ItemStack.EMPTY;
    }

    public FluidStack getJobFluid() {
        return controller != null ? controller.getClientJobFluid() : FluidStack.EMPTY;
    }

    public boolean isEnergyConnected() {
        return (data.get(DATA_ENERGY_CONNECTED) & 1) != 0;
    }

    public long getNetworkStoredPower() {
        return (data.get(DATA_POWER_LO) & 0xFFFFFFFFL) | ((long) data.get(DATA_POWER_HI) << 32);
    }

    public int getMaxParallel() {
        return data.get(DATA_PARALLEL);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_SLOT_FIRST) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_FIRST, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!Upgrades.isUpgradeCardItem(stack)
                || !moveItemStackTo(stack, 0, PLAYER_SLOT_FIRST, false)) {
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
