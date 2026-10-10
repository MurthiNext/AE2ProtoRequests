/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.guidebook.PageAnchor;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.requester.RequesterConstants;
import com.murthinext.ae2pr.requester.RequesterUtils;
import com.murthinext.ae2pr.requester.client.abstraction.AbstractRedstoneRequesterScreen;
import com.murthinext.ae2pr.requester.client.abstraction.RequesterReference;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;
import com.murthinext.ae2pr.requester.RedstoneRequesterMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static com.murthinext.ae2pr.requester.RequesterUtils.f;

public class RedstoneRequesterScreen extends AbstractRedstoneRequesterScreen<RedstoneRequesterMenu> {

    private static final ResourceLocation TEXTURE = RequesterUtils.getRL(f("textures/gui/{}.png", RequesterConstants.REQUESTER_ID));
    private static final Rect2i FOOTER_BBOX = new Rect2i(0, 114, GUI_WIDTH, GUI_FOOTER_HEIGHT);
    private static final int MAX_ROW_COUNT = 10;

    @Nullable private RequesterReference requesterReference;

    public RedstoneRequesterScreen(
        RedstoneRequesterMenu menu, Inventory playerInventory, Component name, ScreenStyle style
    ) {
        super(menu, playerInventory, name, style, TEXTURE);
    }

    @Override
    protected void init() {
        var possibleRows = (height - GUI_HEADER_HEIGHT - GUI_FOOTER_HEIGHT) / ROW_HEIGHT;
        rowAmount = Mth.clamp(possibleRows, MIN_ROW_COUNT, Math.min(RequesterPlatform.getRequestLimit(), MAX_ROW_COUNT));
        super.init();
    }

    @Override
    protected void clear() {}

    @Override
    protected void refreshList() {
        if (requesterReference != null) {
            lines.clear();
            lines.ensureCapacity(RequesterPlatform.getRequestLimit());
            for (var i = 0; i < requesterReference.getRequests().size(); i++) {
                lines.add(requesterReference.getRequests().get(i));
            }
        }
        refreshList = false;
        resetScrollbar();
    }

    @Override
    protected Set<RequesterReference> getByName(String name) {
        if (requesterReference == null || !requesterReference.getDisplayName().equals(name)) {
            throw new IllegalStateException("reference is null or name doesn't match");
        }
        return Collections.singleton(requesterReference);
    }

    @Override
    protected RequesterReference getById(long requesterId, String name, long sortBy) {
        if (requesterReference == null) {
            requesterReference = new RequesterReference(requesterId, name, sortBy);
            refreshList = true;
        }
        return requesterReference;
    }

    @Override
    protected Rect2i getFooterBounds() {
        return FOOTER_BBOX;
    }

    /** 复用 AE2 界面自带的帮助按钮，改为打开本模组指南。 */
    @Override
    protected void openHelp() {
        ModGuide.openAt(ModGuide.INDEX_PAGE);
    }

    /** 返回非空即可让 AE2 的帮助按钮显示（实际点击由 {@link #openHelp()} 处理）。 */
    @Override
    protected PageAnchor getHelpTopic() {
        return new PageAnchor(ModGuide.INDEX_PAGE, null);
    }
}
