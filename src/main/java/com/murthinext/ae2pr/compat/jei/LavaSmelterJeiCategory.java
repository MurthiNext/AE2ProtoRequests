package com.murthinext.ae2pr.compat.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;

/**
 * 熔岩冶炼炉配方的 JEI 分类。
 */
public class LavaSmelterJeiCategory implements IRecipeCategory<LavaSmelterRecipe> {

    public static final RecipeType<LavaSmelterRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "lava_smelter",
            LavaSmelterRecipe.class);

    private static final int WIDTH = 142;
    private static final int HEIGHT = 98;
    private static final int OUTPUT_X = 117;
    private static final int OUTPUT_Y = 12;
    private static final int OUTPUT_GAP = 18;
    private static final int COLOR_TEXT = 0x404040;

    private final IDrawable icon;

    public LavaSmelterJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()));
    }

    @Override
    public RecipeType<LavaSmelterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.high_reactivity_lava_smelter");
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
    public ResourceLocation getRegistryName(LavaSmelterRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LavaSmelterRecipe recipe, IFocusGroup focuses) {
        // 物品输入
        List<CountedIngredient> ingredients = recipe.getItemInputs();
        for (int index = 0; index < LavaSmelterJeiFurnace.ITEM_COUNT; index++) {
            int x = LavaSmelterJeiFurnace.itemSlotX(index);
            int y = LavaSmelterJeiFurnace.itemSlotY(index);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
            if (index < ingredients.size()) {
                CountedIngredient ingredient = ingredients.get(index);
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : ingredient.ingredient().getItems()) {
                    stacks.add(stack.copyWithCount(ingredient.count()));
                }
                slot.addItemStacks(stacks);
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            }
        }

        // 流体输入
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int index = 0; index < LavaSmelterJeiFurnace.FLUID_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    LavaSmelterJeiFurnace.fluidSlotX(index), LavaSmelterJeiFurnace.fluidSlotY(index));
            if (index >= fluidInputs.size()) {
                continue;
            }
            FluidStack fluid = fluidInputs.get(index);
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount());
            slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
        }

        // 主/副产物槽固定显示并对齐输入槽
        List<ItemStack> results = recipe.getItemOutputs();
        for (int index = 0; index < LavaSmelterRecipe.MAX_RESULTS; index++) {
            IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT,
                    OUTPUT_X, OUTPUT_Y + index * OUTPUT_GAP);
            outputSlot.setStandardSlotBackground();
            if (index < results.size()) {
                outputSlot.addItemStack(results.get(index));
                outputSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            }
        }
    }

    @Override
    public void draw(LavaSmelterRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;
        LavaSmelterJeiFurnace.draw(graphics);
        graphics.drawString(font, Component.translatable("jei.ae2pr.lava_smelter.durability",
                1), 4, 67, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.lava_smelter.duration",
                recipe.getDuration()), 4, 77, COLOR_TEXT, false);
    }
}
