package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

public record JigRecipe(ResourceLocation id, List<CountedIngredient> itemInputs, List<FluidStack> fluidInputs,
        List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration,
        @Nullable Double energyPerParallel) implements Recipe<Container>, MultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    public JigRecipe {
        itemInputs = List.copyOf(itemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
    }

    @Override
    public List<CountedIngredient> getItemInputs() {
        return itemInputs;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public double getEnergyPerParallel() {
        return energyPerParallel != null ? energyPerParallel : Config.jigEnergyPerParallel();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return itemOutputs.isEmpty() ? ItemStack.EMPTY : itemOutputs.get(0);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.JIG.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.JIG_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<JigRecipe> {

        @Override
        public JigRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> itemInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject input = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(input, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs count must be >= 1");
                }
                itemInputs.add(new CountedIngredient(Ingredient.fromJson(input), count));
            }
            List<FluidStack> fluidInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_inputs", new JsonArray())) {
                fluidInputs.add(readFluid(element.getAsJsonObject()));
            }
            List<ItemStack> itemOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_outputs", new JsonArray())) {
                JsonObject output = element.getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(output, "item"));
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                int count = GsonHelper.getAsInt(output, "count", 1);
                if (item == null || item == Items.AIR || count < 1) {
                    throw new JsonSyntaxException("Invalid item output: " + itemId);
                }
                itemOutputs.add(new ItemStack(item, count));
            }
            List<FluidStack> fluidOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_outputs", new JsonArray())) {
                fluidOutputs.add(readFluid(element.getAsJsonObject()));
            }
            if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
                throw new JsonSyntaxException("Recipe requires at least one input");
            }
            if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                throw new JsonSyntaxException("Recipe requires at least one output");
            }
            int duration = GsonHelper.getAsInt(json, "duration", DEFAULT_DURATION);
            if (duration < 1) {
                throw new JsonSyntaxException("duration must be >= 1");
            }
            Double energy = json.has("energy") ? GsonHelper.getAsDouble(json, "energy") : null;
            if (energy != null && (!Double.isFinite(energy) || energy < 0.0)) {
                throw new JsonSyntaxException("energy must be finite and >= 0");
            }
            return new JigRecipe(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, energy);
        }

        private static FluidStack readFluid(JsonObject json) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            int amount = GsonHelper.getAsInt(json, "amount", 1000);
            if (fluid == null || fluid == Fluids.EMPTY || amount < 1) {
                throw new JsonSyntaxException("Invalid fluid: " + fluidId);
            }
            return new FluidStack(fluid, amount);
        }

        @Override
        public JigRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<CountedIngredient> itemInputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                itemInputs.add(new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            count = buffer.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                fluidInputs.add(buffer.readFluidStack());
            }
            count = buffer.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack stack = buffer.readItem();
                stack.setCount(buffer.readVarInt());
                itemOutputs.add(stack);
            }
            count = buffer.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                fluidOutputs.add(buffer.readFluidStack());
            }
            Double energy = buffer.readBoolean() ? buffer.readDouble() : null;
            return new JigRecipe(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, buffer.readVarInt(), energy);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, JigRecipe recipe) {
            buffer.writeVarInt(recipe.itemInputs.size());
            for (CountedIngredient input : recipe.itemInputs) {
                input.ingredient().toNetwork(buffer);
                buffer.writeVarInt(input.count());
            }
            buffer.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack input : recipe.fluidInputs) {
                buffer.writeFluidStack(input);
            }
            buffer.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack output : recipe.itemOutputs) {
                buffer.writeItem(output.copyWithCount(1));
                buffer.writeVarInt(output.getCount());
            }
            buffer.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack output : recipe.fluidOutputs) {
                buffer.writeFluidStack(output);
            }
            buffer.writeBoolean(recipe.energyPerParallel != null);
            if (recipe.energyPerParallel != null) {
                buffer.writeDouble(recipe.energyPerParallel);
            }
            buffer.writeVarInt(recipe.duration);
        }
    }
}
