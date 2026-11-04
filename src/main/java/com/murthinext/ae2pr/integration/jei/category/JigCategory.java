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

public class JigCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "jig",
            MachineRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 106;
    private static final int INFO_Y = 72;
    private static final int PROCESS_W = 60;
    private static final int PROCESS_H = 64;

    private final IDrawable process;

    public JigCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack(ModMachines.JIG.item()), WIDTH, HEIGHT);
        process = new AnimatedTextureDrawable(Ids.of("textures/gui/jei/jig_process.png"),
                PROCESS_W, PROCESS_H, 40, 50);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.jig");
    }

    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(1, 1, 4, 17);
    }

    @Override
    protected SlotArea fluidInputs() {
        return SlotArea.column(1, 4, 35);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(4, 2, 100, 17);
    }

    @Override
    protected SlotArea fluidOutputs() {
        return SlotArea.column(2, 136, 17);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected int infoStep() {
        return 11;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        process.draw(graphics, 30, 2);
    }
}
