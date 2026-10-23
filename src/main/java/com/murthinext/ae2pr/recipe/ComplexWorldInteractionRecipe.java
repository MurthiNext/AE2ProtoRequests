package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.ModRecipes;

/**
 * 支持带数量原料、多产物和流体源替换的复杂世界交互配方。
 */
public class ComplexWorldInteractionRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final List<CountedIngredient> ingredients;
    private final WorldInteractionCircumstance circumstance;
    private final List<ItemStack> results;
    @Nullable
    private final Fluid resultFluid;

    public ComplexWorldInteractionRecipe(ResourceLocation id, List<CountedIngredient> ingredients,
            WorldInteractionCircumstance circumstance, List<ItemStack> results,
            @Nullable Fluid resultFluid) {
        this.id = id;
        this.ingredients = List.copyOf(ingredients);
        this.circumstance = circumstance;
        this.results = results.stream().map(ItemStack::copy).toList();
        this.resultFluid = resultFluid;
    }

    public boolean canTrigger(ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    public List<CountedIngredient> getCountedIngredients() {
        return ingredients;
    }

    public WorldInteractionCircumstance getCircumstance() {
        return circumstance;
    }

    public List<ItemStack> getResults() {
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
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> vanilla = NonNullList.create();
        ingredients.forEach(entry -> vanilla.add(entry.ingredient()));
        return vanilla;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPLEX_WORLD_INTERACTION.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ComplexWorldInteractionRecipe> {

        @Override
        public ComplexWorldInteractionRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs")) {
                JsonObject entry = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(entry, "count", 1);
                Ingredient ingredient = Ingredient.fromJson(entry);
                if (count <= 0 || ingredient.isEmpty()) {
                    throw new JsonSyntaxException("item_inputs requires non-empty ingredients and positive counts");
                }
                ingredients.add(new CountedIngredient(ingredient, count));
            }
            if (ingredients.isEmpty()) {
                throw new JsonSyntaxException("item_inputs must not be empty");
            }
            WorldInteractionCircumstance circumstance = WorldInteractionCircumstance.fromJson(
                    GsonHelper.getAsJsonObject(json, "circumstance"));
            List<ItemStack> results = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_outputs", new JsonArray())) {
                ItemStack result = ShapedRecipe.itemStackFromJson(element.getAsJsonObject());
                if (result.isEmpty() || result.getCount() <= 0) {
                    throw new JsonSyntaxException("item_outputs requires non-empty stacks and positive counts");
                }
                results.add(result);
            }
            JsonArray fluidOutputs = GsonHelper.getAsJsonArray(json, "fluid_outputs", new JsonArray());
            if (fluidOutputs.size() > 1) {
                throw new JsonSyntaxException("fluid_outputs allows at most one output");
            }
            Fluid resultFluid = null;
            if (!fluidOutputs.isEmpty()) {
                if (circumstance.isExplosion() || !circumstance.sourceOnly()) {
                    throw new JsonSyntaxException("fluid_outputs requires a source-only fluid circumstance");
                }
                JsonObject output = fluidOutputs.get(0).getAsJsonObject();
                ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(output, "fluid"));
                resultFluid = ForgeRegistries.FLUIDS.getValue(fluidId);
                if (resultFluid == null || resultFluid == Fluids.EMPTY || !resultFluid.defaultFluidState().isSource()
                        || GsonHelper.getAsInt(output, "amount") != FluidType.BUCKET_VOLUME) {
                    throw new JsonSyntaxException("fluid_outputs requires a source fluid and amount must be 1000");
                }
            }
            if (results.isEmpty() && resultFluid == null) {
                throw new JsonSyntaxException("World interaction recipes must have at least one output");
            }
            return new ComplexWorldInteractionRecipe(id, ingredients, circumstance, results, resultFluid);
        }

        @Override
        public ComplexWorldInteractionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int count = buffer.readVarInt();
                ingredients.add(new CountedIngredient(Ingredient.fromNetwork(buffer), count));
            }
            WorldInteractionCircumstance circumstance = WorldInteractionCircumstance.fromNetwork(buffer);
            List<ItemStack> results = buffer.readList(FriendlyByteBuf::readItem);
            Fluid resultFluid = buffer.readBoolean()
                    ? ForgeRegistries.FLUIDS.getValue(buffer.readResourceLocation())
                    : null;
            return new ComplexWorldInteractionRecipe(id, ingredients, circumstance, results, resultFluid);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ComplexWorldInteractionRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient entry : recipe.ingredients) {
                buffer.writeVarInt(entry.count());
                entry.ingredient().toNetwork(buffer);
            }
            recipe.circumstance.toNetwork(buffer);
            buffer.writeCollection(recipe.results, FriendlyByteBuf::writeItem);
            buffer.writeBoolean(recipe.resultFluid != null);
            if (recipe.resultFluid != null) {
                buffer.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(recipe.resultFluid));
            }
        }
    }
}
