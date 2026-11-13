package com.murthinext.ae2pr.item.proto_terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * 玩家阅读进度。
 *
 * <p>数据分两层：
 * <ul>
 * <li>权威态——{@code variables}、{@code decisions}、{@code finished}、{@code log}。
 * 它们只描述玩家做过什么、看到过什么，与编译产物无关，更新剧情时全部保留。</li>
 * <li>派生态——{@code inkState}（Ink 执行位置快照）。它只在 {@code programId} 与当前程序一致时有效，
 * 否则由 {@link TerminalStoryRunner} 回放权威态重建。</li>
 * </ul>
 * 因此重新编译剧情不会清空进度；最坏情况只是某个分支被改动的章节退回本章开头。
 */
public final class TerminalStoryProgress {

    public static final int SCHEMA_VERSION = 3;
    public static final int MAX_JSON_BYTES = 1048576;
    public static final int MAX_UPDATE_BYTES = 30000;
    public static final int MAX_SNAPSHOT_BYTES = 1000000;
    public static final int MAX_LOG_LINES = 1024;
    public static final int MAX_DECISIONS = 8192;
    private static final Gson GSON = new Gson();

    public int schemaVersion = SCHEMA_VERSION;
    public long epoch;
    public String programId = "";
    // 派生态
    public String inkState = "";
    // 权威态
    public Map<String, JsonPrimitive> variables = new LinkedHashMap<>();
    public Map<String, NodeProgress> nodes = new LinkedHashMap<>();

    public record Line(String id, String speaker, String text, int delay, boolean answer) {

        public String prefix() {
            return speaker.isBlank() ? "" : "<" + speaker + "> ";
        }
    }

    public static final class NodeProgress {

        public String entry = "";
        public List<String> decisions = new ArrayList<>();
        public boolean finished;
        public List<Line> log = new ArrayList<>();
        public Playback playback = new Playback();
    }

    public static final class Playback {

        public boolean typing;
        public int typed;
        public int delay;
        public Line pendingLine;
        public int answerDelay;
    }

    public static TerminalStoryProgress fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new TerminalStoryProgress();
        }
        if (json.length() > MAX_JSON_BYTES) {
            throw new IllegalArgumentException("Story snapshot is too large");
        }
        TerminalStoryProgress progress = GSON.fromJson(json, TerminalStoryProgress.class);
        if (progress == null || progress.schemaVersion != SCHEMA_VERSION || progress.epoch < 0 ||
            progress.programId == null || progress.inkState == null || progress.variables == null ||
            progress.nodes == null || progress.variables.size() > 256 || progress.nodes.size() > 128 ||
            !progress.programId.isEmpty() && !progress.programId.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid story snapshot");
        }
        for (var entry : progress.variables.entrySet()) {
            JsonPrimitive value = entry.getValue();
            if (entry.getKey().isBlank() || entry.getKey().length() > 256 || value == null ||
                value.isNumber() && !Double.isFinite(value.getAsDouble())) {
                throw new IllegalArgumentException("Invalid story variable: " + entry.getKey());
            }
        }
        for (var entry : progress.nodes.entrySet()) {
            NodeProgress node = entry.getValue();
            if (entry.getKey().isBlank() || entry.getKey().length() > 256 || node == null || node.entry == null ||
                node.entry.isBlank() || node.entry.length() > 256 || node.decisions == null ||
                node.decisions.size() > MAX_DECISIONS ||
                node.decisions.stream().anyMatch(
                        decision -> decision == null || decision.isBlank() || decision.length() > 256) ||
                node.log == null || node.log.size() > MAX_LOG_LINES) {
                throw new IllegalArgumentException("Invalid story node progress: " + entry.getKey());
            }
            node.playback = node.playback == null ? new Playback() : node.playback;
            for (Line line : node.log) {
                validate(line);
            }
            Playback playback = node.playback;
            if (playback.delay < 0 || playback.delay > 60000 || playback.answerDelay < 0 ||
                playback.answerDelay > 320 || playback.typed < 0 || playback.typed > 17000 ||
                playback.pendingLine != null && playback.typing) {
                throw new IllegalArgumentException("Invalid story playback state: " + entry.getKey());
            }
            if (playback.pendingLine != null) {
                validate(playback.pendingLine);
            }
        }
        if (!progress.inkState.isEmpty()) {
            JsonObject state = JsonParser.parseString(progress.inkState).getAsJsonObject();
            if (!state.has("flows") || !state.get("flows").isJsonObject() || !state.has("visitCounts") ||
                !state.get("visitCounts").isJsonObject() || !state.has("turnIndices") ||
                !state.get("turnIndices").isJsonObject() || !state.has("currentFlowName") ||
                !state.get("currentFlowName").isJsonPrimitive() ||
                !state.getAsJsonObject("flows").has(state.get("currentFlowName").getAsString()) ||
                !state.getAsJsonObject("flows").has("DEFAULT_FLOW") || !state.has("inkSaveVersion")) {
                throw new IllegalArgumentException("Invalid Ink snapshot");
            }
        }
        return progress;
    }

    static void validate(Line line) {
        if (line == null || line.id() == null || line.speaker() == null || line.text() == null || line.id().isBlank() ||
            line.id().length() > 256 || line.speaker().length() > 256 || line.text().length() > 16384 ||
            line.delay() < 0 || line.delay() > 60000) {
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
        if (nodes.remove(id) == null) {
            return false;
        }
        inkState = "";
        return true;
    }
}
