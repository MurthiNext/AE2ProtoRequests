package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;

/**
 * 晶能工作站配方。
 */
public class CrystalWorkstationRecipe extends AbstractMultiblockRecipe {

    public static final int MAX_ITEM_INPUTS = 6;
    public static final int MAX_ITEM_OUTPUTS = 6;
    public static final int MAX_FLUIDS = 3;
    public static final int DEFAULT_DURATION = 200;

    private final CrystalWorkstationMode mode;

    public CrystalWorkstationRecipe(ResourceLocation id, CrystalWorkstationMode mode, RecipeDraft draft) {
        super(id, draft);
        this.mode = mode;
    }

    public CrystalWorkstationMode getMode() {
        return mode;
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mode == CrystalWorkstationMode.ETCHING ? ModRecipes.CRYSTAL_WORKSTATION_ETCHING.get()
                : ModRecipes.CRYSTAL_WORKSTATION_CUTTING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mode.recipeType();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<CrystalWorkstationRecipe> {

        private final CrystalWorkstationMode mode;

        public Serializer(CrystalWorkstationMode mode) {
            super(DEFAULT_DURATION);
            this.mode = mode;
        }

        @Override
        protected void validate(RecipeDraft draft, JsonObject json) {
            super.validate(draft, json);
            if (draft.itemInputs().size() > MAX_ITEM_INPUTS || draft.itemOutputs().size() > MAX_ITEM_OUTPUTS
                    || draft.fluidInputs().size() > MAX_FLUIDS || draft.fluidOutputs().size() > MAX_FLUIDS) {
                throw new JsonSyntaxException(
                        "Crystal workstation supports at most 6 item and 3 fluid inputs/outputs each");
            }
        }

        @Override
        protected boolean energyRequired() {
            return true;
        }

        @Override
        protected CrystalWorkstationRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new CrystalWorkstationRecipe(id, mode, draft);
        }

        @Override
        protected CrystalWorkstationRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            return new CrystalWorkstationRecipe(id, mode, draft);
        }
    }
}
