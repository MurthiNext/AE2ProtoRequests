package com.murthinext.ae2pr.api.machine.client;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import appeng.core.localization.ButtonToolTips;

public class GuideButton extends ToolbarButton {

    public GuideButton(int x, int y, Runnable onPress) {
        super(x, y, GuiTextures.GUIDE, () -> false, onPress);
        setMessage(ButtonToolTips.OpenGuide.text());
    }

    public boolean renderTooltipIfHovered(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isHovered()) {
            return false;
        }
        GuiTooltips.render(graphics, font,
                           List.of(ButtonToolTips.OpenGuide.text(), ButtonToolTips.OpenGuideDetail.text()), mouseX,
                           mouseY);
        return true;
    }
}
