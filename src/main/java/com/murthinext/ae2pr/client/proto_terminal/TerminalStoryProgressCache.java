package com.murthinext.ae2pr.client.proto_terminal;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import com.murthinext.ae2pr.network.TerminalStoryWatchPacket;
import net.minecraft.client.Minecraft;

/**
 * 客户端缓存的玩家阅读进度与已满足的解锁条件；进度改动即时上报服务端存档。
 */
public final class TerminalStoryProgressCache {

    // 与 TerminalStoryUpdatePacket 的字段上限保持一致
    private static final int MAX_PAYLOAD = 16000;

    private static final Set<String> touched = new HashSet<>();
    private static final Set<String> satisfiedUnlocks = new LinkedHashSet<>();
    private static TerminalStoryProgress progress = new TerminalStoryProgress();
    // 服务端首次快照是否已经抵达
    private static boolean synced;

    private TerminalStoryProgressCache() {
    }

    public static TerminalStoryProgress get() {
        return progress;
    }

    public static TerminalStoryProgress.NodeProgress node(String id) {
        return progress.nodes.get(id);
    }

    public static boolean isUnlocked(String key) {
        return satisfiedUnlocks.contains(key);
    }

    public static boolean isSynced() {
        return synced;
    }

    public static void reset() {
        progress = new TerminalStoryProgress();
        satisfiedUnlocks.clear();
        touched.clear();
        synced = false;
    }

    // 请求进度快照
    public static void request(Collection<String> conditions) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ModNetwork.sendToServer(new TerminalStoryRequestPacket());
        ModNetwork.sendToServer(new TerminalStoryWatchPacket(List.copyOf(conditions)));
    }

    public static void stopWatching() {
        if (Minecraft.getInstance().getConnection() != null) {
            ModNetwork.sendToServer(new TerminalStoryWatchPacket(List.of()));
        }
    }

    public static void applyUnlocks(Collection<String> conditions) {
        satisfiedUnlocks.clear();
        satisfiedUnlocks.addAll(conditions);
        if (Minecraft.getInstance().screen instanceof TerminalScreen screen) {
            screen.onUnlocksSynced();
        }
    }

    public static void apply(String json, boolean replace) {
        synced = true;
        if (replace) {
            progress = TerminalStoryJson.parseProgress(json);
            touched.clear();
            TerminalScreen.onProgressSynced(true);
            return;
        }
        TerminalStoryProgress syncedProgress = TerminalStoryJson.parseProgress(json);
        for (String tag : progress.tags) {
            if (!syncedProgress.tags.contains(tag)) {
                syncedProgress.tags.add(tag);
            }
        }
        for (Map.Entry<String, TerminalStoryProgress.NodeProgress> entry : progress.nodes.entrySet()) {
            if (touched.contains(entry.getKey())) {
                syncedProgress.nodes.put(entry.getKey(), entry.getValue());
            }
        }
        progress = syncedProgress;
        touched.clear();
        TerminalScreen.onProgressSynced(false);
    }

    public static void record(String nodeId, TerminalStoryProgress.NodeProgress node, Collection<String> tags) {
        progress.nodes.put(nodeId, node);
        progress.tags = List.copyOf(tags);
        touched.add(nodeId);
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        String json = TerminalStoryJson.writeProgress(node);
        if (json.length() > MAX_PAYLOAD) {
            ae2pr.LOGGER.warn("源始终端剧情进度过大，未上报服务端：{}（{} 字符）", nodeId, json.length());
            return;
        }
        ModNetwork.sendToServer(new TerminalStoryUpdatePacket(nodeId, json, progress.tags));
    }
}
