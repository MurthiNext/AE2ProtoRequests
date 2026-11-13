package com.murthinext.ae2pr.requester.abstraction;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;

import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;

public abstract class AbstractRedstoneRequesterMenu extends AEBaseMenu {

    private long idSerial = Long.MIN_VALUE;

    protected AbstractRedstoneRequesterMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
        createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        var requestTracker = getRequestTracker(id);
        if (requestTracker != null) {
            RequesterMenuActions.apply(this, requestTracker.getServer(), action, slot);
        }
    }

    public void applyDragAndDrop(ServerPlayer player, int requestIndex, long requesterId, ItemStack item) {
        var carried = getCarried();
        try {
            setCarried(item);
            doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, requestIndex, requesterId);
        } finally {
            setCarried(carried);
        }
    }

    public void updateRequesterState(long requesterId, int requestIndex, boolean state) {
        var requestTracker = getRequestTracker(requesterId);
        if (requestTracker == null || !requestTracker.getServer().isValidIndex(requestIndex)) return;
        var request = requestTracker.getServer().get(requestIndex);
        request.updateState(state);
    }

    public void updateRequesterNumbers(long requesterId, int requestIndex, long amount) {
        var requestTracker = getRequestTracker(requesterId);
        if (requestTracker == null || !requestTracker.getServer().isValidIndex(requestIndex)) return;
        var request = requestTracker.getServer().get(requestIndex);
        request.updateAmount(amount);
    }

    protected abstract void sendFullUpdate(@Nullable IGrid grid);

    protected abstract void sendPartialUpdate();

    protected void syncRequestTrackerFull(RequestTracker requestTracker) {
        RequesterPlatform.sendInventoryData(getPlayer(), requestTracker.getId(), requestTracker.createFullUpdate());
    }

    protected void syncRequestTrackerPartial(RequestTracker requestTracker) {
        var tag = requestTracker.createPartialUpdate();
        if (tag != null) {
            RequesterPlatform.sendInventoryData(getPlayer(), requestTracker.getId(), tag);
        }
    }

    protected RequestTracker createTracker(RedstoneRequesterBlockEntity requester) {
        return new RequestTracker(requester, idSerial++);
    }

    @Nullable
    protected abstract RequestTracker getRequestTracker(long id);
}
