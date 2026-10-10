package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.block.machine_part.FluidHatchMenu;

/**
 * 客户端 -> 服务端：机器部件流体仓的流体槽点击（存入/取出），由打开的菜单校验并执行。
 */
public class HatchTankClickPacket {

    private final BlockPos pos;
    private final int tank;
    private final int button;

    public HatchTankClickPacket(BlockPos pos, int tank, int button) {
        this.pos = pos;
        this.tank = tank;
        this.button = button;
    }

    public static void encode(HatchTankClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.tank);
        buffer.writeVarInt(packet.button);
    }

    public static HatchTankClickPacket decode(FriendlyByteBuf buffer) {
        return new HatchTankClickPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(HatchTankClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof FluidHatchMenu menu
                    && menu.getBlockPos().equals(packet.pos)) {
                menu.handleTankClick(packet.tank, packet.button);
            }
        });
        context.setPacketHandled(true);
    }
}
