package com.murthinext.ae2pr.api.machine.client;

import appeng.core.localization.ButtonToolTips;
import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.client.IconButton;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class GuideButton extends IconButton {

    public GuideButton(int x, int y, Runnable onPress) {
        super(
                x,
                y,
                ButtonToolTips.OpenGuide.text(),
                GuiTextures.BUTTON_SKIN,
                () -> GuiTextures.GUIDE,
                () -> false,
                onPress);
    }

    public boolean renderTooltipIfTarget(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!GuiTooltips.isTooltipTarget(this)) {
            return false;
        }
        GuiTooltips.render(
                graphics,
                font,
                List.of(ButtonToolTips.OpenGuide.text(), ButtonToolTips.OpenGuideDetail.text()),
                mouseX,
                mouseY);
        return true;
    }
}
