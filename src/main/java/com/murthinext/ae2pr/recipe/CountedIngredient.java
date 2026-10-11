package com.murthinext.ae2pr.recipe;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * 带数量的配方原料。
 */
public record CountedIngredient(Ingredient ingredient, int count) {
}
