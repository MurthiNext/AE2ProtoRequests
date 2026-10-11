package com.murthinext.ae2pr.compat.jei;

import net.minecraft.client.gui.GuiGraphics;

import mezz.jei.api.gui.drawable.IDrawable;

/**
 * 代码绘制的 JEI 流体槽背景（18x18：1px 描边 + 深色内底），与机器界面槽位同款配色。
 */
public final class FluidSlotBackground implements IDrawable {

    /** 槽位尺寸（含 1px 描边） */
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
