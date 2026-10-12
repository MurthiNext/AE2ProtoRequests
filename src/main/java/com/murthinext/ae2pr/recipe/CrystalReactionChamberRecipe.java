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
public class CrystalReactionChamberRecipe implements Recipe<Container> {

    public static final int DEFAULT_DURATION = 200;

    /**
     * 物品输入：匹配器与单次消耗数量。
     */
    public record ItemInput(Ingredient ingredient, int count) {

        public boolean test(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack);
        }
    }

    private final ResourceLocation id;
    private final List<ItemInput> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    @Nullable
    private final Double energyPerParallel;

    public CrystalReactionChamberRecipe(ResourceLocation id, List<ItemInput> itemInputs, List<FluidStack> fluidInputs,
            List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration,
            @Nullable Double energyPerParallel) {
        this.id = id;
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.itemOutputs = List.copyOf(itemOutputs);
        this.fluidOutputs = List.copyOf(fluidOutputs);
        this.duration = duration;
        this.energyPerParallel = energyPerParallel;
    }

    public List<ItemInput> getItemInputs() {
        return itemInputs;
    }

    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    public int getDuration() {
        return duration;
    }

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
            List<ItemInput> itemInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs 的 count 必须 >= 1");
                }
                itemInputs.add(new ItemInput(Ingredient.fromJson(obj), count));
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
                    throw new JsonSyntaxException("未知物品：" + itemId);
                }
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_outputs 的 count 必须 >= 1");
                }
                itemOutputs.add(new ItemStack(item, count));
            }

            List<FluidStack> fluidOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_outputs", new JsonArray())) {
                fluidOutputs.add(readFluid(element.getAsJsonObject(), "fluid_outputs"));
            }

            if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
                throw new JsonSyntaxException("配方至少需要一个输入");
            }
            if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                throw new JsonSyntaxException("配方至少需要一个产物");
            }
            int duration = GsonHelper.getAsInt(json, "duration", DEFAULT_DURATION);
            if (duration < 1) {
                throw new JsonSyntaxException("duration 必须 >= 1");
            }
            Double energyPerParallel = null;
            if (json.has("energy_per_parallel")) {
                energyPerParallel = GsonHelper.getAsDouble(json, "energy_per_parallel");
                if (energyPerParallel < 0) {
                    throw new JsonSyntaxException("energy_per_parallel 必须 >= 0");
                }
            }
            return new CrystalReactionChamberRecipe(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration,
                    energyPerParallel);
        }

        private static FluidStack readFluid(JsonObject obj, String key) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(obj, "fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException(key + " 的流体不存在：" + fluidId);
            }
            int amount = GsonHelper.getAsInt(obj, "amount", 1000);
            if (amount < 1) {
                throw new JsonSyntaxException(key + " 的 amount 必须 >= 1");
            }
            return new FluidStack(fluid, amount);
        }

        @Override
        public CrystalReactionChamberRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int itemInputCount = buffer.readVarInt();
            List<ItemInput> itemInputs = new ArrayList<>(itemInputCount);
            for (int i = 0; i < itemInputCount; i++) {
                itemInputs.add(new ItemInput(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
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
            for (ItemInput input : recipe.itemInputs) {
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
