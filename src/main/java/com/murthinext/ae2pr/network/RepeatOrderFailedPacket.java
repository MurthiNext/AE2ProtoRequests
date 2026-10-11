package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import com.murthinext.ae2pr.logic.repeat.FailureReason;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：重复订单失败通知。
 */
public class RepeatOrderFailedPacket {
    private final AEKey what;
    private final long amountPerRound;
    private final int totalRounds;
    private final int completedRounds;
    private final FailureReason reason;

    public RepeatOrderFailedPacket(AEKey what, long amountPerRound, int totalRounds, int completedRounds,
            FailureReason reason) {
        this.what = what;
        this.amountPerRound = amountPerRound;
        this.totalRounds = totalRounds;
        this.completedRounds = completedRounds;
        this.reason = reason;
    }

    public static void encode(RepeatOrderFailedPacket packet, FriendlyByteBuf buffer) {
        GenericStack.writeBuffer(new GenericStack(packet.what, packet.amountPerRound), buffer);
        buffer.writeVarInt(packet.totalRounds);
        buffer.writeVarInt(packet.completedRounds);
        buffer.writeEnum(packet.reason);
    }

    public static RepeatOrderFailedPacket decode(FriendlyByteBuf buffer) {
        var stack = GenericStack.readBuffer(buffer);
        var totalRounds = buffer.readVarInt();
        var completedRounds = buffer.readVarInt();
        var reason = buffer.readEnum(FailureReason.class);
        var what = stack != null ? stack.what() : null;
        var amount = stack != null ? stack.amount() : 0;
        return new RepeatOrderFailedPacket(what, amount, totalRounds, completedRounds, reason);
    }

    public static void handle(RepeatOrderFailedPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.ClientRepeatOrderFailures.onFailure(
                        packet.what, packet.amountPerRound, packet.totalRounds, packet.completedRounds,
                        packet.reason)));
        context.setPacketHandled(true);
    }
}
