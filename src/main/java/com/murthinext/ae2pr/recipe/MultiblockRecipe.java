package com.murthinext.ae2pr.recipe;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface MultiblockRecipe {

    ResourceLocation getId();

    List<CountedIngredient> getItemInputs();

    default List<CountedIngredient> getItemCatalysts() {
        return List.of();
    }

    List<FluidStack> getFluidInputs();

    List<ItemStack> getItemOutputs();

    List<ChancedItemStack> getChancedItemOutputs();

    List<FluidStack> getFluidOutputs();

    int getDuration();

    double getEnergyPerParallel();
}
