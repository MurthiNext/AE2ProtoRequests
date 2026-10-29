package com.murthinext.ae2pr.block.multi_factory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import com.murthinext.ae2pr.Config;

// 通式加工厂工作模式，直接对应 AE2 原生配方类型
public enum MultiFactoryMode {

    INSCRIBING("inscribing", InscriberRecipe.TYPE),

    CHARGING("charging", ChargerRecipe.TYPE);

    private final String id;
    private final RecipeType<?> recipeType;

    MultiFactoryMode(String id, RecipeType<?> recipeType) {
        this.id = id;
        this.recipeType = recipeType;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("gui.ae2pr.multi_factory.mode." + id);
    }

    public RecipeType<?> recipeType() {
        return recipeType;
    }

    public int durationTicks() {
        return this == INSCRIBING ? Config.multiFactoryInscribingDuration() : Config.multiFactoryChargingDuration();
    }

    public double energyPerParallel() {
        return this == INSCRIBING ? Config.multiFactoryInscribingEnergyPerParallel()
                : Config.multiFactoryChargingEnergyPerParallel();
    }

    public MultiFactoryMode next() {
        return this == INSCRIBING ? CHARGING : INSCRIBING;
    }

    public static MultiFactoryMode byId(String id) {
        return CHARGING.id.equals(id) ? CHARGING : INSCRIBING;
    }
}
