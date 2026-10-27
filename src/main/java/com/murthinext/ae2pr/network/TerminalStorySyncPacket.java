package com.murthinext.ae2pr.network;

import java.io.IOException;
import java.util.function.Supplier;

import com.murthinext.ae2pr.logic.terminal.TerminalStoryProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// 服务端 -> 客户端：本玩家在当前存档里的源始终端阅读进度
public class TerminalStorySyncPacket {

    private final byte[] snapshot;

    public TerminalStorySyncPacket(TerminalStoryProgress progress) {
        try {
            this.snapshot = progress.compress();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Story snapshot could not be synced", exception);
        }
    }

    private TerminalStorySyncPacket(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(TerminalStorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.snapshot);
    }

    public static TerminalStorySyncPacket decode(FriendlyByteBuf buffer) {
        return new TerminalStorySyncPacket(buffer.readByteArray(TerminalStoryProgress.MAX_SNAPSHOT_BYTES));
    }

    public static void handle(TerminalStorySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.proto_terminal.TerminalStoryProgressCache.apply(packet.snapshot)));
        context.setPacketHandled(true);
    }
}
