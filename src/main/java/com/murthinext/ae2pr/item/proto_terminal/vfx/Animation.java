package com.murthinext.ae2pr.item.proto_terminal.vfx;

import com.murthinext.ae2pr.item.proto_terminal.component.Panel;
import com.murthinext.ae2pr.item.proto_terminal.story.Tree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;

public final class Animation {

    public static final long OPEN_DURATION = 900;
    public static final long CONTENT_START = 250;
    public static final long HEADER_START = OPEN_DURATION - 150;
    public static final long NODE_DURATION = 240;
    public static final long CONNECTION_DURATION = 180;
    private static final long HEADER_DURATION = 160;
    private final Map<String, Entrance> entrances = new HashMap<>();

    private record Entrance(int layer, List<Tree.Node> sources) {}

    public Animation(List<Tree.Node> nodes) {
        Map<String, Tree.Node> byId = new HashMap<>();
        Map<String, List<Tree.Node>> neighbors = new HashMap<>();
        for (Tree.Node node : nodes) {
            byId.put(node.id(), node);
            neighbors.put(node.id(), new ArrayList<>());
        }
        for (Tree.Node node : nodes) {
            for (String link : node.links()) {
                Tree.Node source = byId.get(link);
                if (source != null
                        && source != node
                        && !neighbors.get(node.id()).contains(source)) {
                    neighbors.get(node.id()).add(source);
                    neighbors.get(source.id()).add(node);
                }
            }
        }
        List<Tree.Node> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparingInt(Tree.Node::x)
                .thenComparingInt(Tree.Node::position)
                .thenComparingInt(Tree.Node::y)
                .thenComparing(Tree.Node::id));
        int column = 0;
        int columnLayer = 0;
        int lastLayer = -1;
        for (Tree.Node node : ordered) {
            if (entrances.isEmpty() || column != node.x()) {
                column = node.x();
                columnLayer = lastLayer + 1;
            }
            int layer = columnLayer;
            List<Tree.Node> sources = new ArrayList<>();
            for (Tree.Node neighbor : neighbors.get(node.id())) {
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

    public List<Tree.Node> sources(Tree.Node node) {
        return entrances.get(node.id()).sources();
    }

    public float opening(long elapsed) {
        return Mth.clamp(elapsed / (float) OPEN_DURATION, 0, 1);
    }

    public float headerOpacity(long elapsed) {
        return Panel.ease((elapsed - HEADER_START) / (float) HEADER_DURATION);
    }

    public long connectionStart(Tree.Node node) {
        return CONTENT_START + layer(node) * (CONNECTION_DURATION + NODE_DURATION);
    }

    public int layer(Tree.Node node) {
        return entrances.get(node.id()).layer();
    }

    public static float connectionProgress(long elapsed, long start) {
        return Mth.clamp((elapsed - start) / (float) CONNECTION_DURATION, 0, 1);
    }

    public static float nodeProgress(long elapsed, long start) {
        return Panel.ease((elapsed - start - CONNECTION_DURATION) / (float) NODE_DURATION);
    }

    public static float fadeOut(long elapsed, long duration) {
        return 1 - Panel.ease(elapsed / (float) Math.max(1, duration));
    }
}
