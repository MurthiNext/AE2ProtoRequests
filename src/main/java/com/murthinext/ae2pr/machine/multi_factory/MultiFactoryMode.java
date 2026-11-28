package com.murthinext.ae2pr.machine.multi_factory;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import com.murthinext.ae2pr.core.config.ServerConfig;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.RecipeSpec;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public enum MultiFactoryMode implements Mode {
    INSCRIBING(InscriberRecipe.TYPE),
    CHARGING(ChargerRecipe.TYPE);

    private final RecipeType<?> recipeType;

    MultiFactoryMode(RecipeType<?> recipeType) {
        this.recipeType = recipeType;
    }

    @Override
    public String machineId() {
        return "multi_factory";
    }

    @Override
    @Nullable
    public RecipeSpec<?> recipe() {
        return null;
    }

    @Override
    public RecipeType<?> recipeType() {
        return recipeType;
    }

    public int durationTicks() {
        return this == INSCRIBING
                ? ServerConfig.multiFactoryInscribingDuration()
                : ServerConfig.multiFactoryChargingDuration();
    }

    public double energyPerParallel() {
        return this == INSCRIBING
                ? ServerConfig.multiFactoryInscribingEnergyPerParallel()
                : ServerConfig.multiFactoryChargingEnergyPerParallel();
    }
}
