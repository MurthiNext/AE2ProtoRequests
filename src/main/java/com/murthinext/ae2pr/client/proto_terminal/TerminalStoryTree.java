package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryCondition;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryProgress;
import com.murthinext.ae2pr.logic.terminal.TerminalStoryRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

/**
 * 与剧情脚本分离的终端节点目录与布局。
 */
record TerminalStoryTree(List<Node> nodes, String program) {

    private static final String NODE_ROOT = "story/nodes";
    private static final int STEP_X = 168;
    private static final int STEP_Y = 70;

    record Node(String id, String entry, int position, List<String> links, int x, int y, Component title,
            TerminalStoryCondition visibleWhen, TerminalStoryCondition availableWhen, String tip,
            ResourceLocation image, int imageWidth, int imageHeight) {
    }

    private record Layout(Integer x, Integer y, String relativeTo, int dx, int dy) {
    }

    static TerminalStoryTree load() throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        return load(minecraft.getResourceManager(), minecraft.getLanguageManager().getSelected(),
                TerminalStoryText.playerName());
    }

    static TerminalStoryTree load(ResourceManager resources, String selectedLanguage, String playerName)
            throws IOException {
        String program = null;
        for (String language : List.of(selectedLanguage, "en_us", "zh_cn")) {
            var resource = resources.getResource(ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
                    "story/" + language + "/main.ink.json"));
            if (resource.isPresent()) {
                try (Reader reader = resource.get().openAsReader()) {
                    program = JsonParser.parseReader(reader).toString();
                }
                break;
            }
        }
        if (program == null) {
            throw new IOException("未找到已编译的 Ink 剧情，请先运行 compileStories");
        }
        TerminalStoryRunner runner = new TerminalStoryRunner(program, new TerminalStoryProgress(), playerName);
        Map<String, Node> definitions = new LinkedHashMap<>();
        Map<String, Layout> layouts = new HashMap<>();
        for (var entry : resources.listResources(NODE_ROOT,
                id -> id.getPath().startsWith(NODE_ROOT + "/") && id.getPath().endsWith(".json")).entrySet()) {
            String path = entry.getKey().getPath();
            String id = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(),
                    path.substring(NODE_ROOT.length() + 1, path.length() - 5)).toString();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String knot = GsonHelper.getAsString(json, "entry");
                if (id.length() > 256 || knot.length() > 256 || !runner.hasEntry(knot)) {
                    throw new IllegalArgumentException("剧情入口不存在：" + knot);
                }
                List<String> links = new ArrayList<>();
                if (json.has("links")) {
                    for (var link : json.getAsJsonArray("links")) {
                        links.add(link.getAsString());
                    }
                }
                ResourceLocation image = json.has("image")
                        ? ResourceLocation.parse(GsonHelper.getAsString(json, "image")) : null;
                if (image != null && resources.getResource(image).isEmpty()) {
                    throw new IllegalArgumentException("剧情图片不存在：" + image);
                }
                int imageWidth = GsonHelper.getAsInt(json, "image_width", 256);
                int imageHeight = GsonHelper.getAsInt(json, "image_height", 256);
                if (imageWidth < 1 || imageWidth > 4096 || imageHeight < 1 || imageHeight > 4096) {
                    throw new IllegalArgumentException("剧情图片尺寸无效");
                }
                definitions.put(id, new Node(id, knot, GsonHelper.getAsInt(json, "position", 0), List.copyOf(links),
                        0, 0, Component.translatable(GsonHelper.getAsString(json, "title")),
                        TerminalStoryCondition.parse(json.get("visible_when")),
                        TerminalStoryCondition.parse(json.get("available_when")),
                        GsonHelper.getAsString(json, "locked_tip", ""), image, imageWidth, imageHeight));
                layouts.put(id, parseLayout(GsonHelper.getAsJsonObject(json, "layout", new JsonObject())));
            } catch (RuntimeException exception) {
                throw new IOException("剧情节点无效：" + entry.getKey(), exception);
            }
        }
        if (definitions.isEmpty() || definitions.size() > 128) {
            throw new IOException("剧情节点目录必须包含 1–128 个节点");
        }
        Map<String, int[]> positions = new HashMap<>();
        List<Node> nodes = new ArrayList<>();
        for (Node node : definitions.values()) {
            for (String link : node.links()) {
                if (link.equals(node.id()) || !definitions.containsKey(link)) {
                    throw new IOException("剧情连线引用无效：" + node.id() + " -> " + link);
                }
            }
            node.visibleWhen().validate(definitions.keySet(), runner);
            node.availableWhen().validate(definitions.keySet(), runner);
            int[] position = resolve(node.id(), layouts, positions, new HashSet<>());
            nodes.add(new Node(node.id(), node.entry(), node.position(), node.links(), position[0], position[1],
                    node.title(), node.visibleWhen(), node.availableWhen(), node.tip(),
                    node.image(), node.imageWidth(), node.imageHeight()));
        }
        nodes.sort(Comparator.comparingInt(Node::position).thenComparing(Node::id));
        TerminalStoryTree tree = new TerminalStoryTree(List.copyOf(nodes), program);
        if (tree.inventoryKeys().size() > 128) {
            throw new IOException("背包条件不能超过 128 条");
        }
        return tree;
    }

    List<String> inventoryKeys() {
        return nodes.stream().flatMap(node -> java.util.stream.Stream.concat(
                node.visibleWhen().inventoryKeys().stream(), node.availableWhen().inventoryKeys().stream()))
                .distinct().toList();
    }

    private static Layout parseLayout(JsonObject json) {
        Integer x = json.has("x") ? GsonHelper.getAsInt(json, "x") : null;
        Integer y = json.has("y") ? GsonHelper.getAsInt(json, "y") : null;
        String relative = GsonHelper.getAsString(json, "relative_to", "");
        if ((x == null) != (y == null) || x != null && !relative.isEmpty()) {
            throw new IllegalArgumentException("布局需成对提供 x/y，且不能同时指定 relative_to");
        }
        String anchor = GsonHelper.getAsString(json, "anchor", "center-right");
        int[] direction = switch (anchor) {
            case "up-left" -> new int[] { -1, -1 };
            case "up-center" -> new int[] { 0, -1 };
            case "up-right" -> new int[] { 1, -1 };
            case "center-left" -> new int[] { -1, 0 };
            case "center-right" -> new int[] { 1, 0 };
            case "down-left" -> new int[] { -1, 1 };
            case "down-center" -> new int[] { 0, 1 };
            case "down-right" -> new int[] { 1, 1 };
            default -> throw new IllegalArgumentException("剧情布局方位无效：" + anchor);
        };
        float distance = GsonHelper.getAsFloat(json, "distance", 1);
        if (!Float.isFinite(distance) || distance < 0.25F || distance > 16) {
            throw new IllegalArgumentException("布局距离应在 0.25–16 之间");
        }
        return new Layout(x, y, relative, Math.round(direction[0] * STEP_X * distance),
                Math.round(direction[1] * STEP_Y * distance));
    }

    private static int[] resolve(String id, Map<String, Layout> layouts, Map<String, int[]> positions,
            Set<String> visiting) {
        if (positions.containsKey(id)) {
            return positions.get(id);
        }
        Layout layout = layouts.get(id);
        if (layout == null || !visiting.add(id)) {
            throw new IllegalArgumentException("布局引用不存在或成环：" + id);
        }
        int[] position;
        if (layout.x() != null) {
            position = new int[] { layout.x(), layout.y() };
        } else if (layout.relativeTo().isBlank()) {
            position = new int[] { 42, 140 };
        } else {
            int[] parent = resolve(layout.relativeTo(), layouts, positions, visiting);
            position = new int[] { parent[0] + layout.dx(), parent[1] + layout.dy() };
        }
        if (Math.abs((long) position[0]) > 4096 || Math.abs((long) position[1]) > 4096) {
            throw new IllegalArgumentException("布局坐标超出范围：" + id);
        }
        visiting.remove(id);
        positions.put(id, position);
        return position;
    }
}
