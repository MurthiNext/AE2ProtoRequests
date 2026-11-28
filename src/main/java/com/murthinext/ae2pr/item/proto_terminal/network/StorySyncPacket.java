package com.murthinext.ae2pr.item.proto_terminal.network;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.item.proto_terminal.story.Progress;
import com.murthinext.ae2pr.item.proto_terminal.story.ProgressCache;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// 服务端 -> 客户端：本玩家在当前存档里的源始终端阅读进度
public class StorySyncPacket {

    private final byte[] snapshot;

    public StorySyncPacket(Progress progress) {
        try {
            this.snapshot = progress.compress();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Story snapshot could not be synced", exception);
        }
    }

    private StorySyncPacket(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(StorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.snapshot);
    }

    public static StorySyncPacket decode(FriendlyByteBuf buffer) {
        return new StorySyncPacket(buffer.readByteArray(Progress.MAX_SNAPSHOT_BYTES));
    }

    public static void handle(StorySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHooks.run(() -> () -> ProgressCache.apply(packet.snapshot)));
        context.setPacketHandled(true);
    }
}
