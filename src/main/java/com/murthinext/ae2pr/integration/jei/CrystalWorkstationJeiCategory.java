package com.murthinext.ae2pr.integration.jei;

import java.text.NumberFormat;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.recipe.CrystalWorkstationRecipe;

public class CrystalWorkstationJeiCategory implements IRecipeCategory<CrystalWorkstationRecipe> {

    public static final RecipeType<CrystalWorkstationRecipe> ETCHING = RecipeType.create(ae2pr.MODID,
            "crystal_workstation_etching", CrystalWorkstationRecipe.class);
    public static final RecipeType<CrystalWorkstationRecipe> CUTTING = RecipeType.create(ae2pr.MODID,
            "crystal_workstation_cutting", CrystalWorkstationRecipe.class);

    private static final int WIDTH = 174;
    private static final int HEIGHT = 96;

    // 中间工艺动画：正好填满流体输入列与产物列之间的空隙（62..112）
    private static final int PROCESS_X = 62;
    private static final int PROCESS_Y = 4;
    private static final int PROCESS_W = 50;
    private static final int PROCESS_H = 54;
    private static final int PROCESS_FRAMES = 24;
    private static final int PROCESS_FRAME_MS = 80;

    private final CrystalWorkstationMode mode;
    private final IDrawable icon;
    private final IDrawable fluidSlot;
    private final IDrawable process;

    public CrystalWorkstationJeiCategory(IGuiHelper guiHelper, CrystalWorkstationMode mode) {
        this.mode = mode;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.CRYSTAL_WORKSTATION.get()));
        this.fluidSlot = FluidSlotBackground.create(guiHelper);
        this.process = new AnimatedTextureDrawable(processTexture(mode), PROCESS_W, PROCESS_H,
                PROCESS_FRAMES, PROCESS_FRAME_MS);
    }

    private static ResourceLocation processTexture(CrystalWorkstationMode mode) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID,
                "textures/gui/jei/crystal_workstation_" + mode.id() + "_process.png");
    }

    public static RecipeType<CrystalWorkstationRecipe> recipeType(CrystalWorkstationMode mode) {
        return mode == CrystalWorkstationMode.ETCHING ? ETCHING : CUTTING;
    }

    @Override
    public RecipeType<CrystalWorkstationRecipe> getRecipeType() {
        return recipeType(mode);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ae2pr.crystal_workstation." + mode.id());
    }

    @Override
    public int getWidth() {
        return 174;
    }

    @Override
    public int getHeight() {
        return 96;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(CrystalWorkstationRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalWorkstationRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < CrystalWorkstationRecipe.MAX_ITEM_INPUTS; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 4 + i % 2 * 18, 4 + i / 2 * 18)
                    .setStandardSlotBackground();
            if (i < recipe.getItemInputs().size()) {
                var input = recipe.getItemInputs().get(i);
                slot.addItemStacks(Arrays.stream(input.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(input.count())).toList());
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            }
        }
        for (int i = 0; i < CrystalWorkstationRecipe.MAX_ITEM_OUTPUTS; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 112 + i % 2 * 18, 4 + i / 2 * 18)
                    .setStandardSlotBackground();
            slot.setCustomRenderer(VanillaTypes.ITEM_STACK,
                    ChancedItemStackRenderer.of(recipe.getChancedItemOutputs(), i));
            if (i < recipe.getChancedItemOutputs().size()) {
                slot.addItemStack(recipe.getChancedItemOutputs().get(i).stack());
            }
        }
        for (int i = 0; i < CrystalWorkstationRecipe.MAX_FLUIDS; i++) {
            var input = builder.addSlot(RecipeIngredientRole.INPUT, 44, 4 + i * 18)
                    .setBackground(fluidSlot, -1, -1);
            if (i < recipe.getFluidInputs().size()) {
                input.addIngredient(ForgeTypes.FLUID_STACK, recipe.getFluidInputs().get(i));
                input.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
            }
            var output = builder.addSlot(RecipeIngredientRole.OUTPUT, 152, 4 + i * 18)
                    .setBackground(fluidSlot, -1, -1);
            if (i < recipe.getFluidOutputs().size()) {
                output.addIngredient(ForgeTypes.FLUID_STACK, recipe.getFluidOutputs().get(i));
                output.setCustomRenderer(ForgeTypes.FLUID_STACK, FluidStackAmountRenderer.INSTANCE);
            }
        }
    }

    @Override
    public void draw(CrystalWorkstationRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        process.draw(graphics, PROCESS_X, PROCESS_Y);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_workstation.duration", recipe.getDuration()),
                4, 62, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.ae2pr.crystal_workstation.energy",
                NumberFormat.getIntegerInstance().format(recipe.getEnergyPerParallel())), 4, 73, 0x404040, false);
    }
}
