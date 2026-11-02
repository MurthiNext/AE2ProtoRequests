package com.murthinext.ae2pr.machine.crystal_workstation;

import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.machine.MachineRecipe;
import com.murthinext.ae2pr.machine.Mode;
import com.murthinext.ae2pr.registry.ModMachines;

public enum CrystalWorkstationMode implements Mode {

    ETCHING("etching"), CUTTING("cutting");

    private final String id;

    CrystalWorkstationMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "gui.ae2pr.crystal_workstation.mode." + id;
    }

    @Override
    public RecipeType<MachineRecipe> recipeType() {
        return this == ETCHING ? ModMachines.CRYSTAL_WORKSTATION_ETCHING_RECIPE.type()
                : ModMachines.CRYSTAL_WORKSTATION_CUTTING_RECIPE.type();
    }
}
