package com.murthinext.ae2pr.logic.terminal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.bladecoder.ink.runtime.Story;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryProgress.Line;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryProgress.NodeProgress;

/**
 * 每位玩家共享变量、每个剧情块独立 Flow 的 Ink 运行器。
 */
public final class TerminalStoryRunner {

    public record Choice(String id, String text, int index) {
    }

    private final Story story;
    private final TerminalStoryProgress progress;
    private String activeNode;

    public TerminalStoryRunner(String program, TerminalStoryProgress saved, String playerName) {
        try {
            story = new Story(program);
            String programId = programId(program);
            if (!saved.programId.isEmpty() && !saved.programId.equals(programId)) {
                throw new IllegalArgumentException("剧情程序已改变，需要重置阅读进度");
            }
            progress = saved;
            progress.programId = programId;
            if (!saved.inkState.isEmpty()) {
                story.getState().loadJson(saved.inkState);
            }
            if (hasVariable("player_name")) {
                story.getVariablesState().set("player_name", playerName);
            }
            snapshot();
        } catch (Exception exception) {
            throw new IllegalArgumentException("无法恢复 Ink 剧情", exception);
        }
    }

    public static String programId(String program) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(program.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public boolean hasEntry(String entry) {
        return story.hasFunction(entry);
    }

    public boolean hasVariable(String name) {
        return variable(name) != null;
    }

    public Object variable(String name) {
        return story.getVariablesState().get(name);
    }

    public boolean completed(String id) {
        NodeProgress node = progress.nodes.get(id);
        return node != null && node.finished;
    }

    public NodeProgress open(String id, String entry) {
        if (!hasEntry(entry)) {
            throw new IllegalArgumentException("剧情入口不存在：" + entry);
        }
        try {
            activeNode = id;
            boolean started = progress.nodes.containsKey(id);
            story.switchFlow(id);
            NodeProgress node = progress.nodes.computeIfAbsent(id, key -> new NodeProgress());
            if (!started) {
                node.entry = entry;
                story.choosePathString(entry);
                snapshot();
            } else if (!node.entry.equals(entry)) {
                throw new IllegalArgumentException("剧情块入口已改变：" + id);
            }
            return node;
        } catch (Exception exception) {
            throw new IllegalStateException("无法打开剧情块：" + id, exception);
        }
    }

    public Line advance() {
        try {
            long deadline = System.nanoTime() + 100000000;
            for (int i = 0; i < 256 && story.canContinue(); i++) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("剧情执行超时，请检查无输出的循环");
                }
                story.continueAsync(20);
                if (!story.asyncContinueComplete()) {
                    throw new IllegalStateException("剧情执行超时，请检查无输出的循环");
                }
                String output = story.getCurrentText().strip();
                List<String> tags = story.getCurrentTags();
                if (output.isBlank()) {
                    continue;
                }
                String id = tag(tags, "id", story.getState().getCurrentPathString());
                String speaker = tag(tags, "speaker", "");
                int separator = output.indexOf(':');
                if (speaker.isEmpty() && separator > 0 && separator <= 64
                        && !output.substring(0, separator).contains("\n")) {
                    speaker = output.substring(0, separator).strip();
                    output = output.substring(separator + 1).stripLeading();
                }
                int delay = Integer.parseInt(tag(tags, "delay", "0"));
                if (delay < 0 || delay > 60000) {
                    throw new IllegalArgumentException("剧情延时应在 0–60000 毫秒之间");
                }
                Line line = new Line(id == null ? activeNode + "/end" : id, speaker,
                        output.replace("<br/>", "\n"), delay, tags.contains("answer"));
                TerminalStoryProgress.validate(line);
                snapshot();
                return line;
            }
            snapshot();
            return null;
        } catch (Exception exception) {
            throw new IllegalStateException("剧情推进失败：" + activeNode, exception);
        }
    }

    private static String tag(List<String> tags, String key, String fallback) {
        return tags.stream().map(String::strip).filter(tag -> tag.startsWith(key + ":"))
                .map(tag -> tag.substring(key.length() + 1).strip()).findFirst().orElse(fallback);
    }

    public List<Choice> choices() {
        if (story.getCurrentChoices().size() > 12) {
            throw new IllegalStateException("同一组选项不能超过 12 个");
        }
        return story.getCurrentChoices().stream().map(choice -> {
            try {
                return new Choice(tag(choice.getTags() == null ? List.of() : choice.getTags(), "id",
                        choice.getPathStringOnChoice()), choice.getText(), choice.getIndex());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }).toList();
    }

    public void choose(String id) {
        Choice selected = choices().stream().filter(choice -> choice.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("剧情选项不存在：" + id));
        try {
            story.chooseChoiceIndex(selected.index());
            snapshot();
        } catch (Exception exception) {
            throw new IllegalStateException("无法选择剧情选项：" + id, exception);
        }
    }

    public boolean canContinue() {
        return story.canContinue();
    }

    public void append(Line line) {
        NodeProgress node = progress.nodes.get(activeNode);
        node.log.add(line);
        if (node.log.size() > TerminalStoryProgress.MAX_LOG_LINES) {
            node.log.remove(0);
        }
    }

    public void finish() {
        if (!canContinue() && choices().isEmpty()) {
            progress.nodes.get(activeNode).finished = true;
        }
    }

    public TerminalStoryProgress snapshot() {
        try {
            String json = story.getState().toJson();
            if (activeNode != null) {
                JsonObject before = progress.inkState.isEmpty() ? new JsonObject()
                        : JsonParser.parseString(progress.inkState).getAsJsonObject().getAsJsonObject("visitCounts");
                JsonObject after = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("visitCounts");
                for (var entry : after.entrySet()) {
                    if (!entry.getValue().equals(before.get(entry.getKey()))) {
                        progress.nodes.get(activeNode).visitedPaths.add(entry.getKey());
                    }
                }
            }
            progress.inkState = json;
            return progress;
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存 Ink 剧情", exception);
        }
    }
}
