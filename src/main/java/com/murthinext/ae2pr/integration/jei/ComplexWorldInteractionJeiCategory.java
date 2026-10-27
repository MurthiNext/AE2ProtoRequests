package com.murthinext.ae2pr.integration.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidType;

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

import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.logic.world_interaction.WorldInteractionCatalyst;
import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;
import com.murthinext.ae2pr.recipe.CountedIngredient;

public class ComplexWorldInteractionJeiCategory implements IRecipeCategory<ComplexWorldInteractionRecipe> {

    public static final RecipeType<ComplexWorldInteractionRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
            "complex_world_interaction", ComplexWorldInteractionRecipe.class);

    private static final int WIDTH = 130;
    private static final int HEIGHT = 62;
    private static final int SLOT = 18;
    private static final int ROWS = 3;
    private static final int TOP = 6;
    private static final int CATALYST_X = 56;
    private static final int OUTPUT_X = 106;
    private static final int INTERACTION_Y = 24;
    private static final int COLOR_TEXT = 0x404040;
    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
            "textures/gui/jei/recipe_arrow.png");

    private final IDrawable icon;

    public ComplexWorldInteractionJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(AEItems.FLUIX_CRYSTAL.stack());
    }

    @Override
    public RecipeType<ComplexWorldInteractionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ae2pr.complex_world_interaction");
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
    public ResourceLocation getRegistryName(ComplexWorldInteractionRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ComplexWorldInteractionRecipe recipe, IFocusGroup focuses) {
        int index = 0;
        int inputTop = TOP + (ROWS - Math.min(ROWS, recipe.getItemInputs().size())) * SLOT / 2;
        for (CountedIngredient ingredient : recipe.getItemInputs()) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    6 + index / ROWS * SLOT, inputTop + index % ROWS * SLOT);
            slot.setStandardSlotBackground();
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : ingredient.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(ingredient.count()));
            }
            slot.addItemStacks(stacks);
            slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            index++;
        }

        IRecipeSlotBuilder catalystSlot = builder.addSlot(RecipeIngredientRole.CATALYST, CATALYST_X, INTERACTION_Y);
        if (recipe.getCircumstance().isExplosion()) {
            catalystSlot.addItemStack(new ItemStack(Items.TNT));
        } else {
            recipe.getCircumstance().getFluids()
                    .forEach(fluid -> catalystSlot.addFluidStack(fluid, FluidType.BUCKET_VOLUME));
            catalystSlot.setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidBlockIngredientRenderer());
            catalystSlot.addRichTooltipCallback((view, tooltip) -> {
                if (recipe.getCircumstance().sourceOnly()) {
                    tooltip.add(Component.translatable("jei.ae2pr.complex_world_interaction.source_only"));
                }
                view.getDisplayedIngredient(ForgeTypes.FLUID_STACK).ifPresent(stack -> {
                    var state = stack.getFluid().defaultFluidState().createLegacyBlock();
                    if (state.getBlock() instanceof WorldInteractionCatalyst catalyst) {
                        tooltip.add(Component.translatable("jei.ae2pr.complex_world_interaction.uses",
                                catalyst.getRemainingInteractions(state)));
                    }
                });
            });
        }

        index = 0;
        int outputCount = recipe.getItemOutputs().size() + (recipe.convertsFluid() ? 1 : 0);
        int outputTop = TOP + (ROWS - Math.min(ROWS, outputCount)) * SLOT / 2;
        for (ItemStack result : recipe.getItemOutputs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X - index / ROWS * SLOT,
                    outputTop + index % ROWS * SLOT)
                    .setStandardSlotBackground()
                    .addItemStack(result)
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            index++;
        }
        if (recipe.getResultFluid() != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X - index / ROWS * SLOT,
                    outputTop + index % ROWS * SLOT)
                    .addFluidStack(recipe.getResultFluid(), FluidType.BUCKET_VOLUME)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidBlockIngredientRenderer());
        }
    }

    @Override
    public void draw(ComplexWorldInteractionRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.blit(ARROW, 25, INTERACTION_Y - 1, 0, 0, 22, 16);
        graphics.blit(ARROW, 76, INTERACTION_Y - 1, 0, 0, 22, 16);
        Component condition = Component.translatable(recipe.getCircumstance().isExplosion()
                ? "jei.ae2pr.complex_world_interaction.explosion"
                : "jei.ae2pr.complex_world_interaction.submerge");
        graphics.drawString(font, condition, (WIDTH - font.width(condition)) / 2, 10, COLOR_TEXT, false);
    }
}
