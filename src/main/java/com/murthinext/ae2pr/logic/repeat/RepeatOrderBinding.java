package com.murthinext.ae2pr.logic.repeat;

import org.jetbrains.annotations.Nullable;

/**
 * 线程本地绑定，用于传递本次提交的重复轮数。
 */
public final class RepeatOrderBinding {
    private static final ThreadLocal<Integer> ROUNDS = new ThreadLocal<>();

    private RepeatOrderBinding() {
    }

    public static void set(int rounds) {
        ROUNDS.set(rounds);
    }

    @Nullable
    public static Integer consume() {
        var rounds = ROUNDS.get();
        ROUNDS.remove();
        return rounds;
    }

    public static void clear() {
        ROUNDS.remove();
    }
}
