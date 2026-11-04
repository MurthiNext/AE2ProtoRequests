package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.JigCategory;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class JigPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("jig");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JigCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, JigCategory.RECIPE_TYPE, ModMachines.JIG_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(JigCategory.RECIPE_TYPE, ModMachines.JIG.item());
    }
}
