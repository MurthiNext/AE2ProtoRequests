package com.murthinext.ae2pr.block.crystal_workstation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.recipe.CrystalWorkstationRecipe;

public enum CrystalWorkstationMode {

    ETCHING("etching"), CUTTING("cutting");

    private final String id;

    CrystalWorkstationMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("gui.ae2pr.crystal_workstation.mode." + id);
    }

    public RecipeType<CrystalWorkstationRecipe> recipeType() {
        return this == ETCHING ? ModRecipes.CRYSTAL_WORKSTATION_ETCHING_TYPE.get()
                : ModRecipes.CRYSTAL_WORKSTATION_CUTTING_TYPE.get();
    }

    public CrystalWorkstationMode next() {
        return this == ETCHING ? CUTTING : ETCHING;
    }

    public static CrystalWorkstationMode byId(String id) {
        return CUTTING.id.equals(id) ? CUTTING : ETCHING;
    }
}
