package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.integration.jei.render.AnimatedTextureDrawable;
import com.murthinext.ae2pr.registry.ModMachines;

public class CrystalReactionChamberCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
                                                                                  "crystal_reaction_chamber",
                                                                                  MachineRecipe.class);

    private static final int SLOT = SlotArea.SIZE;
    private static final int PADDING = 4;
    private static final int ITEM_COLS = 2;
    private static final int ITEM_ROWS = 3;
    private static final int FLUID_SLOT_COUNT = 3;
    private static final int CONTENT_Y = PADDING + 6;
    private static final int FLUIDS_X = PADDING + ITEM_COLS * SLOT + 4;
    private static final int PROCESS_X = FLUIDS_X + SLOT + 4;
    private static final int PROCESS_W = 44;
    private static final int PROCESS_H = ITEM_ROWS * SLOT;
    private static final int OUTPUT_X = PROCESS_X + PROCESS_W + 4;
    private static final int OUTPUT_Y = CONTENT_Y + SLOT;
    private static final int OUTPUT_FLUIDS_X = OUTPUT_X + SLOT + 4;
    private static final int INFO_Y = CONTENT_Y + PROCESS_H + 4;
    private static final int WIDTH = OUTPUT_FLUIDS_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * 10 + 2;

    private final IDrawable process;

    public CrystalReactionChamberCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack(ModMachines.CRYSTAL_REACTION_CHAMBER.block()), WIDTH, HEIGHT);
        process = new AnimatedTextureDrawable(Ids.of("textures/gui/jei/crystal_reaction_chamber_process.png"),
                                              PROCESS_W, PROCESS_H, 40, 50);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.crystal_reaction_chamber");
    }

    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(ITEM_COLS * ITEM_ROWS, ITEM_COLS, PADDING, CONTENT_Y);
    }

    @Override
    protected SlotArea fluidInputs() {
        return SlotArea.column(FLUID_SLOT_COUNT, FLUIDS_X, CONTENT_Y);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(1, 1, OUTPUT_X, OUTPUT_Y);
    }

    @Override
    protected SlotArea fluidOutputs() {
        return SlotArea.column(FLUID_SLOT_COUNT, OUTPUT_FLUIDS_X, CONTENT_Y);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, PROCESS_X, CONTENT_Y);
    }
}
