package com.murthinext.ae2pr.item.proto_terminal.network;

import com.murthinext.ae2pr.item.proto_terminal.story.UnlockCondition;
import com.murthinext.ae2pr.item.proto_terminal.story.UnlockTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 -> 服务端：登记终端当前用到的解锁条件，由服务端持续判定并推送结果；
 * 条件为空表示终端已关闭，停止跟踪。
 */
public class StoryWatchPacket {

    private static final int MAX_CONDITIONS = 256;
    private static final int MAX_CONDITION_LENGTH = UnlockCondition.MAX_KEY_LENGTH;

    private final List<String> conditions;

    public StoryWatchPacket(List<String> conditions) {
        this.conditions = List.copyOf(conditions);
    }

    public static void encode(StoryWatchPacket packet, FriendlyByteBuf buffer) {
        int count = Math.min(packet.conditions.size(), MAX_CONDITIONS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(packet.conditions.get(i), MAX_CONDITION_LENGTH);
        }
    }

    public static StoryWatchPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_CONDITIONS);
        List<String> conditions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            conditions.add(buffer.readUtf(MAX_CONDITION_LENGTH));
        }
        return new StoryWatchPacket(conditions);
    }

    public static void handle(StoryWatchPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                UnlockTracker.watch(player, packet.conditions);
            }
        });
        context.setPacketHandled(true);
    }
}
