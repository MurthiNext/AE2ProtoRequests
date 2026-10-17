package com.murthinext.ae2pr.client.proto_terminal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * 台词文本。
 */
final class TerminalStoryText {

    // 打字机每秒显示的字符数
    private static final float GLYPHS_PER_SECOND = 34;
    private static final Pattern DELAY = Pattern.compile("<delay\\s*=\\s*(\\d{1,5})\\s*/?\\s*>",
            Pattern.CASE_INSENSITIVE);
    private static final String PLAYER_NAME = "%playerName%";
    private static final int MAX_DELAY = 60000;

    private final String raw;
    private String resolvedName;
    private String plain = "";
    // timeline[i] 为显示完前 i 个字符所需的秒数
    private double[] timeline = { 0 };

    private TerminalStoryText(String raw) {
        this.raw = raw;
    }

    static TerminalStoryText of(String raw) {
        return new TerminalStoryText(raw == null ? "" : raw);
    }

    static String playerName() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? "?" : minecraft.player.getGameProfile().getName();
    }

    static String plainOf(String raw) {
        return DELAY.matcher(raw.replace(PLAYER_NAME, playerName())).replaceAll("");
    }

    String plain() {
        resolve();
        return plain;
    }

    int length() {
        return plain().length();
    }

    int glyphsAt(double seconds) {
        resolve();
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
        resolve();
        return timeline[Mth.clamp(glyphs, 0, plain.length())];
    }

    private void resolve() {
        String name = playerName();
        if (name.equals(resolvedName)) {
            return;
        }
        resolvedName = name;
        String expanded = raw.replace(PLAYER_NAME, name);
        StringBuilder visible = new StringBuilder(expanded.length());
        // pauses[i] 为显示第 i 个字符前的停顿毫秒
        int[] pauses = new int[expanded.length() + 1];
        Matcher matcher = DELAY.matcher(expanded);
        int cursor = 0;
        while (matcher.find()) {
            visible.append(expanded, cursor, matcher.start());
            pauses[Math.min(visible.length(), pauses.length - 1)] += Integer.parseInt(matcher.group(1));
            cursor = matcher.end();
        }
        visible.append(expanded, cursor, expanded.length());
        plain = visible.toString();
        timeline = new double[plain.length() + 1];
        for (int i = 0; i < plain.length(); i++) {
            timeline[i + 1] = timeline[i] + 1.0 / GLYPHS_PER_SECOND + Math.min(MAX_DELAY, pauses[i]) / 1000.0;
        }
    }
}
