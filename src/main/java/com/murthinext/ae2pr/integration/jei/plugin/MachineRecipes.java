package com.murthinext.ae2pr.integration.jei.plugin;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import mezz.jei.api.registration.IRecipeRegistration;

public final class MachineRecipes {

    private MachineRecipes() {
    }

    public static <C extends Container, R extends Recipe<C>> void register(IRecipeRegistration registration,
            mezz.jei.api.recipe.RecipeType<R> jeiType, RecipeType<R> vanillaType) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(jeiType, List.copyOf(level.getRecipeManager().getAllRecipesFor(vanillaType)));
        }
    }
}
