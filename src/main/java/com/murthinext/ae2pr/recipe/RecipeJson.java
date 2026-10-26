package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 配方 JSON 的通用解析。
 */
public final class RecipeJson {

    private RecipeJson() {
    }

    public static RecipeDraft readDraft(JsonObject json, int defaultDuration) {
        return new RecipeDraft(readItemInputs(json, "item_inputs"), readFluidStacks(json, "fluid_inputs"),
                readItemOutputs(json, "item_outputs"), readFluidStacks(json, "fluid_outputs"),
                readDuration(json, defaultDuration), readEnergy(json));
    }

    public static List<CountedIngredient> readItemInputs(JsonObject json, String key) {
        List<CountedIngredient> inputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            int count = GsonHelper.getAsInt(entry, "count", 1);
            Ingredient ingredient = Ingredient.fromJson(entry);
            if (count < 1 || ingredient.isEmpty()) {
                throw new JsonSyntaxException(key + " requires non-empty ingredients and positive counts");
            }
            inputs.add(new CountedIngredient(ingredient, count));
        }
        return inputs;
    }

    public static List<ChancedItemStack> readItemOutputs(JsonObject json, String key) {
        List<ChancedItemStack> outputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            ItemStack stack = readItemStack(entry, key);
            float chance = GsonHelper.getAsFloat(entry, "chance", 1.0F);
            if (!(chance > 0.0F) || chance > 1.0F) {
                throw new JsonSyntaxException(key + " chance must be in (0, 1]");
            }
            outputs.add(new ChancedItemStack(stack, chance));
        }
        return outputs;
    }

    /**
     * 不带概率的物品产物。
     */
    public static List<ItemStack> readItemStacks(JsonObject json, String key) {
        List<ItemStack> stacks = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            stacks.add(readItemStack(element.getAsJsonObject(), key));
        }
        return stacks;
    }

    private static ItemStack readItemStack(JsonObject entry, String key) {
        ItemStack stack = ShapedRecipe.itemStackFromJson(entry);
        if (stack.isEmpty() || stack.getCount() < 1) {
            throw new JsonSyntaxException(key + " requires non-empty stacks and positive counts");
        }
        return stack;
    }

    public static List<FluidStack> readFluidStacks(JsonObject json, String key) {
        List<FluidStack> stacks = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(entry, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            int amount = GsonHelper.getAsInt(entry, "amount", 1000);
            if (fluid == null || fluid == Fluids.EMPTY || amount < 1) {
                throw new JsonSyntaxException("Invalid fluid in " + key + ": " + fluidId);
            }
            stacks.add(new FluidStack(fluid, amount));
        }
        return stacks;
    }

    public static int readDuration(JsonObject json, int defaultDuration) {
        int duration = GsonHelper.getAsInt(json, "duration", defaultDuration);
        if (duration < 1) {
            throw new JsonSyntaxException("duration must be >= 1");
        }
        return duration;
    }

    @Nullable
    public static Double readEnergy(JsonObject json) {
        if (!json.has("energy")) {
            return null;
        }
        double energy = GsonHelper.getAsDouble(json, "energy");
        if (!Double.isFinite(energy) || energy < 0) {
            throw new JsonSyntaxException("energy must be a finite non-negative number");
        }
        return energy;
    }
}
