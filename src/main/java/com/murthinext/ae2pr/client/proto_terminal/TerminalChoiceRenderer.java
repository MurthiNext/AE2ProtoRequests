package com.murthinext.ae2pr.client.proto_terminal;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 分叉选项入场时的色差与信号撕裂。
 */
final class TerminalChoiceRenderer {

    private static final int CYAN = 0xFF54E8F2;
    private static final int RED = 0xFFFF537D;

    private TerminalChoiceRenderer() {
    }

    static void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, String text,
            boolean hovered, float opacity, float progress, long now, int index) {
        float appear = TerminalGraphicsRenderer.ease(progress);
        float glitch = (1 - progress) * (1 - progress);
        int signal = (int) (now / 32) + index * 131;
        signal = (signal ^ signal >>> 16) * 0x45D9F3B;
        signal ^= signal >>> 16;
        int jitter = Math.round(((signal & 7) - 3) * glitch);
        float split = (2 + (signal >>> 3 & 3)) * glitch;
        float slant = TerminalGraphicsRenderer.slant(height);
        float alpha = opacity * appear;
        if (alpha < 4.0F / 255) {
            return;
        }
        int cyan = TerminalGraphicsRenderer.alpha(CYAN, alpha * glitch * 0.8F);
        int red = TerminalGraphicsRenderer.alpha(RED, alpha * glitch * 0.8F);

        if (glitch > 0.01F) {
            TerminalGraphicsRenderer.panel(graphics, x + jitter - split, y + glitch, width, height, slant, 0, cyan);
            TerminalGraphicsRenderer.panel(graphics, x + jitter + split, y - glitch, width, height, slant, 0, red);
        }
        TerminalGraphicsRenderer.panel(graphics, x + jitter, y, width, height, slant,
                TerminalGraphicsRenderer.alpha(hovered ? 0xFF10233D : TerminalGraphicsRenderer.PANEL, alpha),
                TerminalGraphicsRenderer.alpha(hovered ? TerminalGraphicsRenderer.ACCENT
                        : TerminalGraphicsRenderer.BORDER, alpha));

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(jitter, 0, 0);
            if (cyan >>> 24 >= 4) {
                graphics.pose().translate(-split, 0, 0);
                graphics.drawString(font, text, x + 12, y + 6, cyan, false);
                graphics.pose().translate(split * 2, 0, 0);
                graphics.drawString(font, text, x + 12, y + 6, red, false);
                graphics.pose().translate(-split, 0, 0);
            }
            graphics.drawString(font, text, x + 12, y + 6,
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.TEXT, alpha), false);
        } finally {
            graphics.pose().popPose();
        }

        if (glitch > 0.06F) {
            int tearY = y + 2 + (signal >>> 6 & 15) % (height - 4);
            int tearWidth = Math.max(3, Math.round(width * glitch * 0.42F));
            int tearX = x + (signal >>> 10 & 31) % Math.max(1, width - tearWidth - 6) + 3;
            graphics.fill(tearX + jitter, tearY, tearX + jitter + tearWidth, tearY + 1, cyan);
            graphics.fill(x + width - tearWidth - jitter, tearY + 2,
                    x + width + Math.round(split) - jitter, tearY + 3, red);
        }
    }
}
