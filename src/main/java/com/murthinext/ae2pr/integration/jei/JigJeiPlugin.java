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
import com.murthinext.ae2pr.client.jig.JigScreen;

@JeiPlugin
public class JigJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "jig");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JigJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(JigJeiCategory.RECIPE_TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.JIG_TYPE.get()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(JigJeiCategory.RECIPE_TYPE, ModItems.JIG.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(JigScreen.class, GeneralMachineInfoLayout.TEXT_X, GeneralMachineInfoLayout.JOB_Y,
                100, 9, JigJeiCategory.RECIPE_TYPE);
    }
}
