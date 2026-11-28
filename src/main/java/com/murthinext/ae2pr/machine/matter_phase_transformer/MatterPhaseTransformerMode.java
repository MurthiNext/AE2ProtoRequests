package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.RecipeConstraints;
import com.murthinext.ae2pr.api.machine.RecipeSpec;

public enum MatterPhaseTransformerMode implements Mode {
    MELTING(RecipeSpec.simple(200, phaseTransformerConstraints(1, 1))),
    MIXING(RecipeSpec.simple(200, phaseTransformerConstraints(2, -1)));

    private final RecipeSpec<MachineRecipe> recipe;

    MatterPhaseTransformerMode(RecipeSpec<MachineRecipe> recipe) {
        this.recipe = recipe;
    }

    @Override
    public String machineId() {
        return "matter_phase_transformer";
    }

    @Override
    public RecipeSpec<MachineRecipe> recipe() {
        return recipe;
    }

    private static RecipeConstraints phaseTransformerConstraints(int minItemInputs, int maxItemInputs) {
        return RecipeConstraints.builder()
                .itemInputs(minItemInputs, maxItemInputs)
                .fluidInputs(0, 0)
                .itemOutputs(0, 0)
                .fluidOutputs(1, 1)
                .requireEnergy()
                .build();
    }
}
