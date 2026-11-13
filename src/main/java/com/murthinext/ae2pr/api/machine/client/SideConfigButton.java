package com.murthinext.ae2pr.api.machine.client;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class SideConfigButton extends ToolbarButton {

    public SideConfigButton(int x, int y, BooleanSupplier selected, Runnable onPress) {
        super(x, y, GuiTextures.SIDE_CONFIG, selected, onPress);
        setMessage(Component.translatable("gui.ae2pr.side_config.title"));
    }

    public boolean renderTooltipIfHovered(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isHovered()) {
            return false;
        }
        GuiTooltips.render(graphics, font,
                           List.of(Component.translatable("gui.ae2pr.side_config.title"),
                                   Component.translatable("gui.ae2pr.side_config.desc")),
                           mouseX, mouseY);
        return true;
    }
}
