package com.murthinext.ae2pr.integration.jei.category;

import net.minecraft.world.item.ItemStack;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;

public class FilterPressCategory extends AbstractSimpleMachineCategory {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID, "filter_press",
            MachineRecipe.class);

    public FilterPressCategory(IGuiHelper guiHelper) {
        super(guiHelper, RECIPE_TYPE, new ItemStack(ModMachines.FILTER_PRESS.block()), "block.ae2pr.filter_press",
                "textures/gui/jei/filter_press_process.png");
    }

    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(1, 1, 4, 8);
    }

    @Override
    protected SlotArea fluidInputs() {
        return SlotArea.column(2, 4, 26);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(4, 2, 100, 4);
    }

    @Override
    protected SlotArea fluidOutputs() {
        return SlotArea.column(2, 136, 4);
    }
}
