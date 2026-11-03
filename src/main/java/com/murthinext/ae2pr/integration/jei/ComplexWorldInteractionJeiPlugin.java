package com.murthinext.ae2pr.integration.jei;

import com.murthinext.ae2pr.api.Ids;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.registry.ModRecipes;
import com.murthinext.ae2pr.ae2pr;

@JeiPlugin
public class ComplexWorldInteractionJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of( "complex_world_interaction");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ComplexWorldInteractionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(ComplexWorldInteractionJeiCategory.RECIPE_TYPE,
                    List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get())));
        }
    }
}
