package com.murthinext.ae2pr.api.machine;

import net.minecraft.server.level.ServerPlayer;

// 机器状态同步的注入点，由网络层安装
public final class MachineStatsSender {

    @FunctionalInterface
    public interface Sink {
        void send(ServerPlayer player, int containerId, MachineStats stats);
    }

    private static Sink sink = (player, containerId, stats) -> {};

    private static boolean installed;

    private MachineStatsSender() {}

    public static synchronized void install(Sink value) {
        if (installed) {
            throw new IllegalStateException("Machine stats sender already installed");
        }
        installed = true;
        sink = value;
    }

    public static void send(ServerPlayer player, int containerId, MachineStats stats) {
        sink.send(player, containerId, stats);
    }
}
