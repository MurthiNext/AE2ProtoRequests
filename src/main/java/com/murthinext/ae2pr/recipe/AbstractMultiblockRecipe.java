package com.murthinext.ae2pr.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * 多方块配方基类，持有公共的输入、输出、耗时与能量。
 */
public abstract class AbstractMultiblockRecipe extends AbstractRecipe implements MultiblockRecipe {

    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    @Nullable
    private final Double energy;

    protected AbstractMultiblockRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft.itemInputs());
        this.fluidInputs = draft.fluidInputs().stream().map(FluidStack::copy).toList();
        this.itemOutputs = draft.itemOutputs().stream().map(ItemStack::copy).toList();
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

    /**
     * 配方未指定能量时采用的值。
     */
    protected abstract double defaultEnergyPerParallel();
}
