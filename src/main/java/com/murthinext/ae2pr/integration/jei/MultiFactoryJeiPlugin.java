package com.murthinext.ae2pr.integration.jei;

import com.murthinext.ae2pr.api.Ids;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;

// 复用 AE2 原生压印与充能分类，仅把主机注册为催化剂并提供加工状态入口
@JeiPlugin
public class MultiFactoryJeiPlugin implements IModPlugin {

    // 与 AE2 JEI 分类使用的 RecipeType 完全一致
    private static final RecipeType<InscriberRecipe> INSCRIBER = RecipeType.create("ae2", "inscriber",
            InscriberRecipe.class);
    private static final RecipeType<ChargerRecipe> CHARGER = RecipeType.create("ae2", "charger", ChargerRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of( "multi_factory");
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
