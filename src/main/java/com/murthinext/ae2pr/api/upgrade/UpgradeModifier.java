package com.murthinext.ae2pr.api.upgrade;

import appeng.api.upgrades.IUpgradeInventory;
import com.murthinext.ae2pr.api.machine.MachineStat;
import com.murthinext.ae2pr.api.machine.MachineTuning;
import java.util.List;
import org.jetbrains.annotations.Nullable;

// 升级卡数值修正的注入点，由升级卡实现安装
public final class UpgradeModifier {

    @FunctionalInterface
    public interface Sink {
        double apply(
                IUpgradeInventory inventory,
                MachineUpgradeType type,
                double value,
                double speedPerCard,
                @Nullable List<MachineStat.Source> sources);
    }

    private static Sink sink = (inventory, type, value, speedPerCard, sources) -> value;

    private static boolean installed;

    private UpgradeModifier() {}

    public static synchronized void install(Sink value) {
        if (installed) {
            throw new IllegalStateException("Upgrade modifier already installed");
        }
        installed = true;
        sink = value;
    }

    public static double apply(IUpgradeInventory inventory, MachineUpgradeType type, double value) {
        return sink.apply(inventory, type, value, value * MachineTuning.speedPerCard(), null);
    }

    public static double apply(
            IUpgradeInventory inventory,
            MachineUpgradeType type,
            double value,
            double speedPerCard,
            @Nullable List<MachineStat.Source> sources) {
        return sink.apply(inventory, type, value, speedPerCard, sources);
    }
}
