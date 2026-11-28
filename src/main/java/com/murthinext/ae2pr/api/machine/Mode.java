package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.Ids;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public interface Mode {

    String machineId();

    default String id() {
        return ((Enum<?>) this).name().toLowerCase(Locale.ROOT);
    }

    default String translationKey() {
        return "gui.ae2pr." + machineId() + ".mode." + id();
    }

    @Nullable
    RecipeSpec<?> recipe();

    default RecipeType<?> recipeType() {
        return MachineRecipeTypes.require(this);
    }

    default ResourceLocation recipeTypeId() {
        return Ids.of(machineId() + "_" + id());
    }

    default ResourceLocation recipeId(String name) {
        return Ids.of(machineId() + "/" + id() + "/" + name);
    }

    default Component displayName() {
        return Component.translatable(translationKey());
    }
}
