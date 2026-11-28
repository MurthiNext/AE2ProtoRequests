package com.murthinext.ae2pr.core.network;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineStatsSender;
import com.murthinext.ae2pr.block.machine_part.network.BusSlotClickPacket;
import com.murthinext.ae2pr.block.machine_part.network.HatchTankClickPacket;
import com.murthinext.ae2pr.block.machine_part.network.MachinePartFluidPacket;
import com.murthinext.ae2pr.block.machine_part.network.MachinePartStackPacket;
import com.murthinext.ae2pr.block.machine_part.network.PatternBufferFluidPacket;
import com.murthinext.ae2pr.effect.screen_corruption.network.ScreenCorruptionImpactPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderFailedPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderFinishedPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderRoundPacket;
import com.murthinext.ae2pr.integration.ae2.repeat_order.network.RepeatOrderStatusPacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryRequestPacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StorySyncPacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryUnlockPacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryUpdatePacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryWatchPacket;
import com.murthinext.ae2pr.machine.naming_factory.network.NamingFactoryRenamePacket;
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
        MachineStatsSender.install(
                (player, containerId, stats) -> sendToPlayer(player, new MachineStatsPacket(containerId, stats)));
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
                StoryRequestPacket.class,
                StoryRequestPacket::encode,
                StoryRequestPacket::decode,
                StoryRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                StoryUpdatePacket.class,
                StoryUpdatePacket::encode,
                StoryUpdatePacket::decode,
                StoryUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                StoryWatchPacket.class,
                StoryWatchPacket::encode,
                StoryWatchPacket::decode,
                StoryWatchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(
                nextMessageId++,
                StorySyncPacket.class,
                StorySyncPacket::encode,
                StorySyncPacket::decode,
                StorySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                nextMessageId++,
                StoryUnlockPacket.class,
                StoryUnlockPacket::encode,
                StoryUnlockPacket::decode,
                StoryUnlockPacket::handle,
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
