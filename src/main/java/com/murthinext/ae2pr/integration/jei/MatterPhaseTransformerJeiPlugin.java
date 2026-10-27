package com.murthinext.ae2pr.integration.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;

@JeiPlugin
public class MatterPhaseTransformerJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "matter_phase_transformer");
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
                List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.MATTER_PHASE_TRANSFORMER_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(MatterPhaseTransformerJeiCategory.RECIPE_TYPE,
                ModItems.MATTER_PHASE_TRANSFORMER.get());
    }
}
