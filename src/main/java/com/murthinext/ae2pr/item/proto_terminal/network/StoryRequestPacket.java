package com.murthinext.ae2pr.item.proto_terminal.network;

import com.murthinext.ae2pr.core.network.ModNetwork;
import com.murthinext.ae2pr.item.proto_terminal.story.ProgressStore;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

// 客户端 -> 服务端：请求本玩家在当前存档里的源始终端阅读进度
public final class StoryRequestPacket {

    public static final StoryRequestPacket INSTANCE = new StoryRequestPacket();

    private StoryRequestPacket() {}

    public static void encode(StoryRequestPacket packet, FriendlyByteBuf buffer) {
        // 无载荷
    }

    public static StoryRequestPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(StoryRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ModNetwork.sendToPlayer(
                    player, new StorySyncPacket(ProgressStore.get(player.server).get(player.getUUID())));
        });
        context.setPacketHandled(true);
    }
}
