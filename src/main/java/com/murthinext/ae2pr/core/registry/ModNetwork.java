package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.core.network.BusSlotClickPacket;
import com.murthinext.ae2pr.core.network.HatchTankClickPacket;
import com.murthinext.ae2pr.core.network.MachinePartFluidPacket;
import com.murthinext.ae2pr.core.network.MachinePartStackPacket;
import com.murthinext.ae2pr.core.network.MachineStatsPacket;
import com.murthinext.ae2pr.core.network.NamingFactoryRenamePacket;
import com.murthinext.ae2pr.core.network.PatternBufferFluidPacket;
import com.murthinext.ae2pr.core.network.RepeatOrderFailedPacket;
import com.murthinext.ae2pr.core.network.RepeatOrderFinishedPacket;
import com.murthinext.ae2pr.core.network.RepeatOrderRoundPacket;
import com.murthinext.ae2pr.core.network.RepeatOrderStatusPacket;
import com.murthinext.ae2pr.core.network.ScreenCorruptionImpactPacket;
import com.murthinext.ae2pr.core.network.ServerConfigSyncPacket;
import com.murthinext.ae2pr.core.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.core.network.TerminalStorySyncPacket;
import com.murthinext.ae2pr.core.network.TerminalStoryUnlockPacket;
import com.murthinext.ae2pr.core.network.TerminalStoryUpdatePacket;
import com.murthinext.ae2pr.core.network.TerminalStoryWatchPacket;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(Ids.of("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextMessageId;

    private static boolean registered;

    private ModNetwork() {}

    // 重复注册会产生 ID 冲突，这里一次性完成
    public static synchronized void register() {
        if (registered) {
            throw new IllegalStateException("ModNetwork already registered");
        }
        registered = true;
        CHANNEL.registerMessage(
                nextMessageId++,
                ServerConfigSyncPacket.class,
                ServerConfigSyncPacket::encode,
                ServerConfigSyncPacket::decode,
                ServerConfigSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                RepeatOrderStatusPacket.class,
                RepeatOrderStatusPacket::encode,
                RepeatOrderStatusPacket::decode,
                RepeatOrderStatusPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                RepeatOrderFailedPacket.class,
                RepeatOrderFailedPacket::encode,
                RepeatOrderFailedPacket::decode,
                RepeatOrderFailedPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                RepeatOrderRoundPacket.class,
                RepeatOrderRoundPacket::encode,
                RepeatOrderRoundPacket::decode,
                RepeatOrderRoundPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                RepeatOrderFinishedPacket.class,
                RepeatOrderFinishedPacket::encode,
                RepeatOrderFinishedPacket::decode,
                RepeatOrderFinishedPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                MachinePartFluidPacket.class,
                MachinePartFluidPacket::encode,
                MachinePartFluidPacket::decode,
                MachinePartFluidPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                MachinePartStackPacket.class,
                MachinePartStackPacket::encode,
                MachinePartStackPacket::decode,
                MachinePartStackPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                BusSlotClickPacket.class,
                BusSlotClickPacket::encode,
                BusSlotClickPacket::decode,
                BusSlotClickPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                HatchTankClickPacket.class,
                HatchTankClickPacket::encode,
                HatchTankClickPacket::decode,
                HatchTankClickPacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                NamingFactoryRenamePacket.class,
                NamingFactoryRenamePacket::encode,
                NamingFactoryRenamePacket::decode,
                NamingFactoryRenamePacket::handle);

        CHANNEL.registerMessage(
                nextMessageId++,
                TerminalStoryRequestPacket.class,
                TerminalStoryRequestPacket::encode,
                TerminalStoryRequestPacket::decode,
                TerminalStoryRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                TerminalStoryUpdatePacket.class,
                TerminalStoryUpdatePacket::encode,
                TerminalStoryUpdatePacket::decode,
                TerminalStoryUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                TerminalStoryWatchPacket.class,
                TerminalStoryWatchPacket::encode,
                TerminalStoryWatchPacket::decode,
                TerminalStoryWatchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                TerminalStorySyncPacket.class,
                TerminalStorySyncPacket::encode,
                TerminalStorySyncPacket::decode,
                TerminalStorySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                TerminalStoryUnlockPacket.class,
                TerminalStoryUnlockPacket::encode,
                TerminalStoryUnlockPacket::decode,
                TerminalStoryUnlockPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                ScreenCorruptionImpactPacket.class,
                ScreenCorruptionImpactPacket::encode,
                ScreenCorruptionImpactPacket::decode,
                ScreenCorruptionImpactPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                MachineStatsPacket.class,
                MachineStatsPacket::encode,
                MachineStatsPacket::decode,
                MachineStatsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                PatternBufferFluidPacket.class,
                PatternBufferFluidPacket::encode,
                PatternBufferFluidPacket::decode,
                PatternBufferFluidPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static int messageCount() {
        return nextMessageId;
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
