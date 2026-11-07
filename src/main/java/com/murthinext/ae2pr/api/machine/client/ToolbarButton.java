package com.murthinext.ae2pr.api.machine.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// 左侧工具栏通用按钮，外观全部来自材质
public class ToolbarButton extends AbstractWidget {

    private static final int SIZE = 18;

    private final ResourceLocation icon;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public ToolbarButton(int x, int y, ResourceLocation icon, BooleanSupplier selected, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.icon = icon;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTextures.frame(graphics, selected.getAsBoolean() ? GuiTextures.BUTTON_SELECTED
                : isHovered() ? GuiTextures.BUTTON_HOVER : GuiTextures.BUTTON, getX(), getY());
        GuiTextures.frame(graphics, icon, getX(), getY());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
