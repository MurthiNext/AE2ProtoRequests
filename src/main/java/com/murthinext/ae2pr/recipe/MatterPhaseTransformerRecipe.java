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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModRecipes;

/**
 * 物质相变仪配方。
 */
public class MatterPhaseTransformerRecipe implements Recipe<Container>, MultiblockRecipe {

    public static final int DEFAULT_DURATION = 200;

    private final ResourceLocation id;
    private final List<CountedIngredient> ingredients;
    private final FluidStack fluidOutput;
    private final int duration;
    @Nullable
    private final Double energyPerParallel;

    public MatterPhaseTransformerRecipe(ResourceLocation id, List<CountedIngredient> ingredients, FluidStack fluidOutput,
            int duration, @Nullable Double energyPerParallel) {
        this.id = id;
        this.ingredients = List.copyOf(ingredients);
        this.fluidOutput = fluidOutput.copy();
        this.duration = duration;
        this.energyPerParallel = energyPerParallel;
    }

    @Override
    public List<CountedIngredient> getItemInputs() {
        return ingredients;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return List.of();
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return List.of();
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return List.of(fluidOutput);
    }

    /**
     * 唯一流体产物的副本。
     */
    public FluidStack getFluidOutput() {
        return fluidOutput.copy();
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public double getEnergyPerParallel() {
        return energyPerParallel != null ? energyPerParallel : Config.matterPhaseTransformerEnergyPerParallel();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
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
        return ModRecipes.MATTER_PHASE_TRANSFORMER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MATTER_PHASE_TRANSFORMER_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /**
     * JSON 与网络序列化。
     */
    public static class Serializer implements RecipeSerializer<MatterPhaseTransformerRecipe> {

        @Override
        public MatterPhaseTransformerRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs")) {
                JsonObject entry = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(entry, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("ingredients 的 count 必须 >= 1");
                }
                ingredients.add(new CountedIngredient(Ingredient.fromJson(entry), count));
            }
            if (ingredients.isEmpty()) {
                throw new JsonSyntaxException("配方至少需要一个输入");
            }

            JsonArray outputs = GsonHelper.getAsJsonArray(json, "fluid_outputs");
            if (outputs.size() != 1) {
                throw new JsonSyntaxException("配方必须恰有一个流体输出");
            }
            JsonObject output = outputs.get(0).getAsJsonObject();
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(
                    ResourceLocation.parse(GsonHelper.getAsString(output, "fluid")));
            if (fluid == null) {
                throw new JsonSyntaxException("fluid_output 的流体不存在：" + output.get("fluid"));
            }
            int amount = GsonHelper.getAsInt(output, "amount", 1);
            if (amount < 1) {
                throw new JsonSyntaxException("fluid_output 的 amount 必须 >= 1");
            }

            int duration = GsonHelper.getAsInt(json, "duration", DEFAULT_DURATION);
            if (duration < 1) {
                throw new JsonSyntaxException("duration 必须 >= 1");
            }
            Double energyPerParallel = null;
            if (json.has("energy")) {
                energyPerParallel = GsonHelper.getAsDouble(json, "energy");
                if (energyPerParallel < 0) {
                    throw new JsonSyntaxException("energy 必须 >= 0");
                }
            }
            return new MatterPhaseTransformerRecipe(id, ingredients, new FluidStack(fluid, amount), duration, energyPerParallel);
        }

        @Override
        public MatterPhaseTransformerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int count = buffer.readVarInt();
                ingredients.add(new CountedIngredient(Ingredient.fromNetwork(buffer), count));
            }
            CompoundTag fluidTag = buffer.readNbt();
            FluidStack fluidOutput = fluidTag == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(fluidTag);
            int duration = buffer.readVarInt();
            Double energyPerParallel = buffer.readBoolean() ? buffer.readDouble() : null;
            return new MatterPhaseTransformerRecipe(id, ingredients, fluidOutput, duration, energyPerParallel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MatterPhaseTransformerRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient entry : recipe.ingredients) {
                buffer.writeVarInt(entry.count());
                entry.ingredient().toNetwork(buffer);
            }
            buffer.writeNbt(recipe.fluidOutput.writeToNBT(new CompoundTag()));
            buffer.writeVarInt(recipe.duration);
            buffer.writeBoolean(recipe.energyPerParallel != null);
            if (recipe.energyPerParallel != null) {
                buffer.writeDouble(recipe.energyPerParallel);
            }
        }
    }
}
