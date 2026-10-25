package com.murthinext.ae2pr.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 多方块配方的序列化模板，统一处理公共字段。
 */
public abstract class AbstractMultiblockRecipeSerializer<T extends AbstractMultiblockRecipe>
        implements RecipeSerializer<T> {

    private final int defaultDuration;

    protected AbstractMultiblockRecipeSerializer(int defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    @Override
    public final T fromJson(ResourceLocation id, JsonObject json) {
        RecipeDraft draft = RecipeJson.readDraft(json, defaultDuration);
        validate(draft, json);
        return create(id, draft, json);
    }

    @Override
    public final T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return createFromNetwork(id, RecipeNetwork.readDraft(buffer), buffer);
    }

    @Override
    public final void toNetwork(FriendlyByteBuf buffer, T recipe) {
        RecipeNetwork.writeDraft(buffer, recipe.toDraft());
        writeExtra(buffer, recipe);
    }

    /**
     * 公共字段以外的校验。
     */
    protected void validate(RecipeDraft draft, JsonObject json) {
        if (draft.itemInputs().isEmpty() && draft.fluidInputs().isEmpty()) {
            throw new JsonSyntaxException("Recipe requires at least one input");
        }
        if (draft.itemOutputs().isEmpty() && draft.fluidOutputs().isEmpty()) {
            throw new JsonSyntaxException("Recipe requires at least one output");
        }
        if (energyRequired() && draft.energy() == null) {
            throw new JsonSyntaxException("energy is required for this recipe");
        }
    }

    protected boolean energyRequired() {
        return false;
    }

    protected abstract T create(ResourceLocation id, RecipeDraft draft, JsonObject json);

    protected abstract T createFromNetwork(ResourceLocation id, RecipeDraft draft, FriendlyByteBuf buffer);

    protected void writeExtra(FriendlyByteBuf buffer, T recipe) {
    }
}
