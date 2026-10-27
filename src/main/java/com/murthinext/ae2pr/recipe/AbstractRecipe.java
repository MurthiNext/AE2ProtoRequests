package com.murthinext.ae2pr.recipe;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

public abstract class AbstractRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final List<CountedIngredient> itemInputs;

    protected AbstractRecipe(ResourceLocation id, List<CountedIngredient> itemInputs) {
        this.id = id;
        this.itemInputs = List.copyOf(itemInputs);
    }

    public final List<CountedIngredient> getItemInputs() {
        return itemInputs;
    }

    public abstract List<ItemStack> getItemOutputs();

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> vanilla = NonNullList.create();
        itemInputs.forEach(entry -> vanilla.add(entry.ingredient()));
        return vanilla;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        List<ItemStack> outputs = getItemOutputs();
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
