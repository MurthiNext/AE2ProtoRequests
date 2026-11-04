package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.registry.ModMachines;

// 复用 AE2 原生压印与充能分类，仅把主机注册为催化剂并提供加工状态入口
@JeiPlugin
public class MultiFactoryPlugin implements IModPlugin {

    private static final RecipeType<InscriberRecipe> INSCRIBER = RecipeType.create("ae2", "inscriber",
            InscriberRecipe.class);
    private static final RecipeType<ChargerRecipe> CHARGER = RecipeType.create("ae2", "charger", ChargerRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("multi_factory");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(INSCRIBER, ModMachines.MULTI_FACTORY.item());
        registration.addRecipeCatalysts(CHARGER, ModMachines.MULTI_FACTORY.item());
    }

    public static RecipeType<?> recipeType(MultiFactoryMode mode) {
        return mode == MultiFactoryMode.CHARGING ? CHARGER : INSCRIBER;
    }
}
