package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.murthinext.ae2pr.api.recipe.AbstractRecipe;
import com.murthinext.ae2pr.api.recipe.FluidOutput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.api.recipe.RecipeJson;
import com.murthinext.ae2pr.api.recipe.RecipeNetwork;
import com.murthinext.ae2pr.api.recipe.WorldInteractionCircumstance;
import com.murthinext.ae2pr.core.registry.ModRecipes;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

// 支持带数量原料、多产物和流体源替换的复杂世界交互配方
public class ComplexWorldInteractionRecipe extends AbstractRecipe {

    private final WorldInteractionCircumstance circumstance;
    private final List<ItemOutput> results;

    @Nullable
    private final Fluid resultFluid;

    public ComplexWorldInteractionRecipe(
            ResourceLocation id,
            List<ItemInput> ingredients,
            WorldInteractionCircumstance circumstance,
            List<ItemOutput> results,
            @Nullable Fluid resultFluid) {
        super(id, ingredients);
        this.circumstance = circumstance;
        this.results = List.copyOf(results);
        this.resultFluid = resultFluid;
    }

    public boolean canTrigger(ItemStack stack) {
        return getItemInputs().stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    public WorldInteractionCircumstance getCircumstance() {
        return circumstance;
    }

    @Override
    public List<ItemOutput> getItemOutputs() {
        return results;
    }

    @Nullable
    public Fluid getResultFluid() {
        return resultFluid;
    }

    public boolean convertsFluid() {
        return resultFluid != null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPLEX_WORLD_INTERACTION.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ComplexWorldInteractionRecipe> {

        @Override
        public ComplexWorldInteractionRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<ItemInput> ingredients = RecipeJson.readItemInputs(json, "item_inputs");
            if (ingredients.isEmpty()) {
                throw new JsonSyntaxException("item_inputs must not be empty");
            }
            WorldInteractionCircumstance circumstance =
                    WorldInteractionCircumstance.fromJson(GsonHelper.getAsJsonObject(json, "circumstance"));
            List<ItemOutput> results = RecipeJson.readItemOutputs(json, "item_outputs");
            List<FluidOutput> fluidOutputs = RecipeJson.readFluidOutputs(json, "fluid_outputs");
            if (fluidOutputs.size() > 1) {
                throw new JsonSyntaxException("fluid_outputs allows at most one output");
            }
            Fluid resultFluid = null;
            if (!fluidOutputs.isEmpty()) {
                if (circumstance.isExplosion() || !circumstance.sourceOnly()) {
                    throw new JsonSyntaxException("fluid_outputs requires a source-only fluid circumstance");
                }
                FluidOutput output = fluidOutputs.get(0);
                Fluid fluid = output.stack().getFluid();
                if (!output.certain()
                        || fluid == Fluids.EMPTY
                        || !fluid.defaultFluidState().isSource()
                        || output.stack().getAmount() != FluidType.BUCKET_VOLUME) {
                    throw new JsonSyntaxException(
                            "fluid_outputs requires a certain source fluid and amount must be 1000");
                }
                resultFluid = fluid;
            }
            if (results.isEmpty() && resultFluid == null) {
                throw new JsonSyntaxException("World interaction recipes must have at least one output");
            }
            return new ComplexWorldInteractionRecipe(id, ingredients, circumstance, results, resultFluid);
        }

        @Override
        public ComplexWorldInteractionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            List<ItemInput> ingredients = RecipeNetwork.readItemInputs(buffer);
            WorldInteractionCircumstance circumstance = WorldInteractionCircumstance.fromNetwork(buffer);
            List<ItemOutput> results = RecipeNetwork.readItemOutputs(buffer);
            Fluid resultFluid =
                    buffer.readBoolean() ? ForgeRegistries.FLUIDS.getValue(buffer.readResourceLocation()) : null;
            return new ComplexWorldInteractionRecipe(id, ingredients, circumstance, results, resultFluid);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ComplexWorldInteractionRecipe recipe) {
            RecipeNetwork.writeItemInputs(buffer, recipe.getItemInputs());
            recipe.circumstance.toNetwork(buffer);
            RecipeNetwork.writeItemOutputs(buffer, recipe.results);
            buffer.writeBoolean(recipe.resultFluid != null);
            if (recipe.resultFluid != null) {
                buffer.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(recipe.resultFluid));
            }
        }
    }
}
