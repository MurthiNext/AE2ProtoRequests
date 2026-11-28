package com.murthinext.ae2pr.item.proto_terminal.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// 终端按钮
public final class Button extends net.minecraft.client.gui.components.Button {

    public enum Icon {
        CLOSE,
        BACK
    }

    private final float slant;
    private final Icon icon;
    private float opacity = 1;
    private float scale = 1;
    private float offsetX;
    private float offsetY;
    private boolean locked;

    Button(int x, int y, int width, Component label, OnPress action) {
        this(x, y, width, 22, label, action);
    }

    public Button(int x, int y, int width, int height, Component label, OnPress action) {
        this(x, y, width, height, null, label, action);
    }

    public Button(int x, int y, int size, Icon icon, Component label, OnPress action) {
        this(x, y, size, size, icon, label, action);
    }

    private Button(int x, int y, int width, int height, Icon icon, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.slant = Panel.slant(height);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (opacity < 4.0F / 255) {
            return;
        }
        isHovered = isMouseOver(mouseX, mouseY);
        boolean highlighted = isHovered
                || isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        int border = Panel.alpha(
                highlighted
                        ? (locked ? Panel.LOCKED_ACCENT : Panel.ACCENT)
                        : locked ? Panel.LOCKED_BORDER : Panel.BORDER,
                opacity);
        int text = Panel.alpha(Panel.TEXT, opacity);
        int fill = Panel.alpha(
                highlighted ? (locked ? 0xFF08111F : 0xFF10233D) : locked ? Panel.LOCKED_PANEL : Panel.PANEL, opacity);
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + offsetX, getY() + offsetY, 0);
        graphics.pose().scale(scale, scale, 1);
        try {
            Panel.panel(graphics, 0, 0, width, height, slant, fill, border);
            if (icon != null) {
                float x = width / 2.0F;
                float y = height / 2.0F;
                graphics.drawManaged(() -> {
                    if (icon == Icon.CLOSE) {
                        Panel.line(graphics, x - 4, y - 4, x + 4, y + 4, text);
                        Panel.line(graphics, x - 4, y + 4, x + 4, y - 4, text);
                    } else {
                        Panel.line(graphics, x - 5, y, x + 5, y, text);
                        Panel.line(graphics, x - 5, y, x - 1, y - 4, text);
                        Panel.line(graphics, x - 5, y, x - 1, y + 4, text);
                    }
                });
                return;
            }
            var font = Minecraft.getInstance().font;
            if (locked) {
                // 未解锁的剧情块不显示标题
                graphics.fill(14, height - 7, 24, height - 6, border);
            } else if (height > 22) {
                var lines = font.split(getMessage(), width - 30);
                int count = Math.min(2, lines.size());
                for (int i = 0; i < count; i++) {
                    var line = lines.get(i);
                    graphics.drawString(
                            font,
                            line,
                            (width - font.width(line)) / 2,
                            (height - count * 12) / 2 + i * 12 + 2,
                            text,
                            false);
                }
                graphics.fill(14, height - 7, 24, height - 6, border);
            } else {
                graphics.drawCenteredString(font, getMessage(), width / 2, 7, text);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
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
        return Panel.contains(
                (mouseX - getX() - offsetX) / scale, (mouseY - getY() - offsetY) / scale, 0, 0, width, height, slant);
    }
}
