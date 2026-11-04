package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.CrystalWorkstationCategory;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class CrystalWorkstationPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("crystal_workstation");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
            registration.addRecipeCategories(new CrystalWorkstationCategory(guiHelper, mode));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
            registration.addRecipes(CrystalWorkstationCategory.recipeType(mode),
                    level.getRecipeManager().getAllRecipesFor(mode.recipeType()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalWorkstationCategory.ETCHING, ModMachines.CRYSTAL_WORKSTATION.item());
        registration.addRecipeCatalysts(CrystalWorkstationCategory.CUTTING, ModMachines.CRYSTAL_WORKSTATION.item());
    }
}
