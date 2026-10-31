package com.murthinext.ae2pr.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;

@JeiPlugin
public class FilterPressJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "filter_press");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FilterPressJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(FilterPressJeiCategory.RECIPE_TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModMachines.FILTER_PRESS_RECIPE.type()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FilterPressJeiCategory.RECIPE_TYPE, ModMachines.FILTER_PRESS.item());
    }

}
