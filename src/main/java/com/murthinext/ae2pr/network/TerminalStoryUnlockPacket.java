package com.murthinext.ae2pr.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// 服务端 -> 客户端：本玩家当前已满足的剧情块解锁条件
public class TerminalStoryUnlockPacket {

    private static final int MAX_CONDITIONS = 256;
    private static final int MAX_CONDITION_LENGTH = 160;

    private final List<String> conditions;

    public TerminalStoryUnlockPacket(List<String> conditions) {
        this.conditions = List.copyOf(conditions);
    }

    public static void encode(TerminalStoryUnlockPacket packet, FriendlyByteBuf buffer) {
        int count = Math.min(packet.conditions.size(), MAX_CONDITIONS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(packet.conditions.get(i), MAX_CONDITION_LENGTH);
        }
    }

    public static TerminalStoryUnlockPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_CONDITIONS);
        List<String> conditions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            conditions.add(buffer.readUtf(MAX_CONDITION_LENGTH));
        }
        return new TerminalStoryUnlockPacket(conditions);
    }

    public static void handle(TerminalStoryUnlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.proto_terminal.TerminalStoryProgressCache
                        .applyUnlocks(packet.conditions)));
        context.setPacketHandled(true);
    }
}
