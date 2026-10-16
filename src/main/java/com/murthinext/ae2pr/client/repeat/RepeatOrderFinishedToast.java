package com.murthinext.ae2pr.client.repeat;

import java.util.List;


import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 重复订单全部轮次完成的 Toast。
 */
public class RepeatOrderFinishedToast implements Toast {
    private static final long TIME_VISIBLE = 2500;
    private static final int TITLE_COLOR = 0xFF500050;
    private static final int TEXT_COLOR = 0xFF000000;

    /**
     * 弹出条件与 AE2 完成提示相同。
     */
    public static void show(AEKey what, long amountPerRound, int totalRounds) {
        var minecraft = Minecraft.getInstance();
        if (AEConfig.instance().isNotifyForFinishedCraftingJobs()
                && !(minecraft.screen instanceof MEStorageScreen<?>)
                && minecraft.player != null
                && ClientRepeatOrderFailures.hasNotificationEnablingItem(minecraft.player)) {
            minecraft.getToasts().addToast(new RepeatOrderFinishedToast(what, amountPerRound, totalRounds));
        }
    }

    private final AEKey what;
    private final List<FormattedCharSequence> lines;
    private final int height;

    public RepeatOrderFinishedToast(AEKey what, long amountPerRound, int totalRounds) {
        this.what = what;

        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        var formattedAmount = what.formatAmount(amountPerRound, AmountFormat.SLOT);
        var text = Component.translatable("gui.ae2pr.repeat_order.finished.text",
                formattedAmount, AEKeyRendering.getDisplayName(what), totalRounds);
        this.lines = font.split(text, width() - 35);
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

        guiGraphics.drawString(font, GuiText.ToastCraftingJobFinishedTitle.text(), 30, 7, TITLE_COLOR, false);

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
}
