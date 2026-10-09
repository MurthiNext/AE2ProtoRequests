package com.murthinext.ae2pr.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：机器部件总线存储（类型 + 数量）与自动搬运开关同步。
 * <p>
 * 原版 {@code writeItem} 的数量按 byte 传输（最大 127），因此物品只传 1 件，数量单独用 VarInt 传输。
 */
public class MachinePartStackPacket {

    private final BlockPos pos;
    private final List<ItemStack> stacks;
    private final boolean autoTransfer;

    public MachinePartStackPacket(BlockPos pos, List<ItemStack> stacks, boolean autoTransfer) {
        this.pos = pos;
        this.stacks = stacks;
        this.autoTransfer = autoTransfer;
    }

    public static void encode(MachinePartStackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.stacks.size());
        for (ItemStack stack : packet.stacks) {
            buffer.writeItem(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            buffer.writeVarInt(stack.getCount());
        }
        buffer.writeBoolean(packet.autoTransfer);
    }

    public static MachinePartStackPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int count = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = buffer.readItem();
            int amount = buffer.readVarInt();
            if (!stack.isEmpty()) {
                stack.setCount(amount);
            }
            stacks.add(stack);
        }
        return new MachinePartStackPacket(pos, stacks, buffer.readBoolean());
    }

    public static void handle(MachinePartStackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.assembly_line.ItemBusScreen
                        .applyStackSync(packet.pos, packet.stacks, packet.autoTransfer)));
        context.setPacketHandled(true);
    }
}
