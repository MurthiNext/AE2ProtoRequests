package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.logic.terminal.TerminalStorySavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 -> 服务端：请求本玩家在当前存档里的源始终端阅读进度。
 */
public class TerminalStoryRequestPacket {

    public static void encode(TerminalStoryRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static TerminalStoryRequestPacket decode(FriendlyByteBuf buffer) {
        return new TerminalStoryRequestPacket();
    }

    public static void handle(TerminalStoryRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ModNetwork.sendToPlayer(player, new TerminalStorySyncPacket(
                    TerminalStorySavedData.get(player.server).get(player.getUUID())));
        });
        context.setPacketHandled(true);
    }
}
