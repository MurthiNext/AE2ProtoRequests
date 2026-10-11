package com.murthinext.ae2pr.compat.jei;

import net.minecraft.client.gui.GuiGraphics;

import mezz.jei.api.gui.drawable.IDrawable;

/**
 * 代码绘制的 JEI 流体槽背景。
 */
public final class FluidSlotBackground implements IDrawable {

    public static final int SIZE = 18;

    private static final int COLOR_LINE = 0xFF39424E;
    private static final int COLOR_BG = 0xFF1B222B;

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        graphics.fill(xOffset, yOffset, xOffset + SIZE, yOffset + SIZE, COLOR_LINE);
        graphics.fill(xOffset + 1, yOffset + 1, xOffset + SIZE - 1, yOffset + SIZE - 1, COLOR_BG);
    }
}
