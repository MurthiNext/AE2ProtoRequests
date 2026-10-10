/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.mixin;

import appeng.client.gui.WidgetContainer;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@SuppressWarnings("ALL")
@Mixin(WidgetContainer.class)
public interface WidgetContainerMixin {
    @Accessor(value = "widgets", remap = false)
    Map<String, AbstractWidget> ae2pr$getWidgets();
}
