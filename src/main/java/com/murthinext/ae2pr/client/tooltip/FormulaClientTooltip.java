package com.murthinext.ae2pr.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class FormulaClientTooltip implements ClientTooltipComponent {
    private final FormulaLayout.Box layout;
    private final float scale;

    public FormulaClientTooltip(FormulaTooltip tooltip) {
        layout = new FormulaLayout(Minecraft.getInstance().font).formula(tooltip.formula());
        scale = Math.min(1, Math.max(1, tooltip.maxWidth()) / Math.max(1, layout.width()));
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil((layout.ascent() + layout.descent()) * scale) + 6;
    }

    @Override
    public int getWidth(Font font) {
        return (int) Math.ceil(layout.width() * scale);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 3 + layout.ascent() * scale, 0);
        graphics.pose().scale(scale, scale, 1);
        layout.draw(graphics);
        graphics.pose().popPose();
    }
}
