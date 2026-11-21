package com.murthinext.ae2pr.client.tooltip;

import com.mojang.datafixers.util.Either;
import com.murthinext.ae2pr.api.text.GradientText;
import com.murthinext.ae2pr.api.text.Voltage;
import com.murthinext.ae2pr.client.tooltip.ColorEffectLine.Segment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

// 统一物品 Tooltip 文本处理与渲染入口
public final class TooltipRenderer {
    private TooltipRenderer() {}

    // 流动
    public static MutableComponent flow(String text, int... colors) {
        return flow(text, GradientText.DEFAULT_SPEED, colors);
    }

    public static MutableComponent flow(String text, float speed, int[] colors) {
        return MutableComponent.create(GradientText.flow(text, speed, colors));
    }

    public static void animateTitle(List<Either<FormattedText, TooltipComponent>> elements, String descriptionId) {
        Voltage voltage = Voltage.of(descriptionId);
        if (voltage == null
                || elements.isEmpty()
                || !(elements.get(0).left().orElse(null) instanceof Component title)) {
            return;
        }
        ComponentContents original =
                Component.translatable(descriptionId, voltage.label()).getContents();
        ComponentContents animated =
                Component.translatable(descriptionId, voltage.gradientLabel()).getContents();
        Component result = replaceTitleName(title, original, animated);
        if (result != title) {
            elements.set(0, Either.left(result));
        }
    }

    private static Component replaceTitleName(
            Component component, ComponentContents original, ComponentContents animated) {
        if (original.equals(component.getContents())) {
            MutableComponent result = MutableComponent.create(animated).setStyle(component.getStyle());
            component.getSiblings().forEach(result::append);
            return result;
        }
        MutableComponent result = null;
        List<Component> siblings = component.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            Component sibling = siblings.get(i);
            Component replacement = replaceTitleName(sibling, original, animated);
            if (replacement != sibling) {
                if (result == null) {
                    result = component.copy();
                }
                result.getSiblings().set(i, replacement);
            }
        }
        return result == null ? component : result;
    }

    // 元素列表中是否包含特效文本
    public static boolean containsEffect(List<Either<FormattedText, TooltipComponent>> elements) {
        for (var element : elements) {
            if (element.left().filter(ColorEffectLine::containsEffect).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static List<Either<FormattedText, TooltipComponent>> prepareElements(
            List<Either<FormattedText, TooltipComponent>> elements) {
        return prepareElements(elements, null, 0);
    }

    // 携带特效的文本行改由 TooltipComponent 渲染，其余保持原样
    public static List<Either<FormattedText, TooltipComponent>> prepareElements(
            List<Either<FormattedText, TooltipComponent>> elements, Font font, int maxWidth) {
        List<Either<FormattedText, TooltipComponent>> result = new ArrayList<>(elements.size());
        for (var element : elements) {
            element.ifLeft(text -> prepareText(text, result, font, maxWidth)).ifRight(component -> result.add(element));
        }
        return result;
    }

    private static void prepareText(
            FormattedText text, List<Either<FormattedText, TooltipComponent>> result, Font font, int maxWidth) {
        if (ColorEffectLine.containsEffect(text)) {
            List<Segment> segments = ColorEffectLine.collect(text);
            List<List<Segment>> lines =
                    font == null ? ColorEffectLine.split(segments) : ColorEffectLine.wrap(segments, font, maxWidth);
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
