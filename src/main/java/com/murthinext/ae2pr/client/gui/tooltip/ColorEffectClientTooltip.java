package com.murthinext.ae2pr.client.gui.tooltip;

import java.util.ArrayList;
import java.util.List;

import com.murthinext.ae2pr.client.gui.tooltip.ColorEffectLine.Segment;
import com.murthinext.ae2pr.logic.tooltip.TooltipColorEffect;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

/**
 * 渲染单个特效文本行。
 */
@OnlyIn(Dist.CLIENT)
public final class ColorEffectClientTooltip implements ClientTooltipComponent {

    private static final int LINE_HEIGHT = 10;
    private static final int TRANSPARENT_LIGHT = 15728880;

    private static final float SWITCH_STEPS_PER_SECOND = 4.0F;
    private static final float GRADIENT_LOOPS_PER_SECOND = 0.2F;

    private final ColorEffectLine line;

    public ColorEffectClientTooltip(ColorEffectLine line) {
        this.line = line;
    }

    @Override
    public int getHeight() {
        return LINE_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Segment segment : line.segments()) {
            width += segmentWidth(font, segment);
        }
        return width;
    }

    @Override
    public void renderText(Font font, int lineX, int y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource) {
        long now = Util.getMillis();
        int lineWidth = getWidth(font);
        int advance = 0;
        for (Segment segment : line.segments()) {
            if (segment.effect() == null) {
                font.drawInBatch(FormattedCharSequence.forward(segment.text(), segment.style()), lineX + advance, y, -1,
                        true, pose, bufferSource, Font.DisplayMode.NORMAL, 0, TRANSPARENT_LIGHT);
            } else {
                renderEffect(segment, font, lineX, lineX + advance, y, lineWidth, now, pose, bufferSource);
            }
            advance += segmentWidth(font, segment);
        }
    }

    private static int segmentWidth(Font font, Segment segment) {
        return font.width(FormattedText.of(segment.text(), segment.style()));
    }

    private void renderEffect(Segment segment, Font font, int lineX, int x, int y, int lineWidth, long now,
            Matrix4f pose, MultiBufferSource.BufferSource bufferSource) {
        TooltipColorEffect effect = segment.effect();
        if (effect.hardSwitch()) {
            // 颜色写在字符样式上，阴影由原版按亮度系数自动压暗
            int shift = (int) (now / 1000.0D * effect.speed() * SWITCH_STEPS_PER_SECOND);
            font.drawInBatch(hardSwitchText(segment, shift), x, y, -1, true, pose, bufferSource,
                    Font.DisplayMode.NORMAL, 0, TRANSPARENT_LIGHT);
        } else {
            // 顶点颜色由渐变着色器接管，样式固定白色以让亮度系数只表示明暗
            float phase = (float) (now / 1000.0D * effect.speed() * GRADIENT_LOOPS_PER_SECOND % 1.0D);
            GradientBufferSource gradient = new GradientBufferSource(effect.colors());
            gradient.configure(bufferSource, lineX, lineWidth, phase);
            font.drawInBatch(FormattedCharSequence.forward(segment.text(), segment.style().withColor(0xFFFFFF)), x, y,
                    -1, true, pose, gradient, Font.DisplayMode.NORMAL, 0, TRANSPARENT_LIGHT);
        }
    }

    // 每个字符取列表中顺序下一个颜色，整体随时间轮换
    private static FormattedCharSequence hardSwitchText(Segment segment, int shift) {
        TooltipColorEffect effect = segment.effect();
        Style style = segment.style();
        String text = segment.text();
        List<FormattedCharSequence> characters = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < text.length(); index++) {
            int codePoint = text.codePointAt(i);
            characters.add(FormattedCharSequence.codepoint(codePoint, style.withColor(effect.colorAt(index - shift))));
            i += Character.charCount(codePoint);
        }
        return FormattedCharSequence.composite(characters);
    }
}
