package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：同步确认界面重复轮数。
 */
public class RepeatOrderConfirmRoundsPacket {
    private final int containerId;
    private final int rounds;

    public RepeatOrderConfirmRoundsPacket(int containerId, int rounds) {
        this.containerId = containerId;
        this.rounds = rounds;
    }

    public static void encode(RepeatOrderConfirmRoundsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.rounds);
    }

    public static RepeatOrderConfirmRoundsPacket decode(FriendlyByteBuf buffer) {
        return new RepeatOrderConfirmRoundsPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(RepeatOrderConfirmRoundsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.ClientConfirmRounds.update(
                        packet.containerId, packet.rounds)));
        context.setPacketHandled(true);
    }
}
