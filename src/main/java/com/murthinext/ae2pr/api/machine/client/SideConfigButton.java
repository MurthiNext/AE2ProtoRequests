package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.client.IconButton;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class SideConfigButton extends IconButton {

    public SideConfigButton(int x, int y, BooleanSupplier selected, Runnable onPress) {
        super(
                x,
                y,
                Component.translatable("gui.ae2pr.side_config.title"),
                GuiTextures.BUTTON_SKIN,
                () -> GuiTextures.SIDE_CONFIG,
                selected,
                onPress);
    }

    public boolean renderTooltipIfTarget(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!GuiTooltips.isTooltipTarget(this)) {
            return false;
        }
        GuiTooltips.render(
                graphics,
                font,
                List.of(
                        Component.translatable("gui.ae2pr.side_config.title"),
                        Component.translatable("gui.ae2pr.side_config.desc")),
                mouseX,
                mouseY);
        return true;
    }
}
