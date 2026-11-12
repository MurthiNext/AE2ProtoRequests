package com.murthinext.ae2pr.api.upgrade;

import java.util.function.Supplier;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.math.LatexFormula;

public final class MachineUpgradeDefinition {
    private final String id;
    private final MachineUpgradeType type;
    private final String fallback;
    private final Supplier<String> configuredFormula;
    private LatexFormula formula;
    private boolean warned;

    public MachineUpgradeDefinition(String id, MachineUpgradeType type, String fallback,
            Supplier<String> configuredFormula) {
        this.id = id;
        this.type = type;
        this.fallback = fallback;
        this.configuredFormula = configuredFormula;
    }

    public String id() {
        return id;
    }

    public MachineUpgradeType type() {
        return type;
    }

    // 配置校验已保证公式合法；这里再兜底一次，避免同步后的坏值抛异常
    public synchronized LatexFormula formula() {
        String source = configuredFormula.get();
        if (formula == null || !formula.source().equals(source)) {
            try {
                formula = LatexFormula.parse(source);
                warned = false;
            } catch (IllegalArgumentException exception) {
                if (!warned) {
                    ae2pr.LOGGER.warn("Invalid formula {} for upgrade {}; using default", source, id);
                    warned = true;
                }
                if (formula == null || !formula.source().equals(fallback)) {
                    formula = LatexFormula.parse(fallback);
                }
            }
        }
        return formula;
    }

    public synchronized double apply(double value) {
        double result = formula().evaluate(value);
        if (!Double.isFinite(result)) {
            if (!warned) {
                ae2pr.LOGGER.warn("Upgrade {} produced a non-finite result for x={}; ignoring this application", id, value);
                warned = true;
            }
            return value;
        }
        return type.clamp(value, result);
    }
}
