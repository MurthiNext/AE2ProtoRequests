package com.murthinext.ae2pr.item.proto_terminal.network;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.core.network.ModNetwork;
import com.murthinext.ae2pr.item.proto_terminal.story.Progress;
import com.murthinext.ae2pr.item.proto_terminal.story.ProgressStore;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

// 客户端上报压缩后的剧情快照
public class StoryUpdatePacket {

    private final byte[] snapshot;

    public StoryUpdatePacket(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(StoryUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.snapshot);
    }

    public static StoryUpdatePacket decode(FriendlyByteBuf buffer) {
        return new StoryUpdatePacket(buffer.readByteArray(Progress.MAX_UPDATE_BYTES));
    }

    public static void handle(StoryUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) {
                return;
            }
            try {
                Progress progress = Progress.decompress(packet.snapshot);
                ProgressStore data = ProgressStore.get(player.server);
                if (!data.update(player.getUUID(), progress)) {
                    ModNetwork.sendToPlayer(player, new StorySyncPacket(data.get(player.getUUID())));
                }
            } catch (IOException | RuntimeException exception) {
                ae2pr.LOGGER.warn("Invalid proto terminal story snapshot from player {}", player.getUUID(), exception);
                player.sendSystemMessage(Component.translatable("gui.ae2pr.proto_terminal.save_failed"));
                ModNetwork.sendToPlayer(
                        player,
                        new StorySyncPacket(ProgressStore.get(player.server).get(player.getUUID())));
            }
        });
        context.setPacketHandled(true);
    }
}
