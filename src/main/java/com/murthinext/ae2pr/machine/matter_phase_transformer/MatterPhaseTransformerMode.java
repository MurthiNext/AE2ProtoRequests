package com.murthinext.ae2pr.machine.matter_phase_transformer;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRecipe;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.registry.ModMachines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public enum MatterPhaseTransformerMode implements Mode {
    MELTING("melting"),
    MIXING("mixing");

    private static final String MACHINE_ID = "matter_phase_transformer";

    private final String id;

    MatterPhaseTransformerMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "gui.ae2pr." + MACHINE_ID + ".mode." + id;
    }

    public ResourceLocation recipeTypeId() {
        return Ids.of(MACHINE_ID + "_" + id);
    }

    public ResourceLocation recipeId(String name) {
        return Ids.of(MACHINE_ID + "/" + id + "/" + name);
    }

    @Override
    public RecipeType<MachineRecipe> recipeType() {
        return this == MELTING
                ? ModMachines.MATTER_PHASE_TRANSFORMER_MELTING_RECIPE.type()
                : ModMachines.MATTER_PHASE_TRANSFORMER_MIXING_RECIPE.type();
    }
}
