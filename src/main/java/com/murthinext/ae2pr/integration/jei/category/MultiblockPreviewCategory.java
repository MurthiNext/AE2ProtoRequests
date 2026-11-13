package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.integration.jei.recipe.MultiblockPreviewRecipe;
import com.murthinext.ae2pr.integration.jei.widget.MultiblockPreviewWidget;
import com.murthinext.ae2pr.registry.ModMachines;

public class MultiblockPreviewCategory implements IRecipeCategory<MultiblockPreviewRecipe> {

    public static final RecipeType<MultiblockPreviewRecipe> RECIPE_TYPE = RecipeType.create(
            ae2pr.MODID, "multiblock_preview", MultiblockPreviewRecipe.class);
    public static final int WIDTH = 160;
    public static final int HEIGHT = 220;

    private final IDrawable icon;

    public MultiblockPreviewCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModMachines.CRYSTAL_ASSEMBLY_LINE.block()));
    }

    @Override
    public RecipeType<MultiblockPreviewRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ae2pr.multiblock.title");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(MultiblockPreviewRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockPreviewRecipe recipe, IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.ingredients());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(recipe.controller()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockPreviewRecipe recipe, IFocusGroup focuses) {
        MultiblockPreviewWidget widget = new MultiblockPreviewWidget(recipe);
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);
    }
}
