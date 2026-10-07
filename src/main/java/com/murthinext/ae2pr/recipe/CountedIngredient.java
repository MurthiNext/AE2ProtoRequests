package com.murthinext.ae2pr.recipe;

import net.minecraft.world.item.crafting.Ingredient;

/** 带数量的原料：机器配方按任意数量消耗。 */
public record CountedIngredient(Ingredient ingredient, int count) {
}
