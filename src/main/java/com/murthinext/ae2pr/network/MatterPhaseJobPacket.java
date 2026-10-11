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
public class MatterPhaseJobPacket {

    private final BlockPos pos;
    private final FluidStack fluid;

    public MatterPhaseJobPacket(BlockPos pos, FluidStack fluid) {
        this.pos = pos;
        this.fluid = fluid.copy();
    }

    public static void encode(MatterPhaseJobPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeNbt(packet.fluid.writeToNBT(new CompoundTag()));
    }

    public static MatterPhaseJobPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag tag = buffer.readNbt();
        FluidStack fluid = tag == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(tag);
        return new MatterPhaseJobPacket(pos, fluid);
    }

    public static void handle(MatterPhaseJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.matter_phase_transformer.MatterPhaseTransformerScreen
                        .applyJobSync(packet.pos, packet.fluid)));
        context.setPacketHandled(true);
    }
}
