package com.murthinext.ae2pr.api.recipe;

import net.minecraftforge.fluids.FluidStack;

public record FluidInput(FluidStack stack, boolean catalyst) {

    public FluidInput {
        stack = stack.copy();
    }
}
