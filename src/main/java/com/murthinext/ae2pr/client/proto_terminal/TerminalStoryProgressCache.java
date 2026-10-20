package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryProgress;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import com.murthinext.ae2pr.network.TerminalStoryWatchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 当前存档的客户端剧情快照与服务端背包条件缓存。
 */
public final class TerminalStoryProgressCache {

    private static final Set<String> satisfiedUnlocks = new LinkedHashSet<>();
    private static TerminalStoryProgress progress = new TerminalStoryProgress();
    private static boolean synced;
    private static boolean unlocksSynced;
    private static boolean saveErrorReported;

    private TerminalStoryProgressCache() {
    }

    public static TerminalStoryProgress get() {
        return progress;
    }

    public static boolean isUnlocked(String key) {
        return satisfiedUnlocks.contains(key);
    }

    public static boolean isSynced() {
        return synced;
    }

    public static boolean areUnlocksSynced() {
        return unlocksSynced;
    }

    public static void reset() {
        progress = new TerminalStoryProgress();
        satisfiedUnlocks.clear();
        synced = false;
        unlocksSynced = false;
        saveErrorReported = false;
    }

    public static void request(Collection<String> conditions) {
        synced = false;
        unlocksSynced = conditions.isEmpty();
        satisfiedUnlocks.clear();
        if (Minecraft.getInstance().getConnection() != null) {
            ModNetwork.sendToServer(new TerminalStoryRequestPacket());
            ModNetwork.sendToServer(new TerminalStoryWatchPacket(List.copyOf(conditions)));
        }
    }

    public static void stopWatching() {
        if (Minecraft.getInstance().getConnection() != null) {
            ModNetwork.sendToServer(new TerminalStoryWatchPacket(List.of()));
        }
    }

    public static void applyUnlocks(Collection<String> conditions) {
        unlocksSynced = true;
        satisfiedUnlocks.clear();
        satisfiedUnlocks.addAll(conditions);
        if (Minecraft.getInstance().screen instanceof TerminalScreen screen) {
            screen.onUnlocksSynced();
        }
    }

    public static void apply(byte[] snapshot) {
        try {
            progress = TerminalStoryProgress.decompress(snapshot);
            synced = true;
            saveErrorReported = false;
            TerminalScreen.onProgressSynced();
        } catch (IOException | RuntimeException exception) {
            ae2pr.LOGGER.error("源始终端进度同步失败", exception);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.translatable("gui.ae2pr.proto_terminal.load_failed"));
            }
        }
    }

    public static void record(TerminalStoryProgress snapshot, String nodeId) {
        progress = snapshot;
        if (!synced || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        try {
            TerminalStoryProgress update = new TerminalStoryProgress();
            update.epoch = snapshot.epoch;
            update.programId = snapshot.programId;
            update.inkState = snapshot.inkState;
            if (nodeId != null) {
                update.nodes.put(nodeId, snapshot.nodes.get(nodeId));
            }
            byte[] bytes = update.compress();
            if (bytes.length > TerminalStoryProgress.MAX_UPDATE_BYTES) {
                throw new IOException("单个剧情块与共享 Ink 状态超过上传上限");
            }
            ModNetwork.sendToServer(new TerminalStoryUpdatePacket(bytes));
        } catch (IOException exception) {
            if (!saveErrorReported) {
                ae2pr.LOGGER.error("源始终端阅读进度未能保存", exception);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable("gui.ae2pr.proto_terminal.save_failed"));
                }
                saveErrorReported = true;
            }
        }
    }
}
