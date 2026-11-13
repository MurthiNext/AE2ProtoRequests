package com.murthinext.ae2pr.machine.multi_factory;

import net.minecraft.world.item.crafting.RecipeType;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.machine.Mode;

// 通式加工厂工作模式，直接对应 AE2 原生配方类型
public enum MultiFactoryMode implements Mode {

    INSCRIBING("inscribing", InscriberRecipe.TYPE),
    CHARGING("charging", ChargerRecipe.TYPE);

    private final String id;
    private final RecipeType<?> recipeType;

    MultiFactoryMode(String id, RecipeType<?> recipeType) {
        this.id = id;
        this.recipeType = recipeType;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "gui.ae2pr.multi_factory.mode." + id;
    }

    @Override
    public RecipeType<?> recipeType() {
        return recipeType;
    }

    public int durationTicks() {
        return this == INSCRIBING ? ServerConfig.multiFactoryInscribingDuration()
                                  : ServerConfig.multiFactoryChargingDuration();
    }

    public double energyPerParallel() {
        return this == INSCRIBING ? ServerConfig.multiFactoryInscribingEnergyPerParallel()
                                  : ServerConfig.multiFactoryChargingEnergyPerParallel();
    }
}
