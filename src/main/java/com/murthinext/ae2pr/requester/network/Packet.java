package com.murthinext.ae2pr.requester.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface Packet<T> {
    void encode(T packet, FriendlyByteBuf buffer);

    T decode(FriendlyByteBuf buffer);

    void handle(T packet, Supplier<? extends NetworkEvent.Context> context);
}
