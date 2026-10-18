package com.murthinext.ae2pr.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;

@JeiPlugin
public class CrystalWorkstationJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_workstation");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
            registration.addRecipeCategories(new CrystalWorkstationJeiCategory(registration.getJeiHelpers().getGuiHelper(), mode));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
                registration.addRecipes(CrystalWorkstationJeiCategory.recipeType(mode),
                        level.getRecipeManager().getAllRecipesFor(mode.recipeType()));
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalWorkstationJeiCategory.ETCHING, ModItems.CRYSTAL_WORKSTATION.get());
        registration.addRecipeCatalysts(CrystalWorkstationJeiCategory.CUTTING, ModItems.CRYSTAL_WORKSTATION.get());
    }
}
