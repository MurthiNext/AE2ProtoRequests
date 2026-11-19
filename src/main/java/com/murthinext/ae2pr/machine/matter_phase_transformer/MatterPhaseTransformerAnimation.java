package com.murthinext.ae2pr.machine.matter_phase_transformer;

import net.minecraft.util.Mth;

final class MatterPhaseTransformerAnimation {

    private float power;
    private float previousPower;
    private float heat;
    private float previousHeat;
    private long pourTicks;

    void tick(boolean formed, boolean running) {
        if (!formed) {
            previousPower = power = 0;
            previousHeat = heat = 0;
            pourTicks = 0;
            return;
        }
        previousPower = power;
        previousHeat = heat;
        power = Mth.approach(power, running ? 1 : 0, running ? 1 / 10F : 1 / 5F);
        float targetHeat = power * power;
        heat = Mth.approach(heat, targetHeat, targetHeat > heat ? 1 / 24F : 1 / 54F);
        if (heat >= 0.32F || (pourTicks > 0 && heat > 0)) {
            pourTicks++;
        } else {
            pourTicks = 0;
        }
    }

    float power(float partialTick) {
        return Mth.lerp(partialTick, previousPower, power);
    }

    float heat(float partialTick) {
        return Mth.lerp(partialTick, previousHeat, heat);
    }

    double pourTime(float partialTick) {
        return pourTicks == 0 ? 0 : (double) (pourTicks - 1) + partialTick;
    }
}
