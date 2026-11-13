package com.murthinext.ae2pr.integration.jei.category;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidType;

import appeng.core.definitions.AEItems;

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

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.recipe.CountedIngredient;
import com.murthinext.ae2pr.integration.jei.render.FluidBlockIngredientRenderer;
import com.murthinext.ae2pr.integration.jei.render.ItemStackAmountRenderer;
import com.murthinext.ae2pr.logic.world_interaction.WorldInteractionCatalyst;
import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;

public class ComplexWorldInteractionCategory implements IRecipeCategory<ComplexWorldInteractionRecipe> {

    public static final RecipeType<ComplexWorldInteractionRecipe> RECIPE_TYPE = RecipeType.create(
            ae2pr.MODID, "complex_world_interaction", ComplexWorldInteractionRecipe.class);

    private static final int WIDTH = 130;
    private static final int HEIGHT = 62;
    private static final int SLOT = SlotArea.SIZE;
    private static final int ROWS = 3;
    private static final int TOP = 6;
    private static final int INPUT_X = 6;
    private static final int CATALYST_X = 56;
    private static final int OUTPUT_X = 106;
    private static final int INTERACTION_Y = 24;
    private static final int COLOR_TEXT = 0x404040;
    private static final ResourceLocation ARROW = Ids.of("textures/gui/jei/recipe_arrow.png");

    private final IDrawable icon;

    public ComplexWorldInteractionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(AEItems.FLUIX_CRYSTAL.stack());
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
        List<CountedIngredient> inputs = recipe.getItemInputs();
        int inputTop = columnTop(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            CountedIngredient input = inputs.get(index);
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : input.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(input.count()));
            }
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X + index / ROWS * SLOT, inputTop + index % ROWS * SLOT)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks)
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
        }

        addCatalyst(builder, recipe);

        List<ItemStack> results = recipe.getItemOutputs();
        int outputTop = columnTop(results.size() + (recipe.getResultFluid() != null ? 1 : 0));
        int index = 0;
        for (ItemStack result : results) {
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

    private static void addCatalyst(IRecipeLayoutBuilder builder, ComplexWorldInteractionRecipe recipe) {
        IRecipeSlotBuilder catalyst = builder.addSlot(RecipeIngredientRole.CATALYST, CATALYST_X, INTERACTION_Y);
        if (recipe.getCircumstance().isExplosion()) {
            catalyst.addItemStack(new ItemStack(Items.TNT));
            return;
        }
        recipe.getCircumstance().getFluids().forEach(fluid -> catalyst.addFluidStack(fluid, FluidType.BUCKET_VOLUME));
        catalyst.setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidBlockIngredientRenderer());
        catalyst.addRichTooltipCallback((view, tooltip) -> {
            if (recipe.getCircumstance().sourceOnly()) {
                tooltip.add(Component.translatable("jei.ae2pr.complex_world_interaction.source_only"));
            }
            view.getDisplayedIngredient(ForgeTypes.FLUID_STACK).ifPresent(stack -> {
                var state = stack.getFluid().defaultFluidState().createLegacyBlock();
                if (state.getBlock() instanceof WorldInteractionCatalyst block) {
                    tooltip.add(Component.translatable("jei.ae2pr.complex_world_interaction.uses",
                                                       block.getRemainingInteractions(state)));
                }
            });
        });
    }

    // 每列最多 ROWS 个槽位，数量不足时整列居中对齐
    private static int columnTop(int count) {
        return TOP + (ROWS - Math.min(ROWS, count)) * SLOT / 2;
    }

    @Override
    public void draw(ComplexWorldInteractionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
                     double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.blit(ARROW, 25, INTERACTION_Y - 1, 0, 0, 22, 16);
        graphics.blit(ARROW, 76, INTERACTION_Y - 1, 0, 0, 22, 16);
        Component condition = Component.translatable(recipe.getCircumstance().isExplosion()
                                                             ? "jei.ae2pr.complex_world_interaction.explosion"
                                                             : "jei.ae2pr.complex_world_interaction.submerge");
        graphics.drawString(font, condition, (WIDTH - font.width(condition)) / 2, 10, COLOR_TEXT, false);
    }
}
