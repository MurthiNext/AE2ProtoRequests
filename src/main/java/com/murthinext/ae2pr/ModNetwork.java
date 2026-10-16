package com.murthinext.ae2pr;

import com.murthinext.ae2pr.network.AssemblyLineJobPacket;
import com.murthinext.ae2pr.network.BusSlotClickPacket;
import com.murthinext.ae2pr.network.CrystalReactionChamberJobPacket;
import com.murthinext.ae2pr.network.HatchTankClickPacket;
import com.murthinext.ae2pr.network.LavaSmelterJobPacket;
import com.murthinext.ae2pr.network.MachinePartFluidPacket;
import com.murthinext.ae2pr.network.MachinePartStackPacket;
import com.murthinext.ae2pr.network.MatterPhaseTransformerJobPacket;
import com.murthinext.ae2pr.network.NamingFactoryRenamePacket;
import com.murthinext.ae2pr.network.RepeatOrderFailedPacket;
import com.murthinext.ae2pr.network.RepeatOrderFinishedPacket;
import com.murthinext.ae2pr.network.RepeatOrderRoundPacket;
import com.murthinext.ae2pr.network.RepeatOrderStatusPacket;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStorySyncPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组自有网络通道。
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "main"))
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
                AssemblyLineJobPacket.class,
                AssemblyLineJobPacket::encode,
                AssemblyLineJobPacket::decode,
                AssemblyLineJobPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                LavaSmelterJobPacket.class,
                LavaSmelterJobPacket::encode,
                LavaSmelterJobPacket::decode,
                LavaSmelterJobPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                MatterPhaseTransformerJobPacket.class,
                MatterPhaseTransformerJobPacket::encode,
                MatterPhaseTransformerJobPacket::decode,
                MatterPhaseTransformerJobPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                CrystalReactionChamberJobPacket.class,
                CrystalReactionChamberJobPacket::encode,
                CrystalReactionChamberJobPacket::decode,
                CrystalReactionChamberJobPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                NamingFactoryRenamePacket.class,
                NamingFactoryRenamePacket::encode,
                NamingFactoryRenamePacket::decode,
                NamingFactoryRenamePacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryRequestPacket.class,
                TerminalStoryRequestPacket::encode,
                TerminalStoryRequestPacket::decode,
                TerminalStoryRequestPacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStoryUpdatePacket.class,
                TerminalStoryUpdatePacket::encode,
                TerminalStoryUpdatePacket::decode,
                TerminalStoryUpdatePacket::handle);

        CHANNEL.registerMessage(nextMessageId++,
                TerminalStorySyncPacket.class,
                TerminalStorySyncPacket::encode,
                TerminalStorySyncPacket::decode,
                TerminalStorySyncPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, RepeatOrderStatusPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, TerminalStorySyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, RepeatOrderFailedPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, RepeatOrderRoundPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, RepeatOrderFinishedPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, MachinePartFluidPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, MachinePartStackPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, AssemblyLineJobPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, LavaSmelterJobPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, MatterPhaseTransformerJobPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, CrystalReactionChamberJobPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(BusSlotClickPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(HatchTankClickPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(NamingFactoryRenamePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(TerminalStoryRequestPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(TerminalStoryUpdatePacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
