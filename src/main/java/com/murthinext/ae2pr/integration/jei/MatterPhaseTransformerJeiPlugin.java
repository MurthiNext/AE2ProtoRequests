package com.murthinext.ae2pr.integration.jei;

import com.murthinext.ae2pr.api.Ids;

import java.util.List;

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
public class MatterPhaseTransformerJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of( "matter_phase_transformer");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MatterPhaseTransformerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(MatterPhaseTransformerJeiCategory.RECIPE_TYPE,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(ModMachines.MATTER_PHASE_TRANSFORMER_RECIPE.type())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(MatterPhaseTransformerJeiCategory.RECIPE_TYPE,
                ModMachines.MATTER_PHASE_TRANSFORMER.item());
    }
}
