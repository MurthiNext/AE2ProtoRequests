package com.murthinext.ae2pr.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.GeneralMachineInfoLayout;
import com.murthinext.ae2pr.client.filter_press.FilterPressScreen;

@JeiPlugin
public class FilterPressJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "filter_press");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FilterPressJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(FilterPressJeiCategory.RECIPE_TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.FILTER_PRESS_TYPE.get()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FilterPressJeiCategory.RECIPE_TYPE, ModItems.FILTER_PRESS.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(FilterPressScreen.class, GeneralMachineInfoLayout.TEXT_X,
                GeneralMachineInfoLayout.JOB_Y, 100, 9, FilterPressJeiCategory.RECIPE_TYPE);
    }
}
