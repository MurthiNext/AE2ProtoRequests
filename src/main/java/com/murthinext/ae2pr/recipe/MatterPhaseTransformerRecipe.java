package com.murthinext.ae2pr.recipe;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

/**
 * 物质相变仪配方。
 */
public class MatterPhaseTransformerRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    public MatterPhaseTransformerRecipe(ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
    }

    /**
     * 唯一流体产物。
     */
    public FluidStack getFluidOutput() {
        List<FluidStack> outputs = getFluidOutputs();
        return outputs.isEmpty() ? FluidStack.EMPTY : outputs.get(0).copy();
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return Config.matterPhaseTransformerEnergyPerParallel();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MATTER_PHASE_TRANSFORMER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MATTER_PHASE_TRANSFORMER_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<MatterPhaseTransformerRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected void validate(RecipeDraft draft, JsonObject json) {
            super.validate(draft, json);
            if (draft.itemInputs().isEmpty()) {
                throw new JsonSyntaxException("Recipe requires at least one item input");
            }
            if (!draft.fluidInputs().isEmpty()) {
                throw new JsonSyntaxException("Matter phase transformer does not support fluid inputs");
            }
            if (draft.fluidOutputs().size() != 1) {
                throw new JsonSyntaxException("Recipe must have exactly one fluid output");
            }
        }

        @Override
        protected MatterPhaseTransformerRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new MatterPhaseTransformerRecipe(id, draft);
        }

        @Override
        protected MatterPhaseTransformerRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            return new MatterPhaseTransformerRecipe(id, draft);
        }
    }
}
