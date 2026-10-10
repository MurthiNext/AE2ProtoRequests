package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.multiblock.module.ModuleRequirement;
import com.murthinext.ae2pr.multiblock.module.ModuleType;

/**
 * 水晶装配线配方。
 */
public class CrystalAssemblyLineRecipe implements Recipe<Container> {

    /** 物品输入：匹配器 + 单次执行的消耗数量 */
    public record ItemInput(Ingredient ingredient, int count) {

        public boolean test(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack);
        }
    }

    private final ResourceLocation id;
    private final List<ItemInput> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<ModuleRequirement> moduleRequirements;
    private final int duration;
    /** 每并行耗电覆盖值（AE）；null 表示使用配置默认值 */
    @Nullable
    private final Double energyPerParallel;

    public CrystalAssemblyLineRecipe(ResourceLocation id, List<ItemInput> itemInputs, List<FluidStack> fluidInputs,
            List<ItemStack> itemOutputs, List<ModuleRequirement> moduleRequirements, int duration,
            @Nullable Double energyPerParallel) {
        this.id = id;
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.itemOutputs = List.copyOf(itemOutputs);
        this.moduleRequirements = List.copyOf(moduleRequirements);
        this.duration = duration;
        this.energyPerParallel = energyPerParallel;
    }

    /** 物品输入 */
    public List<ItemInput> getItemInputs() {
        return itemInputs;
    }

    /** 流体输入 */
    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    /** 物品产物 */
    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    /** 配方对模块的要求（全部满足才可执行） */
    public List<ModuleRequirement> getModuleRequirements() {
        return moduleRequirements;
    }

    /** 加工耗时（tick） */
    public int getDuration() {
        return duration;
    }

    /** 每并行一次执行消耗的能量（AE）；未在配方中设置时使用配置默认值。 */
    public double getEnergyPerParallel() {
        return energyPerParallel != null ? energyPerParallel : Config.assemblyEnergyPerParallel();
    }

    // ---------------------------------------------------------------- Recipe 接口（本模组自行匹配，不走原版容器流程）

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
        return ModRecipes.CRYSTAL_ASSEMBLY_LINE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRYSTAL_ASSEMBLY_LINE_TYPE.get();
    }

    /** JSON 与网络序列化。 */
    public static class Serializer implements RecipeSerializer<CrystalAssemblyLineRecipe> {

        @Override
        public CrystalAssemblyLineRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<ItemInput> itemInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_inputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(obj);
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count < 1) {
                    throw new JsonSyntaxException("item_inputs 的 count 必须 >= 1");
                }
                itemInputs.add(new ItemInput(ingredient, count));
            }

            List<FluidStack> fluidInputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "fluid_inputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(obj, "fluid"));
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
                if (fluid == null || fluid == Fluids.EMPTY) {
                    throw new JsonSyntaxException("未知流体：" + fluidId);
                }
                int amount = GsonHelper.getAsInt(obj, "amount", 1000);
                if (amount < 1) {
                    throw new JsonSyntaxException("fluid_inputs 的 amount 必须 >= 1");
                }
                fluidInputs.add(new FluidStack(fluid, amount));
            }

            List<ItemStack> itemOutputs = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_outputs", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(obj, "item"));
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

            List<ModuleRequirement> moduleRequirements = new ArrayList<>();
            Set<ResourceLocation> seenModules = new HashSet<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "module_requirements", new JsonArray())) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation moduleId = new ResourceLocation(GsonHelper.getAsString(obj, "module"));
                ModuleType type = ModModules.byId(moduleId);
                if (type == null) {
                    throw new JsonSyntaxException("未知模块：" + moduleId);
                }
                if (!seenModules.add(moduleId)) {
                    throw new JsonSyntaxException("重复的模块要求：" + moduleId);
                }
                int minLevel = GsonHelper.getAsInt(obj, "min_level", 1);
                if (minLevel < 1) {
                    throw new JsonSyntaxException("module_requirements 的 min_level 必须 >= 1");
                }
                moduleRequirements.add(new ModuleRequirement(type, minLevel));
            }

            if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
                throw new JsonSyntaxException("配方至少需要一个输入");
            }
            if (itemOutputs.isEmpty()) {
                throw new JsonSyntaxException("配方至少需要一个物品输出");
            }
            int duration = GsonHelper.getAsInt(json, "duration", 100);
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
            return new CrystalAssemblyLineRecipe(id, itemInputs, fluidInputs, itemOutputs, moduleRequirements, duration,
                    energyPerParallel);
        }

        @Override
        public CrystalAssemblyLineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int itemCount = buffer.readVarInt();
            List<ItemInput> itemInputs = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                itemInputs.add(new ItemInput(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            int fluidCount = buffer.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidCount);
            for (int i = 0; i < fluidCount; i++) {
                fluidInputs.add(buffer.readFluidStack());
            }
            int outputCount = buffer.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(outputCount);
            for (int i = 0; i < outputCount; i++) {
                ItemStack stack = buffer.readItem();
                stack.setCount(buffer.readVarInt());
                itemOutputs.add(stack);
            }
            int requirementCount = buffer.readVarInt();
            List<ModuleRequirement> moduleRequirements = new ArrayList<>(requirementCount);
            for (int i = 0; i < requirementCount; i++) {
                ResourceLocation moduleId = buffer.readResourceLocation();
                ModuleType type = ModModules.byId(moduleId);
                if (type == null) {
                    throw new IllegalArgumentException("未知模块：" + moduleId);
                }
                moduleRequirements.add(new ModuleRequirement(type, buffer.readVarInt()));
            }
            Double energyPerParallel = buffer.readBoolean() ? buffer.readDouble() : null;
            return new CrystalAssemblyLineRecipe(id, itemInputs, fluidInputs, itemOutputs, moduleRequirements,
                    buffer.readVarInt(), energyPerParallel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CrystalAssemblyLineRecipe recipe) {
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
            buffer.writeVarInt(recipe.moduleRequirements.size());
            for (ModuleRequirement requirement : recipe.moduleRequirements) {
                buffer.writeResourceLocation(requirement.type().id());
                buffer.writeVarInt(requirement.minLevel());
            }
            buffer.writeBoolean(recipe.energyPerParallel != null);
            if (recipe.energyPerParallel != null) {
                buffer.writeDouble(recipe.energyPerParallel);
            }
            buffer.writeVarInt(recipe.duration);
        }
    }
}
