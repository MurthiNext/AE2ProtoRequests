package com.murthinext.ae2pr.api.machine.client;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiPanels {
    public static final int COLOR_BACKGROUND = 0xFF101418;
    public static final int COLOR_BORDER = 0xFF5A7A9A;
    public static final int COLOR_HIGHLIGHT = 0xFF8ABBEF;
    private static final int COLOR_POPUP = 0xFF171D24;

    private GuiPanels() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int dividerY) {
        frame(graphics, x, y, width, height, COLOR_BACKGROUND);
        if (dividerY > 0 && dividerY < height - 1) {
            graphics.fill(x + 1, y + dividerY, x + width - 1, y + dividerY + 1, COLOR_BORDER);
        }
    }

    public static void popup(GuiGraphics graphics, int x, int y, int width, int height) {
        frame(graphics, x, y, width, height, COLOR_POPUP);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, COLOR_HIGHLIGHT);
    }

    private static void frame(GuiGraphics graphics, int x, int y, int width, int height, int background) {
        graphics.fill(x, y, x + width, y + height, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
    }
}
