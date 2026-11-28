package com.murthinext.ae2pr.item.proto_terminal.story;

import com.murthinext.ae2pr.ae2pr;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class ProgressStore extends SavedData {

    private static final String DATA_NAME = "ae2pr_terminal_ink";
    private final Map<UUID, Progress> players = new HashMap<>();

    public static ProgressStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProgressStore::load, ProgressStore::new, DATA_NAME);
    }

    public Progress get(UUID player) {
        return players.getOrDefault(player, new Progress());
    }

    public boolean update(UUID player, Progress progress) throws IOException {
        Progress current = players.get(player);
        if (progress.epoch != (current == null ? 0 : current.epoch)) {
            return false;
        }
        // 上传只带当前块，其余块沿用存档
        if (current != null) {
            Map<String, Progress.NodeProgress> nodes = new LinkedHashMap<>(current.nodes);
            nodes.putAll(progress.nodes);
            progress.nodes = nodes;
            // 快照只在同一编译产物下可复用
            if (progress.inkState.isEmpty() && progress.programId.equals(current.programId)) {
                progress.inkState = current.inkState;
            }
        }
        if (progress.nodes.size() > 128) {
            throw new IOException("Story node save data exceeds the size limit");
        }
        players.put(player, progress);
        setDirty();
        return true;
    }

    public void resetAll(UUID player) {
        Progress previous = players.get(player);
        Progress progress = new Progress();
        progress.epoch = previous == null ? 1 : previous.epoch + 1;
        players.put(player, progress);
        setDirty();
    }

    public boolean resetNode(UUID player, String nodeId) {
        Progress progress = players.get(player);
        if (progress == null || !progress.resetNode(nodeId)) {
            return false;
        }
        progress.epoch++;
        setDirty();
        return true;
    }

    private static ProgressStore load(CompoundTag tag) {
        ProgressStore data = new ProgressStore();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                Progress progress = Progress.decompress(players.getByteArray(key));
                data.players.put(UUID.fromString(key), progress);
            } catch (IOException | RuntimeException exception) {
                ae2pr.LOGGER.warn(
                        "Invalid proto terminal story save data, ignored record of player {}", key, exception);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((player, progress) -> {
            try {
                playersTag.putByteArray(player.toString(), progress.compress());
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to save player story snapshot: " + player, exception);
            }
        });
        tag.put("players", playersTag);
        return tag;
    }
}
