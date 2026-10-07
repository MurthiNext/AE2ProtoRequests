package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：熔岩冶炼炉主机当前作业产物同步。
 */
public class LavaSmelterJobPacket {

    private final BlockPos pos;
    private final ItemStack stack;

    public LavaSmelterJobPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public static void encode(LavaSmelterJobPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeItem(packet.stack.isEmpty() ? ItemStack.EMPTY : packet.stack.copyWithCount(1));
        buffer.writeVarInt(packet.stack.getCount());
    }

    public static LavaSmelterJobPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ItemStack stack = buffer.readItem();
        int amount = buffer.readVarInt();
        if (!stack.isEmpty()) {
            stack.setCount(amount);
        }
        return new LavaSmelterJobPacket(pos, stack);
    }

    public static void handle(LavaSmelterJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.lava_smelter.LavaSmelterScreen
                        .applyJobSync(packet.pos, packet.stack)));
        context.setPacketHandled(true);
    }
}
