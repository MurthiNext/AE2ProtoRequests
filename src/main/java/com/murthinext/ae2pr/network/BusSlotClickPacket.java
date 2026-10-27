package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.block.machine_part.ItemBusMenu;

// 客户端 -> 服务端：总线存储区点击
public class BusSlotClickPacket {

    private final BlockPos pos;
    private final int slot;
    private final int button;
    private final boolean shift;
    private final boolean creativeClone;

    public BusSlotClickPacket(BlockPos pos, int slot, int button, boolean shift, boolean creativeClone) {
        this.pos = pos;
        this.slot = slot;
        this.button = button;
        this.shift = shift;
        this.creativeClone = creativeClone;
    }

    public static void encode(BusSlotClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.button);
        buffer.writeBoolean(packet.shift);
        buffer.writeBoolean(packet.creativeClone);
    }

    public static BusSlotClickPacket decode(FriendlyByteBuf buffer) {
        return new BusSlotClickPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(BusSlotClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof ItemBusMenu menu
                    && menu.getBlockPos().equals(packet.pos)) {
                if (packet.creativeClone) {
                    menu.handleCreativeClone(packet.slot, player);
                } else {
                    menu.handleStorageClick(packet.slot, packet.button, packet.shift);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
