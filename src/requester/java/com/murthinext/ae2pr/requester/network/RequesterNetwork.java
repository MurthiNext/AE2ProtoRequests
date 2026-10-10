/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.network;

import com.murthinext.ae2pr.requester.RequesterUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RequesterNetwork {

    private static final ResourceLocation ID = RequesterUtils.getRL("network");
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(ID)
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();

    private RequesterNetwork() {}

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static void init() {
        var packetId = -1;
        // server to client
        register(++packetId, RequesterSyncPacket.class, new RequesterSyncPacket());
        // client to server
        register(++packetId, RequestUpdatePacket.class, new RequestUpdatePacket());
        register(++packetId, DragAndDropPacket.class, new DragAndDropPacket());
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> void register(int packetId, Class<T> clazz, Packet<T> packet) {
        CHANNEL.registerMessage(packetId, clazz, packet::encode, packet::decode, packet::handle);
    }
}
