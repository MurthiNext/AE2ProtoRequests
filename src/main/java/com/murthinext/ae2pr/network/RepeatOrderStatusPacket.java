package com.murthinext.ae2pr.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：菜单可见 CPU 的轮次信息。
 */
public class RepeatOrderStatusPacket {
    private final int containerId;
    private final List<CpuRoundInfo> entries;

    public RepeatOrderStatusPacket(int containerId, List<CpuRoundInfo> entries) {
        this.containerId = containerId;
        this.entries = List.copyOf(entries);
    }

    public static void encode(RepeatOrderStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.containerId);
        buffer.writeVarInt(packet.entries.size());
        for (var entry : packet.entries) {
            buffer.writeInt(entry.serial());
            buffer.writeVarInt(entry.round());
            buffer.writeVarInt(entry.total());
        }
    }

    public static RepeatOrderStatusPacket decode(FriendlyByteBuf buffer) {
        var containerId = buffer.readInt();
        var count = buffer.readVarInt();
        var entries = new ArrayList<CpuRoundInfo>(count);
        for (var i = 0; i < count; i++) {
            entries.add(new CpuRoundInfo(buffer.readInt(), buffer.readVarInt(), buffer.readVarInt()));
        }
        return new RepeatOrderStatusPacket(containerId, entries);
    }

    public static void handle(RepeatOrderStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.repeat.ClientRepeatOrderState.update(packet.containerId, packet.entries)));
        context.setPacketHandled(true);
    }
}
