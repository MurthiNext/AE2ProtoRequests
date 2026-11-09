package com.murthinext.ae2pr.registry;

import com.murthinext.ae2pr.api.Ids;

import java.util.Optional;

import com.murthinext.ae2pr.network.BusSlotClickPacket;
import com.murthinext.ae2pr.network.HatchTankClickPacket;
import com.murthinext.ae2pr.network.MachinePartFluidPacket;
import com.murthinext.ae2pr.network.MachinePartStackPacket;
import com.murthinext.ae2pr.network.NamingFactoryRenamePacket;
import com.murthinext.ae2pr.network.RepeatOrderFailedPacket;
import com.murthinext.ae2pr.network.RepeatOrderFinishedPacket;
import com.murthinext.ae2pr.network.RepeatOrderRoundPacket;
import com.murthinext.ae2pr.network.RepeatOrderStatusPacket;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStorySyncPacket;
import com.murthinext.ae2pr.network.TerminalStoryUnlockPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import com.murthinext.ae2pr.network.TerminalStoryWatchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(Ids.of( "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextMessageId;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextMessageId++,
                RepeatOrderStatusPacket.class,
                RepeatOrderStatusPacket::encode,
                RepeatOrderStatusPacket::decode,
                RepeatOrderStatusPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                RepeatOrderFailedPacket.class,
                RepeatOrderFailedPacket::encode,
                RepeatOrderFailedPacket::decode,
                RepeatOrderFailedPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                RepeatOrderRoundPacket.class,
                RepeatOrderRoundPacket::encode,
                RepeatOrderRoundPacket::decode,
                RepeatOrderRoundPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                RepeatOrderFinishedPacket.class,
                RepeatOrderFinishedPacket::encode,
                RepeatOrderFinishedPacket::decode,
                RepeatOrderFinishedPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                MachinePartFluidPacket.class,
                MachinePartFluidPacket::encode,
                MachinePartFluidPacket::decode,
                MachinePartFluidPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                MachinePartStackPacket.class,
                MachinePartStackPacket::encode,
                MachinePartStackPacket::decode,
                MachinePartStackPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                BusSlotClickPacket.class,
                BusSlotClickPacket::encode,
                BusSlotClickPacket::decode,
                BusSlotClickPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                HatchTankClickPacket.class,
                HatchTankClickPacket::encode,
                HatchTankClickPacket::decode,
                HatchTankClickPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                NamingFactoryRenamePacket.class,
                NamingFactoryRenamePacket::encode,
                NamingFactoryRenamePacket::decode,
                NamingFactoryRenamePacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryRequestPacket.class,
                TerminalStoryRequestPacket::encode,
                TerminalStoryRequestPacket::decode,
                TerminalStoryRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryUpdatePacket.class,
                TerminalStoryUpdatePacket::encode,
                TerminalStoryUpdatePacket::decode,
                TerminalStoryUpdatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryWatchPacket.class,
                TerminalStoryWatchPacket::encode,
                TerminalStoryWatchPacket::decode,
                TerminalStoryWatchPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStorySyncPacket.class,
                TerminalStorySyncPacket::encode,
                TerminalStorySyncPacket::decode,
                TerminalStorySyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryUnlockPacket.class,
                TerminalStoryUnlockPacket::encode,
                TerminalStoryUnlockPacket::decode,
                TerminalStoryUnlockPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
