package com.murthinext.ae2pr.block.crystal_workstation;

import net.minecraft.network.FriendlyByteBuf;
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

public class CrystalWorkstationMenu extends AbstractContainerMenu {

    public static final MenuType<CrystalWorkstationMenu> TYPE = IForgeMenuType.create(CrystalWorkstationMenu::new);
    public static final int SWITCH_MODE = 0;

    private static final int FLAGS = 0;
    private static final int ERROR = 1;
    private static final int ELAPSED = 2;
    private static final int DURATION = 4;
    private static final int POWER_LO = 6;
    private static final int POWER_HI = 8;
    private static final int PARALLEL = 10;
    private static final int MODE = 12;
    private static final int UPGRADE_SLOTS = 4;

    private final CrystalWorkstationControllerBlockEntity controller;
    private final SimpleContainerData data = new SimpleContainerData(13);
    private final boolean clientSide;

    public CrystalWorkstationMenu(int id, Inventory inventory, CrystalWorkstationControllerBlockEntity controller) {
        super(TYPE, id);
        this.controller = controller;
        this.clientSide = inventory.player.level().isClientSide;
        if (controller != null) {
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                addSlot(new SlotItemHandler(controller.getUpgrades().toItemHandler(), i, 180, 4 + i * 20));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
        addDataSlots(data);
    }

    public CrystalWorkstationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level().getBlockEntity(buffer.readBlockPos())
                instanceof CrystalWorkstationControllerBlockEntity workstation ? workstation : null);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || id != SWITCH_MODE || !stillValid(player) || !controller.switchMode()) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide && controller != null) {
            data.set(FLAGS, (controller.isFormed() ? 1 : 0) | (controller.isRunning() ? 2 : 0)
                    | (controller.isPaused() ? 4 : 0) | (controller.isBusy() ? 8 : 0)
                    | (controller.isEnergyConnected() ? 16 : 0));
            data.set(ERROR, controller.getError().ordinal());
            setIntData(ELAPSED, controller.getJobElapsed());
            setIntData(DURATION, controller.getJobDuration());
            long power = (long) Math.max(0, controller.getNetworkStoredPower());
            setIntData(POWER_LO, (int) power);
            setIntData(POWER_HI, (int) (power >>> 32));
            setIntData(PARALLEL, controller.maxParallel());
            data.set(MODE, controller.getMode().ordinal());
        }
        super.broadcastChanges();
    }

    // 原版容器数据包只传输 16 位数值。
    private void setIntData(int index, int value) {
        data.set(index, value & 0xFFFF);
        data.set(index + 1, value >>> 16);
    }

    private int getIntData(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public CrystalWorkstationMode getMode() {
        return data.get(MODE) == CrystalWorkstationMode.CUTTING.ordinal()
                ? CrystalWorkstationMode.CUTTING : CrystalWorkstationMode.ETCHING;
    }

    public boolean isFormed() {
        return (data.get(FLAGS) & 1) != 0;
    }

    public boolean isRunning() {
        return (data.get(FLAGS) & 2) != 0;
    }

    public boolean isPaused() {
        return (data.get(FLAGS) & 4) != 0;
    }

    public boolean isBusy() {
        return (data.get(FLAGS) & 8) != 0;
    }

    public boolean isEnergyConnected() {
        return (data.get(FLAGS) & 16) != 0;
    }

    public int getErrorCode() {
        return data.get(ERROR);
    }

    public int getJobElapsed() {
        return getIntData(ELAPSED);
    }

    public int getJobDuration() {
        return getIntData(DURATION);
    }

    public int getMaxParallel() {
        return getIntData(PARALLEL);
    }

    public long getNetworkStoredPower() {
        return (getIntData(POWER_LO) & 0xFFFFFFFFL) | ((long) getIntData(POWER_HI) << 32);
    }

    public ItemStack getJobItem() {
        return controller != null && isBusy() ? controller.getJobItem() : ItemStack.EMPTY;
    }

    public FluidStack getJobFluid() {
        return controller != null && isBusy() ? controller.getJobFluid() : FluidStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (controller == null || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
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
        return controller != null && !controller.isRemoved() && controller.getLevel() == player.level()
                && player.level().getBlockEntity(controller.getBlockPos()) == controller
                && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64.0;
    }
}
