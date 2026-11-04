package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.MatterPhaseTransformerCategory;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class MatterPhaseTransformerPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("matter_phase_transformer");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MatterPhaseTransformerCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, MatterPhaseTransformerCategory.RECIPE_TYPE,
                ModMachines.MATTER_PHASE_TRANSFORMER_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(MatterPhaseTransformerCategory.RECIPE_TYPE,
                ModMachines.MATTER_PHASE_TRANSFORMER.item());
    }
}
