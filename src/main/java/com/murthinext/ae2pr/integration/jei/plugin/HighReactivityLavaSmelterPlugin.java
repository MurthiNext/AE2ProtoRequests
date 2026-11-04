package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.HighReactivityLavaSmelterCategory;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class HighReactivityLavaSmelterPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("high_reactivity_lava_smelter");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new HighReactivityLavaSmelterCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, HighReactivityLavaSmelterCategory.RECIPE_TYPE,
                ModMachines.HIGH_REACTIVITY_LAVA_SMELTER_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(HighReactivityLavaSmelterCategory.RECIPE_TYPE,
                ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.item());
    }
}
