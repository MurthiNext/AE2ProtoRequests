package com.murthinext.ae2pr.logic.terminal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.murthinext.ae2pr.ae2pr;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 存档内的源始终端阅读进度，按玩家 UUID 保存。
 */
public class TerminalStorySavedData extends SavedData {

    private static final String DATA_NAME = "ae2pr_terminal_story";
    private static final String PLAYERS_KEY = "players";
    private static final int MAX_NODE_JSON = 16384;
    private static final int MAX_NODES = 256;
    private static final int MAX_PROGRESS = 1048576;

    private final Map<UUID, String> players = new HashMap<>();

    public static TerminalStorySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(TerminalStorySavedData::load, TerminalStorySavedData::new, DATA_NAME);
    }

    /**
     * 玩家当前存档的完整进度 JSON，没有记录时返回空串。
     */
    public String get(UUID player) {
        String json = players.getOrDefault(player, "");
        if (json.length() > MAX_PROGRESS) {
            ae2pr.LOGGER.warn("源始终端阅读进度过大，已忽略：{}", player);
            return "";
        }
        return json;
    }

    /**
     * 合并单个剧情块进度与标签集合。
     */
    public void merge(UUID player, String nodeId, String nodeJson, List<String> tags) {
        if (nodeId.isEmpty() || nodeJson == null || nodeJson.length() > MAX_NODE_JSON) {
            return;
        }
        JsonObject root;
        try {
            root = players.containsKey(player) ? JsonParser.parseString(players.get(player)).getAsJsonObject()
                    : new JsonObject();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            ae2pr.LOGGER.warn("源始终端阅读进度损坏，已重建：{}", player, exception);
            root = new JsonObject();
        }
        JsonObject node;
        try {
            node = JsonParser.parseString(nodeJson).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            ae2pr.LOGGER.warn("源始终端剧情进度无效：{} / {}", player, nodeId, exception);
            return;
        }
        JsonArray tagArray = new JsonArray();
        for (String tag : tags) {
            tagArray.add(tag);
        }
        root.add("tags", tagArray);
        JsonObject nodes = root.has("nodes") && root.get("nodes").isJsonObject()
                ? root.getAsJsonObject("nodes") : new JsonObject();
        if (!nodes.has(nodeId) && nodes.size() >= MAX_NODES) {
            ae2pr.LOGGER.warn("源始终端阅读进度剧情块过多，已忽略：{} / {}", player, nodeId);
            return;
        }
        nodes.add(nodeId, node);
        root.add("nodes", nodes);
        players.put(player, root.toString());
        setDirty();
    }

    public void resetAll(UUID player) {
        if (players.remove(player) != null) {
            setDirty();
        }
    }

    public boolean resetNode(UUID player, String nodeId) {
        String json = players.get(player);
        if (json == null) {
            return false;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            players.remove(player);
            setDirty();
            ae2pr.LOGGER.warn("源始终端阅读进度损坏，已清空：{}", player, exception);
            return false;
        }
        if (!root.has("nodes") || !root.get("nodes").isJsonObject()
                || root.getAsJsonObject("nodes").remove(nodeId) == null) {
            return false;
        }
        players.put(player, root.toString());
        setDirty();
        return true;
    }

    private static TerminalStorySavedData load(CompoundTag tag) {
        TerminalStorySavedData data = new TerminalStorySavedData();
        CompoundTag playersTag = tag.getCompound(PLAYERS_KEY);
        for (String key : playersTag.getAllKeys()) {
            try {
                data.players.put(UUID.fromString(key), playersTag.getString(key));
            } catch (IllegalArgumentException exception) {
                ae2pr.LOGGER.warn("源始终端阅读进度玩家标识无效：{}", key);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((player, json) -> playersTag.putString(player.toString(), json));
        tag.put(PLAYERS_KEY, playersTag);
        return tag;
    }
}
