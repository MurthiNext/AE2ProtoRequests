package com.murthinext.ae2pr.integration.jei.category;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.core.registry.ModMachines;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

public class JigCategory extends AbstractSimpleMachineCategory {

    public static final RecipeType<MachineRecipe> RECIPE_TYPE =
            RecipeType.create(ae2pr.MODID, "jig", MachineRecipe.class);

    public JigCategory(IGuiHelper guiHelper) {
        super(
                guiHelper,
                RECIPE_TYPE,
                new ItemStack(ModMachines.jig().item()),
                "block.ae2pr.jig",
                "textures/gui/jei/jig_process.png");
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
}
