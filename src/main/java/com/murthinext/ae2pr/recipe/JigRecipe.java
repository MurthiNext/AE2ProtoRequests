package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

public class JigRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    public JigRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return Config.jigEnergyPerParallel();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.JIG.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.JIG_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<JigRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected JigRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new JigRecipe(id, draft);
        }

        @Override
        protected JigRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft, FriendlyByteBuf buffer) {
            return new JigRecipe(id, draft);
        }
    }
}
