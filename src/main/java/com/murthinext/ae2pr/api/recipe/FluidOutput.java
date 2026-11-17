package com.murthinext.ae2pr.api.recipe;

import net.minecraftforge.fluids.FluidStack;

public record FluidOutput(FluidStack stack, float chance) implements ChancedOutput {

    public FluidOutput {
        stack = stack.copy();
        chance = ChancedOutput.checkChance(chance);
    }

    public static FluidOutput certain(FluidStack stack) {
        return new FluidOutput(stack, 1.0F);
    }
}
