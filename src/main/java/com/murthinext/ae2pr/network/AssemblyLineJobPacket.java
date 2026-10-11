package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：同步装配线作业产物。
 */
public class AssemblyLineJobPacket {

    private final BlockPos pos;
    private final ItemStack stack;

    public AssemblyLineJobPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public static void encode(AssemblyLineJobPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeItem(packet.stack.isEmpty() ? ItemStack.EMPTY : packet.stack.copyWithCount(1));
        buffer.writeVarInt(packet.stack.getCount());
    }

    public static AssemblyLineJobPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ItemStack stack = buffer.readItem();
        int amount = buffer.readVarInt();
        if (!stack.isEmpty()) {
            stack.setCount(amount);
        }
        return new AssemblyLineJobPacket(pos, stack);
    }

    public static void handle(AssemblyLineJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.assembly_line.AssemblyLineScreen
                        .applyJobSync(packet.pos, packet.stack)));
        context.setPacketHandled(true);
    }
}
