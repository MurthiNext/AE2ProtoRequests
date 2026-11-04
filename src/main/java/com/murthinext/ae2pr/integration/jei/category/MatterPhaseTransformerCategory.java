package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.integration.jei.render.AnimatedTextureDrawable;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;

public class MatterPhaseTransformerCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
            "matter_phase_transformer", MachineRecipe.class);

    private static final int SLOT = SlotArea.SIZE;
    private static final int PADDING = 4;
    private static final int INPUT_COLS = 2;
    private static final int INPUT_ROWS = 3;
    private static final int INPUT_X = PADDING;
    private static final int INPUT_Y = PADDING + 6;
    private static final int PROCESS_X = INPUT_X + INPUT_COLS * SLOT - 1;
    private static final int PROCESS_W = 72;
    private static final int PROCESS_H = INPUT_ROWS * SLOT;
    private static final int OUTPUT_X = PROCESS_X + PROCESS_W + 4;
    private static final int OUTPUT_Y = INPUT_Y + (INPUT_ROWS * SLOT - SLOT) / 2;
    private static final int INFO_Y = INPUT_Y + INPUT_ROWS * SLOT + 4;
    private static final int WIDTH = OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * 10 + 2;

    private final IDrawable process;

    public MatterPhaseTransformerCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack(ModMachines.MATTER_PHASE_TRANSFORMER.block()), WIDTH, HEIGHT);
        process = new AnimatedTextureDrawable(Ids.of("textures/gui/jei/matter_phase_transformer_process.png"),
                PROCESS_W, PROCESS_H, 40, 50);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.matter_phase_transformer");
    }

    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(INPUT_COLS * INPUT_ROWS, INPUT_COLS, INPUT_X, INPUT_Y);
    }

    @Override
    protected SlotArea fluidOutputs() {
        return new SlotArea(1, 1, OUTPUT_X, OUTPUT_Y);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, PROCESS_X, INPUT_Y);
    }
}
