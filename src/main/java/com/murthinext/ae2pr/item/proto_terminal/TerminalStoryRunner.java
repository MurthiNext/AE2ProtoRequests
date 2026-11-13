package com.murthinext.ae2pr.item.proto_terminal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bladecoder.ink.runtime.Story;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.murthinext.ae2pr.item.proto_terminal.TerminalStoryProgress.Line;
import com.murthinext.ae2pr.item.proto_terminal.TerminalStoryProgress.NodeProgress;

/**
 * 单玩家的 Ink 运行器。
 *
 * <p>存档里的 {@code variables}、{@code decisions}、{@code finished} 属于权威态，与编译产物无关。
 * {@code inkState} 与当前编译产物一致时直接加载；不一致时按选择记录在新程序上回放，
 * 重建各章执行位置与对话记录，再以存档变量为准。回放失败的章节退回本章开头，其余章节不受影响。
 */
public final class TerminalStoryRunner {

    // 决策 token：a<N> 连续推进 N 句，c<选项 ID> 选择某个选项
    private static final String ADVANCE = "a";
    private static final String CHOOSE = "c";

    public record Choice(String id, String text, int index) {
    }

    private final Story story;
    private final TerminalStoryProgress progress;
    private String activeNode;
    private boolean programChanged;

    public TerminalStoryRunner(String program, TerminalStoryProgress saved, String playerName) {
        try {
            story = new Story(program);
            progress = saved;
            String programId = programId(program);
            boolean programDiffers = !saved.programId.isEmpty() && !saved.programId.equals(programId);
            if (!programDiffers && !saved.inkState.isEmpty()) {
                story.getState().loadJson(saved.inkState);
            } else if (!saved.nodes.isEmpty() || programDiffers) {
                // 编译产物变了或快照缺失：按选择记录回放
                rebuild(programId, programDiffers);
            }
            progress.programId = programId;
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

    public boolean programChanged() {
        return programChanged;
    }

    public NodeProgress open(String id, String entry) {
        if (!hasEntry(entry)) {
            throw new IllegalArgumentException("Story entry does not exist: " + entry);
        }
        try {
            activeNode = id;
            NodeProgress node = progress.nodes.computeIfAbsent(id, key -> new NodeProgress());
            node.entry = entry;
            boolean started = story.aliveFlowNames().contains(id);
            story.switchFlow(id);
            if (node.finished) {
                story.getState().forceEnd();
                return node;
            }
            if (!started) {
                // 没有可接续的执行位置，旧台词也不再对应当前程序，从头重读本章
                node.log = new ArrayList<>();
                node.decisions.clear();
                story.choosePathString(entry);
                snapshot();
            }
            return node;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to open story node: " + id, exception);
        }
    }

    public Line advance() {
        try {
            Line line = nextLine();
            if (line != null) {
                recordAdvance();
            }
            snapshot();
            return line;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to advance story: " + activeNode, exception);
        }
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
            recordChoice(id);
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
            NodeProgress node = progress.nodes.get(activeNode);
            node.finished = true;
            // 读完的章节只需保留回顾用的台词，决策记录没有回放价值
            node.decisions.clear();
        }
    }

    public TerminalStoryProgress snapshot() {
        try {
            progress.inkState = story.getState().toJson();
            progress.variables.clear();
            JsonElement variables = JsonParser.parseString(progress.inkState).getAsJsonObject().get("variablesState");
            if (variables != null && variables.isJsonObject()) {
                for (String name : variables.getAsJsonObject().keySet()) {
                    JsonPrimitive primitive = primitive(variable(name));
                    if (primitive != null) {
                        progress.variables.put(name, primitive);
                    }
                }
            }
            return progress;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save Ink story", exception);
        }
    }

    private static JsonPrimitive primitive(Object value) {
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        return value instanceof String text ? new JsonPrimitive(text) : null;
    }

    private Line nextLine() throws Exception {
        long deadline = System.nanoTime() + 100000000L;
        for (int i = 0; i < 256 && story.canContinue(); i++) {
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("Story execution timed out, check for loops without output");
            }
            story.continueAsync(20);
            if (!story.asyncContinueComplete()) {
                throw new IllegalStateException("Story execution timed out, check for loops without output");
            }
            String output = story.getCurrentText().strip();
            if (output.isBlank()) {
                continue;
            }
            List<String> tags = story.getCurrentTags();
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
            return line;
        }
        return null;
    }

    private static String tag(List<String> tags, String key, String fallback) {
        return tags.stream().map(String::strip).filter(tag -> tag.startsWith(key + ":"))
                .map(tag -> tag.substring(key.length() + 1).strip()).findFirst().orElse(fallback);
    }

    private void recordAdvance() {
        NodeProgress node = activeNode == null ? null : progress.nodes.get(activeNode);
        if (node == null) {
            return;
        }
        List<String> decisions = node.decisions;
        if (!decisions.isEmpty() && decisions.get(decisions.size() - 1).startsWith(ADVANCE)) {
            int count = Integer.parseInt(decisions.get(decisions.size() - 1).substring(ADVANCE.length()));
            decisions.set(decisions.size() - 1, ADVANCE + (count + 1));
            return;
        }
        append(node, ADVANCE + 1);
    }

    private void recordChoice(String id) {
        NodeProgress node = activeNode == null ? null : progress.nodes.get(activeNode);
        if (node != null) {
            append(node, CHOOSE + id);
        }
    }

    private void append(NodeProgress node, String decision) {
        if (node.decisions.size() >= TerminalStoryProgress.MAX_DECISIONS) {
            // 记录过长说明本章线性推进极多，回放代价过高：放弃记录，程序变更时直接重读本章
            node.decisions.clear();
            return;
        }
        node.decisions.add(decision);
    }

    /**
     * 用新程序重建执行位置。
     *
     * <p>变量、完成标记与对话属于权威态，直接保留；每章的回放会在新程序上重演一遍推进与选择，
     * 得到与存档一致的执行位置和更新后的台词。无法回放的章节退回本章开头重读。
     */
    private void rebuild(String programId, boolean programDiffers) {
        programChanged = programDiffers;
        Map<String, JsonPrimitive> savedVariables = new LinkedHashMap<>(progress.variables);
        progress.inkState = "";
        // 回放前先灌入存档变量
        restoreVariables(savedVariables);
        for (Map.Entry<String, NodeProgress> entry : progress.nodes.entrySet()) {
            NodeProgress node = entry.getValue();
            node.playback = new TerminalStoryProgress.Playback();
            if (node.finished) {
                node.decisions.clear();
                continue;
            }
            if (!node.decisions.isEmpty() && replay(entry.getKey(), node)) {
                continue;
            }
            // 分支被改写或未记录选择：该章从开头重读
            node.decisions.clear();
            node.log = new ArrayList<>();
            try {
                story.removeFlow(entry.getKey());
            } catch (Exception ignored) {
                // 该 Flow 尚未建立
            }
        }
        restoreVariables(savedVariables);
    }

    // 回放会重新执行脚本里的赋值，结束时以存档变量覆盖
    private boolean replay(String id, NodeProgress node) {
        List<Line> replayed = new ArrayList<>();
        String previous = activeNode;
        activeNode = id;
        try {
            story.switchFlow(id);
            story.choosePathString(node.entry);
            for (String decision : node.decisions) {
                if (decision.startsWith(ADVANCE)) {
                    int count = Integer.parseInt(decision.substring(ADVANCE.length()));
                    for (int i = 0; i < count; i++) {
                        Line line = nextLine();
                        if (line == null) {
                            return false;
                        }
                        replayed.add(line);
                    }
                } else if (decision.startsWith(CHOOSE)) {
                    String choiceId = decision.substring(CHOOSE.length());
                    Choice selected = choices().stream().filter(choice -> choice.id().equals(choiceId))
                            .findFirst().orElse(null);
                    if (selected == null) {
                        return false;
                    }
                    story.chooseChoiceIndex(selected.index());
                } else {
                    return false;
                }
            }
            if (replayed.size() > TerminalStoryProgress.MAX_LOG_LINES) {
                replayed = replayed.subList(replayed.size() - TerminalStoryProgress.MAX_LOG_LINES,
                        replayed.size());
            }
            node.log = replayed;
            return true;
        } catch (Exception exception) {
            return false;
        } finally {
            activeNode = previous;
        }
    }

    private void restoreVariables(Map<String, JsonPrimitive> variables) {
        for (Map.Entry<String, JsonPrimitive> entry : variables.entrySet()) {
            Object value = value(entry.getValue());
            if (value == null || variable(entry.getKey()) == null) {
                continue;
            }
            try {
                story.getVariablesState().set(entry.getKey(), value);
            } catch (Exception ignored) {
                // 新程序改变了该变量的类型，保留默认值
            }
        }
    }

    private static Object value(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            String text = primitive.getAsString();
            return text.contains(".") || text.contains("e") || text.contains("E")
                    ? (Object) primitive.getAsFloat() : (Object) primitive.getAsInt();
        }
        return primitive.getAsString();
    }
}
