package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// 服务端 -> 客户端：重复订单全部完成提示
public class RepeatOrderFinishedPacket {
    private final AEKey what;
    private final long amountPerRound;
    private final int totalRounds;

    public RepeatOrderFinishedPacket(AEKey what, long amountPerRound, int totalRounds) {
        this.what = what;
        this.amountPerRound = amountPerRound;
        this.totalRounds = totalRounds;
    }

    public static void encode(RepeatOrderFinishedPacket packet, FriendlyByteBuf buffer) {
        GenericStack.writeBuffer(new GenericStack(packet.what, packet.amountPerRound), buffer);
        buffer.writeVarInt(packet.totalRounds);
    }

    public static RepeatOrderFinishedPacket decode(FriendlyByteBuf buffer) {
        var stack = GenericStack.readBuffer(buffer);
        var what = stack != null ? stack.what() : null;
        var amount = stack != null ? stack.amount() : 0;
        var total = buffer.readVarInt();
        return new RepeatOrderFinishedPacket(what, amount, total);
    }

    public static void handle(RepeatOrderFinishedPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.repeat.ClientRepeatOrderRounds.onFinished(
                        packet.what, packet.amountPerRound, packet.totalRounds)));
        context.setPacketHandled(true);
    }
}
