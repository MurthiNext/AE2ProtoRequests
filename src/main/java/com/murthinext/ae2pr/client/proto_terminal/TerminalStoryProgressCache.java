package com.murthinext.ae2pr.client.proto_terminal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.network.TerminalStoryRequestPacket;
import com.murthinext.ae2pr.network.TerminalStoryUpdatePacket;
import net.minecraft.client.Minecraft;

/**
 * 客户端缓存的玩家阅读进度；改动即时上报服务端存档。
 */
public final class TerminalStoryProgressCache {

    // 与 TerminalStoryUpdatePacket 的字段上限保持一致
    private static final int MAX_PAYLOAD = 16000;

    private static final Set<String> touched = new HashSet<>();
    private static TerminalStoryProgress progress = new TerminalStoryProgress();

    private TerminalStoryProgressCache() {
    }

    public static TerminalStoryProgress get() {
        return progress;
    }

    public static TerminalStoryProgress.NodeProgress node(String id) {
        return progress.nodes.get(id);
    }

    public static void reset() {
        progress = new TerminalStoryProgress();
        touched.clear();
    }

    public static void request() {
        if (Minecraft.getInstance().getConnection() != null) {
            ModNetwork.sendToServer(new TerminalStoryRequestPacket());
        }
    }

    public static void apply(String json, boolean replace) {
        if (replace) {
            progress = TerminalStoryJson.parseProgress(json);
            touched.clear();
            TerminalScreen.onProgressSynced(true);
            return;
        }
        TerminalStoryProgress synced = TerminalStoryJson.parseProgress(json);
        for (String tag : progress.tags) {
            if (!synced.tags.contains(tag)) {
                synced.tags.add(tag);
            }
        }
        for (Map.Entry<String, TerminalStoryProgress.NodeProgress> entry : progress.nodes.entrySet()) {
            if (touched.contains(entry.getKey())) {
                synced.nodes.put(entry.getKey(), entry.getValue());
            }
        }
        progress = synced;
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
