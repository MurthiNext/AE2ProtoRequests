package com.murthinext.ae2pr.compat.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;

/**
 * JEI 插件：注册水晶装配线配方分类与主机催化方块。
 */
@JeiPlugin
public class CrystalAssemblyLineJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_assembly_line");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrystalAssemblyLineJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(CrystalAssemblyLineJeiCategory.RECIPE_TYPE,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.CRYSTAL_ASSEMBLY_LINE_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CrystalAssemblyLineJeiCategory.RECIPE_TYPE,
                ModBlocks.CRYSTAL_ASSEMBLY_LINE.get());
    }
}
