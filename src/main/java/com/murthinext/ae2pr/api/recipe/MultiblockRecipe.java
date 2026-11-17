package com.murthinext.ae2pr.api.recipe;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public interface MultiblockRecipe {

    ResourceLocation getId();

    List<ItemInput> getItemInputs();

    List<FluidInput> getFluidInputs();

    List<ItemOutput> getItemOutputs();

    List<FluidOutput> getFluidOutputs();

    int getDuration();

    double getEnergyPerParallel();
}
