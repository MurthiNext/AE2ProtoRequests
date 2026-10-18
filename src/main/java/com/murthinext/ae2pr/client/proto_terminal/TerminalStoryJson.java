package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Anchor;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Choice;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Definition;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Line;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Unlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * 终端剧情资源与阅读进度的 JSON 读写。
 */
final class TerminalStoryJson {

    private static final Gson GSON = new Gson();
    private static final String STORY_ROOT = "story/";
    private static final int MAX_LINES = 256;
    private static final int MAX_CHOICES = 12;
    private static final int MAX_NESTING = 4;
    private static final int MAX_UNLOCKS = 8;
    private static final int MAX_UNLOCK_COUNT = 9999;
    private static final int MAX_TAG_LENGTH = 64;
    private static final float MIN_DISTANCE = 0.25F;
    private static final float MAX_DISTANCE = 16;

    private TerminalStoryJson() {
    }

    static List<Definition> read(ResourceManager resourceManager, String language) {
        List<Definition> definitions = new ArrayList<>();
        try {
            for (Map.Entry<ResourceLocation, Resource> entry : resourceManager
                    .listResources(STORY_ROOT + language, location -> location.getNamespace().equals(ae2pr.MODID)
                            && location.getPath().endsWith(".json")).entrySet()) {
                String path = entry.getKey().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                try (Reader reader = entry.getValue().openAsReader()) {
                    definitions.add(parse(id, JsonParser.parseReader(reader).getAsJsonObject(), resourceManager));
                } catch (IOException | RuntimeException exception) {
                    ae2pr.LOGGER.warn("源始终端剧情块读取失败：{}", entry.getKey(), exception);
                }
            }
        } catch (RuntimeException exception) {
            ae2pr.LOGGER.warn("源始终端剧情目录读取失败：{}", STORY_ROOT + language, exception);
        }
        return definitions;
    }

    static Definition parse(String id, JsonObject json, ResourceManager resourceManager) {
        List<Line> lines = parseLines(json, id);
        if (lines.isEmpty()) {
            ae2pr.LOGGER.warn("源始终端剧情块没有台词内容：{}", id);
        }
        List<String> requires = tags(json, "requires");
        String parent = GsonHelper.getAsString(json, "parent", "");
        Integer x = json.has("x") ? Mth.clamp(GsonHelper.getAsInt(json, "x"), -4096, 4096) : null;
        Integer y = json.has("y") ? Mth.clamp(GsonHelper.getAsInt(json, "y"), -4096, 4096) : null;
        if ((x == null) != (y == null)) {
            ae2pr.LOGGER.warn("源始终端剧情块 x/y 需同时提供：{}", id);
            x = null;
            y = null;
        }
        String anchorName = GsonHelper.getAsString(json, "anchor", "");
        Anchor anchor = switch (anchorName) {
            case "up-left" -> Anchor.UP_LEFT;
            case "up-center", "up" -> Anchor.UP_CENTER;
            case "up-right" -> Anchor.UP_RIGHT;
            case "center-left", "left" -> Anchor.CENTER_LEFT;
            case "center-right", "right" -> Anchor.CENTER_RIGHT;
            case "down-left" -> Anchor.DOWN_LEFT;
            case "down-center", "down" -> Anchor.DOWN_CENTER;
            case "down-right" -> Anchor.DOWN_RIGHT;
            default -> null;
        };
        if (!anchorName.isBlank() && anchor == null) {
            ae2pr.LOGGER.warn("源始终端剧情块方位无效：{} -> {}", id, anchorName);
        }
        if (x != null && anchor != null) {
            ae2pr.LOGGER.warn("源始终端剧情块同时提供坐标与方位：{}，以坐标为准", id);
        }
        ResourceLocation image = json.has("image")
                ? ResourceLocation.parse(GsonHelper.getAsString(json, "image")) : null;
        if (image != null && resourceManager.getResource(image).isEmpty()) {
            ae2pr.LOGGER.warn("源始终端剧情图片不存在：{}", image);
            image = null;
        }
        return new Definition(id, GsonHelper.getAsInt(json, "position", 0), parent.isBlank() ? null : parent, x, y,
                anchor, Mth.clamp(GsonHelper.getAsFloat(json, "distance", 1), MIN_DISTANCE, MAX_DISTANCE),
                Component.literal(GsonHelper.getAsString(json, "title")), lines, requires, parseUnlocks(json, id),
                GsonHelper.getAsString(json, "tip", ""), image,
                Mth.clamp(GsonHelper.getAsInt(json, "image_width", 256), 1, 4096),
                Mth.clamp(GsonHelper.getAsInt(json, "image_height", 256), 1, 4096));
    }

    /**
     * 剧情块的额外解锁条件。
     */
    private static List<Unlock> parseUnlocks(JsonObject json, String id) {
        if (!json.has("unlocks") || !json.get("unlocks").isJsonArray()) {
            return List.of();
        }
        List<Unlock> unlocks = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("unlocks")) {
            if (unlocks.size() >= MAX_UNLOCKS) {
                ae2pr.LOGGER.warn("源始终端剧情块解锁条件过多，已截断：{}", id);
                break;
            }
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject unlock = element.getAsJsonObject();
            boolean tag = unlock.has("tag");
            String raw = GsonHelper.getAsString(unlock, tag ? "tag" : "item", "").trim();
            if (raw.isEmpty()) {
                continue;
            }
            ResourceLocation location;
            try {
                location = ResourceLocation.parse(raw);
            } catch (RuntimeException exception) {
                ae2pr.LOGGER.warn("源始终端剧情块解锁条件无效：{} -> {}", id, raw);
                continue;
            }
            unlocks.add(new Unlock(location, tag,
                    Mth.clamp(GsonHelper.getAsInt(unlock, "count", 1), 1, MAX_UNLOCK_COUNT)));
        }
        return List.copyOf(unlocks);
    }

    private static List<Line> parseLines(JsonObject json, String id) {
        JsonElement element = json.get("lines");
        return element == null || !element.isJsonArray() ? List.of()
                : parseLineArray(element.getAsJsonArray(), id, 0);
    }

    private static List<Line> parseLineArray(JsonArray array, String id, int depth) {
        List<Line> lines = new ArrayList<>();
        for (JsonElement element : array) {
            if (lines.size() >= MAX_LINES) {
                ae2pr.LOGGER.warn("源始终端剧情台词过多，已截断：{}", id);
                break;
            }
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject line = element.getAsJsonObject();
            String speaker = GsonHelper.getAsString(line, "speaker", "").trim();
            lines.add(new Line(speaker.isEmpty() ? null : Component.literal(speaker),
                    GsonHelper.getAsString(line, "text", ""),
                    Mth.clamp(GsonHelper.getAsInt(line, "delay", 0), 0, 60000), tags(line, "requires"),
                    parseChoices(line, id, depth)));
        }
        return List.copyOf(lines);
    }

    private static List<Choice> parseChoices(JsonObject line, String id, int depth) {
        if (!line.has("choices") || !line.get("choices").isJsonArray()) {
            return List.of();
        }
        if (depth >= MAX_NESTING) {
            ae2pr.LOGGER.warn("源始终端剧情选项嵌套过深，已忽略：{}", id);
            return List.of();
        }
        List<Choice> choices = new ArrayList<>();
        for (JsonElement element : line.getAsJsonArray("choices")) {
            if (choices.size() >= MAX_CHOICES) {
                ae2pr.LOGGER.warn("源始终端剧情选项过多，已截断：{}", id);
                break;
            }
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject choice = element.getAsJsonObject();
            String mode = GsonHelper.getAsString(choice, "mode", "main");
            if (!mode.equals("main") && !mode.equals("side")) {
                ae2pr.LOGGER.warn("源始终端剧情选项模式无效：{} -> {}，按 main 处理", id, mode);
            }
            String choiceText = GsonHelper.getAsString(choice, "text", "");
            String contentText = GsonHelper.getAsString(choice, "content", "");
            List<Line> branch = choice.has("lines") && choice.get("lines").isJsonArray()
                    ? parseLineArray(choice.getAsJsonArray("lines"), id, depth + 1) : List.of();
            choices.add(new Choice(choiceText, contentText.isBlank() ? choiceText : contentText,
                    GsonHelper.getAsBoolean(choice, "silence", false), !mode.equals("side"),
                    tags(choice, "tags"),
                    tags(choice, "requires"), branch));
        }
        return List.copyOf(choices);
    }

    private static List<String> tags(JsonObject json, String name) {
        if (!json.has(name) || !json.get(name).isJsonArray()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(name)) {
            if (element.isJsonPrimitive()) {
                String tag = element.getAsString().trim();
                if (!tag.isEmpty() && tag.length() <= MAX_TAG_LENGTH && !tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        return List.copyOf(tags);
    }

    static TerminalStoryProgress parseProgress(String json) {
        if (json == null || json.isBlank()) {
            return new TerminalStoryProgress();
        }
        try {
            TerminalStoryProgress progress = GSON.fromJson(json, TerminalStoryProgress.class);
            return progress == null || progress.tags == null || progress.nodes == null
                    ? new TerminalStoryProgress() : progress;
        } catch (JsonSyntaxException exception) {
            ae2pr.LOGGER.warn("源始终端阅读进度解析失败", exception);
            return new TerminalStoryProgress();
        }
    }

    static String writeProgress(TerminalStoryProgress progress) {
        return GSON.toJson(progress);
    }

    static String writeProgress(TerminalStoryProgress.NodeProgress node) {
        return GSON.toJson(node);
    }
}
