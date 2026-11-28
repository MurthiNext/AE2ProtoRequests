package com.murthinext.ae2pr.integration.jei.category;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.integration.jei.render.AnimatedTextureDrawable;
import com.murthinext.ae2pr.registry.ModMachines;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IntegratedMassFabricatorCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE =
            RecipeType.create(ae2pr.MODID, "integrated_mass_fabricator", MachineRecipe.class);

    private static final int SLOT = SlotArea.SIZE;
    private static final int PADDING = 4;
    private static final int PROCESS_Y = 6;
    private static final int PROCESS_W = 58;
    private static final int PROCESS_H = 48;
    private static final int ROW_Y = PROCESS_Y + (PROCESS_H - 2 * SLOT) / 2;
    private static final int FLUID_X = PADDING + 2 * SLOT + PADDING;
    private static final int PROCESS_X = FLUID_X + SLOT + PADDING;
    private static final int OUTPUT_X = PROCESS_X + PROCESS_W + PADDING;
    private static final int INFO_Y = PROCESS_Y + PROCESS_H + PADDING;
    private static final int WIDTH = OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 30 + 2;
    private static final SlotArea ITEM_INPUTS = new SlotArea(4, 2, PADDING, ROW_Y);
    private static final SlotArea FLUID_INPUTS = SlotArea.column(1, FLUID_X, ROW_Y + SLOT / 2);
    private static final SlotArea ITEM_OUTPUTS = SlotArea.column(2, OUTPUT_X, ROW_Y);

    private final IDrawable process;

    public IntegratedMassFabricatorCategory(IGuiHelper guiHelper) {
        super(guiHelper, ModMachines.INTEGRATED_MASS_FABRICATOR.item().getDefaultInstance(), WIDTH, HEIGHT);
        process = new AnimatedTextureDrawable(
                Ids.of("textures/gui/jei/integrated_mass_fabricator_process.png"), PROCESS_W, PROCESS_H, 40, 70);
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
        return INFO_Y;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, PROCESS_X, PROCESS_Y);
    }
}
