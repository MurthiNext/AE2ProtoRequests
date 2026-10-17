package com.murthinext.ae2pr.client.proto_terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 叙事屏幕的平行四边形按钮。
 */
final class TerminalButton extends Button {

    enum Icon {
        CLOSE, BACK
    }

    private final float slant;
    private final Icon icon;
    private float opacity = 1;
    private float scale = 1;
    private float offsetX;
    private float offsetY;

    TerminalButton(int x, int y, int width, Component label, OnPress action) {
        this(x, y, width, 22, label, action);
    }

    TerminalButton(int x, int y, int width, int height, Component label, OnPress action) {
        this(x, y, width, height, null, label, action);
    }

    TerminalButton(int x, int y, int size, Icon icon, Component label, OnPress action) {
        this(x, y, size, size, icon, label, action);
    }

    private TerminalButton(int x, int y, int width, int height, Icon icon, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.slant = TerminalGraphicsRenderer.slant(height);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (opacity < 4.0F / 255) {
            return;
        }
        isHovered = isMouseOver(mouseX, mouseY);
        boolean highlighted = isHovered || isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        int border = TerminalGraphicsRenderer.alpha(
                highlighted ? TerminalGraphicsRenderer.ACCENT : TerminalGraphicsRenderer.BORDER, opacity);
        int text = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.TEXT, opacity);
        int fill = TerminalGraphicsRenderer.alpha(highlighted ? 0xFF10233D : TerminalGraphicsRenderer.PANEL, opacity);
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + offsetX, getY() + offsetY, 0);
        graphics.pose().scale(scale, scale, 1);
        try {
            TerminalGraphicsRenderer.panel(graphics, 0, 0, width, height, slant, fill, border);
            if (icon != null) {
                float x = width / 2.0F;
                float y = height / 2.0F;
                graphics.drawManaged(() -> {
                    if (icon == Icon.CLOSE) {
                        TerminalGraphicsRenderer.line(graphics, x - 4, y - 4, x + 4, y + 4, text);
                        TerminalGraphicsRenderer.line(graphics, x - 4, y + 4, x + 4, y - 4, text);
                    } else {
                        TerminalGraphicsRenderer.line(graphics, x - 5, y, x + 5, y, text);
                        TerminalGraphicsRenderer.line(graphics, x - 5, y, x - 1, y - 4, text);
                        TerminalGraphicsRenderer.line(graphics, x - 5, y, x - 1, y + 4, text);
                    }
                });
                return;
            }
            var font = Minecraft.getInstance().font;
            if (height > 22) {
                var lines = font.split(getMessage(), width - 30);
                int count = Math.min(2, lines.size());
                for (int i = 0; i < count; i++) {
                    var line = lines.get(i);
                    graphics.drawString(font, line, (width - font.width(line)) / 2,
                            (height - count * 12) / 2 + i * 12 + 2, text, false);
                }
                graphics.fill(14, height - 7, 24, height - 6, border);
            } else {
                graphics.drawCenteredString(font, getMessage(), width / 2, 7, text);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!active || !visible) {
            return false;
        }
        return TerminalGraphicsRenderer.contains((mouseX - getX() - offsetX) / scale,
                (mouseY - getY() - offsetY) / scale, 0, 0, width, height, slant);
    }
}
