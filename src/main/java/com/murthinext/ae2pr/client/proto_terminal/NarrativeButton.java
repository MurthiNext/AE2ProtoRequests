package com.murthinext.ae2pr.client.proto_terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 叙事屏幕的平行四边形按钮。
 */
final class NarrativeButton extends Button {

    enum Icon {
        CLOSE, BACK
    }

    private final float slant;
    private final Icon icon;
    private float opacity = 1;
    private float scale = 1;

    NarrativeButton(int x, int y, int width, Component label, OnPress action) {
        this(x, y, width, 22, label, action);
    }

    NarrativeButton(int x, int y, int width, int height, Component label, OnPress action) {
        this(x, y, width, height, null, label, action);
    }

    NarrativeButton(int x, int y, int size, Icon icon, Component label, OnPress action) {
        this(x, y, size, size, icon, label, action);
    }

    private NarrativeButton(int x, int y, int width, int height, Icon icon, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.slant = CrystalNarrativeGraphics.slant(height);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (opacity < 4.0F / 255) {
            return;
        }
        isHovered = isMouseOver(mouseX, mouseY);
        boolean highlighted = isHovered || isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        int border = CrystalNarrativeGraphics.alpha(
                highlighted ? CrystalNarrativeGraphics.ACCENT : CrystalNarrativeGraphics.BORDER, opacity);
        int text = CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.TEXT, opacity);
        int fill = CrystalNarrativeGraphics.alpha(highlighted ? 0xFF10233D : CrystalNarrativeGraphics.PANEL, opacity);
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        graphics.pose().scale(scale, scale, 1);
        try {
            CrystalNarrativeGraphics.panel(graphics, 0, 0, width, height, slant, fill, border);
            if (icon != null) {
                float x = width / 2.0F;
                float y = height / 2.0F;
                graphics.drawManaged(() -> {
                    if (icon == Icon.CLOSE) {
                        CrystalNarrativeGraphics.line(graphics, x - 4, y - 4, x + 4, y + 4, text);
                        CrystalNarrativeGraphics.line(graphics, x - 4, y + 4, x + 4, y - 4, text);
                    } else {
                        CrystalNarrativeGraphics.line(graphics, x - 5, y, x + 5, y, text);
                        CrystalNarrativeGraphics.line(graphics, x - 5, y, x - 1, y - 4, text);
                        CrystalNarrativeGraphics.line(graphics, x - 5, y, x - 1, y + 4, text);
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
        return CrystalNarrativeGraphics.contains((mouseX - getX()) / scale, (mouseY - getY()) / scale,
                0, 0, width, height, slant);
    }
}
