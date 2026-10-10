/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.network;

import com.murthinext.ae2pr.requester.client.abstraction.AbstractRedstoneRequesterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

public class RequesterSyncPacket extends ServerToClientPacket<RequesterSyncPacket> {

    private boolean clearData;
    private long requesterId;
    private CompoundTag data;

    private RequesterSyncPacket(boolean clearData, long requesterId, CompoundTag data) {
        this.clearData = clearData;
        this.requesterId = requesterId;
        this.data = data;
    }

    RequesterSyncPacket() {}

    public static RequesterSyncPacket clearData() {
        return new RequesterSyncPacket(true, -1, new CompoundTag());
    }

    public static RequesterSyncPacket inventory(long requesterId, CompoundTag data) {
        return new RequesterSyncPacket(false, requesterId, data);
    }

    @Override
    public void encode(RequesterSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.clearData);
        buffer.writeLong(packet.requesterId);
        buffer.writeNbt(packet.data);
    }

    @Override
    public RequesterSyncPacket decode(FriendlyByteBuf buffer) {
        return new RequesterSyncPacket(
            buffer.readBoolean(),
            buffer.readLong(),
            Objects.requireNonNull(buffer.readNbt())
        );
    }

    @Override
    protected void handlePacket(RequesterSyncPacket packet, ClientLevel level) {
        if (Minecraft.getInstance().screen instanceof AbstractRedstoneRequesterScreen<?> screen) {
            screen.updateFromMenu(packet.clearData, packet.requesterId, packet.data);
        }
    }
}
