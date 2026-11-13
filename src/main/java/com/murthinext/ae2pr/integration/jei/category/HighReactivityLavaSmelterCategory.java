package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import com.murthinext.ae2pr.registry.ModMachines;

public class HighReactivityLavaSmelterCategory extends AbstractMachineCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
                                                                                  "high_reactivity_lava_smelter",
                                                                                  MachineRecipe.class);

    private static final int WIDTH = 142;
    private static final int HEIGHT = 98;
    private static final int OUTPUT_X = 117;
    private static final int OUTPUT_Y = 12;
    private static final int MAX_RESULTS = 2;
    private static final int INFO_Y = 67;
    private static final int COLOR_TEXT = 0x404040;

    public HighReactivityLavaSmelterCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.block()), WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.high_reactivity_lava_smelter");
    }

    // 炉膛贴图自带槽位底纹
    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(HighReactivityLavaSmelterFurnace.ITEM_COUNT, HighReactivityLavaSmelterFurnace.ITEM_COLS,
                            HighReactivityLavaSmelterFurnace.SLOT_X, HighReactivityLavaSmelterFurnace.SLOT_Y, false);
    }

    @Override
    protected SlotArea fluidInputs() {
        return new SlotArea(HighReactivityLavaSmelterFurnace.FLUID_COUNT, 1,
                            HighReactivityLavaSmelterFurnace.fluidSlotX(0), HighReactivityLavaSmelterFurnace.SLOT_Y,
                            false);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(MAX_RESULTS, 1, OUTPUT_X, OUTPUT_Y);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected boolean showsHiddenHint() {
        return false;
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        HighReactivityLavaSmelterFurnace.draw(graphics);
    }

    @Override
    protected void drawInfo(GuiGraphics graphics, MachineRecipe recipe) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.ae2pr.high_reactivity_lava_smelter.durability_cost", 1),
                            4, INFO_Y, COLOR_TEXT, false);
        graphics.drawString(font, JeiRecipeInfo.duration(recipe.getDuration()), 4, INFO_Y + 10, COLOR_TEXT, false);
    }
}
