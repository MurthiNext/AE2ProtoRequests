package com.murthinext.ae2pr.api.recipe;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public record RecipeDraft(
        List<ItemInput> itemInputs,
        List<FluidInput> fluidInputs,
        List<ItemOutput> itemOutputs,
        List<FluidOutput> fluidOutputs,
        int duration,
        @Nullable Double energy) {

    public RecipeDraft {
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
    }
}
