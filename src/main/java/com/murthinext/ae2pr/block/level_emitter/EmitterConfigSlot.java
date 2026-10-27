package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.FakeSlot;

import com.murthinext.ae2pr.item.filter_cell.FilterCell;

public class EmitterConfigSlot extends FakeSlot {

    private final InternalInventory filterInventory;

    public EmitterConfigSlot(InternalInventory configInventory, InternalInventory filterInventory) {
        super(configInventory, 0);
        this.filterInventory = filterInventory;
    }

    private ItemStack getFilterCell() {
        return filterInventory.getStackInSlot(0);
    }

    public ItemStack getFilterCellStack() {
        var cell = getFilterCell();
        return cell.isEmpty() ? null : cell;
    }

    public void clearFilterCell() {
        filterInventory.setItemDirect(0, ItemStack.EMPTY);
    }

    private boolean isClientSide() {
        var menu = getMenu();
        return menu != null && menu.isClientSide();
    }

    @Override
    public ItemStack getItem() {
        var cell = getFilterCell();
        return cell.isEmpty() ? super.getItem() : cell;
    }

    @Override
    public void set(ItemStack stack) {
        if (stack.getItem() instanceof FilterCell) {
            // 仅客户端同步显示，服务端插入走 increase
            if (isClientSide()) {
                filterInventory.setItemDirect(0, stack.copy());
            }
            return;
        }

        if (!getFilterCell().isEmpty()) {
            if (!isClientSide()) {
                // 已插入过滤元件时不再接受标记
                return;
            }
            filterInventory.setItemDirect(0, ItemStack.EMPTY);
        }

        super.set(stack);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getFilterCell().isEmpty() || super.mayPickup(player);
    }

    @Override
    public ItemStack remove(int amount) {
        var cell = getFilterCell();
        if (!cell.isEmpty()) {
            filterInventory.setItemDirect(0, ItemStack.EMPTY);
            return cell.copy();
        }
        return super.remove(amount);
    }

    @Override
    public void increase(ItemStack hand) {
        if (!getFilterCell().isEmpty()) {
            if (hand.isEmpty()) {
                pickUpFilterCell();
            }
            return;
        }
        if (hand.getItem() instanceof FilterCell) {
            insertFilterCell(hand);
            return;
        }
        super.increase(hand);
    }

    @Override
    public void decrease(ItemStack hand) {
        if (!getFilterCell().isEmpty()) {
            if (hand.isEmpty()) {
                pickUpFilterCell();
            }
            return;
        }
        super.decrease(hand);
    }

    @Override
    public boolean canSetFilterTo(ItemStack stack) {
        return !(stack.getItem() instanceof FilterCell) && super.canSetFilterTo(stack);
    }

    public boolean insertFromShiftClick(Slot source) {
        if (!getFilterCell().isEmpty()) {
            return false;
        }
        var stack = source.getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof FilterCell)) {
            return false;
        }
        // 插入过滤元件时清除原有标记
        super.set(ItemStack.EMPTY);
        filterInventory.setItemDirect(0, stack.copyWithCount(1));
        stack.shrink(1);
        if (stack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        }
        source.setChanged();
        setChanged();
        return true;
    }

    private void insertFilterCell(ItemStack hand) {
        var menu = getMenu();
        if (menu == null) {
            return;
        }
        // 用菜单真实光标，保证从光标消耗 1 个
        var carried = menu.getCarried();
        if (carried.isEmpty()) {
            carried = hand;
        }
        // 插入过滤元件时清除原有标记
        super.set(ItemStack.EMPTY);
        filterInventory.setItemDirect(0, carried.copyWithCount(1));
        carried.shrink(1);
        menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        setChanged();
    }

    private void pickUpFilterCell() {
        var menu = getMenu();
        var cell = getFilterCell();
        if (menu == null || cell.isEmpty()) {
            return;
        }
        filterInventory.setItemDirect(0, ItemStack.EMPTY);
        menu.setCarried(cell.copy());
        setChanged();
    }
}
