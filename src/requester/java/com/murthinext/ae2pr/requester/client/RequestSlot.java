/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.client;

import appeng.menu.slot.FakeSlot;
import com.murthinext.ae2pr.requester.RequesterUtils;
import com.murthinext.ae2pr.requester.client.abstraction.RequestDisplay;
import com.murthinext.ae2pr.requester.client.abstraction.RequesterReference;
import com.murthinext.ae2pr.requester.mixin.SlotMixin;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class RequestSlot extends FakeSlot {

    private final RequestDisplay host;
    private final RequesterReference requesterReference;
    private final int slot;

    private boolean isLocked;

    public RequestSlot(RequestDisplay host, RequesterReference requesterReference, int slot, int x, int y) {
        super(requesterReference.getRequests(), slot);
        this.host = host;
        this.requesterReference = requesterReference;
        this.slot = slot;
        RequesterUtils.cast(this, SlotMixin.class).ae2pr$setX(x);
        RequesterUtils.cast(this, SlotMixin.class).ae2pr$setY(y);
    }

    @Override
    public void increase(ItemStack is) {}

    @Override
    public void decrease(ItemStack is) {}

    @Override
    public boolean hasItem() {
        // hide item tooltip when locked
        return !isLocked && super.hasItem();
    }

    @Nullable
    @Override
    public List<Component> getCustomTooltip(ItemStack carried) {
        if (isLocked) {
            return Collections.singletonList(RequesterUtils.translate("tooltip", "locked").withStyle(ChatFormatting.RED));
        }
        // custom tooltip for fluid containers
        var emptyingTooltip = host.getEmptyingTooltip(this, carried);
        if (emptyingTooltip == null) return super.getCustomTooltip(carried);
        return emptyingTooltip;
    }

    @Override
    public final int getMaxStackSize() {
        return 0;
    }

    public RequesterReference getRequesterReference() {
        return requesterReference;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }
    
    @Override
    public boolean canSetFilterTo(ItemStack stack) {
        return isLocked ? false : super.canSetFilterTo(stack);
    }

    @Override
    public void setFilterTo(ItemStack stack) {
        RequesterPlatform.sendDragAndDrop(requesterReference.getRequesterId(), slot, stack);
    }
}
