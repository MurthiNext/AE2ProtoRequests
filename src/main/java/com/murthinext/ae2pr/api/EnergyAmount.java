package com.murthinext.ae2pr.api;

public final class EnergyAmount {

    public static final double MAX = (double) Long.MAX_VALUE;

    private EnergyAmount() {
    }

    public static double clamp(double amount) {
        if (!(amount > 0)) {
            return 0;
        }
        return Math.min(amount, MAX);
    }

    public static long toLong(double amount) {
        return Math.round(clamp(amount));
    }
}
