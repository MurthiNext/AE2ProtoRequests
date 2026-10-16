package com.murthinext.ae2pr.client.repeat;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;


import appeng.api.stacks.AEKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端重复订单物品的剩余/总轮数记录。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientRepeatOrderRounds {
    private static final Map<AEKey, RoundInfo> ROUNDS = new HashMap<>();

    private ClientRepeatOrderRounds() {
    }

    public static void update(@Nullable AEKey what, int remainingRounds, int totalRounds) {
        if (what == null) {
            return;
        }
        if (remainingRounds <= 0) {
            ROUNDS.remove(what);
        } else {
            ROUNDS.put(what, new RoundInfo(remainingRounds, totalRounds));
        }
    }

    @Nullable
    public static RoundInfo get(@Nullable AEKey what) {
        if (what == null) {
            return null;
        }
        return ROUNDS.get(what);
    }

    public static void onFinished(@Nullable AEKey what, long amountPerRound, int totalRounds) {
        if (what == null) {
            return;
        }
        ROUNDS.remove(what);
        RepeatOrderFinishedToast.show(what, amountPerRound, totalRounds);
    }

    public static void clear() {
        ROUNDS.clear();
    }

    public record RoundInfo(int remainingRounds, int totalRounds) {
    }
}
