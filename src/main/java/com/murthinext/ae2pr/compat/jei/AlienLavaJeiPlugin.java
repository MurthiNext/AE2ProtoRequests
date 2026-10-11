package com.murthinext.ae2pr.compat.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.murthinext.ae2pr.ModFluids;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;

/**
 * JEI 插件：注册异星熔岩世界交互配方分类、催化剂与流体条目。
 */
@JeiPlugin
public class AlienLavaJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "alien_lava");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new AlienLavaJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(AlienLavaJeiCategory.RECIPE_TYPE,
                List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.ALIEN_LAVA_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(AlienLavaJeiCategory.RECIPE_TYPE, ForgeTypes.FLUID_STACK,
                List.of(new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME),
                        new FluidStack(ModFluids.ALIEN_LAVA.get(), FluidType.BUCKET_VOLUME)));
        registration.addRecipeCatalyst(ModItems.ALIEN_LAVA_BUCKET.get(), AlienLavaJeiCategory.RECIPE_TYPE);
    }
}
