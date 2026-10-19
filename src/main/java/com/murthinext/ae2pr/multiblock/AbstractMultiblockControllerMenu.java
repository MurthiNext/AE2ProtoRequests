package com.murthinext.ae2pr.multiblock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import appeng.api.upgrades.Upgrades;

/**
 * 多方块控制器菜单的公共基底：升级槽、玩家背包与状态同步。
 */
public abstract class AbstractMultiblockControllerMenu<T extends AbstractMultiblockControllerBlockEntity>
        extends AbstractContainerMenu {

    protected static final int UPGRADE_SLOTS = 4;
    protected static final int UPGRADE_X = 180;
    protected static final int UPGRADE_Y = 4;
    protected static final int UPGRADE_STEP = 20;

    protected static final int DATA_FLAGS = 0;
    protected static final int DATA_ERROR = 1;
    protected static final int DATA_ELAPSED_LO = 2;
    protected static final int DATA_ELAPSED_HI = 3;
    protected static final int DATA_DURATION_LO = 4;
    protected static final int DATA_DURATION_HI = 5;
    protected static final int DATA_ENERGY_CONNECTED = 6;
    protected static final int DATA_POWER_LO = 7;
    protected static final int DATA_POWER_HI = 8;
    protected static final int DATA_PARALLEL_LO = 9;
    protected static final int DATA_PARALLEL_HI = 10;
    protected static final int DATA_COUNT = 11;

    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;
    private static final int SLOT = 18;
    private static final int INV_X = 8;
    private static final int INV_Y = 122;
    private static final int HOTBAR_Y = 180;

    private static final int FLAG_FORMED = 1;
    private static final int FLAG_RUNNING = 2;
    private static final int FLAG_PAUSED = 4;
    private static final int FLAG_BUSY = 8;

    protected final T controller;
    protected final boolean clientSide;
    private final SimpleContainerData data;
    private final int playerSlotFirst;

    protected AbstractMultiblockControllerMenu(MenuType<?> type, int id, Inventory playerInventory, T controller,
            int extraDataSlots) {
        super(type, id);
        this.controller = controller;
        this.clientSide = playerInventory.player.level().isClientSide;
        this.data = new SimpleContainerData(DATA_COUNT + extraDataSlots);
        this.playerSlotFirst = UPGRADE_SLOTS + extraSlots();

        if (controller != null) {
            IItemHandler upgrades = controller.getUpgrades().toItemHandler();
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                addSlot(new SlotItemHandler(upgrades, i, UPGRADE_X, UPGRADE_Y + i * UPGRADE_STEP));
            }
            addExtraSlots(controller);
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

    protected int extraSlots() {
        return 0;
    }

    protected void addExtraSlots(T controller) {
    }

    protected void writeExtraData(SimpleContainerData data) {
    }

    protected SimpleContainerData data() {
        return data;
    }

    // 写入一个跨越两个 16 位容器的整数
    protected static void setIntData(SimpleContainerData data, int index, int value) {
        data.set(index, value & 0xFFFF);
        data.set(index + 1, (value >>> 16) & 0xFFFF);
    }

    protected static int getIntData(SimpleContainerData data, int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            data.set(DATA_FLAGS, (controller.isFormed() ? FLAG_FORMED : 0)
                    | (controller.isRunning() ? FLAG_RUNNING : 0)
                    | (controller.isPaused() ? FLAG_PAUSED : 0)
                    | (controller.isBusy() ? FLAG_BUSY : 0));
            data.set(DATA_ERROR, controller.getError().ordinal());
            setIntData(data, DATA_ELAPSED_LO, controller.getJobElapsed());
            setIntData(data, DATA_DURATION_LO, controller.getJobDuration());
            data.set(DATA_ENERGY_CONNECTED, controller.isEnergyConnected() ? 1 : 0);
            long power = (long) Math.min(Math.max(controller.getNetworkStoredPower(), 0), Long.MAX_VALUE);
            data.set(DATA_POWER_LO, (int) (power & 0xFFFFFFFFL));
            data.set(DATA_POWER_HI, (int) (power >>> 32));
            setIntData(data, DATA_PARALLEL_LO, controller.maxParallel());
            writeExtraData(data);
        }
        super.broadcastChanges();
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

    public boolean isBusy() {
        return (data.get(DATA_FLAGS) & FLAG_BUSY) != 0;
    }

    public int getErrorCode() {
        return data.get(DATA_ERROR);
    }

    public int getJobElapsed() {
        return getIntData(data, DATA_ELAPSED_LO);
    }

    public int getJobDuration() {
        return getIntData(data, DATA_DURATION_LO);
    }

    public boolean isEnergyConnected() {
        return (data.get(DATA_ENERGY_CONNECTED) & 1) != 0;
    }

    public long getNetworkStoredPower() {
        return (data.get(DATA_POWER_LO) & 0xFFFFFFFFL) | ((long) data.get(DATA_POWER_HI) << 32);
    }

    public int getMaxParallel() {
        return getIntData(data, DATA_PARALLEL_LO);
    }

    public String machineKey() {
        return controller != null ? controller.machineKey() : "";
    }

    public ItemStack getJobItem() {
        return controller != null ? controller.getClientJobItem() : ItemStack.EMPTY;
    }

    public FluidStack getJobFluid() {
        return controller != null ? controller.getClientJobFluid() : FluidStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < playerSlotFirst) {
            if (!moveItemStackTo(stack, playerSlotFirst, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!Upgrades.isUpgradeCardItem(stack) || !moveItemStackTo(stack, 0, UPGRADE_SLOTS, false)) {
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
        return controller != null && !controller.isRemoved()
                && controller.getLevel() == player.level()
                && player.level().getBlockEntity(controller.getBlockPos()) == controller
                && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }

    /**
     * 读取缓冲区中的控制器引用。
     */
    @Nullable
    protected static <C> C readController(Player player, FriendlyByteBuf buffer, Class<C> type) {
        var blockEntity = player.level().getBlockEntity(buffer.readBlockPos());
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }
}


