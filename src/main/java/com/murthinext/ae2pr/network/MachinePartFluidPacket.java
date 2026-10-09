package com.murthinext.ae2pr.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：机器部件流体仓的罐内流体与自动搬运开关同步（仅发给打开对应界面的玩家）。
 */
public class MachinePartFluidPacket {

    private final BlockPos pos;
    private final List<FluidStack> fluids;
    private final boolean autoTransfer;

    public MachinePartFluidPacket(BlockPos pos, List<FluidStack> fluids, boolean autoTransfer) {
        this.pos = pos;
        this.fluids = fluids;
        this.autoTransfer = autoTransfer;
    }

    public static void encode(MachinePartFluidPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.fluids.size());
        for (FluidStack fluid : packet.fluids) {
            buffer.writeFluidStack(fluid);
        }
        buffer.writeBoolean(packet.autoTransfer);
    }

    public static MachinePartFluidPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int count = buffer.readVarInt();
        List<FluidStack> fluids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fluids.add(buffer.readFluidStack());
        }
        return new MachinePartFluidPacket(pos, fluids, buffer.readBoolean());
    }

    public static void handle(MachinePartFluidPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.assembly_line.FluidHatchScreen
                        .applyFluidSync(packet.pos, packet.fluids, packet.autoTransfer)));
        context.setPacketHandled(true);
    }
}
