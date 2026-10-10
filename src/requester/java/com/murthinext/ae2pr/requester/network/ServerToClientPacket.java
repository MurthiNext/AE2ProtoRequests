/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class ServerToClientPacket<T> implements Packet<T> {

    @Override
    public void handle(T packet, Supplier<? extends NetworkEvent.Context> context) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        context.get().enqueueWork(() -> handlePacket(packet, level));
        context.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract void handlePacket(T packet, ClientLevel level);
}
