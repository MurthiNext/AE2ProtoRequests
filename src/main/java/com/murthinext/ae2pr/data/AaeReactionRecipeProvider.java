package com.murthinext.ae2pr.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * 把 AdvancedAE 反应仓配方转写成水晶反应仓配方，配方中出现的其他模组物品自动补 mod_loaded 条件。
 */
public class AaeReactionRecipeProvider implements DataProvider {

    private static final int DURATION = 100;
    private static final String RECIPE_TYPE = "ae2pr:crystal_reaction_chamber";
    private static final String CONDITIONAL_TYPE = "forge:conditional";
    private static final String MOD_LOADED_CONDITION = "forge:mod_loaded";
    // 这些命名空间由本模组自身与必装依赖提供，无需条件
    private static final Set<String> UNCONDITIONED = Set.of("minecraft", "ae2", "ae2pr", "forge", "c");

    private final PackOutput output;

    public AaeReactionRecipeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path directory = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("ae2pr/recipes/crystal_reaction_chamber");
        Map<String, Integer> usedIds = new HashMap<>();
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (AaeReactionRecipes.Reaction reaction : AaeReactionRecipes.readAll()) {
            JsonObject recipe = convert(reaction);
            Set<String> mods = requiredMods(reaction);
            JsonObject json = mods.isEmpty() ? recipe : conditional(recipe, mods);
            writes.add(DataProvider.saveStable(cache, json,
                    directory.resolve(nextId(reaction, usedIds) + ".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "AE2 Proto Requests AdvancedAE Reaction Chamber Recipes";
    }

    private static JsonObject convert(AaeReactionRecipes.Reaction reaction) {
        JsonObject json = new JsonObject();
        json.addProperty("type", RECIPE_TYPE);
        json.addProperty("duration", DURATION);
        json.addProperty("energy", reaction.energy());

        JsonArray itemInputs = new JsonArray();
        for (JsonObject input : reaction.inputs()) {
            itemInputs.add(convertInput(input));
        }
        if (!itemInputs.isEmpty()) {
            json.add("item_inputs", itemInputs);
        }
        if (reaction.fluid() != null) {
            JsonObject fluid = reaction.fluid().getAsJsonObject("fluidStack");
            JsonArray fluidInputs = new JsonArray();
            fluidInputs.add(fluidEntry(GsonHelper.getAsString(fluid, "FluidName"), GsonHelper.getAsInt(fluid, "Amount")));
            json.add("fluid_inputs", fluidInputs);
        }

        JsonObject output = reaction.output();
        String id = GsonHelper.getAsString(output, "id");
        int amount = GsonHelper.getAsInt(output, "#");
        String kind = GsonHelper.getAsString(output, "#c", "ae2:i");
        JsonArray outputs = new JsonArray();
        if ("ae2:f".equals(kind)) {
            outputs.add(fluidEntry(id, amount));
            json.add("fluid_outputs", outputs);
        } else {
            JsonObject entry = new JsonObject();
            entry.addProperty("item", id);
            entry.addProperty("count", amount);
            outputs.add(entry);
            json.add("item_outputs", outputs);
        }
        return json;
    }

    private static JsonObject convertInput(JsonObject input) {
        JsonObject ingredient = GsonHelper.getAsJsonObject(input, "ingredient");
        JsonObject entry = new JsonObject();
        if (ingredient.has("item")) {
            entry.addProperty("item", GsonHelper.getAsString(ingredient, "item"));
        } else if (ingredient.has("tag")) {
            entry.addProperty("tag", GsonHelper.getAsString(ingredient, "tag"));
        } else {
            throw new IllegalStateException("Unsupported ingredient: " + ingredient);
        }
        entry.addProperty("count", GsonHelper.getAsInt(input, "amount"));
        return entry;
    }

    private static JsonObject fluidEntry(String fluid, int amount) {
        JsonObject entry = new JsonObject();
        entry.addProperty("fluid", fluid);
        entry.addProperty("amount", amount);
        return entry;
    }

    private static JsonObject conditional(JsonObject recipe, Set<String> mods) {
        JsonArray conditions = new JsonArray();
        for (String mod : mods) {
            JsonObject condition = new JsonObject();
            condition.addProperty("type", MOD_LOADED_CONDITION);
            condition.addProperty("modid", mod);
            conditions.add(condition);
        }
        JsonObject entry = new JsonObject();
        entry.add("conditions", conditions);
        entry.add("recipe", recipe);
        JsonArray recipes = new JsonArray();
        recipes.add(entry);
        JsonObject json = new JsonObject();
        json.addProperty("type", CONDITIONAL_TYPE);
        json.add("recipes", recipes);
        return json;
    }

    // 配方里出现的其他模组命名空间都需要对应的模组
    private static Set<String> requiredMods(AaeReactionRecipes.Reaction reaction) {
        Set<String> mods = new LinkedHashSet<>(reaction.conditions());
        for (JsonObject input : reaction.inputs()) {
            collectNamespaces(GsonHelper.getAsJsonObject(input, "ingredient"), mods);
        }
        if (reaction.fluid() != null) {
            JsonObject fluid = reaction.fluid().getAsJsonObject("fluidStack");
            collectNamespace(GsonHelper.getAsString(fluid, "FluidName"), mods);
        }
        collectNamespace(GsonHelper.getAsString(reaction.output(), "id"), mods);
        mods.removeAll(UNCONDITIONED);
        return mods;
    }

    private static void collectNamespaces(JsonObject ingredient, Set<String> mods) {
        for (String key : List.of("item", "tag")) {
            if (ingredient.has(key)) {
                collectNamespace(GsonHelper.getAsString(ingredient, key), mods);
            }
        }
    }

    private static void collectNamespace(String id, Set<String> mods) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            throw new IllegalStateException("Invalid resource location in recipe: " + id);
        }
        mods.add(location.getNamespace());
    }

    // 同名产物按读取顺序加序号，保证输出文件名稳定
    private static String nextId(AaeReactionRecipes.Reaction reaction, Map<String, Integer> usedIds) {
        ResourceLocation outputId = ResourceLocation.parse(GsonHelper.getAsString(reaction.output(), "id"));
        String base = reaction.source() + "_" + outputId.getPath();
        int index = usedIds.merge(base, 1, Integer::sum);
        return index == 1 ? base : base + "_" + index;
    }
}
