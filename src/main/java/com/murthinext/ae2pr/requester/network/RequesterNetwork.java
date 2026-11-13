package com.murthinext.ae2pr.requester.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.murthinext.ae2pr.api.Ids;

public final class RequesterNetwork {

    private static final ResourceLocation ID = Ids.of("network");
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(ID)
                                                        .networkProtocolVersion(() -> PROTOCOL)
                                                        .clientAcceptedVersions(PROTOCOL::equals)
                                                        .serverAcceptedVersions(PROTOCOL::equals)
                                                        .simpleChannel();

    private RequesterNetwork() {
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static void init() {
        var packetId = -1;
        // server to client
        register(++packetId, RequesterSyncPacket.class, new RequesterSyncPacket());
        // client to server
        register(++packetId, RequestUpdatePacket.class, new RequestUpdatePacket());
        register(++packetId, DragAndDropPacket.class, new DragAndDropPacket());
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> void register(int packetId, Class<T> clazz, Packet<T> packet) {
        CHANNEL.registerMessage(packetId, clazz, packet::encode, packet::decode, packet::handle);
    }
}
