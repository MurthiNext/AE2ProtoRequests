package com.murthinext.ae2pr.compat.jei;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
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

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;

/**
 * 水晶装配线配方的 JEI 分类。
 */
public class CrystalAssemblyLineJeiCategory implements IRecipeCategory<CrystalAssemblyLineRecipe> {

    public static final RecipeType<CrystalAssemblyLineRecipe> RECIPE_TYPE = RecipeType.create(
            ae2pr.MODID, "crystal_assembly_line", CrystalAssemblyLineRecipe.class);

    private static final int SLOT = 18;
    private static final int PADDING = 4;
    private static final int ITEM_COLS = 4;
    private static final int ITEM_ROWS = 4;
    private static final int ITEM_SLOT_COUNT = ITEM_COLS * ITEM_ROWS;
    private static final int MAX_ITEM_INPUTS = ITEM_SLOT_COUNT;
    private static final int FLUID_SLOT_COUNT = 4;
    private static final int MAX_FLUID_INPUTS = FLUID_SLOT_COUNT;

    // 对齐偏移
    private static final int ITEMS_X = PADDING;
    private static final int ITEMS_Y = PADDING;
    private static final int FLUID_X = PADDING + 89;
    private static final int OUTPUT_X = PADDING + 126;
    private static final int OUTPUT_Y = PADDING;
    private static final int PIPE_X = PADDING + 71;
    private static final int PIPE_Y = PADDING;
    // 管道贴图
    private static final int PIPE_WIDTH = 54;
    private static final int PIPE_HEIGHT = 72;
    private static final int PIPE_TEXTURE_HEIGHT = 144;
    // 管道动画时长
    private static final long PIPE_CYCLE_MS = 2000L;

    private static final int CONTENT_BOTTOM = PADDING + ITEM_ROWS * SLOT;
    private static final int INFO_Y = CONTENT_BOTTOM + 8;
    private static final int WIDTH = OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * 10 + 2;
    private static final int COLOR_HINT = 0x808080;
    private static final int COLOR_TEXT = 0x404040;
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    private static final ResourceLocation PIPE_TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/jei/assembly_line_pipes.png");
    private static final ResourceLocation FLUID_SLOT_TEXTURE = new ResourceLocation(ae2pr.MODID,
            "textures/gui/jei/fluid_slot.png");

    private final IDrawable icon;
    private final IDrawable fluidSlotBackground;

    public CrystalAssemblyLineJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()));
        this.fluidSlotBackground = guiHelper.drawableBuilder(FLUID_SLOT_TEXTURE, 0, 0, SLOT, SLOT)
                .setTextureSize(SLOT, SLOT)
                .build();
    }

    @Override
    public RecipeType<CrystalAssemblyLineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.crystal_assembly_line");
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
    public ResourceLocation getRegistryName(CrystalAssemblyLineRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalAssemblyLineRecipe recipe, IFocusGroup focuses) {
        // 物品输入：4x4 网格
        List<CrystalAssemblyLineRecipe.ItemInput> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < ITEM_SLOT_COUNT; i++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    ITEMS_X + (i % ITEM_COLS) * SLOT, ITEMS_Y + (i / ITEM_COLS) * SLOT);
            slot.setStandardSlotBackground();
            if (i >= itemInputs.size() || i >= MAX_ITEM_INPUTS) {
                continue;
            }
            CrystalAssemblyLineRecipe.ItemInput input = itemInputs.get(i);
            final int inputIndex = i;
            // 消耗数量直接写入 ItemStack
            List<ItemStack> stacks = new ArrayList<>(input.ingredient().getItems().length);
            for (ItemStack stack : input.ingredient().getItems()) {
                stacks.add(stack.copyWithCount(input.count()));
            }
            slot.addItemStacks(stacks);
            slot.addRichTooltipCallback(
                    (view, tooltip) -> tooltip.add(orderTooltip(inputIndex, Config.assemblyItemsOrdered())));
        }

        // 流体输入：单列，使用 GT 风格深色槽位背景
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < FLUID_SLOT_COUNT; i++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT,
                    FLUID_X, ITEMS_Y + i * SLOT);
            slot.setBackground(fluidSlotBackground, -1, -1);
            if (i >= fluidInputs.size() || i >= MAX_FLUID_INPUTS) {
                continue;
            }
            FluidStack fluid = fluidInputs.get(i);
            final int fluidIndex = i;
            slot.addFluidStack(fluid.getFluid(), fluid.getAmount())
                    .setFluidRenderer(fluid.getAmount(), true, 16, 16);
            slot.addRichTooltipCallback(
                    (view, tooltip) -> tooltip.add(orderTooltip(fluidIndex, Config.assemblyFluidsOrdered())));
        }

        // 产物：右上角
        List<ItemStack> outputs = recipe.getItemOutputs();
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y);
        outputSlot.setStandardSlotBackground();
        if (!outputs.isEmpty()) {
            outputSlot.addItemStack(outputs.get(0));
        }
    }

    @Override
    public void draw(CrystalAssemblyLineRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        drawPipeProgress(graphics);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_assembly_line.duration",
                recipe.getDuration()), PADDING, INFO_Y, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_assembly_line.energy",
                NUMBER.format(Config.assemblyEnergyPerParallel())), PADDING, INFO_Y + 10, COLOR_TEXT, false);
        int hidden = recipe.getItemInputs().size() - MAX_ITEM_INPUTS;
        if (hidden > 0) {
            graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_assembly_line.more_inputs",
                    hidden), PADDING, INFO_Y + 20, COLOR_HINT, false);
        } else if (recipe.getItemOutputs().size() > 1) {
            graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_assembly_line.more_outputs",
                    recipe.getItemOutputs().size() - 1), PADDING, INFO_Y + 20, COLOR_HINT, false);
        }
    }

    /** 管道进度 */
    private static void drawPipeProgress(GuiGraphics graphics) {
        float progress = (Util.getMillis() % PIPE_CYCLE_MS) / (float) PIPE_CYCLE_MS;
        graphics.blit(PIPE_TEXTURE, PIPE_X, PIPE_Y, 0.0F, 0.0F, PIPE_WIDTH, PIPE_HEIGHT,
                PIPE_WIDTH, PIPE_TEXTURE_HEIGHT);
        int filled = Math.round(PIPE_WIDTH * progress);
        if (filled > 0) {
            graphics.blit(PIPE_TEXTURE, PIPE_X, PIPE_Y, 0.0F, PIPE_HEIGHT, filled, PIPE_HEIGHT,
                    PIPE_WIDTH, PIPE_TEXTURE_HEIGHT);
        }
    }

    /** 有序时标注该输入的序号。 */
    private static Component orderTooltip(int index, boolean ordered) {
        if (!ordered) {
            return Component.translatable("jei.ae2pr.crystal_assembly_line.slot.unordered")
                    .withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("jei.ae2pr.crystal_assembly_line.slot.index", index + 1)
                .withStyle(ChatFormatting.GRAY);
    }
}
