package com.murthinext.ae2pr.integration.jei;

import java.util.Collection;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.client.GeneralMachineInfoLayout;
import com.murthinext.ae2pr.client.multi_factory.MultiFactoryScreen;

// 复用 AE2 原生压印与充能分类，仅把主机注册为催化剂并提供加工状态入口
@JeiPlugin
public class MultiFactoryJeiPlugin implements IModPlugin {

    // 与 AE2 JEI 分类使用的 RecipeType 完全一致
    private static final RecipeType<InscriberRecipe> INSCRIBER = RecipeType.create("ae2", "inscriber",
            InscriberRecipe.class);
    private static final RecipeType<ChargerRecipe> CHARGER = RecipeType.create("ae2", "charger", ChargerRecipe.class);

    private static final int JOB_AREA_WIDTH = 100;
    private static final int JOB_AREA_HEIGHT = 9;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multi_factory");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(INSCRIBER, ModItems.MULTI_FACTORY.get());
        registration.addRecipeCatalysts(CHARGER, ModItems.MULTI_FACTORY.get());
    }

    // 点击界面上的加工状态文字，按当前模式打开对应原生类别
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(MultiFactoryScreen.class, new IGuiContainerHandler<MultiFactoryScreen>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(MultiFactoryScreen screen, double mouseX,
                    double mouseY) {
                return List.of(IGuiClickableArea.createBasic(GeneralMachineInfoLayout.TEXT_X,
                        GeneralMachineInfoLayout.JOB_Y, JOB_AREA_WIDTH, JOB_AREA_HEIGHT,
                        recipeType(screen.getMenu().getMode())));
            }
        });
    }

    private static RecipeType<?> recipeType(MultiFactoryMode mode) {
        return mode == MultiFactoryMode.CHARGING ? CHARGER : INSCRIBER;
    }
}
