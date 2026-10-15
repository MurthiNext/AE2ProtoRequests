package com.murthinext.ae2pr.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.murthinext.ae2pr.logic.terminal.TerminalStorySavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 -> 服务端：上报单个剧情块的阅读进度与全局标签。
 */
public class TerminalStoryUpdatePacket {

    private static final int MAX_NODE_ID = 64;
    private static final int MAX_NODE_JSON = 16384;
    private static final int MAX_TAG = 64;
    private static final int MAX_TAGS = 64;

    private final String nodeId;
    private final String nodeJson;
    private final List<String> tags;

    public TerminalStoryUpdatePacket(String nodeId, String nodeJson, List<String> tags) {
        this.nodeId = nodeId;
        this.nodeJson = nodeJson;
        this.tags = List.copyOf(tags);
    }

    public static void encode(TerminalStoryUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.nodeId, MAX_NODE_ID);
        buffer.writeUtf(packet.nodeJson, MAX_NODE_JSON);
        buffer.writeVarInt(Math.min(packet.tags.size(), MAX_TAGS));
        for (int i = 0; i < packet.tags.size() && i < MAX_TAGS; i++) {
            buffer.writeUtf(packet.tags.get(i), MAX_TAG);
        }
    }

    public static TerminalStoryUpdatePacket decode(FriendlyByteBuf buffer) {
        String nodeId = buffer.readUtf(MAX_NODE_ID);
        String nodeJson = buffer.readUtf(MAX_NODE_JSON);
        int count = Math.min(buffer.readVarInt(), MAX_TAGS);
        List<String> tags = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tags.add(buffer.readUtf(MAX_TAG));
        }
        return new TerminalStoryUpdatePacket(nodeId, nodeJson, tags);
    }

    public static void handle(TerminalStoryUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            TerminalStorySavedData.get(player.server)
                    .merge(player.getUUID(), packet.nodeId, packet.nodeJson, packet.tags);
        });
        context.setPacketHandled(true);
    }
}
