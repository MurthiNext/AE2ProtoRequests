package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.murthinext.ae2pr.ae2pr;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 终端剧情树的数据结构与节点布局。
 */
record TerminalStoryTree(List<Node> nodes) {

    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final int MAX_NODES = 128;
    private static final int STEP_X = 168;
    private static final int STEP_Y = 70;
    // 无坐标根剧情块的默认落点
    private static final int DEFAULT_X = 42;
    private static final int DEFAULT_Y = 140;

    /**
     * 单个剧情块。
     */
    record Node(String id, int position, String parent, int x, int y, Component title,
            List<Line> lines, List<String> requires, List<Unlock> unlocks, String tip,
            ResourceLocation image, int imageWidth, int imageHeight) {
    }

    /**
     * 一条台词；text 保留原始标记，由 TerminalStoryText 在播放时展开。
     */
    record Line(Component speaker, String text, int delay, List<String> requires, List<Choice> choices) {

        String prefix() {
            return speaker == null ? "" : "<" + TerminalStoryText.plainOf(speaker.getString()) + "> ";
        }

        Component display() {
            return Component.literal(prefix() + TerminalStoryText.plainOf(text));
        }
    }

    /**
     * 剧情块的额外解锁条件；id 为物品或物品标签（tag 为 true 时）的标识。
     */
    record Unlock(ResourceLocation id, boolean tag, int count) {
    }

    /**
     * 分叉口选项：main 为主线选项；side 为旁支选项；text 为选项文本；content 为内容文本。
     */
    record Choice(String text, String content, boolean silence, boolean mainLine, List<String> tags,
            List<String> requires, List<Line> lines) {
    }

    record Definition(String id, int position, String parent, Integer x, Integer y, Anchor anchor,
            float distance, Component title, List<Line> lines, List<String> requires, List<Unlock> unlocks,
            String tip, ResourceLocation image, int imageWidth, int imageHeight) {
    }

    enum Anchor {

        UP_LEFT(-1, -1), UP_CENTER(0, -1), UP_RIGHT(1, -1),
        CENTER_LEFT(-1, 0), CENTER_RIGHT(1, 0),
        DOWN_LEFT(-1, 1), DOWN_CENTER(0, 1), DOWN_RIGHT(1, 1);

        private final int dirX;
        private final int dirY;

        Anchor(int dirX, int dirY) {
            this.dirX = dirX;
            this.dirY = dirY;
        }

        int offsetX(float distance) {
            return Math.round(dirX * STEP_X * distance);
        }

        int offsetY(float distance) {
            return Math.round(dirY * STEP_Y * distance);
        }
    }

    static TerminalStoryTree load() {
        Minecraft minecraft = Minecraft.getInstance();
        List<Definition> definitions = TerminalStoryJson.read(minecraft.getResourceManager(),
                minecraft.getLanguageManager().getSelected());
        if (definitions.isEmpty()) {
            definitions = TerminalStoryJson.read(minecraft.getResourceManager(), FALLBACK_LANGUAGE);
        }
        if (definitions.isEmpty()) {
            return new TerminalStoryTree(List.of(standby()));
        }
        definitions.sort(Comparator.comparingInt(Definition::position).thenComparing(Definition::id));
        if (definitions.size() > MAX_NODES) {
            definitions = new ArrayList<>(definitions.subList(0, MAX_NODES));
        }
        Map<String, Definition> byId = new HashMap<>();
        for (Definition definition : definitions) {
            byId.put(definition.id(), definition);
        }
        for (Definition definition : definitions) {
            if (definition.parent() != null
                    && (definition.parent().equals(definition.id()) || !byId.containsKey(definition.parent()))) {
                ae2pr.LOGGER.warn("源始终端剧情父节点无效：{} -> {}", definition.id(), definition.parent());
            }
        }
        Map<String, int[]> positions = new HashMap<>();
        List<Node> nodes = new ArrayList<>();
        for (Definition definition : definitions) {
            int[] position = resolve(definition, byId, positions, new HashSet<>());
            nodes.add(new Node(definition.id(), definition.position(), definition.parent(), position[0], position[1],
                    definition.title(), definition.lines(), definition.requires(), definition.unlocks(),
                    definition.tip(), definition.image(), definition.imageWidth(), definition.imageHeight()));
        }
        return new TerminalStoryTree(List.copyOf(nodes));
    }

    /**
     * 显式 x/y 优先，否则依据父节点方位与距离乘数推导位置。
     */
    private static int[] resolve(Definition definition, Map<String, Definition> byId, Map<String, int[]> positions,
            Set<String> visiting) {
        int[] cached = positions.get(definition.id());
        if (cached != null) {
            return cached;
        }
        int[] position;
        if (definition.x() != null) {
            position = new int[] { definition.x(), definition.y() };
        } else if (definition.parent() == null) {
            position = new int[] { DEFAULT_X, DEFAULT_Y };
        } else if (!visiting.add(definition.id())) {
            ae2pr.LOGGER.warn("源始终端剧情父节点成环：{} -> {}", definition.id(), definition.parent());
            return null;
        } else {
            Definition parent = definition.parent().equals(definition.id()) ? null : byId.get(definition.parent());
            int[] parentPosition = parent == null ? null : resolve(parent, byId, positions, visiting);
            visiting.remove(definition.id());
            position = parentPosition == null ? null : anchorPosition(definition, parentPosition);
        }
        if (position == null) {
            ae2pr.LOGGER.warn("源始终端剧情父节点无法定位：{} -> {}", definition.id(), definition.parent());
            position = new int[] { DEFAULT_X, DEFAULT_Y };
        }
        positions.put(definition.id(), position);
        return position;
    }

    private static int[] anchorPosition(Definition definition, int[] parentPosition) {
        Anchor anchor = definition.anchor() == null ? Anchor.CENTER_RIGHT : definition.anchor();
        return new int[] { parentPosition[0] + anchor.offsetX(definition.distance()),
                parentPosition[1] + anchor.offsetY(definition.distance()) };
    }

    private static Node standby() {
        return new Node("standby", 0, null, DEFAULT_X, DEFAULT_Y,
                Component.translatable("gui.ae2pr.proto_terminal.standby.title"),
                List.of(new Line(Component.translatable("gui.ae2pr.proto_terminal.speaker"),
                        Component.translatable("gui.ae2pr.proto_terminal.standby.text").getString(), 0, List.of(),
                        List.of())),
                List.of(), List.of(), "", null, 256, 256);
    }
}
