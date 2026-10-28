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

// 服务端 -> 客户端：同步流体仓流体与搬运开关
public class MachinePartFluidPacket {

    private final BlockPos pos;
    private final List<FluidStack> fluids;
    private final boolean autoTransfer;
    private final boolean recipeIsolated;

    public MachinePartFluidPacket(BlockPos pos, List<FluidStack> fluids, boolean autoTransfer,
            boolean recipeIsolated) {
        this.pos = pos;
        this.fluids = fluids;
        this.autoTransfer = autoTransfer;
        this.recipeIsolated = recipeIsolated;
    }

    public static void encode(MachinePartFluidPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.fluids.size());
        for (FluidStack fluid : packet.fluids) {
            buffer.writeFluidStack(fluid);
        }
        buffer.writeBoolean(packet.autoTransfer);
        buffer.writeBoolean(packet.recipeIsolated);
    }

    public static MachinePartFluidPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int count = buffer.readVarInt();
        List<FluidStack> fluids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fluids.add(buffer.readFluidStack());
        }
        return new MachinePartFluidPacket(pos, fluids, buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(MachinePartFluidPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.machine_part.FluidHatchScreen
                        .applyFluidSync(packet.pos, packet.fluids, packet.autoTransfer, packet.recipeIsolated)));
        context.setPacketHandled(true);
    }
}
