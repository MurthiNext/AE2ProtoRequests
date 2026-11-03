package com.murthinext.ae2pr.client.repeat;

import java.util.List;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class RepeatOrderFinishedToast extends AbstractRepeatOrderToast {

    private static final int TITLE_COLOR = 0xFF500050;

    public static void show(AEKey what, long amountPerRound, int totalRounds) {
        var minecraft = Minecraft.getInstance();
        if (AEConfig.instance().isNotifyForFinishedCraftingJobs()
                && !(minecraft.screen instanceof MEStorageScreen<?>)
                && minecraft.player != null
                && ClientRepeatOrderFailures.hasNotificationEnablingItem(minecraft.player)) {
            minecraft.getToasts().addToast(new RepeatOrderFinishedToast(what, amountPerRound, totalRounds));
        }
    }

    private RepeatOrderFinishedToast(AEKey what, long amountPerRound, int totalRounds) {
        super(what, GuiText.ToastCraftingJobFinishedTitle.text(), TITLE_COLOR, List.of(
                Component.translatable("gui.ae2pr.repeat_order.finished.text",
                        what.formatAmount(amountPerRound, AmountFormat.SLOT),
                        AEKeyRendering.getDisplayName(what), totalRounds)));
    }
}
