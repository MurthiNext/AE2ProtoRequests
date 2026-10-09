package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.block.assembly_line.FluidHatchMenu;

/**
 * 客户端 -> 服务端：赛特斯石英水晶输入/输出仓的流体槽点击（存入/取出），由打开的菜单校验并执行。
 */
public class HatchTankClickPacket {

    private final BlockPos pos;
    private final int button;

    public HatchTankClickPacket(BlockPos pos, int button) {
        this.pos = pos;
        this.button = button;
    }

    public static void encode(HatchTankClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.button);
    }

    public static HatchTankClickPacket decode(FriendlyByteBuf buffer) {
        return new HatchTankClickPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(HatchTankClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof FluidHatchMenu menu
                    && menu.getBlockPos().equals(packet.pos)) {
                menu.handleTankClick(packet.button);
            }
        });
        context.setPacketHandled(true);
    }
}
