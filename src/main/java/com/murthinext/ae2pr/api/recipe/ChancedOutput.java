package com.murthinext.ae2pr.api.recipe;

import net.minecraft.util.RandomSource;

// 带概率的产物，chance 取值 (0, 1]
public interface ChancedOutput {

    float chance();

    default boolean certain() {
        return chance() >= 1.0F;
    }

    default boolean roll(RandomSource random) {
        return certain() || random.nextFloat() < chance();
    }

    static float checkChance(float chance) {
        if (!(chance > 0.0F) || chance > 1.0F) {
            throw new IllegalArgumentException("chance must be in (0, 1]");
        }
        return chance;
    }
}
