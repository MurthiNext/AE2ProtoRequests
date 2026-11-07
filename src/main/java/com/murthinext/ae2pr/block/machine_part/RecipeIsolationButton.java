package com.murthinext.ae2pr.block.machine_part;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.api.machine.client.GuiTextures;
import com.murthinext.ae2pr.api.machine.client.GuiTooltips;

public class RecipeIsolationButton extends AbstractWidget {

    private static final int SIZE = GuiTextures.SIZE;

    private static final ResourceLocation ICON_ON = GuiTextures.ISOLATION_ON;
    private static final ResourceLocation ICON_OFF = GuiTextures.ISOLATION_OFF;

    private final BooleanSupplier state;
    private final Runnable onPress;

    public RecipeIsolationButton(int x, int y, BooleanSupplier state, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.translatable("gui.ae2pr.machine_part.isolation.title"));
        this.state = state;
        this.onPress = onPress;
    }

    public boolean renderTooltipIfHovered(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isHovered()) {
            return false;
        }
        GuiTooltips.render(graphics, font, List.of(
                Component.translatable("gui.ae2pr.machine_part.isolation.title"),
                Component.translatable(state.getAsBoolean() ? "gui.ae2pr.machine_part.isolation.enabled"
                        : "gui.ae2pr.machine_part.isolation.disabled")), mouseX, mouseY);
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 边框只看鼠标悬停，避免点击后焦点残留高亮
        GuiTextures.frame(graphics,
                isHovered() ? GuiTextures.BUTTON_HOVER : GuiTextures.BUTTON, getX(), getY());
        GuiTextures.frame(graphics, state.getAsBoolean() ? ICON_ON : ICON_OFF, getX(), getY());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        setMessage(Component.translatable("gui.ae2pr.machine_part.isolation.title")
                .append(": ").append(Component.translatable(state.getAsBoolean()
                        ? "gui.ae2pr.machine_part.isolation.enabled"
                        : "gui.ae2pr.machine_part.isolation.disabled")));
        defaultButtonNarrationText(narration);
    }
}
