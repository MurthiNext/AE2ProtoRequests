package com.murthinext.ae2pr.client.proto_terminal;

import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 标题栏的自绘像素字标与碎片组成动画。
 */
final class TerminalTitleRenderer {

    static final String TEXT = "Proto Terminal";
    private static final int GLYPH_HEIGHT = 7;
    private static final long CHARACTER_STAGGER = 18;
    private static final long PIXEL_DURATION = 96;
    private static final Map<Character, Glyph> GLYPHS = Map.ofEntries(
            Map.entry('P', new Glyph(5, 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000)),
            Map.entry('r', new Glyph(4, 0, 0, 0b1011, 0b1100, 0b1000, 0b1000, 0b1000)),
            Map.entry('o', new Glyph(5, 0, 0, 0b01110, 0b10001, 0b10001, 0b10001, 0b01110)),
            Map.entry('t', new Glyph(4, 0b0100, 0b0100, 0b1110, 0b0100, 0b0100, 0b0100, 0b0011)),
            Map.entry(' ', new Glyph(3, 0, 0, 0, 0, 0, 0, 0)),
            Map.entry('T', new Glyph(5, 0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100)),
            Map.entry('e', new Glyph(5, 0, 0, 0b01110, 0b10001, 0b11111, 0b10000, 0b01111)),
            Map.entry('m', new Glyph(5, 0, 0, 0b11010, 0b10101, 0b10101, 0b10101, 0b10101)),
            Map.entry('i', new Glyph(1, 1, 0, 1, 1, 1, 1, 1)),
            Map.entry('n', new Glyph(5, 0, 0, 0b11110, 0b10001, 0b10001, 0b10001, 0b10001)),
            Map.entry('a', new Glyph(5, 0, 0, 0b01110, 0b00001, 0b01111, 0b10001, 0b01111)),
            Map.entry('l', new Glyph(2, 0b10, 0b10, 0b10, 0b10, 0b10, 0b10, 0b11)));
    private static final int TEXT_WIDTH = TEXT.chars().map(character -> GLYPHS.get((char) character).width() + 1)
            .sum() - 1;

    private record Glyph(int width, int... rows) {
    }

    private TerminalTitleRenderer() {
    }

    static void render(GuiGraphics graphics, float x, float y, int width, int height, float slant,
            float opacity, long elapsed) {
        if (opacity <= 0 || elapsed < 0) {
            return;
        }
        float textSlant = GLYPH_HEIGHT * TerminalGraphicsRenderer.EDGE_SLOPE;
        float pixelSize = Math.min((width - slant - 20) / (TEXT_WIDTH + textSlant),
                (height - 14.0F) / GLYPH_HEIGHT);
        if (pixelSize <= 0) {
            return;
        }
        float left = x + (width - (TEXT_WIDTH + textSlant) * pixelSize) / 2;
        float top = y + (height - GLYPH_HEIGHT * pixelSize) / 2;
        graphics.drawManaged(() -> {
            float cursor = left;
            for (int index = 0; index < TEXT.length(); index++) {
                Glyph glyph = GLYPHS.get(TEXT.charAt(index));
                long age = elapsed - index * CHARACTER_STAGGER;
                for (int row = 0; row < GLYPH_HEIGHT; row++) {
                    for (int column = 0; column < glyph.width(); column++) {
                        if ((glyph.rows()[row] & 1 << (glyph.width() - 1 - column)) == 0) {
                            continue;
                        }
                        long delay = ((row * 3 + column * 5) % 7) * 7L;
                        float reveal = TerminalGraphicsRenderer.ease((age - delay) / (float) PIXEL_DURATION);
                        if (opacity * reveal < 1.0F / 255) {
                            continue;
                        }
                        float shear = (GLYPH_HEIGHT - 1 - row) * TerminalGraphicsRenderer.EDGE_SLOPE * pixelSize;
                        float shift = (1 - reveal) * pixelSize * 1.5F;
                        float px = cursor + column * pixelSize + shear - shift;
                        float py = top + row * pixelSize + shift;
                        int color = TerminalGraphicsRenderer.mix(TerminalGraphicsRenderer.TEXT,
                                TerminalGraphicsRenderer.ACCENT, row * 0.035F + (1 - reveal) * 0.5F);
                        TerminalGraphicsRenderer.line(graphics, px, py + pixelSize / 2,
                                px + pixelSize, py + pixelSize / 2,
                                TerminalGraphicsRenderer.alpha(color, opacity * reveal), pixelSize);
                    }
                }
                cursor += (glyph.width() + 1) * pixelSize;
            }
        });
    }
}
