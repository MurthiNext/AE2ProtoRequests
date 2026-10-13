package com.murthinext.ae2pr.client.gui;

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

/**
 * 统一物品 Tooltip 文本处理与渲染入口。
 */
public final class TooltipRenderer {
    private TooltipRenderer() {
    }

    public static void renderItem(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    // 富文本和图片组件沿用 Forge 的折行、定位及渲染事件。
    public static void renderElements(GuiGraphics graphics, Font font,
            List<Either<FormattedText, TooltipComponent>> elements, ItemStack stack, int mouseX, int mouseY) {
        graphics.renderComponentTooltipFromElements(font, new ArrayList<>(elements), mouseX, mouseY, stack);
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
        List<Either<FormattedText, TooltipComponent>> result = new ArrayList<>(elements.size());
        for (var element : elements) {
            element.ifLeft(text -> {
                for (Component line : splitLines(text)) {
                    result.add(Either.left(line));
                }
            }).ifRight(component -> result.add(element));
        }
        return result;
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
