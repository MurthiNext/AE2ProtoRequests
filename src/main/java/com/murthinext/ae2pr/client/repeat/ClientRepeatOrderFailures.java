package com.murthinext.ae2pr.client.repeat;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.logic.repeat.FailureReason;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.SearchInventoryEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientRepeatOrderFailures {
    private static final long BLINK_MS = 250L;
    private static final long FLASH_DURATION_MS = BLINK_MS * 2 * 3;
    private static final int FLASH_ALPHA = 0x66;
    private static final int FLASH_DIM_ALPHA = 0x1A;
    private static final int STEADY_ALPHA = 0x59;
    private static final int RED = 0xFF0000;

    private static final Map<AEKey, FailedMark> MARKS = new HashMap<>();

    private ClientRepeatOrderFailures() {
    }

    public static void onFailure(@Nullable AEKey what, long amountPerRound, int totalRounds, int completedRounds,
            FailureReason reason) {
        if (what == null) {
            return;
        }
        MARKS.put(what, new FailedMark(-1));

        var minecraft = Minecraft.getInstance();
        if (Config.notifyOnFailure()
                && !(minecraft.screen instanceof MEStorageScreen<?>)
                && minecraft.player != null
                && hasNotificationEnablingItem(minecraft.player)) {
            minecraft.getToasts().addToast(
                    new RepeatOrderFailedToast(what, amountPerRound, totalRounds, completedRounds, reason));
        }
    }

    public static boolean hasNotificationEnablingItem(LocalPlayer player) {
        for (var stack : SearchInventoryEvent.getItems(player)) {
            if (!stack.isEmpty()
                    && stack.getItem() instanceof WirelessTerminalItem wirelessTerminal
                    && wirelessTerminal.getAECurrentPower(stack) > 0
                    && wirelessTerminal.getLinkedPosition(stack) != null) {
                return true;
            }
        }
        return false;
    }

    public static void renderMark(GuiGraphics guiGraphics, int x, int y, AEKey what) {
        var mark = MARKS.get(what);
        if (mark == null) {
            return;
        }

        var now = System.currentTimeMillis();
        var start = mark.flashStartMillis();
        if (start < 0) {
            start = now;
            MARKS.put(what, new FailedMark(start));
        }

        var elapsed = now - start;
        int alpha;
        if (elapsed < FLASH_DURATION_MS) {
            alpha = (elapsed / BLINK_MS) % 2 == 0 ? FLASH_ALPHA : FLASH_DIM_ALPHA;
        } else {
            alpha = STEADY_ALPHA;
        }

        guiGraphics.fill(x, y, x + 16, y + 16, (alpha << 24) | RED);
    }

    public static void onTerminalOpenedFresh() {
        MARKS.values().removeIf(mark -> mark.flashStartMillis() >= 0);
    }

    public static void clear() {
        MARKS.clear();
    }

    private record FailedMark(long flashStartMillis) {
    }
}
