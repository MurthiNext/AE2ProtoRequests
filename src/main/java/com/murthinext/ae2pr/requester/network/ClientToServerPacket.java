package com.murthinext.ae2pr.requester.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public abstract class ClientToServerPacket<T> implements Packet<T> {

    @Override
    public void handle(T packet, Supplier<? extends NetworkEvent.Context> context) {
        var player = context.get().getSender();
        context.get().enqueueWork(() -> handlePacket(packet, player));
        context.get().setPacketHandled(true);
    }

    protected abstract void handlePacket(T packet, @Nullable ServerPlayer player);
}
