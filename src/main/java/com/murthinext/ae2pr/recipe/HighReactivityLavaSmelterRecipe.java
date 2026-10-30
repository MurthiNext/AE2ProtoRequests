package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.ModRecipes;

public class HighReactivityLavaSmelterRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 100;
    public static final int MAX_RESULTS = 2;
    public static final int MAX_ITEM_INPUTS = 4;
    public static final int MAX_FLUID_INPUTS = 2;

    public HighReactivityLavaSmelterRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
    }

    // 熔岩冶炼炉不按作业扣电，由结构被动耗电承担
    @Override
    protected double defaultEnergyPerParallel() {
        return 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HIGH_REACTIVITY_LAVA_SMELTER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HIGH_REACTIVITY_LAVA_SMELTER_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<HighReactivityLavaSmelterRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected void validate(RecipeDraft draft, JsonObject json) {
            super.validate(draft, json);
            if (draft.itemInputs().size() > MAX_ITEM_INPUTS) {
                throw new JsonSyntaxException("Item input count must be between 0 and " + MAX_ITEM_INPUTS);
            }
            if (draft.fluidInputs().size() > MAX_FLUID_INPUTS) {
                throw new JsonSyntaxException("Fluid input count must be between 0 and " + MAX_FLUID_INPUTS);
            }
            if (!draft.fluidOutputs().isEmpty()) {
                throw new JsonSyntaxException("Lava smelter does not support fluid outputs");
            }
            if (draft.itemOutputs().isEmpty() || draft.itemOutputs().size() > MAX_RESULTS) {
                throw new JsonSyntaxException("Recipe output count must be between 1 and " + MAX_RESULTS);
            }
        }

        @Override
        protected HighReactivityLavaSmelterRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new HighReactivityLavaSmelterRecipe(id, draft);
        }

        @Override
        protected HighReactivityLavaSmelterRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            return new HighReactivityLavaSmelterRecipe(id, draft);
        }
    }
}
