package com.murthinext.ae2pr.compat.jei;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ae2pr;

@JeiPlugin
public class MultiblockPreviewJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multiblock_preview");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MultiblockPreviewJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MultiblockPreviewJeiCategory.RECIPE_TYPE, MultiblockPreviewRecipe.recipes());
    }
}
