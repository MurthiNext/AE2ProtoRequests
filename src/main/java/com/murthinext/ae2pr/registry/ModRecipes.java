package com.murthinext.ae2pr.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;

public final class ModRecipes {

    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
                                                                                               ae2pr.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, ae2pr.MODID);

    public static final RegistryObject<RecipeType<ComplexWorldInteractionRecipe>>
            COMPLEX_WORLD_INTERACTION_TYPE = RECIPE_TYPES.register(
                    "complex_world_interaction", () -> RecipeType.simple(Ids.of("complex_world_interaction")));

    public static final RegistryObject<ComplexWorldInteractionRecipe.Serializer>
            COMPLEX_WORLD_INTERACTION = SERIALIZERS.register("complex_world_interaction",
                                                             ComplexWorldInteractionRecipe.Serializer::new);
}
