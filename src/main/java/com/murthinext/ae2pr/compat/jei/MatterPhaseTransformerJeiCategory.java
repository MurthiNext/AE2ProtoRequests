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
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MatterPhaseTransformerRecipe;

/**
 * 物质相变仪配方的 JEI 分类。
 */
public class MatterPhaseTransformerJeiCategory implements IRecipeCategory<MatterPhaseTransformerRecipe> {

    public static final RecipeType<MatterPhaseTransformerRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "matter_phase_transformer",
            MatterPhaseTransformerRecipe.class);

    private static final int SLOT = 18;
    private static final int PADDING = 4;
    private static final int INPUT_COLS = 2;
    private static final int INPUT_ROWS = 3;
    private static final int INPUT_COUNT = INPUT_COLS * INPUT_ROWS;
    private static final int INPUT_X = PADDING;
    private static final int INPUT_Y = PADDING + 6;
    private static final int CHAMBER_X = INPUT_X + INPUT_COLS * SLOT + 4;
    private static final int CHAMBER_Y = INPUT_Y;
    private static final int CHAMBER_W = 24;
    private static final int CHAMBER_H = INPUT_ROWS * SLOT;
    private static final int OUTPUT_X = CHAMBER_X + CHAMBER_W + 6;
    private static final int OUTPUT_Y = INPUT_Y + (INPUT_ROWS * SLOT - SLOT) / 2;
    private static final int INFO_Y = INPUT_Y + INPUT_ROWS * SLOT + 4;
    private static final int INFO_STEP = 10;
    private static final int WIDTH = OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * INFO_STEP + 2;

    private static final int COLOR_PANEL = 0xFF1B222B;
    private static final int COLOR_PANEL_LINE = 0xFF39424E;
    private static final int COLOR_CORE = 0xFF62D0DA;
    private static final int COLOR_CORE_GLOW = 0xFFA6F2F6;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_HINT = 0x808080;
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    private final IDrawable icon;
    private final IDrawable fluidSlot;

    public MatterPhaseTransformerJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.MATTER_PHASE_TRANSFORMER.get()));
        this.fluidSlot = FluidSlotBackground.create(guiHelper);
    }

    @Override
    public RecipeType<MatterPhaseTransformerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.matter_phase_transformer");
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
    public ResourceLocation getRegistryName(MatterPhaseTransformerRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MatterPhaseTransformerRecipe recipe, IFocusGroup focuses) {
        // 物品输入：固定槽位，消耗数量写入 ItemStack
        List<CountedIngredient> ingredients = recipe.getCountedIngredients();
        for (int index = 0; index < INPUT_COUNT; index++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    INPUT_X + (index % INPUT_COLS) * SLOT, INPUT_Y + (index / INPUT_COLS) * SLOT);
            slot.setStandardSlotBackground();
            if (index >= ingredients.size()) {
                continue;
            }
            CountedIngredient ingredient = ingredients.get(index);
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : ingredient.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(ingredient.count()));
            }
            slot.addItemStacks(stacks);
        }

        // 产物：相变室右侧的流体槽
        FluidStack output = recipe.getFluidOutput();
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y);
        outputSlot.setBackground(fluidSlot, -1, -1);
        outputSlot.addFluidStack(output.getFluid(), output.getAmount())
                .setFluidRenderer(output.getAmount(), true, 16, 16);
    }

    @Override
    public void draw(MatterPhaseTransformerRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;
        drawChamber(graphics);
        graphics.drawString(font, Component.translatable("jei.ae2pr.matter_phase_transformer.duration",
                recipe.getDuration()), PADDING, INFO_Y, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.matter_phase_transformer.energy",
                NUMBER.format(recipe.getEnergyPerParallel())), PADDING, INFO_Y + INFO_STEP, COLOR_TEXT, false);
        int hidden = recipe.getCountedIngredients().size() - INPUT_COUNT;
        if (hidden > 0) {
            graphics.drawString(font, Component.translatable("jei.ae2pr.matter_phase_transformer.more_inputs", hidden),
                    PADDING, INFO_Y + 2 * INFO_STEP, COLOR_HINT, false);
        }
    }

    private static void drawChamber(GuiGraphics graphics) {
        graphics.fill(CHAMBER_X, CHAMBER_Y, CHAMBER_X + CHAMBER_W, CHAMBER_Y + CHAMBER_H, COLOR_PANEL_LINE);
        graphics.fill(CHAMBER_X + 1, CHAMBER_Y + 1, CHAMBER_X + CHAMBER_W - 1, CHAMBER_Y + CHAMBER_H - 1,
                COLOR_PANEL);
        int centerX = CHAMBER_X + CHAMBER_W / 2;
        int centerY = CHAMBER_Y + CHAMBER_H / 2;
        // 核心：中心亮青、外圈青
        graphics.fill(centerX - 4, centerY - 4, centerX + 4, centerY + 4, COLOR_CORE);
        graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, COLOR_CORE_GLOW);
    }
}
