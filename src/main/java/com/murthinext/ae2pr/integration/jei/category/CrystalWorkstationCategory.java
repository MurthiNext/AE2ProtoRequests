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
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import com.murthinext.ae2pr.integration.jei.render.AnimatedTextureDrawable;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.registry.ModMachines;

public class CrystalWorkstationCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> ETCHING = RecipeType.create(ae2pr.MODID,
                                                                              "crystal_workstation_etching",
                                                                              MachineRecipe.class);
    public static final RecipeType<MachineRecipe> CUTTING = RecipeType.create(ae2pr.MODID,
                                                                              "crystal_workstation_cutting",
                                                                              MachineRecipe.class);

    private static final int SLOT = SlotArea.SIZE;
    private static final int PADDING = 4;
    private static final int ITEM_ROWS = 3;
    private static final int FLUID_ROWS = 3;
    private static final int ITEM_INPUT_X = PADDING;
    private static final int FLUID_INPUT_X = ITEM_INPUT_X + 2 * SLOT + PADDING;
    private static final int PROCESS_X = FLUID_INPUT_X + SLOT;
    private static final int PROCESS_W = 50;
    private static final int PROCESS_H = 54;
    private static final int ITEM_OUTPUT_X = PROCESS_X + PROCESS_W;
    private static final int FLUID_OUTPUT_X = ITEM_OUTPUT_X + 2 * SLOT + PADDING;
    private static final int CONTENT_Y = PADDING;
    private static final int INFO_Y = CONTENT_Y + PROCESS_H + PADDING;
    private static final int INFO_STEP = 11;
    private static final int WIDTH = FLUID_OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = 96;

    private final CrystalWorkstationMode mode;
    private final IDrawable process;

    public CrystalWorkstationCategory(IGuiHelper guiHelper, CrystalWorkstationMode mode) {
        super(guiHelper, new ItemStack(ModMachines.CRYSTAL_WORKSTATION.item()), WIDTH, HEIGHT);
        this.mode = mode;
        this.process = new AnimatedTextureDrawable(Ids.of("textures/gui/jei/crystal_workstation_" + mode.id() +
                                                          "_process.png"),
                                                   PROCESS_W, PROCESS_H, 24, 80);
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
    protected SlotArea itemInputs() {
        return new SlotArea(2 * ITEM_ROWS, 2, ITEM_INPUT_X, CONTENT_Y);
    }

    @Override
    protected SlotArea fluidInputs() {
        return SlotArea.column(FLUID_ROWS, FLUID_INPUT_X, CONTENT_Y);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(2 * ITEM_ROWS, 2, ITEM_OUTPUT_X, CONTENT_Y);
    }

    @Override
    protected SlotArea fluidOutputs() {
        return SlotArea.column(FLUID_ROWS, FLUID_OUTPUT_X, CONTENT_Y);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected int infoStep() {
        return INFO_STEP;
    }

    @Override
    protected boolean showsHiddenHint() {
        return false;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, PROCESS_X, CONTENT_Y);
    }
}
