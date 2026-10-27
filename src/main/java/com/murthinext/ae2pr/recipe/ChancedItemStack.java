package com.murthinext.ae2pr.recipe;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

// 带概率的物品产物，概率取 0~1
public record ChancedItemStack(ItemStack stack, float chance) {

    public ChancedItemStack {
        stack = stack.copy();
    }

    public static ChancedItemStack certain(ItemStack stack) {
        return new ChancedItemStack(stack, 1.0F);
    }

    public boolean certain() {
        return chance >= 1.0F;
    }

    public boolean roll(RandomSource random) {
        return certain() || random.nextFloat() < chance;
    }
}
