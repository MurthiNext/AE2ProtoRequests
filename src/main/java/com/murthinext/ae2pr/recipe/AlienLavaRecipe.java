package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ModTags;

/** 异星熔岩世界交互配方：物品实体浸入指定流体后，按数量消耗并产出。 */
public class AlienLavaRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final List<CountedIngredient> ingredients;
    private final TagKey<Fluid> catalyst;
    private final ItemStack resultItem;
    @Nullable
    private final Fluid resultFluid;

    public AlienLavaRecipe(ResourceLocation id, List<CountedIngredient> ingredients, TagKey<Fluid> catalyst,
            ItemStack resultItem, @Nullable Fluid resultFluid) {
        this.id = id;
        this.ingredients = List.copyOf(ingredients);
        this.catalyst = catalyst;
        this.resultItem = resultItem;
        this.resultFluid = resultFluid;
    }

    /** 该流体状态是否可作为本配方的催化剂。 */
    public boolean isCatalyst(FluidState state) {
        return state.is(catalyst);
    }

    /** 触发物品是否匹配首个原料（性能快速短路，仿照 AE2 的转化逻辑）。 */
    public boolean canTrigger(ItemStack stack) {
        return !ingredients.isEmpty() && ingredients.get(0).ingredient().test(stack);
    }

    public List<CountedIngredient> getCountedIngredients() {
        return ingredients;
    }

    public TagKey<Fluid> getCatalyst() {
        return catalyst;
    }

    public ItemStack getResultItem() {
        return resultItem;
    }

    @Nullable
    public Fluid getResultFluid() {
        return resultFluid;
    }

    /** 是否把催化剂源方块转化为结果流体，而不是生成物品。 */
    public boolean convertsCatalyst() {
        return resultFluid != null;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return resultItem.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return resultItem;
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
        return ModRecipes.ALIEN_LAVA.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALIEN_LAVA_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /** JSON 与网络序列化。 */
    public static class Serializer implements RecipeSerializer<AlienLavaRecipe> {

        @Override
        public AlienLavaRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
                JsonObject entry = element.getAsJsonObject();
                int count = GsonHelper.getAsInt(entry, "count", 1);
                ingredients.add(new CountedIngredient(Ingredient.fromJson(entry), count));
            }
            TagKey<Fluid> catalyst = TagKey.create(Registries.FLUID,
                    new ResourceLocation(GsonHelper.getAsString(json, "catalyst", ModTags.ALIEN_LAVA.location().toString())));
            ItemStack resultItem = json.has("result")
                    ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"))
                    : ItemStack.EMPTY;
            Fluid resultFluid = json.has("result_fluid")
                    ? ForgeRegistries.FLUIDS.getValue(
                            new ResourceLocation(GsonHelper.getAsString(json, "result_fluid")))
                    : null;
            return new AlienLavaRecipe(id, ingredients, catalyst, resultItem, resultFluid);
        }

        @Nullable
        @Override
        public AlienLavaRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int count = buffer.readVarInt();
                ingredients.add(new CountedIngredient(Ingredient.fromNetwork(buffer), count));
            }
            TagKey<Fluid> catalyst = TagKey.create(Registries.FLUID, buffer.readResourceLocation());
            ItemStack resultItem = buffer.readItem();
            Fluid resultFluid = buffer.readBoolean()
                    ? ForgeRegistries.FLUIDS.getValue(buffer.readResourceLocation())
                    : null;
            return new AlienLavaRecipe(id, ingredients, catalyst, resultItem, resultFluid);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AlienLavaRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient entry : recipe.ingredients) {
                buffer.writeVarInt(entry.count());
                entry.ingredient().toNetwork(buffer);
            }
            buffer.writeResourceLocation(recipe.catalyst.location());
            buffer.writeItem(recipe.resultItem);
            buffer.writeBoolean(recipe.resultFluid != null);
            if (recipe.resultFluid != null) {
                buffer.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(recipe.resultFluid));
            }
        }
    }
}
