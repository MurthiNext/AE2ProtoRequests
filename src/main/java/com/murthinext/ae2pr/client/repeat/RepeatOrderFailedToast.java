package com.murthinext.ae2pr.client.repeat;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;

import com.murthinext.ae2pr.logic.repeat.FailureReason;

public final class RepeatOrderFailedToast extends AbstractRepeatOrderToast {

    private static final int TITLE_COLOR = 0xFFB00020;

    public static void show(AEKey what, long amount, int totalRounds, int completedRounds, FailureReason reason) {
        Minecraft.getInstance().getToasts().addToast(new RepeatOrderFailedToast(what, amount, totalRounds,
                                                                                completedRounds, reason));
    }
    RepeatOrderFailedToast(AEKey what, long amount, int totalRounds, int completedRounds, FailureReason reason) {
        super(what, Component.translatable("gui.ae2pr.repeat_order.failed.title"), TITLE_COLOR,
              List.of(Component.translatable("gui.ae2pr.repeat_order.failed.text",
                                             what.formatAmount(amount, AmountFormat.SLOT),
                                             AEKeyRendering.getDisplayName(what), completedRounds, totalRounds),
                      Component.translatable("gui.ae2pr.repeat_order.failed.reason",
                                             Component.translatable(reasonKey(reason)))));
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
