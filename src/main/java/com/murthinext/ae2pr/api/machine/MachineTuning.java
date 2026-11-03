package com.murthinext.ae2pr.api.machine;

import java.util.function.DoubleSupplier;

// 机器模板的数值调节入口，由模组配置注入，避免 api 直接读取 Config
public final class MachineTuning {

    private static DoubleSupplier speedPerCard = () -> 0.0D;
    private static DoubleSupplier energyMultiplierPerCard = () -> 1.0D;

    private MachineTuning() {
    }

    public static void install(DoubleSupplier speedPerCard, DoubleSupplier energyMultiplierPerCard) {
        MachineTuning.speedPerCard = speedPerCard;
        MachineTuning.energyMultiplierPerCard = energyMultiplierPerCard;
    }

    public static double speedPerCard() {
        return speedPerCard.getAsDouble();
    }

    public static double energyMultiplierPerCard() {
        return energyMultiplierPerCard.getAsDouble();
    }
}
