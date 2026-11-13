package com.murthinext.ae2pr.requester.abstraction;

import net.minecraft.world.item.ItemStack;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.stacks.GenericStack;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.Requests;

public final class RequesterMenuActions {

    private RequesterMenuActions() {
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void apply(AEBaseMenu menu, Requests requests, InventoryAction action, int index) {
        if (!requests.isValidIndex(index)) {
            ae2pr.LOGGER.warn("Requester menu refers to invalid slot {} of {}", index, requests.getDescription());
            return;
        }

        var slot = requests.getSlotInv(index);
        var carried = menu.getCarried();
        switch (action) {
        case PICKUP_OR_SET_DOWN -> slot.setItemDirect(0, carried.copy());
        case SPLIT_OR_PLACE_SINGLE -> slot.setItemDirect(0, carried.copyWithCount(1));
        case SHIFT_CLICK -> slot.setItemDirect(0, ItemStack.EMPTY);
        case EMPTY_ITEM -> {
            var emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
            if (emptyingAction != null) {
                slot.insertItem(0, GenericStack.wrapInItemStack(emptyingAction.what(), emptyingAction.maxAmount()),
                                false);
            }
        }
        case CREATIVE_DUPLICATE -> {
            if (menu.getPlayer().getAbilities().instabuild && carried.isEmpty()) {
                var stack = slot.getStackInSlot(0);
                menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
            }
        }
        default -> {
        }
        }
    }
}
