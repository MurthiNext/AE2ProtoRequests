package com.murthinext.ae2pr.client.tooltip;

import com.murthinext.ae2pr.api.text.GradientText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record ColorEffectLine(List<Segment> segments) implements TooltipComponent {

    private static final GradientText.StyledEffectConsumer<Boolean> EFFECT_FINDER =
            new GradientText.StyledEffectConsumer<>() {
                @Override
                public Optional<Boolean> accept(Style style, String text) {
                    return Optional.empty();
                }

                @Override
                public Optional<Boolean> acceptEffect(Style style, GradientText effect) {
                    return Optional.of(true);
                }
            };

    public ColorEffectLine(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    // 行内片段
    public record Segment(String text, Style style, GradientText effect) {

        Segment slice(int from, int to) {
            return new Segment(text.substring(from, to), style, effect);
        }
    }

    public static boolean containsEffect(FormattedText text) {
        return text.visit(EFFECT_FINDER, Style.EMPTY).isPresent();
    }

    public static boolean hasEffect(List<Segment> segments) {
        for (Segment segment : segments) {
            if (segment.effect() != null) {
                return true;
            }
        }
        return false;
    }

    public static List<Segment> collect(FormattedText text) {
        List<Segment> segments = new ArrayList<>();
        text.visit(new SegmentCollector(segments), Style.EMPTY);
        return segments;
    }

    private record SegmentCollector(List<Segment> segments) implements GradientText.StyledEffectConsumer<Void> {
        @Override
        public Optional<Void> accept(Style style, String text) {
            segments.add(new Segment(text, style, null));
            return Optional.empty();
        }

        @Override
        public Optional<Void> acceptEffect(Style style, GradientText effect) {
            segments.add(new Segment(effect.text(), style, effect));
            return Optional.empty();
        }
    }

    // 按换行拆分为多行片段集合
    public static List<List<Segment>> split(List<Segment> segments) {
        List<List<Segment>> lines = new ArrayList<>();
        List<Segment> current = new ArrayList<>();
        for (Segment segment : segments) {
            String text = segment.text();
            int length = text.length();
            int start = 0;
            while (start < length) {
                int newline = indexOfNewline(text, start);
                if (newline < 0) {
                    current.add(segment.slice(start, length));
                    break;
                }
                if (newline > start) {
                    current.add(segment.slice(start, newline));
                }
                lines.add(current);
                current = new ArrayList<>();
                // \r\n 视为一次换行
                start = newline + 1 < length && text.charAt(newline) == '\r' && text.charAt(newline + 1) == '\n'
                        ? newline + 2
                        : newline + 1;
            }
        }
        lines.add(current);
        return lines;
    }

    private static int indexOfNewline(String text, int from) {
        int lineFeed = text.indexOf('\n', from);
        int carriageReturn = text.indexOf('\r', from);
        if (lineFeed < 0) {
            return carriageReturn;
        }
        if (carriageReturn < 0) {
            return lineFeed;
        }
        return Math.min(lineFeed, carriageReturn);
    }

    // 先按换行拆行，再按可用宽度折行
    public static List<List<Segment>> wrap(List<Segment> segments, Font font, int maxWidth) {
        List<List<Segment>> lines = new ArrayList<>();
        for (List<Segment> line : split(segments)) {
            lines.addAll(wrapLine(line, font, maxWidth));
        }
        return lines;
    }

    private static List<List<Segment>> wrapLine(List<Segment> segments, Font font, int maxWidth) {
        String plain = plainText(segments);
        if (maxWidth <= 0 || plain.isEmpty() || font.width(plain) <= maxWidth) {
            return List.of(segments);
        }
        List<List<Segment>> lines = new ArrayList<>();
        int start = 0;
        while (start < plain.length()) {
            int end = breakIndex(plain, start, font, maxWidth);
            lines.add(slice(segments, start, end));
            start = end;
            // 断行处的前导空格不再显示
            while (start < plain.length() && plain.charAt(start) == ' ') {
                start++;
            }
        }
        return lines;
    }

    private static int breakIndex(String plain, int start, Font font, int maxWidth) {
        String rest = plain.substring(start);
        int count = font.getSplitter().plainIndexAtWidth(rest, maxWidth, Style.EMPTY);
        if (start + count >= plain.length()) {
            return plain.length();
        }
        // 优先在空格处断开，否则硬断
        int space = plain.lastIndexOf(' ', start + count - 1);
        return space > start ? space : start + Math.max(count, 1);
    }

    private static List<Segment> slice(List<Segment> segments, int from, int to) {
        List<Segment> result = new ArrayList<>();
        int offset = 0;
        for (Segment segment : segments) {
            int length = segment.text().length();
            int begin = Math.max(from, offset);
            int end = Math.min(to, offset + length);
            if (begin < end) {
                result.add(segment.slice(begin - offset, end - offset));
            }
            offset += length;
        }
        return result;
    }

    private static String plainText(List<Segment> segments) {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : segments) {
            builder.append(segment.text());
        }
        return builder.toString();
    }

    // 把无特效的片段重新拼回单个组件，保留原有样式
    public static Component toComponent(List<Segment> segments) {
        MutableComponent result = Component.empty();
        for (Segment segment : segments) {
            if (!segment.text().isEmpty()) {
                result.append(Component.literal(segment.text()).setStyle(segment.style()));
            }
        }
        return result;
    }
}
