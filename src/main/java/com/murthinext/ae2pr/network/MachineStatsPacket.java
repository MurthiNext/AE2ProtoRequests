package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.api.machine.MachineStats;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;

public record MachineStatsPacket(int containerId, MachineStats stats) {
    public static void encode(MachineStatsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId());
        packet.stats().write(buffer);
    }

    public static MachineStatsPacket decode(FriendlyByteBuf buffer) {
        return new MachineStatsPacket(buffer.readVarInt(), MachineStats.read(buffer));
    }

    public static void handle(MachineStatsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHooks.run(() -> () -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu.containerId == packet.containerId() &&
                player.containerMenu instanceof AbstractMultiblockControllerMenu<?> menu) {
                menu.setMachineStats(packet.stats());
            }
        }));
        context.setPacketHandled(true);
    }
}
