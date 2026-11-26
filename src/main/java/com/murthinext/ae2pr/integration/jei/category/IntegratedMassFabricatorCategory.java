package com.murthinext.ae2pr.integration.jei.category;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.registry.ModMachines;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IntegratedMassFabricatorCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE =
            RecipeType.create(ae2pr.MODID, "integrated_mass_fabricator", MachineRecipe.class);

    private static final int WIDTH = 142;
    private static final int HEIGHT = 76;
    private static final int SINGLE_ROW_Y = 17;
    private static final SlotArea ITEM_INPUTS = new SlotArea(4, 2, 6, SINGLE_ROW_Y - SlotArea.SIZE / 2);
    private static final SlotArea FLUID_INPUTS = SlotArea.column(1, 46, SINGLE_ROW_Y);
    private static final SlotArea ITEM_OUTPUTS = new SlotArea(2, 2, 99, SINGLE_ROW_Y);

    private final IDrawable arrow;

    public IntegratedMassFabricatorCategory(IGuiHelper guiHelper) {
        super(guiHelper, ModMachines.INTEGRATED_MASS_FABRICATOR.item().getDefaultInstance(), WIDTH, HEIGHT);
        arrow = guiHelper.createAnimatedRecipeArrow(50);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return ModMachines.INTEGRATED_MASS_FABRICATOR.block().getName();
    }

    @Override
    protected SlotArea itemInputs() {
        return ITEM_INPUTS;
    }

    @Override
    protected SlotArea fluidInputs() {
        return FLUID_INPUTS;
    }

    @Override
    protected SlotArea itemOutputs() {
        return ITEM_OUTPUTS;
    }

    @Override
    protected int infoY() {
        return 51;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        arrow.draw(graphics, 68, SINGLE_ROW_Y);
    }
}
