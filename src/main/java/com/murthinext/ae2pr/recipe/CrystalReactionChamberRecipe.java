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

/**
 * 水晶反应仓配方。
 */
public class CrystalReactionChamberRecipe implements Recipe<Container>, MultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    private final ResourceLocation id;
    private final List<CountedIngredient> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    @Nullable
    private final Double energyPerParallel;

    public CrystalReactionChamberRecipe(ResourceLocation id, List<CountedIngredient> itemInputs,
            List<FluidStack> fluidInputs, List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration,
            @Nullable Double energyPerParallel) {
        this.id = id;
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.itemOutputs = List.copyOf(itemOutputs);
        this.fluidOutputs = List.copyOf(fluidOutputs);
        this.duration = duration;
        this.energyPerParallel = energyPerParallel;
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
        return energyPerParallel != null ? energyPerParallel : Config.crystalReactionEnergyPerParallel();
    }

    // Recipe 接口

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
        return ModRecipes.CRYSTAL_REACTION_CHAMBER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRYSTAL_REACTION_CHAMBER_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * JSON 与网络序列化。
     */
    public static class Serializer implements RecipeSerializer<CrystalReactionChamberRecipe> {

        @Override
        public CrystalReactionChamberRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> itemInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs count must be >= 1");
                }
                itemInputs.add(new CountedIngredient(Ingredient.fromJson(obj), count));
            }

            List<FluidStack> fluidInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_inputs", new JsonArray())) {
                fluidInputs.add(readFluid(element.getAsJsonObject(), "fluid_inputs"));
            }

            List<ItemStack> itemOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_outputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(obj, "item"));
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null || item == Items.AIR) {
                    throw new JsonSyntaxException("Unknown item: " + itemId);
                }
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_outputs count must be >= 1");
                }
                itemOutputs.add(new ItemStack(item, count));
            }

            List<FluidStack> fluidOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_outputs", new JsonArray())) {
                fluidOutputs.add(readFluid(element.getAsJsonObject(), "fluid_outputs"));
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
            Double energyPerParallel = null;
            if (json.has("energy")) {
                energyPerParallel = GsonHelper.getAsDouble(json, "energy");
                if (energyPerParallel < 0) {
                    throw new JsonSyntaxException("energy must be >= 0");
                }
            }
            return new CrystalReactionChamberRecipe(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration,
                    energyPerParallel);
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
        public CrystalReactionChamberRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int itemInputCount = buffer.readVarInt();
            List<CountedIngredient> itemInputs = new ArrayList<>(itemInputCount);
            for (int i = 0; i < itemInputCount; i++) {
                itemInputs.add(new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            int fluidInputCount = buffer.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidInputCount);
            for (int i = 0; i < fluidInputCount; i++) {
                fluidInputs.add(buffer.readFluidStack());
            }
            int itemOutputCount = buffer.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(itemOutputCount);
            for (int i = 0; i < itemOutputCount; i++) {
                ItemStack stack = buffer.readItem();
                stack.setCount(buffer.readVarInt());
                itemOutputs.add(stack);
            }
            int fluidOutputCount = buffer.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutputCount);
            for (int i = 0; i < fluidOutputCount; i++) {
                fluidOutputs.add(buffer.readFluidStack());
            }
            Double energyPerParallel = buffer.readBoolean() ? buffer.readDouble() : null;
            return new CrystalReactionChamberRecipe(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs,
                    buffer.readVarInt(), energyPerParallel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CrystalReactionChamberRecipe recipe) {
            buffer.writeVarInt(recipe.itemInputs.size());
            for (CountedIngredient input : recipe.itemInputs) {
                input.ingredient().toNetwork(buffer);
                buffer.writeVarInt(input.count());
            }
            buffer.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack fluid : recipe.fluidInputs) {
                buffer.writeFluidStack(fluid);
            }
            buffer.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack output : recipe.itemOutputs) {
                buffer.writeItem(output.copyWithCount(1));
                buffer.writeVarInt(output.getCount());
            }
            buffer.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack fluid : recipe.fluidOutputs) {
                buffer.writeFluidStack(fluid);
            }
            buffer.writeBoolean(recipe.energyPerParallel != null);
            if (recipe.energyPerParallel != null) {
                buffer.writeDouble(recipe.energyPerParallel);
            }
            buffer.writeVarInt(recipe.duration);
        }
    }
}
