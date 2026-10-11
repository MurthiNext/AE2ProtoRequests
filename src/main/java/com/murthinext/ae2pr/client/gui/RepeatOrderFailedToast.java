package com.murthinext.ae2pr.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.murthinext.ae2pr.logic.repeat.FailureReason;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 重复订单失败的 Toast。
 */
public class RepeatOrderFailedToast implements Toast {
    private static final long TIME_VISIBLE = 2500;
    private static final int TITLE_COLOR = 0xFFB00020;
    private static final int TEXT_COLOR = 0xFF000000;

    private final AEKey what;
    private final List<FormattedCharSequence> lines;
    private final int height;

    public RepeatOrderFailedToast(AEKey what, long amount, int totalRounds, int completedRounds,
            FailureReason reason) {
        this.what = what;

        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        var formattedAmount = what.formatAmount(amount, AmountFormat.SLOT);
        var text = Component.translatable("gui.ae2pr.repeat_order.failed.text",
                formattedAmount, AEKeyRendering.getDisplayName(what), completedRounds, totalRounds);
        var reasonText = Component.translatable("gui.ae2pr.repeat_order.failed.reason",
                Component.translatable(reasonKey(reason)));

        var lines = new ArrayList<FormattedCharSequence>();
        lines.addAll(font.split(text, width() - 35));
        lines.addAll(font.split(reasonText, width() - 35));
        this.lines = lines;
        this.height = Toast.super.height() + (lines.size() - 1) * font.lineHeight;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        guiGraphics.blit(TEXTURE, 0, 0, 0, 32, this.width(), 8);
        int middleHeight = height - 16;
        for (var middleY = 0; middleY < middleHeight; middleY += 16) {
            var tileHeight = Math.min(middleHeight - middleY, 16);
            guiGraphics.blit(TEXTURE, 0, 8 + middleY, 0, 32 + 8, this.width(), tileHeight);
        }
        guiGraphics.blit(TEXTURE, 0, height - 8, 0, 32 + 32 - 8, this.width(), 8);

        guiGraphics.drawString(font, Component.translatable("gui.ae2pr.repeat_order.failed.title"), 30, 7,
                TITLE_COLOR, false);

        var lineY = 18;
        for (var line : lines) {
            guiGraphics.drawString(font, line, 30, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight;
        }

        AEKeyRendering.drawInGui(minecraft, guiGraphics, 8, 8, what);

        return timeSinceLastVisible >= TIME_VISIBLE ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int height() {
        return height;
    }

    private static String reasonKey(FailureReason reason) {
        return switch (reason) {
            case MISSING_MATERIAL -> "gui.ae2pr.repeat_order.failed.reason.missing";
            case CPU_TOO_SMALL -> "gui.ae2pr.repeat_order.failed.reason.cpu_small";
            case CPU_BUSY -> "gui.ae2pr.repeat_order.failed.reason.cpu_busy";
            case CPU_OFFLINE -> "gui.ae2pr.repeat_order.failed.reason.cpu_offline";
            case NO_CPU -> "gui.ae2pr.repeat_order.failed.reason.no_cpu";
            case UNKNOWN -> "gui.ae2pr.repeat_order.failed.reason.unknown";
        };
    }
}
