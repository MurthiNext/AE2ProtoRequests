package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.recipe.AbstractMultiblockRecipe;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

public record RecipeSpec<R extends AbstractMultiblockRecipe>(
        @Nullable String id,
        int defaultDuration,
        RecipeConstraints constraints,
        @Nullable Supplier<RecipeSerializer<R>> customSerializer) {

    public static RecipeSpec<MachineRecipe> simple(int duration, RecipeConstraints constraints) {
        return new RecipeSpec<>(null, duration, constraints, null);
    }

    public static <R extends AbstractMultiblockRecipe> RecipeSpec<R> custom(
            String id, Supplier<RecipeSerializer<R>> serializer) {
        return new RecipeSpec<>(id, 0, RecipeConstraints.NONE, serializer);
    }

    @SuppressWarnings("unchecked") // 简单配方恒为 MachineRecipe，与 R 一致
    public RecipeDefinition<R> declare(String defaultId) {
        String recipeId = id != null ? id : defaultId;
        if (customSerializer != null) {
            return RecipeDefinition.custom(recipeId, customSerializer);
        }
        return (RecipeDefinition<R>) RecipeDefinition.simple(recipeId, defaultDuration, constraints);
    }
}
