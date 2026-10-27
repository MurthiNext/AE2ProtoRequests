package com.murthinext.ae2pr.logic.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class TerminalStoryProgress {

    public static final int MAX_JSON_BYTES = 1048576;
    public static final int MAX_UPDATE_BYTES = 30000;
    public static final int MAX_SNAPSHOT_BYTES = 1000000;
    public static final int MAX_LOG_LINES = 1024;
    private static final Gson GSON = new Gson();

    public long epoch;
    public String programId = "";
    public String inkState = "";
    public Map<String, NodeProgress> nodes = new LinkedHashMap<>();

    public record Line(String id, String speaker, String text, int delay, boolean answer) {

        public String prefix() {
            return speaker.isBlank() ? "" : "<" + speaker + "> ";
        }
    }

    public static final class Playback {

        public boolean typing;
        public int typed;
        public int delay;
        public Line pendingLine;
        public int answerDelay;
    }

    public static final class NodeProgress {

        public String entry = "";
        public List<Line> log = new ArrayList<>();
        public Set<String> visitedPaths = new LinkedHashSet<>();
        public Playback playback = new Playback();
        public boolean finished;
    }

    public static TerminalStoryProgress fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new TerminalStoryProgress();
        }
        if (json.length() > MAX_JSON_BYTES) {
            throw new IllegalArgumentException("Story snapshot is too large");
        }
        TerminalStoryProgress progress = GSON.fromJson(json, TerminalStoryProgress.class);
        if (progress == null || progress.epoch < 0 || progress.programId == null || progress.inkState == null
                || progress.nodes == null || progress.nodes.size() > 128
                || !progress.programId.isEmpty() && !progress.programId.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid story snapshot");
        }
        for (var entry : progress.nodes.entrySet()) {
            NodeProgress node = entry.getValue();
            if (entry.getKey().isBlank() || entry.getKey().length() > 256 || node == null
                    || node.entry == null || node.entry.isBlank() || node.entry.length() > 256 || node.log == null
                    || node.log.size() > MAX_LOG_LINES || node.playback == null || node.visitedPaths == null
                    || node.visitedPaths.size() > 16384
                    || node.visitedPaths.stream().anyMatch(path -> path == null || path.length() > 256)) {
                throw new IllegalArgumentException("Invalid story node progress: " + entry.getKey());
            }
            Playback playback = node.playback;
            if (playback.delay < 0 || playback.delay > 60000 || playback.answerDelay < 0 || playback.answerDelay > 320
                    || playback.typed < 0 || playback.typed > 17000
                    || playback.pendingLine != null && playback.typing) {
                throw new IllegalArgumentException("Invalid story playback state: " + entry.getKey());
            }
            for (Line line : node.log) {
                validate(line);
            }
            if (node.playback.pendingLine != null) {
                validate(node.playback.pendingLine);
            }
        }
        if (!progress.inkState.isEmpty()) {
            JsonObject state = JsonParser.parseString(progress.inkState).getAsJsonObject();
            if (!state.has("flows") || !state.get("flows").isJsonObject()
                    || !state.has("visitCounts") || !state.get("visitCounts").isJsonObject()
                    || !state.has("turnIndices") || !state.get("turnIndices").isJsonObject()
                    || !state.has("currentFlowName") || !state.get("currentFlowName").isJsonPrimitive()
                    || !state.getAsJsonObject("flows").has(state.get("currentFlowName").getAsString())
                    || !state.getAsJsonObject("flows").has("DEFAULT_FLOW") || !state.has("inkSaveVersion")) {
                throw new IllegalArgumentException("Invalid Ink snapshot");
            }
        } else if (!progress.nodes.isEmpty()) {
            throw new IllegalArgumentException("Story node progress is missing the Ink snapshot");
        }
        return progress;
    }

    static void validate(Line line) {
        if (line == null || line.id() == null || line.speaker() == null || line.text() == null
                || line.id().isBlank() || line.id().length() > 256
                || line.speaker().length() > 256 || line.text().length() > 16384
                || line.delay() < 0 || line.delay() > 60000) {
            throw new IllegalArgumentException("Invalid story line");
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public byte[] compress() throws IOException {
        byte[] json = toJson().getBytes(StandardCharsets.UTF_8);
        if (json.length > MAX_JSON_BYTES) {
            throw new IOException("Story snapshot exceeds 1 MiB");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(json);
        }
        if (output.size() > MAX_SNAPSHOT_BYTES) {
            throw new IOException("Compressed story snapshot exceeds the network limit");
        }
        return output.toByteArray();
    }

    public static TerminalStoryProgress decompress(byte[] bytes) throws IOException {
        if (bytes.length > MAX_SNAPSHOT_BYTES) {
            throw new IOException("Story packet is too large");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] json = gzip.readNBytes(MAX_JSON_BYTES + 1);
            if (json.length > MAX_JSON_BYTES) {
                throw new IOException("Decompressed story snapshot is too large");
            }
            return fromJson(new String(json, StandardCharsets.UTF_8));
        }
    }

    public boolean resetNode(String id) {
        NodeProgress removed = nodes.remove(id);
        if (removed == null) {
            return false;
        }
        if (!inkState.isEmpty()) {
            JsonObject state = JsonParser.parseString(inkState).getAsJsonObject();
            state.getAsJsonObject("flows").remove(id);
            if (id.equals(state.get("currentFlowName").getAsString())) {
                state.addProperty("currentFlowName", "DEFAULT_FLOW");
            }
            for (String path : removed.visitedPaths) {
                if (nodes.values().stream().noneMatch(node -> node.visitedPaths.contains(path))) {
                    state.getAsJsonObject("visitCounts").remove(path);
                    state.getAsJsonObject("turnIndices").remove(path);
                }
            }
            inkState = state.toString();
        }
        return true;
    }
}
