package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

/**
 * 水晶反应仓配方。
 */
public class CrystalReactionChamberRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    public CrystalReactionChamberRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return Config.crystalReactionEnergyPerParallel();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRYSTAL_REACTION_CHAMBER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRYSTAL_REACTION_CHAMBER_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<CrystalReactionChamberRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected CrystalReactionChamberRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new CrystalReactionChamberRecipe(id, draft);
        }

        @Override
        protected CrystalReactionChamberRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            return new CrystalReactionChamberRecipe(id, draft);
        }
    }
}
