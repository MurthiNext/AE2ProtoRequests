package com.murthinext.ae2pr.client.machine_part;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

public class RecipeIsolationButton extends AbstractWidget {

    private static final int SIZE = 18;
    private static final int COLOR_BACKGROUND = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF39424E;
    private static final int COLOR_BORDER_HOVER = 0xFF5A7A9A;

    private static final ResourceLocation ICON_ON = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/guis/chamber_isolation_on.png");
    private static final ResourceLocation ICON_OFF = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/guis/chamber_isolation_off.png");

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
        graphics.renderComponentTooltip(font, List.of(
                Component.translatable("gui.ae2pr.machine_part.isolation.title"),
                Component.translatable(state.getAsBoolean() ? "gui.ae2pr.machine_part.isolation.enabled"
                        : "gui.ae2pr.machine_part.isolation.disabled")), mouseX, mouseY);
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean enabled = state.getAsBoolean();
        // 边框只看鼠标悬停，避免点击后焦点残留高亮
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHovered() ? COLOR_BORDER_HOVER : COLOR_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLOR_BACKGROUND);
        graphics.blit(enabled ? ICON_ON : ICON_OFF, getX(), getY(), 0, 0, width, height, SIZE, SIZE);
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
