package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：同步物质相变仪产物（流体）。
 */
public class MatterPhaseTransformerJobPacket {

    private final BlockPos pos;
    private final FluidStack fluid;

    public MatterPhaseTransformerJobPacket(BlockPos pos, FluidStack fluid) {
        this.pos = pos;
        this.fluid = fluid.copy();
    }

    public static void encode(MatterPhaseTransformerJobPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeNbt(packet.fluid.writeToNBT(new CompoundTag()));
    }

    public static MatterPhaseTransformerJobPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag tag = buffer.readNbt();
        FluidStack fluid = tag == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(tag);
        return new MatterPhaseTransformerJobPacket(pos, fluid);
    }

    public static void handle(MatterPhaseTransformerJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.matter_phase_transformer.MatterPhaseTransformerScreen
                        .applyJobSync(packet.pos, packet.fluid)));
        context.setPacketHandled(true);
    }
}
