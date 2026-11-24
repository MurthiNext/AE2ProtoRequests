package com.murthinext.ae2pr.network;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferScreen;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

public record PatternBufferFluidPacket(int containerId, int slot, FluidStack fluid) {
    public static void encode(PatternBufferFluidPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.slot);
        buffer.writeFluidStack(packet.fluid);
    }

    public static PatternBufferFluidPacket decode(FriendlyByteBuf buffer) {
        return new PatternBufferFluidPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readFluidStack());
    }

    public static void handle(PatternBufferFluidPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHooks.run(
                () -> () -> MEPatternBufferScreen.applyFluidSync(packet.containerId, packet.slot, packet.fluid)));
        context.setPacketHandled(true);
    }
}
