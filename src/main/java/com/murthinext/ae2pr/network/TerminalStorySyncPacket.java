package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：本玩家在当前存档里的源始终端阅读进度。
 */
public class TerminalStorySyncPacket {

    private static final int MAX_JSON = 1048576;

    private final String json;
    private final boolean replace;

    public TerminalStorySyncPacket(String json) {
        this(json, false);
    }

    public TerminalStorySyncPacket(String json, boolean replace) {
        this.json = json == null ? "" : json;
        this.replace = replace;
    }

    public static void encode(TerminalStorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.json, MAX_JSON);
        buffer.writeBoolean(packet.replace);
    }

    public static TerminalStorySyncPacket decode(FriendlyByteBuf buffer) {
        return new TerminalStorySyncPacket(buffer.readUtf(MAX_JSON), buffer.readBoolean());
    }

    public static void handle(TerminalStorySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.proto_terminal.TerminalStoryProgressCache.apply(packet.json,
                        packet.replace)));
        context.setPacketHandled(true);
    }
}
