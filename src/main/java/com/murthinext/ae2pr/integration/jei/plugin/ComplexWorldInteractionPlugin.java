package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.ComplexWorldInteractionCategory;
import com.murthinext.ae2pr.registry.ModRecipes;

@JeiPlugin
public class ComplexWorldInteractionPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("complex_world_interaction");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ComplexWorldInteractionCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(ComplexWorldInteractionCategory.RECIPE_TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get()));
        }
    }
}
