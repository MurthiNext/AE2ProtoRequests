package com.murthinext.ae2pr.block.machine_part;

import com.murthinext.ae2pr.api.machine.client.GuiTextures;
import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.client.IconButton;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RecipeIsolationButton extends IconButton {

    private static final ResourceLocation ICON_ON = GuiTextures.ISOLATION_ON;
    private static final ResourceLocation ICON_OFF = GuiTextures.ISOLATION_OFF;

    private final BooleanSupplier state;

    public RecipeIsolationButton(int x, int y, BooleanSupplier state, Runnable onPress) {
        super(
                x,
                y,
                Component.translatable("gui.ae2pr.machine_part.isolation.title"),
                GuiTextures.BUTTON_SKIN,
                () -> state.getAsBoolean() ? ICON_ON : ICON_OFF,
                () -> false,
                onPress);
        this.state = state;
    }

    public boolean renderTooltipIfTarget(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!GuiTooltips.isTooltipTarget(this)) {
            return false;
        }
        GuiTooltips.render(graphics, font, List.of(getMessage(), stateLabel()), mouseX, mouseY);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
        narration.add(net.minecraft.client.gui.narration.NarratedElementType.HINT, stateLabel());
    }

    private Component stateLabel() {
        return Component.translatable(
                state.getAsBoolean()
                        ? "gui.ae2pr.machine_part.isolation.enabled"
                        : "gui.ae2pr.machine_part.isolation.disabled");
    }
}
