package com.murthinext.ae2pr.logic.terminal;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.murthinext.ae2pr.ae2pr;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class TerminalStorySavedData extends SavedData {

    private static final String DATA_NAME = "ae2pr_terminal_ink";
    private final Map<UUID, TerminalStoryProgress> players = new HashMap<>();

    public static TerminalStorySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(TerminalStorySavedData::load, TerminalStorySavedData::new, DATA_NAME);
    }

    public TerminalStoryProgress get(UUID player) {
        return players.getOrDefault(player, new TerminalStoryProgress());
    }

    public boolean update(UUID player, TerminalStoryProgress progress) throws IOException {
        TerminalStoryProgress current = players.get(player);
        if (progress.epoch != (current == null ? 0 : current.epoch)) {
            return false;
        }
        if (current != null && current.programId.equals(progress.programId)) {
            var nodes = new LinkedHashMap<>(current.nodes);
            nodes.putAll(progress.nodes);
            progress.nodes = nodes;
        }
        if (progress.nodes.size() > 128) {
            throw new IOException("Story node save data exceeds the size limit");
        }
        progress.compress();
        players.put(player, progress);
        setDirty();
        return true;
    }

    public void resetAll(UUID player) {
        TerminalStoryProgress previous = players.get(player);
        TerminalStoryProgress progress = new TerminalStoryProgress();
        progress.epoch = previous == null ? 1 : previous.epoch + 1;
        players.put(player, progress);
        setDirty();
    }

    public boolean resetNode(UUID player, String nodeId) {
        TerminalStoryProgress progress = players.get(player);
        if (progress == null || !progress.resetNode(nodeId)) {
            return false;
        }
        progress.epoch++;
        setDirty();
        return true;
    }

    private static TerminalStorySavedData load(CompoundTag tag) {
        TerminalStorySavedData data = new TerminalStorySavedData();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                TerminalStoryProgress progress = TerminalStoryProgress.decompress(players.getByteArray(key));
                data.players.put(UUID.fromString(key), progress);
            } catch (IOException | RuntimeException exception) {
                ae2pr.LOGGER.warn("Invalid proto terminal story save data, ignored record of player {}", key,
                        exception);
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
