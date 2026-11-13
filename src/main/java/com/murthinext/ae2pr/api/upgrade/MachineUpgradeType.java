package com.murthinext.ae2pr.api.upgrade;

public enum MachineUpgradeType {
    SPEED,
    PARALLEL,
    ENERGY;

    public double clamp(double previous, double result) {
        return switch (this) {
            case SPEED -> Math.min(Integer.MAX_VALUE, Math.max(previous, result));
            case PARALLEL -> Math.min(Integer.MAX_VALUE, Math.max(previous, Math.floor(result)));
            case ENERGY -> Math.max(0, Math.min(previous, result));
        };
    }
}
