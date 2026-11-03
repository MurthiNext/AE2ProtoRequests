package com.murthinext.ae2pr.integration.jei;

import com.murthinext.ae2pr.api.Ids;

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

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;

public class CrystalWorkstationJeiCategory implements IRecipeCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> ETCHING = RecipeType.create(ae2pr.MODID,
            "crystal_workstation_etching", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> CUTTING = RecipeType.create(ae2pr.MODID,
            "crystal_workstation_cutting", MachineRecipe.class);

    private static final int MAX_ITEM_INPUTS = 6;
    private static final int MAX_ITEM_OUTPUTS = 6;
    private static final int MAX_FLUIDS = 3;

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
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModMachines.CRYSTAL_WORKSTATION.item()));
        this.fluidSlot = FluidSlotBackground.create(guiHelper);
        this.process = new AnimatedTextureDrawable(processTexture(mode), PROCESS_W, PROCESS_H,
                PROCESS_FRAMES, PROCESS_FRAME_MS);
    }

    private static ResourceLocation processTexture(CrystalWorkstationMode mode) {
        return Ids.of(
                "textures/gui/jei/crystal_workstation_" + mode.id() + "_process.png");
    }

    public static RecipeType<MachineRecipe> recipeType(CrystalWorkstationMode mode) {
        return mode == CrystalWorkstationMode.ETCHING ? ETCHING : CUTTING;
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return recipeType(mode);
    }

    @Override
    public Component getTitle() {
        return JeiRecipeInfo.titleWithMode(ModMachines.CRYSTAL_WORKSTATION.item().getDescription(), mode.displayName());
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
    public ResourceLocation getRegistryName(MachineRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < MAX_ITEM_INPUTS; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 4 + i % 2 * 18, 4 + i / 2 * 18)
                    .setStandardSlotBackground();
            if (i < recipe.getItemInputs().size()) {
                var input = recipe.getItemInputs().get(i);
                slot.addItemStacks(Arrays.stream(input.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(input.count())).toList());
                slot.setCustomRenderer(VanillaTypes.ITEM_STACK, ItemStackAmountRenderer.INSTANCE);
            }
        }
        for (int i = 0; i < MAX_ITEM_OUTPUTS; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 112 + i % 2 * 18, 4 + i / 2 * 18)
                    .setStandardSlotBackground();
            slot.setCustomRenderer(VanillaTypes.ITEM_STACK,
                    ChancedItemStackRenderer.of(recipe.getChancedItemOutputs(), i));
            if (i < recipe.getChancedItemOutputs().size()) {
                slot.addItemStack(recipe.getChancedItemOutputs().get(i).stack());
            }
        }
        for (int i = 0; i < MAX_FLUIDS; i++) {
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
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        process.draw(graphics, PROCESS_X, PROCESS_Y);
        graphics.drawString(font, JeiRecipeInfo.duration(recipe.getDuration()), 4, 62, 0x404040, false);
        graphics.drawString(font, JeiRecipeInfo.energy(recipe.getEnergyPerParallel()), 4, 73, 0x404040, false);
    }
}
