/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.network;

import com.murthinext.ae2pr.requester.abstraction.AbstractRedstoneRequesterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class DragAndDropPacket extends ClientToServerPacket<DragAndDropPacket> {

    private long requesterId;
    private int requestIndex;

    private ItemStack item;

    public DragAndDropPacket(long requesterId, int requestIndex, ItemStack item) {
        this.requesterId = requesterId;
        this.requestIndex = requestIndex;
        this.item = item;
    }

    DragAndDropPacket() {}

    @Override
    public void encode(DragAndDropPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requesterId);
        buffer.writeVarInt(packet.requestIndex);
        buffer.writeItem(packet.item);
    }

    @Override
    public DragAndDropPacket decode(FriendlyByteBuf buffer) {
        return new DragAndDropPacket(
            buffer.readLong(),
            buffer.readVarInt(),
            buffer.readItem()
        );
    }

    @Override
    protected void handlePacket(DragAndDropPacket packet, @Nullable ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof AbstractRedstoneRequesterMenu requester)) return;
        requester.applyDragAndDrop(player, packet.requestIndex, packet.requesterId, packet.item);
    }
}
