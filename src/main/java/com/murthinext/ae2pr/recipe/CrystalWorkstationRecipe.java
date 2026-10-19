package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;

public class CrystalWorkstationRecipe implements Recipe<Container>, MultiblockRecipe {

    public static final int MAX_ITEM_INPUTS = 6;
    public static final int MAX_ITEM_OUTPUTS = 6;
    public static final int MAX_FLUIDS = 3;

    private final ResourceLocation id;
    private final CrystalWorkstationMode mode;
    private final List<CountedIngredient> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    private final double energy;

    public CrystalWorkstationRecipe(ResourceLocation id, CrystalWorkstationMode mode,
            List<CountedIngredient> itemInputs, List<FluidStack> fluidInputs, List<ItemStack> itemOutputs,
            List<FluidStack> fluidOutputs, int duration, double energy) {
        this.id = id;
        this.mode = mode;
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.itemOutputs = List.copyOf(itemOutputs);
        this.fluidOutputs = List.copyOf(fluidOutputs);
        this.duration = duration;
        this.energy = energy;
    }

    public CrystalWorkstationMode getMode() {
        return mode;
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
        return energy;
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
        return itemOutputs.isEmpty() ? ItemStack.EMPTY : itemOutputs.get(0).copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
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

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CrystalWorkstationRecipe> {

        private final CrystalWorkstationMode mode;

        public Serializer(CrystalWorkstationMode mode) {
            this.mode = mode;
        }

        @Override
        public CrystalWorkstationRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> itemInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject input = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(input, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs 的 count 必须 >= 1");
                }
                itemInputs.add(new CountedIngredient(Ingredient.fromJson(input), count));
            }
            List<ItemStack> itemOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_outputs", new JsonArray())) {
                JsonObject output = element.getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(output, "item"));
                var item = ForgeRegistries.ITEMS.getValue(itemId);
                int count = GsonHelper.getAsInt(output, "count", 1);
                if (item == null || item == Items.AIR || count < 1) {
                    throw new JsonSyntaxException("无效的物品产物：" + output);
                }
                itemOutputs.add(new ItemStack(item, count));
            }
            List<FluidStack> fluidInputs = readFluids(json, "fluid_inputs");
            List<FluidStack> fluidOutputs = readFluids(json, "fluid_outputs");
            if (itemInputs.isEmpty() && fluidInputs.isEmpty() || itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                throw new JsonSyntaxException("配方必须同时具有输入和产物");
            }
            if (itemInputs.size() > MAX_ITEM_INPUTS || itemOutputs.size() > MAX_ITEM_OUTPUTS
                    || fluidInputs.size() > MAX_FLUIDS || fluidOutputs.size() > MAX_FLUIDS) {
                throw new JsonSyntaxException("晶能工作站最多支持各 6 种物品和 3 种流体输入/输出");
            }
            int duration = GsonHelper.getAsInt(json, "duration", 200);
            double energy = GsonHelper.getAsDouble(json, "energy");
            if (duration < 1 || !Double.isFinite(energy) || energy < 0) {
                throw new JsonSyntaxException("duration 必须 >= 1，energy 必须为有限非负数");
            }
            return new CrystalWorkstationRecipe(id, mode, itemInputs, fluidInputs, itemOutputs, fluidOutputs,
                    duration, energy);
        }

        private static List<FluidStack> readFluids(JsonObject json, String key) {
            List<FluidStack> fluids = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, key, new JsonArray())) {
                JsonObject entry = element.getAsJsonObject();
                ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(entry, "fluid"));
                var fluid = ForgeRegistries.FLUIDS.getValue(id);
                int amount = GsonHelper.getAsInt(entry, "amount", 1000);
                if (fluid == null || fluid == Fluids.EMPTY || amount < 1) {
                    throw new JsonSyntaxException("无效的流体：" + entry);
                }
                fluids.add(new FluidStack(fluid, amount));
            }
            return fluids;
        }

        @Override
        public CrystalWorkstationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            List<CountedIngredient> itemInputs = buffer.readList(
                    buf -> new CountedIngredient(Ingredient.fromNetwork(buf), buf.readVarInt()));
            List<FluidStack> fluidInputs = buffer.readList(FriendlyByteBuf::readFluidStack);
            List<ItemStack> itemOutputs = buffer.readList(buf -> {
                ItemStack stack = buf.readItem();
                stack.setCount(buf.readVarInt());
                return stack;
            });
            List<FluidStack> fluidOutputs = buffer.readList(FriendlyByteBuf::readFluidStack);
            return new CrystalWorkstationRecipe(id, mode, itemInputs, fluidInputs, itemOutputs, fluidOutputs,
                    buffer.readVarInt(), buffer.readDouble());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CrystalWorkstationRecipe recipe) {
            buffer.writeCollection(recipe.itemInputs, (buf, input) -> {
                input.ingredient().toNetwork(buf);
                buf.writeVarInt(input.count());
            });
            buffer.writeCollection(recipe.fluidInputs, FriendlyByteBuf::writeFluidStack);
            buffer.writeCollection(recipe.itemOutputs, (buf, output) -> {
                buf.writeItem(output.copyWithCount(1));
                buf.writeVarInt(output.getCount());
            });
            buffer.writeCollection(recipe.fluidOutputs, FriendlyByteBuf::writeFluidStack);
            buffer.writeVarInt(recipe.duration);
            buffer.writeDouble(recipe.energy);
        }
    }
}
