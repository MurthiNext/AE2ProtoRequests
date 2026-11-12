package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.ConfigSync;
import net.minecraftforge.network.HandshakeMessages;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.ClientHooks;

public record ServerConfigSyncPacket(byte[] snapshot) {
    public static void encode(ServerConfigSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.snapshot());
    }

    public static ServerConfigSyncPacket decode(FriendlyByteBuf buffer) {
        return new ServerConfigSyncPacket(buffer.readByteArray(65536));
    }

    public static void handle(ServerConfigSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHooks.run(() -> () -> ConfigSync.INSTANCE.receiveSyncedConfig(
                new HandshakeMessages.S2CConfigData(ServerConfig.FILE_NAME, packet.snapshot()), contextSupplier)));
        context.setPacketHandled(true);
    }
}
