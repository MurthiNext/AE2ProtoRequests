package com.murthinext.ae2pr.client.proto_terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 叙事屏幕的平行四边形按钮。 */
final class NarrativeButton extends Button {

    enum Icon {
        CLOSE, BACK
    }

    private final float slant;
    private final Icon icon;
    private float opacity = 1;

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
        boolean highlighted = isHoveredOrFocused();
        int border = CrystalNarrativeGraphics.alpha(
                highlighted ? CrystalNarrativeGraphics.ACCENT : CrystalNarrativeGraphics.BORDER, opacity);
        int text = CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.TEXT, opacity);
        int fill = CrystalNarrativeGraphics.alpha(highlighted ? 0xFF10233D : CrystalNarrativeGraphics.PANEL, opacity);
        CrystalNarrativeGraphics.panel(graphics, getX(), getY(), width, height, slant, fill, border);
        if (icon != null) {
            float x = getX() + width / 2.0F;
            float y = getY() + height / 2.0F;
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
                graphics.drawString(font, line, getX() + (width - font.width(line)) / 2,
                        getY() + (height - count * 12) / 2 + i * 12 + 2, text, false);
            }
            graphics.fill(getX() + 14, getY() + height - 7, getX() + 24, getY() + height - 6, border);
        } else {
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 7, text);
        }
    }

    void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!active || !visible || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        return CrystalNarrativeGraphics.contains(mouseX, mouseY, getX(), getY(), width, height, slant);
    }
}
