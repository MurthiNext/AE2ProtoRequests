package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// 服务端 -> 客户端：当前轮次通知
public class RepeatOrderRoundPacket {
    private final AEKey what;
    private final int remainingRounds;
    private final int totalRounds;

    public RepeatOrderRoundPacket(AEKey what, int remainingRounds, int totalRounds) {
        this.what = what;
        this.remainingRounds = remainingRounds;
        this.totalRounds = totalRounds;
    }

    public static void encode(RepeatOrderRoundPacket packet, FriendlyByteBuf buffer) {
        GenericStack.writeBuffer(new GenericStack(packet.what, packet.remainingRounds), buffer);
        buffer.writeVarInt(packet.totalRounds);
    }

    public static RepeatOrderRoundPacket decode(FriendlyByteBuf buffer) {
        var stack = GenericStack.readBuffer(buffer);
        var what = stack != null ? stack.what() : null;
        var remaining = stack != null ? (int) Math.min(stack.amount(), Integer.MAX_VALUE) : 0;
        var total = buffer.readVarInt();
        return new RepeatOrderRoundPacket(what, remaining, total);
    }

    public static void handle(RepeatOrderRoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.repeat.ClientRepeatOrderRounds.update(
                        packet.what, packet.remainingRounds, packet.totalRounds)));
        context.setPacketHandled(true);
    }
}
