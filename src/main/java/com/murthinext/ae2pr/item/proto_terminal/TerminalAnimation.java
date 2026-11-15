package com.murthinext.ae2pr.item.proto_terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;

final class TerminalAnimation {

    static final long OPEN_DURATION = 900;
    static final long CONTENT_START = 250;
    static final long HEADER_START = OPEN_DURATION - 150;
    static final long NODE_DURATION = 240;
    static final long CONNECTION_DURATION = 180;
    private static final long HEADER_DURATION = 160;
    private final Map<String, Entrance> entrances = new HashMap<>();

    private record Entrance(int layer, List<TerminalStoryTree.Node> sources) {}

    TerminalAnimation(List<TerminalStoryTree.Node> nodes) {
        Map<String, TerminalStoryTree.Node> byId = new HashMap<>();
        Map<String, List<TerminalStoryTree.Node>> neighbors = new HashMap<>();
        for (TerminalStoryTree.Node node : nodes) {
            byId.put(node.id(), node);
            neighbors.put(node.id(), new ArrayList<>());
        }
        for (TerminalStoryTree.Node node : nodes) {
            for (String link : node.links()) {
                TerminalStoryTree.Node source = byId.get(link);
                if (source != null
                        && source != node
                        && !neighbors.get(node.id()).contains(source)) {
                    neighbors.get(node.id()).add(source);
                    neighbors.get(source.id()).add(node);
                }
            }
        }
        List<TerminalStoryTree.Node> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparingInt(TerminalStoryTree.Node::x)
                .thenComparingInt(TerminalStoryTree.Node::position)
                .thenComparingInt(TerminalStoryTree.Node::y)
                .thenComparing(TerminalStoryTree.Node::id));
        int column = 0;
        int columnLayer = 0;
        int lastLayer = -1;
        for (TerminalStoryTree.Node node : ordered) {
            if (entrances.isEmpty() || column != node.x()) {
                column = node.x();
                columnLayer = lastLayer + 1;
            }
            int layer = columnLayer;
            List<TerminalStoryTree.Node> sources = new ArrayList<>();
            for (TerminalStoryTree.Node neighbor : neighbors.get(node.id())) {
                Entrance previous = entrances.get(neighbor.id());
                if (previous != null) {
                    layer = Math.max(layer, previous.layer() + 1);
                    sources.add(neighbor);
                }
            }
            entrances.put(node.id(), new Entrance(layer, List.copyOf(sources)));
            lastLayer = Math.max(lastLayer, layer);
        }
    }

    List<TerminalStoryTree.Node> sources(TerminalStoryTree.Node node) {
        return entrances.get(node.id()).sources();
    }

    float opening(long elapsed) {
        return Mth.clamp(elapsed / (float) OPEN_DURATION, 0, 1);
    }

    float headerOpacity(long elapsed) {
        return TerminalGraphicsRenderer.ease((elapsed - HEADER_START) / (float) HEADER_DURATION);
    }

    long connectionStart(TerminalStoryTree.Node node) {
        return CONTENT_START + layer(node) * (CONNECTION_DURATION + NODE_DURATION);
    }

    int layer(TerminalStoryTree.Node node) {
        return entrances.get(node.id()).layer();
    }

    static float connectionProgress(long elapsed, long start) {
        return Mth.clamp((elapsed - start) / (float) CONNECTION_DURATION, 0, 1);
    }

    static float nodeProgress(long elapsed, long start) {
        return TerminalGraphicsRenderer.ease((elapsed - start - CONNECTION_DURATION) / (float) NODE_DURATION);
    }

    static float fadeOut(long elapsed, long duration) {
        return 1 - TerminalGraphicsRenderer.ease(elapsed / (float) Math.max(1, duration));
    }
}
