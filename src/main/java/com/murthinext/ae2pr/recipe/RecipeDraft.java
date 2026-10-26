package com.murthinext.ae2pr.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraftforge.fluids.FluidStack;

/**
 * 配方解析出的公共内容。
 */
public record RecipeDraft(List<CountedIngredient> itemInputs, List<FluidStack> fluidInputs,
        List<ChancedItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration, @Nullable Double energy) {

    public RecipeDraft {
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
    }
}
