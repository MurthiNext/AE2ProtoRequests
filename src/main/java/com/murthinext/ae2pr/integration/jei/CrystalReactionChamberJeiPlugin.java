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

/**
 * 注册水晶反应仓配方分类与催化剂的 JEI 插件。
 */
@JeiPlugin
public class CrystalReactionChamberJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_reaction_chamber");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrystalReactionChamberJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(CrystalReactionChamberJeiCategory.RECIPE_TYPE,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.CRYSTAL_REACTION_CHAMBER_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalReactionChamberJeiCategory.RECIPE_TYPE,
                ModItems.CRYSTAL_REACTION_CHAMBER.get());
    }
}
