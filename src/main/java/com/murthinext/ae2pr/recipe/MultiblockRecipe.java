package com.murthinext.ae2pr.recipe;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * 多方块控制器使用的配方视图。
 */
public interface MultiblockRecipe {

    ResourceLocation getId();

    List<CountedIngredient> getItemInputs();

    List<FluidStack> getFluidInputs();

    List<ItemStack> getItemOutputs();

    List<FluidStack> getFluidOutputs();

    int getDuration();

    double getEnergyPerParallel();
}
