package com.murthinext.ae2pr.logic.tooltip;

import java.util.Optional;

import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * 携带特殊取色规则的文本内容。
 */
public final class TooltipColorEffect implements ComponentContents {

    public static final float DEFAULT_SPEED = 1.0F;

    private final String text;
    private final int[] colors;
    private final float speed;
    private final boolean hardSwitch;

    public TooltipColorEffect(String text, int[] colors, float speed, boolean hardSwitch) {
        this.text = text;
        this.colors = normalize(colors);
        this.speed = speed > 0.0F ? speed : DEFAULT_SPEED;
        this.hardSwitch = hardSwitch;
    }

    // 颜色统一按 RGB 处理
    private static int[] normalize(int[] colors) {
        if (colors == null || colors.length == 0) {
            return new int[] { 0xFFFFFF };
        }
        int[] normalized = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            normalized[i] = colors[i] & 0xFFFFFF;
        }
        return normalized;
    }

    public String text() {
        return text;
    }

    public int[] colors() {
        return colors;
    }

    public float speed() {
        return speed;
    }

    public boolean hardSwitch() {
        return hardSwitch;
    }

    // 按顺序取色，索引自动循环
    public int colorAt(int index) {
        return colors[Math.floorMod(index, colors.length)];
    }

    // 脱离本模组渲染途径时的降级颜色
    public int fallbackColor() {
        return colors[0];
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return consumer.accept(text);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return consumer.accept(style.withColor(fallbackColor()), text);
    }
}
