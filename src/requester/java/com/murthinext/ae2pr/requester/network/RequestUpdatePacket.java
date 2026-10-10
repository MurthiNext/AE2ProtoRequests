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

import javax.annotation.Nullable;

public class RequestUpdatePacket extends ClientToServerPacket<RequestUpdatePacket> {

    private long requesterId;
    private int requestIndex;

    private boolean state;
    private long amount;

    private UpdateType updateType;

    public RequestUpdatePacket(long requesterId, int requestIndex, boolean state) {
        this.requesterId = requesterId;
        this.requestIndex = requestIndex;
        this.state = state;
        this.updateType = UpdateType.STATE;
    }

    public RequestUpdatePacket(long requesterId, int requestIndex, long amount) {
        this.requesterId = requesterId;
        this.requestIndex = requestIndex;
        this.amount = amount;
        this.updateType = UpdateType.NUMBERS;
    }

    RequestUpdatePacket() {}

    @Override
    public void encode(RequestUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requesterId);
        buffer.writeVarInt(packet.requestIndex);

        buffer.writeVarInt(packet.updateType.ordinal());
        if (packet.updateType == UpdateType.STATE) {
            buffer.writeBoolean(packet.state);
        } else if (packet.updateType == UpdateType.NUMBERS) {
            buffer.writeLong(packet.amount);
        } else {
            throw new IllegalStateException("Unknown update type: " + packet.updateType);
        }
    }

    @Override
    public RequestUpdatePacket decode(FriendlyByteBuf buffer) {
        var id = buffer.readLong();
        var index = buffer.readVarInt();

        var type = UpdateType.values()[buffer.readVarInt()];
        if (type == UpdateType.STATE) {
            return new RequestUpdatePacket(id, index, buffer.readBoolean());
        }
        if (type == UpdateType.NUMBERS) {
            return new RequestUpdatePacket(id, index, buffer.readLong());
        }
        throw new IllegalStateException("Unknown update type: " + type);
    }

    @Override
    protected void handlePacket(RequestUpdatePacket packet, @Nullable ServerPlayer player) {
        if (player != null && player.containerMenu instanceof AbstractRedstoneRequesterMenu requester) {
            if (packet.updateType == UpdateType.STATE) {
                requester.updateRequesterState(packet.requesterId, packet.requestIndex, packet.state);
            } else if (packet.updateType == UpdateType.NUMBERS) {
                requester.updateRequesterNumbers(packet.requesterId, packet.requestIndex, packet.amount);
            }
        }
    }

    private enum UpdateType {
        STATE,
        NUMBERS
    }
}
