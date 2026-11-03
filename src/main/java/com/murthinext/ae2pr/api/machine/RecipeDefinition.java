package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineRegistries;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.api.recipe.AbstractMultiblockRecipe;

public final class RecipeDefinition<R extends AbstractMultiblockRecipe> {

    private final RegistryObject<RecipeType<R>> type;
    private final RegistryObject<RecipeSerializer<R>> serializer;
    private final DoubleSupplier defaultEnergy;

    private RecipeDefinition(String id, DoubleSupplier defaultEnergy,
            Factory<R> serializerFactory) {
        this.defaultEnergy = defaultEnergy;
        type = registerType(id);
        serializer = registerSerializer(id, serializerFactory);
    }

    public static RecipeDefinition<MachineRecipe> simple(String id, int defaultDuration, DoubleSupplier defaultEnergy) {
        return simple(id, defaultDuration, defaultEnergy, RecipeConstraints.NONE);
    }

    public static RecipeDefinition<MachineRecipe> simple(String id, int defaultDuration, DoubleSupplier defaultEnergy,
            RecipeConstraints constraints) {
        return new RecipeDefinition<>(id, defaultEnergy,
                definition -> new MachineRecipe.Serializer(definition, defaultDuration, constraints));
    }

    public static <R extends AbstractMultiblockRecipe> RecipeDefinition<R> custom(String id,
            Supplier<RecipeSerializer<R>> serializerFactory) {
        return new RecipeDefinition<>(id, () -> 0.0D, definition -> serializerFactory.get());
    }

    @SuppressWarnings("unchecked")
    private RegistryObject<RecipeType<R>> registerType(String id) {
        DeferredRegister<RecipeType<?>> registry = MachineRegistries.recipeTypes();
        return (RegistryObject<RecipeType<R>>) (RegistryObject<?>) registry.register(id,
                (Supplier<RecipeType<?>>) (Supplier<?>) () -> RecipeType.simple(Ids.of(id)));
    }

    @SuppressWarnings("unchecked")
    private RegistryObject<RecipeSerializer<R>> registerSerializer(String id,
            Factory<R> serializerFactory) {
        DeferredRegister<RecipeSerializer<?>> registry = MachineRegistries.recipeSerializers();
        return (RegistryObject<RecipeSerializer<R>>) (RegistryObject<?>) registry.register(id,
                (Supplier<RecipeSerializer<?>>) (Supplier<?>) () -> serializerFactory.create(this));
    }

    public RecipeType<R> type() {
        return type.get();
    }

    public RecipeSerializer<R> serializer() {
        return serializer.get();
    }

    public double defaultEnergy() {
        return defaultEnergy.getAsDouble();
    }

    @FunctionalInterface
    public interface Factory<R extends AbstractMultiblockRecipe> {
        RecipeSerializer<R> create(RecipeDefinition<R> definition);
    }
}
