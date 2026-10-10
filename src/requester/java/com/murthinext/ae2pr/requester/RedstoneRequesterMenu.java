/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester;

import appeng.api.networking.IGrid;
import appeng.menu.implementations.MenuTypeBuilder;
import com.murthinext.ae2pr.requester.RequesterConstants;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;
import com.murthinext.ae2pr.requester.abstraction.AbstractRedstoneRequesterMenu;
import com.murthinext.ae2pr.requester.abstraction.RequestTracker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class RedstoneRequesterMenu extends AbstractRedstoneRequesterMenu {

    public static final MenuType<RedstoneRequesterMenu> TYPE = MenuTypeBuilder
        .create(RedstoneRequesterMenu::new, RedstoneRequesterBlockEntity.class)
        .build(RequesterConstants.REQUESTER_ID);

    @Nullable private RequestTracker requestTracker;

    private RedstoneRequesterMenu(int id, Inventory playerInventory, RedstoneRequesterBlockEntity host) {
        super(TYPE, id, playerInventory, host);
    }

    @Override
    public void broadcastChanges() {
        if (isClientSide()) return;
        super.broadcastChanges();
        if (requestTracker == null) {
            sendFullUpdate(null);
        } else {
            sendPartialUpdate();
        }
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack stack) {
        assert requestTracker != null;
        var firstAvailable = requestTracker.getServer().firstAvailableIndex();
        if (firstAvailable != -1) {
            requestTracker.getServer().insertItem(firstAvailable, stack, false);
        }
        return stack;
    }

    @Override
    protected void sendFullUpdate(@Nullable IGrid grid) {
        RequesterPlatform.sendClearData(getPlayer());
        requestTracker = createTracker((RedstoneRequesterBlockEntity) getBlockEntity());
        syncRequestTrackerFull(requestTracker);
    }

    @Override
    protected void sendPartialUpdate() {
        assert requestTracker != null;
        syncRequestTrackerPartial(requestTracker);
    }

    @Nullable
    @Override
    protected RequestTracker getRequestTracker(long id) {
        return requestTracker;
    }
}
