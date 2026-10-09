package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：赛特斯石英水晶输入/输出仓的罐内流体与自动搬运开关同步（仅发给打开对应界面的玩家）。
 */
public class MachinePartFluidPacket {

    private final BlockPos pos;
    private final FluidStack fluid;
    private final boolean autoTransfer;

    public MachinePartFluidPacket(BlockPos pos, FluidStack fluid, boolean autoTransfer) {
        this.pos = pos;
        this.fluid = fluid;
        this.autoTransfer = autoTransfer;
    }

    public static void encode(MachinePartFluidPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeFluidStack(packet.fluid);
        buffer.writeBoolean(packet.autoTransfer);
    }

    public static MachinePartFluidPacket decode(FriendlyByteBuf buffer) {
        return new MachinePartFluidPacket(buffer.readBlockPos(), buffer.readFluidStack(), buffer.readBoolean());
    }

    public static void handle(MachinePartFluidPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.assembly_line.FluidHatchScreen
                        .applyFluidSync(packet.pos, packet.fluid, packet.autoTransfer)));
        context.setPacketHandled(true);
    }
}
