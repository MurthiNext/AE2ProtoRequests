package com.murthinext.ae2pr.api.machine;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.api.recipe.AbstractMultiblockRecipe;
import com.murthinext.ae2pr.api.recipe.AbstractMultiblockRecipeSerializer;
import com.murthinext.ae2pr.api.recipe.RecipeDraft;

public final class MachineRecipe extends AbstractMultiblockRecipe {

    private final RecipeDefinition<MachineRecipe> definition;

    private MachineRecipe(RecipeDefinition<MachineRecipe> definition, ResourceLocation id, RecipeDraft draft) {
        super(id, draft);
        this.definition = definition;
    }

    public RecipeDefinition<MachineRecipe> definition() {
        return definition;
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return definition.defaultEnergy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return definition.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return definition.type();
    }

    public static final class Serializer extends AbstractMultiblockRecipeSerializer<MachineRecipe> {

        private final RecipeDefinition<MachineRecipe> definition;
        private final RecipeConstraints constraints;

        public Serializer(RecipeDefinition<MachineRecipe> definition, int defaultDuration,
                RecipeConstraints constraints) {
            super(defaultDuration);
            this.definition = definition;
            this.constraints = constraints;
        }

        @Override
        protected boolean energyRequired() {
            return constraints.energyRequired();
        }

        @Override
        protected void validate(RecipeDraft draft, JsonObject json) {
            super.validate(draft, json);
            constraints.validate(draft);
        }

        @Override
        protected MachineRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new MachineRecipe(definition, id, draft);
        }

        @Override
        protected MachineRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft, FriendlyByteBuf buffer) {
            return new MachineRecipe(definition, id, draft);
        }
    }
}
