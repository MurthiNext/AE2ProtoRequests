package com.murthinext.ae2pr.client.gui;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;

/**
 * 界面左侧工具栏的"打开指南"按钮：外观与工具栏其他按钮一致，图标使用 AE2 原版帮助图标。
 */
public class GuideButton extends AbstractWidget {

    private static final int SIZE = 18;
    private static final int COLOR_BACKGROUND = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF39424E;
    private static final int COLOR_BORDER_HOVER = 0xFF5A7A9A;

    private final Runnable onPress;

    public GuideButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.onPress = onPress;
    }

    /** 悬停时绘制按钮提示；返回 true 表示由本按钮消费。 */
    public boolean renderTooltipIfHovered(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isHovered()) {
            return false;
        }
        graphics.renderComponentTooltip(font, List.of(
                ButtonToolTips.OpenGuide.text(),
                ButtonToolTips.OpenGuideDetail.text()), mouseX, mouseY);
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHovered() ? COLOR_BORDER_HOVER : COLOR_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLOR_BACKGROUND);
        // 复制一份 Blitter，避免与 AE2 界面共享的不透明度等状态互相影响
        Icon.HELP.getBlitter().copy().opacity(1.0F).dest(getX() + 1, getY() + 1).blit(graphics);
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
