package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
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

/** 按语言从资源包读取剧情块，并以父节点推导剧情树连接。 */
record TerminalStoryTree(List<Node> nodes) {

    private static final String STORY_ROOT = "story/";
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final int MAX_NODES = 128;

    /** 单个剧情块；dialogues 承载块内可扩展的多轮对话。 */
    record Node(String id, int position, String parent, int x, int y, Component title,
            List<Dialogue> dialogues, ResourceLocation image, int imageWidth, int imageHeight) {

        /** 取首轮对话的说话者，供单页展示。 */
        Component speaker() {
            return dialogues.isEmpty() ? Component.empty() : dialogues.get(0).speaker();
        }

        /** 合并全部对话文本，供单页展示。 */
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

    /** 一轮对话。 */
    record Dialogue(Component speaker, Component text) {
    }

    static TerminalStoryTree load() {
        Minecraft minecraft = Minecraft.getInstance();
        List<Node> nodes = read(minecraft, minecraft.getLanguageManager().getSelected());
        if (nodes.isEmpty()) {
            nodes = read(minecraft, FALLBACK_LANGUAGE);
        }
        if (nodes.isEmpty()) {
            return new TerminalStoryTree(List.of(standby()));
        }
        nodes.sort(Comparator.comparingInt(Node::position).thenComparing(Node::id));
        if (nodes.size() > MAX_NODES) {
            nodes = new ArrayList<>(nodes.subList(0, MAX_NODES));
        }
        Set<String> ids = new HashSet<>();
        for (Node node : nodes) {
            ids.add(node.id());
        }
        for (Node node : nodes) {
            if (node.parent() != null && (node.parent().equals(node.id()) || !ids.contains(node.parent()))) {
                ae2pr.LOGGER.warn("源始终端剧情父节点无效：{} -> {}", node.id(), node.parent());
            }
        }
        return new TerminalStoryTree(List.copyOf(nodes));
    }

    /** 读取指定语言目录下的全部剧情块。 */
    private static List<Node> read(Minecraft minecraft, String language) {
        ResourceManager resourceManager = minecraft.getResourceManager();
        List<Node> nodes = new ArrayList<>();
        try {
            for (Map.Entry<ResourceLocation, Resource> entry : resourceManager
                    .listResources(STORY_ROOT + language, TerminalStoryTree::isStoryFile).entrySet()) {
                String path = entry.getKey().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                try (Reader reader = entry.getValue().openAsReader()) {
                    nodes.add(parse(id, JsonParser.parseReader(reader).getAsJsonObject(), resourceManager));
                } catch (IOException | RuntimeException exception) {
                    ae2pr.LOGGER.warn("源始终端剧情块读取失败：{}", entry.getKey(), exception);
                }
            }
        } catch (RuntimeException exception) {
            ae2pr.LOGGER.warn("源始终端剧情目录读取失败：{}", STORY_ROOT + language, exception);
        }
        return nodes;
    }

    private static boolean isStoryFile(ResourceLocation location) {
        return location.getNamespace().equals(ae2pr.MODID) && location.getPath().endsWith(".json");
    }

    private static Node parse(String id, JsonObject json, ResourceManager resourceManager) {
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
        ResourceLocation image = json.has("image")
                ? ResourceLocation.parse(GsonHelper.getAsString(json, "image"))
                : null;
        if (image != null && resourceManager.getResource(image).isEmpty()) {
            ae2pr.LOGGER.warn("源始终端剧情图片不存在：{}", image);
            image = null;
        }
        return new Node(id, GsonHelper.getAsInt(json, "position", 0), parent.isBlank() ? null : parent,
                Mth.clamp(GsonHelper.getAsInt(json, "x"), -4096, 4096),
                Mth.clamp(GsonHelper.getAsInt(json, "y"), -4096, 4096),
                Component.literal(GsonHelper.getAsString(json, "title")), List.copyOf(dialogues), image,
                Mth.clamp(GsonHelper.getAsInt(json, "image_width", 256), 1, 4096),
                Mth.clamp(GsonHelper.getAsInt(json, "image_height", 256), 1, 4096));
    }

    private static Node standby() {
        return new Node("standby", 0, null, 184, 100,
                Component.translatable("gui.ae2pr.proto_terminal.standby.title"),
                List.of(new Dialogue(Component.translatable("gui.ae2pr.proto_terminal.speaker"),
                        Component.translatable("gui.ae2pr.proto_terminal.standby.text"))),
                null, 256, 256);
    }
}
