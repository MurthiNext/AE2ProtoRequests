package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.integration.jei.category.CrystalAssemblyLineCategory;
import com.murthinext.ae2pr.integration.jei.transfer.CrystalAssemblyLinePatternTransferHandler;
import com.murthinext.ae2pr.registry.ModMachines;

@JeiPlugin
public class CrystalAssemblyLinePlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("crystal_assembly_line");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrystalAssemblyLineCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MachineRecipes.register(registration, CrystalAssemblyLineCategory.RECIPE_TYPE,
                ModMachines.CRYSTAL_ASSEMBLY_LINE_RECIPE.type());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalAssemblyLineCategory.RECIPE_TYPE,
                ModMachines.CRYSTAL_ASSEMBLY_LINE.block());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CrystalAssemblyLinePatternTransferHandler(),
                CrystalAssemblyLineCategory.RECIPE_TYPE);
    }
}
