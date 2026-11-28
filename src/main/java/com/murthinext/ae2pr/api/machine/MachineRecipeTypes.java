package com.murthinext.ae2pr.api.machine;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.crafting.RecipeType;

public final class MachineRecipeTypes {

    private static final Map<Mode, RecipeDefinition<?>> BY_MODE = new IdentityHashMap<>();

    private MachineRecipeTypes() {}

    public static synchronized void bind(Mode mode, RecipeDefinition<?> recipe) {
        if (BY_MODE.put(mode, recipe) != null) {
            throw new IllegalStateException("Mode already has a recipe type: " + mode.id());
        }
    }

    public static RecipeType<?> require(Mode mode) {
        RecipeDefinition<?> recipe = BY_MODE.get(mode);
        if (recipe == null) {
            throw new IllegalStateException("Mode has no recipe type: " + mode.id());
        }
        return recipe.type();
    }
}
