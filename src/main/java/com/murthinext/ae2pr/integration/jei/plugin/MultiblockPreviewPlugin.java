package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.MultiblockPreviewCategory;
import com.murthinext.ae2pr.integration.jei.recipe.MultiblockPreviewRecipe;
import com.murthinext.ae2pr.integration.jei.transfer.MultiblockPatternTransferHandler;

@JeiPlugin
public class MultiblockPreviewPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("multiblock_preview");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockPreviewCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MultiblockPreviewCategory.RECIPE_TYPE, MultiblockPreviewRecipe.recipes());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new MultiblockPatternTransferHandler(),
                MultiblockPreviewCategory.RECIPE_TYPE);
    }
}
