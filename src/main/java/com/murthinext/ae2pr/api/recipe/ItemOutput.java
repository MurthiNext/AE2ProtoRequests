package com.murthinext.ae2pr.api.recipe;

import net.minecraft.world.item.ItemStack;

public record ItemOutput(ItemStack stack, float chance) implements ChancedOutput {

    public ItemOutput {
        stack = stack.copy();
        chance = ChancedOutput.checkChance(chance);
    }

    public static ItemOutput certain(ItemStack stack) {
        return new ItemOutput(stack, 1.0F);
    }
}
