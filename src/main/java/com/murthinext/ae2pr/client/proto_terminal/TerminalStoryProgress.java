package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.murthinext.ae2pr.ae2pr;

/**
 * 单个玩家在源始终端里的阅读进度。
 */
public final class TerminalStoryProgress {

    private static final Gson GSON = new Gson();

    public List<String> tags = new ArrayList<>();
    public Map<String, NodeProgress> nodes = new LinkedHashMap<>();

    /**
     * 单条已显示台词的定位：path 为逐层 [行号, 选项号]，line 为该帧内的行号；
     * choice 不小于 0 时表示玩家选择的回答，正文取自该选项。
     */
    public static final class LogRef {

        public List<Integer> path = new ArrayList<>();
        public int line;
        public int choice = -1;
    }

    /**
     * 一层待播台词序列的游标。
     */
    public static final class FrameState {

        public List<Integer> path = new ArrayList<>();
        public int index;
    }

    /**
     * 分叉口已选过的选项。
     */
    public static final class ConsumedState {

        public List<Integer> path = new ArrayList<>();
        public int line;
        public List<Integer> choices = new ArrayList<>();
    }

    /**
     * 单个剧情块的阅读进度；frames 为空表示该块已经播完，branch 不为空表示正停在分叉口。
     */
    public static final class NodeProgress {

        public List<LogRef> log = new ArrayList<>();
        public List<FrameState> frames = new ArrayList<>();
        public List<ConsumedState> consumed = new ArrayList<>();
        public LogRef branch;
        public int typed;
        public boolean typing;
    }

    public static TerminalStoryProgress fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new TerminalStoryProgress();
        }
        try {
            TerminalStoryProgress progress = GSON.fromJson(json, TerminalStoryProgress.class);
            if (progress == null || progress.tags == null || progress.nodes == null) {
                return new TerminalStoryProgress();
            }
            return progress;
        } catch (JsonSyntaxException exception) {
            ae2pr.LOGGER.warn("源始终端阅读进度解析失败", exception);
            return new TerminalStoryProgress();
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static String toJson(NodeProgress node) {
        return GSON.toJson(node);
    }
}
