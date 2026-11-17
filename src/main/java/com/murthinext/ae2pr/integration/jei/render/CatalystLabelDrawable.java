package com.murthinext.ae2pr.integration.jei.render;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class CatalystLabelDrawable implements IDrawable {

    public static final CatalystLabelDrawable INSTANCE = new CatalystLabelDrawable();

    private static final Component LABEL =
            Component.translatable("jei.ae2pr.recipe.catalyst").withStyle(ChatFormatting.BOLD);
    private static final int COLOR = 0xFFF30000;

    private CatalystLabelDrawable() {}

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        Font font = Minecraft.getInstance().font;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(xOffset + 17.0F, yOffset + 1.0F, 0.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, LABEL, -font.width(LABEL), 0, COLOR, false);
        pose.popPose();
    }
}
