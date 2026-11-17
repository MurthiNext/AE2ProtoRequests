package com.murthinext.ae2pr.api.recipe;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMultiblockRecipe extends AbstractRecipe implements MultiblockRecipe {

    private final List<FluidInput> fluidInputs;
    private final List<ItemOutput> itemOutputs;
    private final List<FluidOutput> fluidOutputs;
    private final int duration;

    @Nullable
    private final Double energy;

    protected AbstractMultiblockRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft.itemInputs());
        this.fluidInputs = draft.fluidInputs();
        this.itemOutputs = draft.itemOutputs();
        this.fluidOutputs = draft.fluidOutputs();
        this.duration = draft.duration();
        this.energy = draft.energy();
    }

    public final RecipeDraft toDraft() {
        return new RecipeDraft(getItemInputs(), fluidInputs, itemOutputs, fluidOutputs, duration, energy);
    }

    @Override
    public final List<FluidInput> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public final List<ItemOutput> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public final List<FluidOutput> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public final int getDuration() {
        return duration;
    }

    @Override
    public final double getEnergyPerParallel() {
        return energy != null ? energy : 0.0D;
    }
}
