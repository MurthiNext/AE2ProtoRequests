package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

public class FilterPressRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    public FilterPressRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return Config.filterPressEnergyPerParallel();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FILTER_PRESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FILTER_PRESS_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<FilterPressRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected FilterPressRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new FilterPressRecipe(id, draft);
        }

        @Override
        protected FilterPressRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            return new FilterPressRecipe(id, draft);
        }
    }
}
