package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.CrystalReactionChamberCategory;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class CrystalReactionChamberPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("crystal_reaction_chamber");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrystalReactionChamberCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, CrystalReactionChamberCategory.RECIPE_TYPE,
                ModMachines.CRYSTAL_REACTION_CHAMBER_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalReactionChamberCategory.RECIPE_TYPE,
                ModMachines.CRYSTAL_REACTION_CHAMBER.item());
    }
}
