package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.ae2pr.api.text.AmountText;
import com.murthinext.nextui.client.GuiTooltips;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

public final class FluidStackDisplay {

    private static final float AMOUNT_SCALE = 0.5F;
    private static final int AMOUNT_MAX_CHARS = 6;

    private FluidStackDisplay() {}

    public static void renderTankTooltip(GuiGraphics graphics, Font font, FluidStack fluid, int mouseX, int mouseY) {
        GuiTooltips.render(
                graphics,
                font,
                List.of(
                        fluid.isEmpty()
                                ? Component.translatable("gui.ae2pr.machine_part.tank.empty")
                                : fluid.getDisplayName(),
                        Component.translatable("gui.ae2pr.unit.mb", AmountText.exact(fluid.getAmount()))),
                mouseX,
                mouseY);
    }

    public static void drawAmount(GuiGraphics graphics, Font font, int milliBuckets, int right, int bottom) {
        String amount = AmountText.compactFluid(milliBuckets, AMOUNT_MAX_CHARS);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(right, bottom, 0);
        pose.scale(AMOUNT_SCALE, AMOUNT_SCALE, 1.0F);
        graphics.drawString(font, amount, -font.width(amount), -font.lineHeight, 0xFFFFFF, true);
        pose.popPose();
    }
}
