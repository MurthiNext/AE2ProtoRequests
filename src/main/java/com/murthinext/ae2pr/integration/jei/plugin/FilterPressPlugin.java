package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.FilterPressCategory;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class FilterPressPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("filter_press");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FilterPressCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, FilterPressCategory.RECIPE_TYPE,
                ModMachines.FILTER_PRESS_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FilterPressCategory.RECIPE_TYPE, ModMachines.FILTER_PRESS.item());
    }
}
