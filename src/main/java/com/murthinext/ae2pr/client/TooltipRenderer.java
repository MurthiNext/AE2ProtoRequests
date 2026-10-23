package com.murthinext.ae2pr.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import com.murthinext.ae2pr.client.tooltip.ColorEffectLine;
import com.murthinext.ae2pr.client.tooltip.ColorEffectLine.Segment;
import com.murthinext.ae2pr.logic.tooltip.TooltipColorEffect;

/**
 * 统一物品 Tooltip 文本处理与渲染入口。
 */
public final class TooltipRenderer {
    private TooltipRenderer() {
    }

    // 渐变文字
    public static MutableComponent gradient(String text, int... colors) {
        return gradient(text, TooltipColorEffect.DEFAULT_SPEED, colors);
    }

    public static MutableComponent gradient(String text, float speed, int[] colors) {
        return MutableComponent.create(new TooltipColorEffect(text, colors, speed, false));
    }

    // 硬切换文字
    public static MutableComponent hardSwitch(String text, int... colors) {
        return hardSwitch(text, TooltipColorEffect.DEFAULT_SPEED, colors);
    }

    public static MutableComponent hardSwitch(String text, float speed, int[] colors) {
        return MutableComponent.create(new TooltipColorEffect(text, colors, speed, true));
    }

    public static void renderItem(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    // 富文本和图片组件沿用 Forge 的折行、定位及渲染事件。
    public static void renderElements(GuiGraphics graphics, Font font,
            List<Either<FormattedText, TooltipComponent>> elements, ItemStack stack, int mouseX, int mouseY) {
        graphics.renderComponentTooltipFromElements(font, new ArrayList<>(prepareElements(elements)), mouseX, mouseY,
                stack);
    }

    // 元素列表中是否包含特效文本
    public static boolean containsEffect(List<Either<FormattedText, TooltipComponent>> elements) {
        for (var element : elements) {
            if (element.left().filter(Component.class::isInstance)
                    .map(Component.class::cast)
                    .filter(ColorEffectLine::containsEffect)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static List<Component> format(List<? extends FormattedText> lines) {
        List<Component> result = new ArrayList<>(lines.size());
        for (FormattedText line : lines) {
            result.addAll(splitLines(line));
        }
        return result;
    }

    public static List<Either<FormattedText, TooltipComponent>> prepareElements(
            List<Either<FormattedText, TooltipComponent>> elements) {
        return prepareElements(elements, null, 0);
    }

    /**
     * 携带特效的文本行改由 TooltipComponent 渲染，其余保持原样。
     */
    public static List<Either<FormattedText, TooltipComponent>> prepareElements(
            List<Either<FormattedText, TooltipComponent>> elements, Font font, int maxWidth) {
        List<Either<FormattedText, TooltipComponent>> result = new ArrayList<>(elements.size());
        for (var element : elements) {
            element.ifLeft(text -> prepareText(text, result, font, maxWidth)).ifRight(component -> result.add(element));
        }
        return result;
    }

    private static void prepareText(FormattedText text, List<Either<FormattedText, TooltipComponent>> result, Font font,
            int maxWidth) {
        if (text instanceof Component component && ColorEffectLine.containsEffect(component)) {
            List<Segment> segments = ColorEffectLine.collect(component);
            List<List<Segment>> lines = font == null ? ColorEffectLine.split(segments)
                    : ColorEffectLine.wrap(segments, font, maxWidth);
            for (List<Segment> line : lines) {
                if (ColorEffectLine.hasEffect(line)) {
                    result.add(Either.right(new ColorEffectLine(line)));
                } else {
                    result.add(Either.left(ColorEffectLine.toComponent(line)));
                }
            }
            return;
        }
        for (Component line : splitLines(text)) {
            result.add(Either.left(line));
        }
    }

    public static List<Component> splitLines(FormattedText text) {
        LineBuilder builder = new LineBuilder();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, builder::accept);
        builder.finishLine();
        return builder.lines;
    }

    private static final class LineBuilder {
        private final List<Component> lines = new ArrayList<>();
        private final StringBuilder run = new StringBuilder();
        private MutableComponent line = Component.empty();
        private Style runStyle = Style.EMPTY;
        private boolean carriageReturn;

        private boolean accept(int index, Style style, int codePoint) {
            if (codePoint == '\n' && carriageReturn) {
                carriageReturn = false;
                return true;
            }
            carriageReturn = codePoint == '\r';
            if (codePoint == '\n' || codePoint == '\r') {
                finishLine();
            } else {
                if (!style.equals(runStyle)) {
                    finishRun();
                    runStyle = style;
                }
                run.appendCodePoint(codePoint);
            }
            return true;
        }

        private void finishRun() {
            if (!run.isEmpty()) {
                line.append(Component.literal(run.toString()).setStyle(runStyle));
                run.setLength(0);
            }
        }

        private void finishLine() {
            finishRun();
            lines.add(line);
            line = Component.empty();
        }
    }
}
