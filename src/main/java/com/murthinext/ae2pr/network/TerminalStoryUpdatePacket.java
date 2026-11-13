package com.murthinext.ae2pr.network;

import java.io.IOException;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.item.proto_terminal.TerminalStoryProgress;
import com.murthinext.ae2pr.item.proto_terminal.TerminalStorySavedData;
import com.murthinext.ae2pr.registry.ModNetwork;

// 客户端上报压缩后的剧情快照
public class TerminalStoryUpdatePacket {

    private final byte[] snapshot;

    public TerminalStoryUpdatePacket(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(TerminalStoryUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.snapshot);
    }

    public static TerminalStoryUpdatePacket decode(FriendlyByteBuf buffer) {
        return new TerminalStoryUpdatePacket(buffer.readByteArray(TerminalStoryProgress.MAX_UPDATE_BYTES));
    }

    public static void handle(TerminalStoryUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) {
                return;
            }
            try {
                TerminalStoryProgress progress = TerminalStoryProgress.decompress(packet.snapshot);
                TerminalStorySavedData data = TerminalStorySavedData.get(player.server);
                if (!data.update(player.getUUID(), progress)) {
                    ModNetwork.sendToPlayer(player, new TerminalStorySyncPacket(data.get(player.getUUID())));
                }
            } catch (IOException | RuntimeException exception) {
                ae2pr.LOGGER.warn("Invalid proto terminal story snapshot from player {}", player.getUUID(), exception);
                player.sendSystemMessage(Component.translatable("gui.ae2pr.proto_terminal.save_failed"));
                ModNetwork.sendToPlayer(
                        player,
                        new TerminalStorySyncPacket(TerminalStorySavedData.get(player.server).get(player.getUUID())));
            }
        });
        context.setPacketHandled(true);
    }
}
