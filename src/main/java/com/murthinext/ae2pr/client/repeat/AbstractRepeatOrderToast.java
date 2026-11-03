package com.murthinext.ae2pr.client.repeat;

import java.util.List;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

abstract class AbstractRepeatOrderToast implements Toast {

    private static final long TIME_VISIBLE = 2500;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int ICON_X = 8;
    private static final int ICON_Y = 8;
    private static final int TEXT_X = 30;
    private static final int TITLE_Y = 7;
    private static final int TEXT_Y = 18;

    protected final AEKey what;
    private final Component title;
    private final int titleColor;
    private final List<FormattedCharSequence> lines;
    private final int height;

    protected AbstractRepeatOrderToast(AEKey what, Component title, int titleColor, List<Component> bodyLines) {
        this.what = what;
        this.title = title;
        this.titleColor = titleColor;
        var font = Minecraft.getInstance().font;
        var wrapped = new java.util.ArrayList<FormattedCharSequence>();
        for (Component line : bodyLines) {
            wrapped.addAll(font.split(line, width() - 35));
        }
        this.lines = wrapped;
        this.height = Toast.super.height() + (lines.size() - 1) * font.lineHeight;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        var font = Minecraft.getInstance().font;
        graphics.blit(TEXTURE, 0, 0, 0, 32, width(), 8);
        int middleHeight = height - 16;
        for (int middleY = 0; middleY < middleHeight; middleY += 16) {
            graphics.blit(TEXTURE, 0, 8 + middleY, 0, 32 + 8, width(), Math.min(middleHeight - middleY, 16));
        }
        graphics.blit(TEXTURE, 0, height - 8, 0, 32 + 32 - 8, width(), 8);

        graphics.drawString(font, title, TEXT_X, TITLE_Y, titleColor, false);
        int lineY = TEXT_Y;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, TEXT_X, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight;
        }
        AEKeyRendering.drawInGui(Minecraft.getInstance(), graphics, ICON_X, ICON_Y, what);

        return timeSinceLastVisible >= TIME_VISIBLE ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int height() {
        return height;
    }
}
