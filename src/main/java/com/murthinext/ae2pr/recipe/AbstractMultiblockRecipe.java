package com.murthinext.ae2pr.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public abstract class AbstractMultiblockRecipe extends AbstractRecipe implements MultiblockRecipe {

    private final List<FluidStack> fluidInputs;
    private final List<ChancedItemStack> itemOutputs;
    private final List<ItemStack> itemOutputStacks;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    @Nullable
    private final Double energy;

    protected AbstractMultiblockRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft.itemInputs());
        this.fluidInputs = draft.fluidInputs().stream().map(FluidStack::copy).toList();
        this.itemOutputs = draft.itemOutputs();
        this.itemOutputStacks = itemOutputs.stream().map(ChancedItemStack::stack).toList();
        this.fluidOutputs = draft.fluidOutputs().stream().map(FluidStack::copy).toList();
        this.duration = draft.duration();
        this.energy = draft.energy();
    }

    public final RecipeDraft toDraft() {
        return new RecipeDraft(getItemInputs(), fluidInputs, itemOutputs, fluidOutputs, duration, energy);
    }

    @Override
    public final List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public final List<ItemStack> getItemOutputs() {
        return itemOutputStacks;
    }

    @Override
    public final List<ChancedItemStack> getChancedItemOutputs() {
        return itemOutputs;
    }

    @Override
    public final List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public final int getDuration() {
        return duration;
    }

    @Override
    public final double getEnergyPerParallel() {
        return energy != null ? energy : defaultEnergyPerParallel();
    }

    protected abstract double defaultEnergyPerParallel();
}
