package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.ModRecipes;

/**
 * 熔岩冶炼炉配方。
 */
public class LavaSmelterRecipe implements Recipe<Container>, MultiblockRecipe {

    public static final int DEFAULT_DURATION = 100;
    public static final int MAX_RESULTS = 2;
    public static final int MAX_ITEM_INPUTS = 4;
    public static final int MAX_FLUID_INPUTS = 2;

    private final ResourceLocation id;
    private final List<CountedIngredient> ingredients;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> results;
    private final int duration;

    public LavaSmelterRecipe(ResourceLocation id, List<CountedIngredient> ingredients, List<FluidStack> fluidInputs,
            List<ItemStack> results, int duration) {
        this.id = id;
        this.ingredients = List.copyOf(ingredients);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.results = List.copyOf(results);
        this.duration = duration;
    }

    @Override
    public List<CountedIngredient> getItemInputs() {
        return ingredients;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return results;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return List.of();
    }

    @Override
    public int getDuration() {
        return duration;
    }

    /**
     * 熔岩冶炼炉不按作业扣电，由结构被动耗电承担。
     */
    @Override
    public double getEnergyPerParallel() {
        return 0;
    }

    /**
     * 主产物。
     */
    public ItemStack getResultItem() {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return getResultItem();
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
        return ModRecipes.LAVA_SMELTER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.LAVA_SMELTER_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * JSON 与网络序列化。
     */
    public static class Serializer implements RecipeSerializer<LavaSmelterRecipe> {

        @Override
        public LavaSmelterRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject entry = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(entry, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs count must be >= 1");
                }
                ingredients.add(new CountedIngredient(Ingredient.fromJson(entry), count));
            }

            List<FluidStack> fluidInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_inputs", new JsonArray())) {
                fluidInputs.add(readFluid(element.getAsJsonObject(), "fluid_inputs"));
            }

            if (ingredients.isEmpty() && fluidInputs.isEmpty()) {
                throw new JsonSyntaxException("Recipe requires at least one input");
            }
            if (fluidInputs.size() > MAX_FLUID_INPUTS) {
                throw new JsonSyntaxException("Fluid input count must be between 0 and " + MAX_FLUID_INPUTS);
            }
            if (ingredients.size() > MAX_ITEM_INPUTS) {
                throw new JsonSyntaxException("Item input count must be between 0 and " + MAX_ITEM_INPUTS);
            }

            List<ItemStack> results = new ArrayList<>();
            if (json.has("item_outputs")) {
                JsonArray resultArray = GsonHelper.getAsJsonArray(json, "item_outputs");
                for (JsonElement element : resultArray) {
                    results.add(ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
                }
            }
            if (results.isEmpty() || results.size() > MAX_RESULTS) {
                throw new JsonSyntaxException("Recipe output count must be between 1 and " + MAX_RESULTS);
            }
            int duration = GsonHelper.getAsInt(json, "duration", DEFAULT_DURATION);
            if (duration < 1) {
                throw new JsonSyntaxException("duration must be >= 1");
            }
            return new LavaSmelterRecipe(id, ingredients, fluidInputs, results, duration);
        }

        private static FluidStack readFluid(JsonObject obj, String key) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(obj, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException("Unknown fluid in " + key + ": " + fluidId);
            }
            int amount = GsonHelper.getAsInt(obj, "amount", 1000);
            if (amount < 1) {
                throw new JsonSyntaxException("Invalid amount in " + key + ", must be >= 1");
            }
            return new FluidStack(fluid, amount);
        }

        @Override
        public LavaSmelterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int count = buffer.readVarInt();
                ingredients.add(new CountedIngredient(Ingredient.fromNetwork(buffer), count));
            }
            int fluidInputCount = buffer.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidInputCount);
            for (int i = 0; i < fluidInputCount; i++) {
                fluidInputs.add(buffer.readFluidStack());
            }
            int resultCount = buffer.readVarInt();
            List<ItemStack> results = new ArrayList<>(resultCount);
            for (int i = 0; i < resultCount; i++) {
                ItemStack result = buffer.readItem();
                result.setCount(buffer.readVarInt());
                results.add(result);
            }
            int duration = buffer.readVarInt();
            return new LavaSmelterRecipe(id, ingredients, fluidInputs, results, duration);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, LavaSmelterRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient entry : recipe.ingredients) {
                buffer.writeVarInt(entry.count());
                entry.ingredient().toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack fluid : recipe.fluidInputs) {
                buffer.writeFluidStack(fluid);
            }
            buffer.writeVarInt(recipe.results.size());
            for (ItemStack result : recipe.results) {
                buffer.writeItem(result.copyWithCount(1));
                buffer.writeVarInt(result.getCount());
            }
            buffer.writeVarInt(recipe.duration);
        }
    }
}
