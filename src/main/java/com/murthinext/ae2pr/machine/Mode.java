package com.murthinext.ae2pr.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

public interface Mode {

    String id();

    String translationKey();

    RecipeType<?> recipeType();

    default Component displayName() {
        return Component.translatable(translationKey());
    }
}
