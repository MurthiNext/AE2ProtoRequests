/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.client.abstraction;

import com.murthinext.ae2pr.requester.client.RequestSlot;
import com.murthinext.ae2pr.requester.Requests.Request;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface RequestDisplay {
    void addSubWidget(String id, AbstractWidget widget, Map<String, AbstractWidget> subWidgets);

    @Nullable
    Request getTargetRequest(int listIndex);

    @Nullable
    List<Component> getEmptyingTooltip(RequestSlot slot, ItemStack carried);
}
