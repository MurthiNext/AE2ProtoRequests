package com.murthinext.ae2pr.client.proto_terminal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.murthinext.ae2pr.ae2pr;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/** 从资源包读取剧情块及其连接关系。 */
record TerminalStoryTree(List<Node> nodes) {

    private static final ResourceLocation RESOURCE = new ResourceLocation(ae2pr.MODID, "story/proto_terminal.json");

    record Node(String id, int x, int y, Component title, Component speaker, Component text,
            List<String> connections, ResourceLocation image, int imageWidth, int imageHeight) {
    }

    static TerminalStoryTree load() {
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(RESOURCE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            List<Node> nodes = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(root, "nodes")) {
                JsonObject node = element.getAsJsonObject();
                String id = GsonHelper.getAsString(node, "id");
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("剧情块 ID 重复：" + id);
                }
                List<String> connections = new ArrayList<>();
                if (node.has("connections")) {
                    for (JsonElement connection : GsonHelper.getAsJsonArray(node, "connections")) {
                        connections.add(connection.getAsString());
                    }
                }
                ResourceLocation image = node.has("image")
                        ? new ResourceLocation(GsonHelper.getAsString(node, "image")) : null;
                if (image != null && Minecraft.getInstance().getResourceManager().getResource(image).isEmpty()) {
                    ae2pr.LOGGER.warn("源始终端剧情图片不存在：{}", image);
                    image = null;
                }
                nodes.add(new Node(id, Mth.clamp(GsonHelper.getAsInt(node, "x"), -4096, 4096),
                        Mth.clamp(GsonHelper.getAsInt(node, "y"), -4096, 4096),
                        Component.translatable(GsonHelper.getAsString(node, "title")),
                        Component.translatable(GsonHelper.getAsString(node, "speaker")),
                        Component.translatable(GsonHelper.getAsString(node, "text")), List.copyOf(connections),
                        image, Mth.clamp(GsonHelper.getAsInt(node, "image_width", 256), 1, 4096),
                        Mth.clamp(GsonHelper.getAsInt(node, "image_height", 256), 1, 4096)));
                if (nodes.size() >= 128) {
                    break;
                }
            }
            if (!nodes.isEmpty()) {
                for (Node node : nodes) {
                    for (String target : node.connections()) {
                        if (!ids.contains(target)) {
                            ae2pr.LOGGER.warn("源始终端剧情连线目标不存在：{} -> {}", node.id(), target);
                        }
                    }
                }
                return new TerminalStoryTree(List.copyOf(nodes));
            }
        } catch (IOException | RuntimeException exception) {
            ae2pr.LOGGER.warn("源始终端剧情树读取失败：{}", RESOURCE, exception);
        }
        return new TerminalStoryTree(List.of(new Node("standby", 184, 100,
                Component.translatable("gui.ae2pr.proto_terminal.standby.title"),
                Component.translatable("gui.ae2pr.proto_terminal.speaker"),
                Component.translatable("gui.ae2pr.proto_terminal.standby.text"), List.of(), null, 256, 256)));
    }
}
