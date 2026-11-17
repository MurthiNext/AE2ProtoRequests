package com.murthinext.ae2pr.api.recipe;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record ItemInput(Ingredient ingredient, int count, boolean catalyst) {

    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    public List<ItemStack> stacks() {
        return Arrays.stream(ingredient.getItems())
                .map(stack -> stack.copyWithCount(count))
                .toList();
    }
}
