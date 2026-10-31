package com.murthinext.ae2pr.machine.crystal_workstation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.machine.Mode;
import com.murthinext.ae2pr.machine.MachineRecipe;

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
    public Component displayName() {
        return Component.translatable("gui.ae2pr.crystal_workstation.mode." + id);
    }

    public RecipeType<MachineRecipe> recipeType() {
        return this == ETCHING ? ModMachines.CRYSTAL_WORKSTATION_ETCHING_RECIPE.type()
                : ModMachines.CRYSTAL_WORKSTATION_CUTTING_RECIPE.type();
    }

    public CrystalWorkstationMode next() {
        return this == ETCHING ? CUTTING : ETCHING;
    }

    public static CrystalWorkstationMode byId(String id) {
        return CUTTING.id.equals(id) ? CUTTING : ETCHING;
    }
}
