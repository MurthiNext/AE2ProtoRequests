package com.murthinext.ae2pr.integration.jei;

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
import com.murthinext.ae2pr.recipe.ChancedItemStack;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.HighReactivityLavaSmelterRecipe;

public class HighReactivityLavaSmelterJeiCategory implements IRecipeCategory<HighReactivityLavaSmelterRecipe> {

    public static final RecipeType<HighReactivityLavaSmelterRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "high_reactivity_lava_smelter",
            HighReactivityLavaSmelterRecipe.class);

    private static final int WIDTH = 142;
    private static final int HEIGHT = 98;
    private static final int OUTPUT_X = 117;
    private static final int OUTPUT_Y = 12;
    private static final int OUTPUT_GAP = 18;
    private static final int COLOR_TEXT = 0x404040;

    private final IDrawable icon;

    public HighReactivityLavaSmelterJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()));
    }

    @Override
    public RecipeType<HighReactivityLavaSmelterRecipe> getRecipeType() {
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
    public ResourceLocation getRegistryName(HighReactivityLavaSmelterRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HighReactivityLavaSmelterRecipe recipe, IFocusGroup focuses) {
        // 物品输入
        List<CountedIngredient> ingredients = recipe.getItemInputs();
        for (int index = 0; index < HighReactivityLavaSmelterJeiFurnace.ITEM_COUNT; index++) {
            int x = HighReactivityLavaSmelterJeiFurnace.itemSlotX(index);
            int y = HighReactivityLavaSmelterJeiFurnace.itemSlotY(index);
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
        for (int index = 0; index < HighReactivityLavaSmelterJeiFurnace.FLUID_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    HighReactivityLavaSmelterJeiFurnace.fluidSlotX(index), HighReactivityLavaSmelterJeiFurnace.fluidSlotY(index));
            if (index >= fluidInputs.size()) {
                continue;
            }
            FluidStack fluid = fluidInputs.get(index);
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount());
            slot.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
        }

        // 主/副产物槽固定显示并对齐输入槽
        List<ChancedItemStack> results = recipe.getChancedItemOutputs();
        for (int index = 0; index < HighReactivityLavaSmelterRecipe.MAX_RESULTS; index++) {
            IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT,
                    OUTPUT_X, OUTPUT_Y + index * OUTPUT_GAP);
            outputSlot.setStandardSlotBackground();
            outputSlot.setCustomRenderer(VanillaTypes.ITEM_STACK, ChancedItemStackRenderer.of(results, index));
            if (index < results.size()) {
                outputSlot.addItemStack(results.get(index).stack());
            }
        }
    }

    @Override
    public void draw(HighReactivityLavaSmelterRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;
        HighReactivityLavaSmelterJeiFurnace.draw(graphics);
        graphics.drawString(font, Component.translatable(
                "jei.ae2pr.high_reactivity_lava_smelter.durability_cost", 1), 4, 67, COLOR_TEXT, false);
        graphics.drawString(font, JeiRecipeInfo.duration(recipe.getDuration()), 4, 77, COLOR_TEXT, false);
    }
}
