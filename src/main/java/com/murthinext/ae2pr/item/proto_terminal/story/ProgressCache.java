package com.murthinext.ae2pr.item.proto_terminal.story;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.core.network.ModNetwork;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryRequestPacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryUpdatePacket;
import com.murthinext.ae2pr.item.proto_terminal.network.StoryWatchPacket;
import com.murthinext.ae2pr.item.proto_terminal.screen.TerminalScreen;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// 终端剧情进度缓存
public final class ProgressCache {

    private static final int SUMMARY_LOG_LINES = 64;
    private static final Set<String> satisfiedUnlocks = new LinkedHashSet<>();
    private static Progress progress = new Progress();
    private static boolean synced;
    private static boolean unlocksSynced;
    private static boolean saveErrorReported;

    private ProgressCache() {}

    public static Progress get() {
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
        progress = new Progress();
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
            ModNetwork.sendToServer(StoryRequestPacket.INSTANCE);
            ModNetwork.sendToServer(new StoryWatchPacket(List.copyOf(conditions)));
        }
    }

    public static void stopWatching() {
        if (Minecraft.getInstance().getConnection() != null) {
            ModNetwork.sendToServer(new StoryWatchPacket(List.of()));
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
            progress = Progress.decompress(snapshot);
            synced = true;
            saveErrorReported = false;
            TerminalScreen.onProgressSynced();
        } catch (IOException | RuntimeException exception) {
            ae2pr.LOGGER.error("Failed to sync proto terminal story progress", exception);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance()
                        .player
                        .sendSystemMessage(Component.translatable("gui.ae2pr.proto_terminal.load_failed"));
            }
        }
    }

    public static void record(Progress snapshot, String nodeId) {
        progress = snapshot;
        if (!synced || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        try {
            byte[] bytes = upload(snapshot, nodeId, false).compress();
            if (bytes.length > Progress.MAX_UPDATE_BYTES) {
                // 超限时退化为只带权威态：对话与访问记录在下次载入时由回放重建
                bytes = upload(snapshot, nodeId, true).compress();
            }
            if (bytes.length > Progress.MAX_UPDATE_BYTES) {
                throw new IOException("Story node and shared Ink state exceed the upload limit");
            }
            ModNetwork.sendToServer(new StoryUpdatePacket(bytes));
        } catch (IOException exception) {
            if (!saveErrorReported) {
                ae2pr.LOGGER.error("Failed to save proto terminal reading progress", exception);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance()
                            .player
                            .sendSystemMessage(Component.translatable("gui.ae2pr.proto_terminal.save_failed"));
                }
                saveErrorReported = true;
            }
        }
    }

    private static Progress upload(Progress snapshot, String nodeId, boolean summaryOnly) {
        Progress update = new Progress();
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

    private static Progress.NodeProgress summary(Progress.NodeProgress node) {
        Progress.NodeProgress copy = new Progress.NodeProgress();
        copy.entry = node.entry;
        copy.decisions = List.copyOf(node.decisions);
        copy.finished = node.finished;
        int from = Math.max(0, node.log.size() - SUMMARY_LOG_LINES);
        copy.log = List.copyOf(node.log.subList(from, node.log.size()));
        return copy;
    }
}
