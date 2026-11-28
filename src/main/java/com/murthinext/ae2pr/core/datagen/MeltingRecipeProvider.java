package com.murthinext.ae2pr.core.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerMode;
import com.murthinext.ae2pr.core.registry.ModMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;

public final class MeltingRecipeProvider implements DataProvider {

    private static final int MB_PER_UNIT = 144;
    private static final int ENERGY_PER_UNIT = 50_000;
    private static final MatterPhaseTransformerMode MODE = MatterPhaseTransformerMode.MELTING;

    private final PackOutput.PathProvider recipes;

    public MeltingRecipeProvider(PackOutput output) {
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (Material material : ModMaterials.ALL) {
            if (!material.hasFluid()) {
                continue;
            }
            for (MaterialForm form : material.forms()) {
                writes.add(save(cache, material, form));
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, Material material, MaterialForm form) {
        ResourceLocation fluid = Ids.of(material.fluid().orElseThrow().id());
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", MODE.recipeTypeId().toString());
        recipe.addProperty("energy", (long) form.amountMb() * ENERGY_PER_UNIT / MB_PER_UNIT);
        if (form.optional()) {
            var condition = new NotCondition(new TagEmptyCondition(form.ingredientId()));
            recipe.add("conditions", array(CraftingHelper.serialize(condition)));
        }

        JsonObject input = new JsonObject();
        input.addProperty(form.usesTag() ? "tag" : "item", form.ingredientId().toString());
        input.addProperty("count", 1);
        recipe.add("item_inputs", array(input));

        JsonObject output = new JsonObject();
        output.addProperty("fluid", fluid.toString());
        output.addProperty("amount", form.amountMb());
        recipe.add("fluid_outputs", array(output));

        var id = MODE.recipeId(fluid.getPath() + "_from_" + form.form().name());
        return DataProvider.saveStable(cache, recipe, recipes.json(id));
    }

    private static JsonArray array(JsonObject entry) {
        JsonArray array = new JsonArray();
        array.add(entry);
        return array;
    }

    @Override
    public String getName() {
        return "AE2 Proto Requests Melting Recipes";
    }
}
