package com.murthinext.ae2pr.machine.crystal_workstation;

import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.RecipeConstraints;
import com.murthinext.ae2pr.api.machine.RecipeSpec;

public enum CrystalWorkstationMode implements Mode {
    ETCHING(RecipeSpec.simple(200, workstationConstraints())),
    CUTTING(RecipeSpec.simple(200, workstationConstraints()));

    private final RecipeSpec<MachineRecipe> recipe;

    CrystalWorkstationMode(RecipeSpec<MachineRecipe> recipe) {
        this.recipe = recipe;
    }

    @Override
    public String machineId() {
        return "crystal_workstation";
    }

    @Override
    public RecipeSpec<MachineRecipe> recipe() {
        return recipe;
    }

    private static RecipeConstraints workstationConstraints() {
        return RecipeConstraints.builder()
                .itemInputs(0, 6)
                .fluidInputs(0, 3)
                .itemOutputs(0, 6)
                .fluidOutputs(0, 3)
                .requireEnergy()
                .build();
    }
}
