package com.murthinext.ae2pr.api.machine.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// 仿制版 AE2.drawTooltipWithHeader
public final class GuiTooltips {

    private GuiTooltips() {
    }

    public static void render(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        graphics.renderComponentTooltip(font, withHeader(lines), mouseX, mouseY);
    }

    public static List<Component> withHeader(List<Component> lines) {
        List<Component> styled = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (i == 0) {
                styled.add(line.copy().withStyle(ChatFormatting.WHITE));
            } else if (line.getStyle().getColor() == null) {
                styled.add(line.copy().withStyle(ChatFormatting.GRAY));
            } else {
                styled.add(line);
            }
        }
        return styled;
    }
}
