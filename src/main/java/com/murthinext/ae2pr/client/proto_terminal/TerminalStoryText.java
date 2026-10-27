package com.murthinext.ae2pr.client.proto_terminal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

// 剧情台词
final class TerminalStoryText {

    // 打字机每秒显示的字符数
    private static final float GLYPHS_PER_SECOND = 34;
    private static final Pattern DELAY = Pattern.compile("<delay\\s*=\\s*(\\d{1,5})\\s*/?\\s*>",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_DELAY = 60000;

    private final String plain;
    // timeline[i] 为显示完前 i 个字符所需的秒数
    private final double[] timeline;

    private TerminalStoryText(String raw) {
        StringBuilder visible = new StringBuilder(raw.length());
        int[] pauses = new int[raw.length() + 1];
        Matcher matcher = DELAY.matcher(raw);
        int cursor = 0;
        while (matcher.find()) {
            visible.append(raw, cursor, matcher.start());
            pauses[visible.length()] = Math.min(MAX_DELAY,
                    pauses[visible.length()] + Integer.parseInt(matcher.group(1)));
            cursor = matcher.end();
        }
        visible.append(raw, cursor, raw.length());
        plain = visible.toString();
        timeline = new double[plain.length() + 1];
        for (int i = 0; i < plain.length(); i++) {
            timeline[i + 1] = timeline[i] + 1.0 / GLYPHS_PER_SECOND + Math.min(MAX_DELAY, pauses[i]) / 1000.0;
        }
    }

    static TerminalStoryText of(String raw) {
        return new TerminalStoryText(raw == null ? "" : raw);
    }

    static String playerName() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? "?" : minecraft.player.getGameProfile().getName();
    }

    static String plainOf(String raw) {
        return DELAY.matcher(raw).replaceAll("");
    }

    String plain() {
        return plain;
    }

    int length() {
        return plain().length();
    }

    int glyphsAt(double seconds) {
        int low = 0;
        int high = plain.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (timeline[mid] <= seconds) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    // 显示完指定字符数的时间点
    double durationAt(int glyphs) {
        return timeline[Mth.clamp(glyphs, 0, plain.length())];
    }
}
