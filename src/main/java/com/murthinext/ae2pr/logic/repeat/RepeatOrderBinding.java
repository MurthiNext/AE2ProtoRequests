package com.murthinext.ae2pr.logic.repeat;

import org.jetbrains.annotations.Nullable;

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
