package com.murthinext.ae2pr.item.proto_terminal;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.murthinext.ae2pr.registry.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import com.murthinext.ae2pr.network.TerminalStoryWatchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// 终端剧情进度缓存
public final class TerminalStoryProgressCache {

    private static final int SUMMARY_LOG_LINES = 64;
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
            ModNetwork.sendToServer(TerminalStoryRequestPacket.INSTANCE);
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
            ae2pr.LOGGER.error("Failed to sync proto terminal story progress", exception);
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
            byte[] bytes = upload(snapshot, nodeId, false).compress();
            if (bytes.length > TerminalStoryProgress.MAX_UPDATE_BYTES) {
                // 超限时退化为只带权威态：对话与访问记录在下次载入时由回放重建
                bytes = upload(snapshot, nodeId, true).compress();
            }
            if (bytes.length > TerminalStoryProgress.MAX_UPDATE_BYTES) {
                throw new IOException("Story node and shared Ink state exceed the upload limit");
            }
            ModNetwork.sendToServer(new TerminalStoryUpdatePacket(bytes));
        } catch (IOException exception) {
            if (!saveErrorReported) {
                ae2pr.LOGGER.error("Failed to save proto terminal reading progress", exception);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable("gui.ae2pr.proto_terminal.save_failed"));
                }
                saveErrorReported = true;
            }
        }
    }

    private static TerminalStoryProgress upload(TerminalStoryProgress snapshot, String nodeId, boolean summaryOnly) {
        TerminalStoryProgress update = new TerminalStoryProgress();
        update.epoch = snapshot.epoch;
        update.programId = snapshot.programId;
        update.variables.putAll(snapshot.variables);
        if (summaryOnly) {
            // 不带执行位置快照；服务端会保留同一编译产物下上一次的合法快照
            if (nodeId != null) {
                update.nodes.put(nodeId, summary(snapshot.nodes.get(nodeId)));
            } else {
                snapshot.nodes.forEach((id, node) -> update.nodes.put(id, summary(node)));
            }
            return update;
        }
        update.inkState = snapshot.inkState;
        if (nodeId != null) {
            update.nodes.put(nodeId, snapshot.nodes.get(nodeId));
        } else {
            update.nodes.putAll(snapshot.nodes);
        }
        return update;
    }

    private static TerminalStoryProgress.NodeProgress summary(TerminalStoryProgress.NodeProgress node) {
        TerminalStoryProgress.NodeProgress copy = new TerminalStoryProgress.NodeProgress();
        copy.entry = node.entry;
        copy.decisions = List.copyOf(node.decisions);
        copy.finished = node.finished;
        int from = Math.max(0, node.log.size() - SUMMARY_LOG_LINES);
        copy.log = List.copyOf(node.log.subList(from, node.log.size()));
        return copy;
    }
}
