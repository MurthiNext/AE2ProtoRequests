package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.murthinext.ae2pr.ae2pr;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * 按语言读取剧情块，自动推导布局。
 */
record TerminalStoryTree(List<Node> nodes) {

    private static final String STORY_ROOT = "story/";
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final int MAX_NODES = 128;
    private static final int STEP_X = 168;
    private static final int STEP_Y = 70;
    private static final float MIN_DISTANCE = 0.25F;
    private static final float MAX_DISTANCE = 16;
    // 无坐标根剧情块的默认落点
    private static final int DEFAULT_X = -42;
    private static final int DEFAULT_Y = 140;

    /**
     * 单个剧情块。
     */
    record Node(String id, int position, String parent, int x, int y, Component title,
            List<Dialogue> dialogues, ResourceLocation image, int imageWidth, int imageHeight) {
        Component text() {
            MutableComponent merged = Component.empty();
            for (int i = 0; i < dialogues.size(); i++) {
                if (i > 0) {
                    merged.append("\n\n");
                }
                merged.append(dialogues.get(i).text());
            }
            return merged;
        }
    }

    record Dialogue(Component speaker, Component text) {
    }

    private record Raw(String id, int position, String parent, Integer x, Integer y, Anchor anchor,
            float distance, Component title, List<Dialogue> dialogues, ResourceLocation image,
            int imageWidth, int imageHeight) {
    }

    private enum Anchor {

        UP_LEFT(-1, -1), UP_CENTER(0, -1), UP_RIGHT(1, -1),
        CENTER_LEFT(-1, 0), CENTER_RIGHT(1, 0),
        DOWN_LEFT(-1, 1), DOWN_CENTER(0, 1), DOWN_RIGHT(1, 1);

        private final int dirX;
        private final int dirY;

        Anchor(int dirX, int dirY) {
            this.dirX = dirX;
            this.dirY = dirY;
        }

        static Anchor byName(String name) {
            return switch (name) {
                case "up-left" -> UP_LEFT;
                case "up-center", "up" -> UP_CENTER;
                case "up-right" -> UP_RIGHT;
                case "center-left", "left" -> CENTER_LEFT;
                case "center-right", "right" -> CENTER_RIGHT;
                case "down-left" -> DOWN_LEFT;
                case "down-center", "down" -> DOWN_CENTER;
                case "down-right" -> DOWN_RIGHT;
                default -> null;
            };
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
        List<Raw> raws = read(minecraft, minecraft.getLanguageManager().getSelected());
        if (raws.isEmpty()) {
            raws = read(minecraft, FALLBACK_LANGUAGE);
        }
        if (raws.isEmpty()) {
            return new TerminalStoryTree(List.of(standby()));
        }
        raws.sort(Comparator.comparingInt(Raw::position).thenComparing(Raw::id));
        if (raws.size() > MAX_NODES) {
            raws = new ArrayList<>(raws.subList(0, MAX_NODES));
        }
        Map<String, Raw> byId = new HashMap<>();
        for (Raw raw : raws) {
            byId.put(raw.id(), raw);
        }
        for (Raw raw : raws) {
            if (raw.parent() != null && (raw.parent().equals(raw.id()) || !byId.containsKey(raw.parent()))) {
                ae2pr.LOGGER.warn("源始终端剧情父节点无效：{} -> {}", raw.id(), raw.parent());
            }
        }
        Map<String, int[]> positions = new HashMap<>();
        List<Node> nodes = new ArrayList<>();
        for (Raw raw : raws) {
            int[] position = resolve(raw, byId, positions, new HashSet<>());
            nodes.add(new Node(raw.id(), raw.position(), raw.parent(), position[0], position[1], raw.title(),
                    raw.dialogues(), raw.image(), raw.imageWidth(), raw.imageHeight()));
        }
        return new TerminalStoryTree(List.copyOf(nodes));
    }

    /**
     * 显式 x/y 优先，否则依据父节点方位与距离乘数推导位置。
     */
    private static int[] resolve(Raw raw, Map<String, Raw> byId, Map<String, int[]> positions,
            Set<String> visiting) {
        int[] cached = positions.get(raw.id());
        if (cached != null) {
            return cached;
        }
        int[] position;
        if (raw.x() != null) {
            position = new int[] { raw.x(), raw.y() };
        } else if (raw.parent() == null) {
            position = new int[] { DEFAULT_X, DEFAULT_Y };
        } else if (!visiting.add(raw.id())) {
            ae2pr.LOGGER.warn("源始终端剧情父节点成环：{} -> {}", raw.id(), raw.parent());
            return null;
        } else {
            Raw parent = raw.parent().equals(raw.id()) ? null : byId.get(raw.parent());
            int[] parentPosition = parent == null ? null : resolve(parent, byId, positions, visiting);
            visiting.remove(raw.id());
            position = parentPosition == null ? null : anchorPosition(raw, parentPosition);
        }
        if (position == null) {
            ae2pr.LOGGER.warn("源始终端剧情父节点无法定位：{} -> {}", raw.id(), raw.parent());
            position = new int[] { DEFAULT_X, DEFAULT_Y };
        }
        positions.put(raw.id(), position);
        return position;
    }

    private static int[] anchorPosition(Raw raw, int[] parentPosition) {
        Anchor anchor = raw.anchor() == null ? Anchor.CENTER_RIGHT : raw.anchor();
        return new int[] { parentPosition[0] + anchor.offsetX(raw.distance()),
                parentPosition[1] + anchor.offsetY(raw.distance()) };
    }

    private static List<Raw> read(Minecraft minecraft, String language) {
        ResourceManager resourceManager = minecraft.getResourceManager();
        List<Raw> raws = new ArrayList<>();
        try {
            for (Map.Entry<ResourceLocation, Resource> entry : resourceManager
                    .listResources(STORY_ROOT + language, TerminalStoryTree::isStoryFile).entrySet()) {
                String path = entry.getKey().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                try (Reader reader = entry.getValue().openAsReader()) {
                    raws.add(parse(id, JsonParser.parseReader(reader).getAsJsonObject(), resourceManager));
                } catch (IOException | RuntimeException exception) {
                    ae2pr.LOGGER.warn("源始终端剧情块读取失败：{}", entry.getKey(), exception);
                }
            }
        } catch (RuntimeException exception) {
            ae2pr.LOGGER.warn("源始终端剧情目录读取失败：{}", STORY_ROOT + language, exception);
        }
        return raws;
    }

    private static boolean isStoryFile(ResourceLocation location) {
        return location.getNamespace().equals(ae2pr.MODID) && location.getPath().endsWith(".json");
    }

    private static Raw parse(String id, JsonObject json, ResourceManager resourceManager) {
        List<Dialogue> dialogues = new ArrayList<>();
        if (json.has("dialogues")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "dialogues")) {
                JsonObject dialogue = element.getAsJsonObject();
                dialogues.add(new Dialogue(Component.literal(GsonHelper.getAsString(dialogue, "speaker", "")),
                        Component.literal(GsonHelper.getAsString(dialogue, "text"))));
            }
        } else {
            ae2pr.LOGGER.warn("源始终端剧情块没有对话内容：{}", id);
        }
        String parent = GsonHelper.getAsString(json, "parent", "");
        Integer x = json.has("x") ? Mth.clamp(GsonHelper.getAsInt(json, "x"), -4096, 4096) : null;
        Integer y = json.has("y") ? Mth.clamp(GsonHelper.getAsInt(json, "y"), -4096, 4096) : null;
        if ((x == null) != (y == null)) {
            ae2pr.LOGGER.warn("源始终端剧情块 x/y 需同时提供：{}", id);
            x = null;
            y = null;
        }
        String anchorName = GsonHelper.getAsString(json, "anchor", "");
        Anchor anchor = anchorName.isBlank() ? null : Anchor.byName(anchorName);
        if (!anchorName.isBlank() && anchor == null) {
            ae2pr.LOGGER.warn("源始终端剧情块方位无效：{} -> {}", id, anchorName);
        }
        if (x != null && anchor != null) {
            ae2pr.LOGGER.warn("源始终端剧情块同时提供坐标与方位：{}，以坐标为准", id);
        }
        ResourceLocation image = json.has("image")
                ? ResourceLocation.parse(GsonHelper.getAsString(json, "image"))
                : null;
        if (image != null && resourceManager.getResource(image).isEmpty()) {
            ae2pr.LOGGER.warn("源始终端剧情图片不存在：{}", image);
            image = null;
        }
        return new Raw(id, GsonHelper.getAsInt(json, "position", 0), parent.isBlank() ? null : parent, x, y,
                anchor, Mth.clamp(GsonHelper.getAsFloat(json, "distance", 1), MIN_DISTANCE, MAX_DISTANCE),
                Component.literal(GsonHelper.getAsString(json, "title")), List.copyOf(dialogues), image,
                Mth.clamp(GsonHelper.getAsInt(json, "image_width", 256), 1, 4096),
                Mth.clamp(GsonHelper.getAsInt(json, "image_height", 256), 1, 4096));
    }

    private static Node standby() {
        return new Node("standby", 0, null, DEFAULT_X, DEFAULT_Y,
                Component.translatable("gui.ae2pr.proto_terminal.standby.title"),
                List.of(new Dialogue(Component.translatable("gui.ae2pr.proto_terminal.speaker"),
                        Component.translatable("gui.ae2pr.proto_terminal.standby.text"))),
                null, 256, 256);
    }
}
