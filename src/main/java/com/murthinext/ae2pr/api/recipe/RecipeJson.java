package com.murthinext.ae2pr.api.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.murthinext.ae2pr.api.EnergyAmount;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class RecipeJson {

    private RecipeJson() {}

    public static RecipeDraft readDraft(JsonObject json, int defaultDuration) {
        return new RecipeDraft(
                readItemInputs(json, "item_inputs"),
                readFluidInputs(json, "fluid_inputs"),
                readItemOutputs(json, "item_outputs"),
                readFluidOutputs(json, "fluid_outputs"),
                readDuration(json, defaultDuration),
                readEnergy(json));
    }

    public static List<ItemInput> readItemInputs(JsonObject json, String key) {
        List<ItemInput> inputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            int count = GsonHelper.getAsInt(entry, "count", 1);
            Ingredient ingredient = Ingredient.fromJson(entry);
            if (count < 1 || ingredient.isEmpty()) {
                throw new JsonSyntaxException(key + " requires non-empty ingredients and positive counts");
            }
            inputs.add(new ItemInput(ingredient, count, GsonHelper.getAsBoolean(entry, "catalyst", false)));
        }
        return inputs;
    }

    public static List<FluidInput> readFluidInputs(JsonObject json, String key) {
        List<FluidInput> inputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            inputs.add(new FluidInput(readFluidStack(entry, key), GsonHelper.getAsBoolean(entry, "catalyst", false)));
        }
        return inputs;
    }

    public static List<ItemOutput> readItemOutputs(JsonObject json, String key) {
        List<ItemOutput> outputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            outputs.add(new ItemOutput(readItemStack(entry, key), readChance(entry, key)));
        }
        return outputs;
    }

    public static List<FluidOutput> readFluidOutputs(JsonObject json, String key) {
        List<FluidOutput> outputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
            JsonObject entry = element.getAsJsonObject();
            outputs.add(new FluidOutput(readFluidStack(entry, key), readChance(entry, key)));
        }
        return outputs;
    }

    private static ItemStack readItemStack(JsonObject entry, String key) {
        ItemStack stack = ShapedRecipe.itemStackFromJson(entry);
        if (stack.isEmpty() || stack.getCount() < 1) {
            throw new JsonSyntaxException(key + " requires non-empty stacks and positive counts");
        }
        return stack;
    }

    private static FluidStack readFluidStack(JsonObject entry, String key) {
        ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(entry, "fluid"));
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        int amount = GsonHelper.getAsInt(entry, "amount", 1000);
        if (fluid == null || fluid == Fluids.EMPTY || amount < 1) {
            throw new JsonSyntaxException("Invalid fluid in " + key + ": " + fluidId);
        }
        return new FluidStack(fluid, amount);
    }

    private static float readChance(JsonObject entry, String key) {
        float chance = GsonHelper.getAsFloat(entry, "chance", 1.0F);
        if (!(chance > 0.0F) || chance > 1.0F) {
            throw new JsonSyntaxException(key + " chance must be in (0, 1]");
        }
        return chance;
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
        if (!Double.isFinite(energy) || energy < 0 || energy > EnergyAmount.MAX) {
            throw new JsonSyntaxException("energy must be a finite number in [0, Long.MAX_VALUE]");
        }
        return energy;
    }
}
