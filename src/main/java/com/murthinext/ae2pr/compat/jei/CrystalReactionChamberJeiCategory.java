package com.murthinext.ae2pr.compat.jei;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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
import com.murthinext.ae2pr.recipe.CrystalReactionChamberRecipe;

/**
 * 水晶反应仓配方的 JEI 分类。
 */
public class CrystalReactionChamberJeiCategory implements IRecipeCategory<CrystalReactionChamberRecipe> {

    public static final RecipeType<CrystalReactionChamberRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
            "crystal_reaction_chamber", CrystalReactionChamberRecipe.class);

    private static final int SLOT = 18;
    private static final int PADDING = 4;
    private static final int ITEM_COLS = 2;
    private static final int ITEM_ROWS = 3;
    private static final int ITEM_SLOT_COUNT = ITEM_COLS * ITEM_ROWS;
    private static final int FLUID_SLOT_COUNT = 3;

    private static final int ITEMS_X = PADDING;
    private static final int CONTENT_Y = PADDING + 6;
    private static final int FLUIDS_X = ITEMS_X + ITEM_COLS * SLOT + 4;
    private static final int CHAMBER_X = FLUIDS_X + SLOT + 4;
    private static final int CHAMBER_W = 24;
    private static final int CHAMBER_H = ITEM_ROWS * SLOT;
    private static final int OUTPUT_X = CHAMBER_X + CHAMBER_W + 6;
    private static final int OUTPUT_FLUIDS_X = OUTPUT_X + SLOT + 4;

    private static final int INFO_Y = CONTENT_Y + CHAMBER_H + 4;
    private static final int INFO_STEP = 10;
    private static final int WIDTH = OUTPUT_FLUIDS_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * INFO_STEP + 2;

    private static final int COLOR_PANEL = 0xFF1B222B;
    private static final int COLOR_PANEL_LINE = 0xFF39424E;
    private static final int COLOR_CORE = 0xFF55A3A8;
    private static final int COLOR_CORE_GLOW = 0xFF96E0DD;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_HINT = 0x808080;
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    private final IDrawable icon;
    private final IDrawable fluidSlot;

    public CrystalReactionChamberJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CRYSTAL_REACTION_CHAMBER.get()));
        this.fluidSlot = FluidSlotBackground.create(guiHelper);
    }

    @Override
    public RecipeType<CrystalReactionChamberRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.crystal_reaction_chamber");
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
    public ResourceLocation getRegistryName(CrystalReactionChamberRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalReactionChamberRecipe recipe, IFocusGroup focuses) {
        // 物品输入：固定槽位，消耗数量写入 ItemStack
        List<CrystalReactionChamberRecipe.ItemInput> itemInputs = recipe.getItemInputs();
        for (int index = 0; index < ITEM_SLOT_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    ITEMS_X + (index % ITEM_COLS) * SLOT, CONTENT_Y + (index / ITEM_COLS) * SLOT);
            slot.setStandardSlotBackground();
            if (index >= itemInputs.size()) {
                continue;
            }
            CrystalReactionChamberRecipe.ItemInput input = itemInputs.get(index);
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : input.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(input.count()));
            }
            slot.addItemStacks(stacks);
        }

        // 流体输入：单列，深色槽位背景
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int index = 0; index < FLUID_SLOT_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, FLUIDS_X, CONTENT_Y + index * SLOT);
            slot.setBackground(fluidSlot, -1, -1);
            if (index >= fluidInputs.size()) {
                continue;
            }
            FluidStack fluid = fluidInputs.get(index);
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount())
                    .setFluidRenderer(fluid.getAmount(), true, 16, 16);
        }

        // 物品产物：相变室右侧
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, CONTENT_Y);
        outputSlot.setStandardSlotBackground();
        if (!itemOutputs.isEmpty()) {
            outputSlot.addItemStack(itemOutputs.get(0));
        }

        // 流体产物：物品产物下方一列
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int index = 0; index < FLUID_SLOT_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_FLUIDS_X,
                    CONTENT_Y + index * SLOT);
            slot.setBackground(fluidSlot, -1, -1);
            if (index >= fluidOutputs.size()) {
                continue;
            }
            FluidStack fluid = fluidOutputs.get(index);
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount())
                    .setFluidRenderer(fluid.getAmount(), true, 16, 16);
        }
    }

    @Override
    public void draw(CrystalReactionChamberRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        drawChamber(graphics);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_reaction_chamber.duration",
                recipe.getDuration()), PADDING, INFO_Y, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_reaction_chamber.energy",
                NUMBER.format(recipe.getEnergyPerParallel())), PADDING, INFO_Y + INFO_STEP, COLOR_TEXT, false);
        int hiddenInputs = recipe.getItemInputs().size() - ITEM_SLOT_COUNT;
        int hiddenOutputs = recipe.getItemOutputs().size() - 1;
        if (hiddenInputs > 0 || hiddenOutputs > 0) {
            graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_reaction_chamber.more_io",
                    Math.max(0, hiddenInputs), Math.max(0, hiddenOutputs)), PADDING, INFO_Y + 2 * INFO_STEP,
                    COLOR_HINT, false);
        }
    }

    private static void drawChamber(GuiGraphics graphics) {
        graphics.fill(CHAMBER_X, CONTENT_Y, CHAMBER_X + CHAMBER_W, CONTENT_Y + CHAMBER_H, COLOR_PANEL_LINE);
        graphics.fill(CHAMBER_X + 1, CONTENT_Y + 1, CHAMBER_X + CHAMBER_W - 1, CONTENT_Y + CHAMBER_H - 1,
                COLOR_PANEL);
        int centerX = CHAMBER_X + CHAMBER_W / 2;
        int centerY = CONTENT_Y + CHAMBER_H / 2;
        // 反应仓：中心亮青、外圈青
        graphics.fill(centerX - 5, centerY - 5, centerX + 5, centerY + 5, COLOR_CORE);
        graphics.fill(centerX - 3, centerY - 3, centerX + 3, centerY + 3, COLOR_CORE_GLOW);
    }
}
