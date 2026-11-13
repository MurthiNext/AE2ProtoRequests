package com.murthinext.ae2pr.api.text;

import java.util.Optional;

import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * 自带配色排布的文本内容。
 * <p>
 * 普通界面按字符写入颜色，物品 Tooltip 内由客户端接管为动态配色。原版无法序列化自定义
 * {@link ComponentContents}，因此不要把它交给方块名、菜单标题等会经网络同步的文本。
 */
public record GradientText(String text, int[] colors, float speed, boolean flow) implements ComponentContents {

    public static final float DEFAULT_SPEED = 1.0F;

    public GradientText {
        colors = normalize(colors);
        speed = speed > 0.0F ? speed : DEFAULT_SPEED;
    }

    // 逐字符顺序取色
    public static GradientText flow(String text, int... colors) {
        return new GradientText(text, colors, DEFAULT_SPEED, true);
    }

    public static GradientText flow(String text, float speed, int[] colors) {
        return new GradientText(text, colors, speed, true);
    }

    // 沿行内位置平滑渐变
    public static GradientText gradient(String text, float speed, int... colors) {
        return new GradientText(text, colors, speed, false);
    }

    // 颜色统一按 RGB 处理
    private static int[] normalize(int[] colors) {
        if (colors == null || colors.length == 0) {
            return new int[]{0xFFFFFF};
        }
        int[] normalized = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            normalized[i] = colors[i] & 0xFFFFFF;
        }
        return normalized;
    }

    // 按顺序取色，索引自动循环
    public int colorAt(int index) {
        return colors[Math.floorMod(index, colors.length)];
    }

    public static int mix(int[] colors, float fraction) {
        float scaled = Math.max(0.0F, Math.min(1.0F, fraction)) * (colors.length - 1);
        int index = (int) scaled;
        int from = colors[index];
        int to = colors[Math.min(index + 1, colors.length - 1)];
        float delta = scaled - index;
        return interpolate(from >>> 16 & 0xFF, to >>> 16 & 0xFF, delta) << 16
                | interpolate(from >>> 8 & 0xFF, to >>> 8 & 0xFF, delta) << 8
                | interpolate(from & 0xFF, to & 0xFF, delta);
    }

    private static int interpolate(int from, int to, float delta) {
        return Math.round(from + (to - from) * delta);
    }

    public int fallbackColor() {
        return colors[0];
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return consumer.accept(text);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        int count = Math.max(text.codePointCount(0, text.length()) - 1, 1);
        int offset = 0;
        for (int step = 0; offset < text.length(); step++) {
            int codePoint = text.codePointAt(offset);
            int color = flow ? colorAt(step) : mix(colors, (float) step / count);
            Optional<T> result = consumer.accept(style.withColor(color), new String(Character.toChars(codePoint)));
            if (result.isPresent()) {
                return result;
            }
            offset += Character.charCount(codePoint);
        }
        return Optional.empty();
    }
}
