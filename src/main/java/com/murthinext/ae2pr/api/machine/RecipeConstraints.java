package com.murthinext.ae2pr.api.machine;

import com.google.gson.JsonSyntaxException;

import com.murthinext.ae2pr.api.recipe.RecipeDraft;

public record RecipeConstraints(int maxItemInputs, int maxFluidInputs, int maxItemOutputs, int maxFluidOutputs,
        int minItemInputs, int minFluidInputs, int minItemOutputs, int minFluidOutputs, boolean energyRequired) {

    public static final RecipeConstraints NONE = builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public void validate(RecipeDraft draft) {
        check("item_inputs", draft.itemInputs().size(), minItemInputs, maxItemInputs);
        check("fluid_inputs", draft.fluidInputs().size(), minFluidInputs, maxFluidInputs);
        check("item_outputs", draft.itemOutputs().size(), minItemOutputs, maxItemOutputs);
        check("fluid_outputs", draft.fluidOutputs().size(), minFluidOutputs, maxFluidOutputs);
        if (energyRequired && draft.energy() == null) {
            throw new JsonSyntaxException("energy is required for this recipe");
        }
    }

    private static void check(String key, int size, int min, int max) {
        if (min >= 0 && size < min) {
            throw new JsonSyntaxException(key + " requires at least " + min + " entries");
        }
        if (max >= 0 && size > max) {
            throw new JsonSyntaxException(key + " supports at most " + max + " entries");
        }
    }

    public static final class Builder {

        private int maxItemInputs = -1;
        private int maxFluidInputs = -1;
        private int maxItemOutputs = -1;
        private int maxFluidOutputs = -1;
        private int minItemInputs = -1;
        private int minFluidInputs = -1;
        private int minItemOutputs = -1;
        private int minFluidOutputs = -1;
        private boolean energyRequired;

        private Builder() {
        }

        public Builder itemInputs(int min, int max) {
            minItemInputs = min;
            maxItemInputs = max;
            return this;
        }

        public Builder fluidInputs(int min, int max) {
            minFluidInputs = min;
            maxFluidInputs = max;
            return this;
        }

        public Builder itemOutputs(int min, int max) {
            minItemOutputs = min;
            maxItemOutputs = max;
            return this;
        }

        public Builder fluidOutputs(int min, int max) {
            minFluidOutputs = min;
            maxFluidOutputs = max;
            return this;
        }

        public Builder requireEnergy() {
            energyRequired = true;
            return this;
        }

        public RecipeConstraints build() {
            return new RecipeConstraints(maxItemInputs, maxFluidInputs, maxItemOutputs, maxFluidOutputs, minItemInputs,
                    minFluidInputs, minItemOutputs, minFluidOutputs, energyRequired);
        }
    }
}
