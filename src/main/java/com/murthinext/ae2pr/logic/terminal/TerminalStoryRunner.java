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
                throw new IllegalArgumentException("Story program changed, reading progress must be reset");
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
            throw new IllegalArgumentException("Failed to restore Ink story", exception);
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
            throw new IllegalArgumentException("Story entry does not exist: " + entry);
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
                throw new IllegalArgumentException("Story node entry changed: " + id);
            }
            return node;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to open story node: " + id, exception);
        }
    }

    public Line advance() {
        try {
            long deadline = System.nanoTime() + 100000000;
            for (int i = 0; i < 256 && story.canContinue(); i++) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Story execution timed out, check for loops without output");
                }
                story.continueAsync(20);
                if (!story.asyncContinueComplete()) {
                    throw new IllegalStateException("Story execution timed out, check for loops without output");
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
                    throw new IllegalArgumentException("Story delay must be between 0 and 60000 ms");
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
            throw new IllegalStateException("Failed to advance story: " + activeNode, exception);
        }
    }

    private static String tag(List<String> tags, String key, String fallback) {
        return tags.stream().map(String::strip).filter(tag -> tag.startsWith(key + ":"))
                .map(tag -> tag.substring(key.length() + 1).strip()).findFirst().orElse(fallback);
    }

    public List<Choice> choices() {
        if (story.getCurrentChoices().size() > 12) {
            throw new IllegalStateException("A single choice group must not exceed 12 options");
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
                .orElseThrow(() -> new IllegalArgumentException("Story choice does not exist: " + id));
        try {
            story.chooseChoiceIndex(selected.index());
            snapshot();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to choose story option: " + id, exception);
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
            throw new IllegalStateException("Failed to save Ink story", exception);
        }
    }
}
