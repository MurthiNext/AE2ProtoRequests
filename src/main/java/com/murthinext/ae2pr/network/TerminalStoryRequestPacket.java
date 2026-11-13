package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.item.proto_terminal.TerminalStorySavedData;
import com.murthinext.ae2pr.registry.ModNetwork;

// 客户端 -> 服务端：请求本玩家在当前存档里的源始终端阅读进度
public final class TerminalStoryRequestPacket {

    public static final TerminalStoryRequestPacket INSTANCE = new TerminalStoryRequestPacket();

    private TerminalStoryRequestPacket() {
    }

    public static void encode(TerminalStoryRequestPacket packet, FriendlyByteBuf buffer) {
        // 无载荷
    }

    public static TerminalStoryRequestPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(TerminalStoryRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ModNetwork.sendToPlayer(
                    player,
                    new TerminalStorySyncPacket(TerminalStorySavedData.get(player.server).get(player.getUUID())));
        });
        context.setPacketHandled(true);
    }
}
