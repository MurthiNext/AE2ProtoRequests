package com.murthinext.ae2pr.data;

import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipeBuilder;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialItems;
import com.murthinext.ae2pr.api.material.TagPrefix;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModMaterials;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * 材料配方：
 * - 有锭 + 粉的材料：粉可熔炼/高炉烧成锭，锭可在压印器中直接压成粉；
 * - 有锭 + 板的材料：锭配合单层板压印模板压成板。
 */
public class MaterialRecipeProvider extends RecipeProvider {

    public MaterialRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        for (Material material : ModMaterials.ALL) {
            if (!material.has(TagPrefix.INGOT)) {
                continue;
            }

            Item ingot = MaterialItems.get(TagPrefix.INGOT, material).get();
            TagKey<Item> ingotTag = forgeTag("ingots/" + material.id());

            if (material.has(TagPrefix.DUST)) {
                Item dust = MaterialItems.get(TagPrefix.DUST, material).get();
                TagKey<Item> dustTag = forgeTag("dusts/" + material.id());

                SimpleCookingRecipeBuilder.smelting(Ingredient.of(dustTag), RecipeCategory.MISC, ingot, 0.7F, 200)
                        .unlockedBy("has_dust", has(dustTag))
                        .save(writer, Ids.of("smelting/" + material.id() + "_dust"));
                SimpleCookingRecipeBuilder.blasting(Ingredient.of(dustTag), RecipeCategory.MISC, ingot, 0.7F, 100)
                        .unlockedBy("has_dust", has(dustTag))
                        .save(writer, Ids.of("blasting/" + material.id() + "_dust"));

                InscriberRecipeBuilder.inscribe(ingotTag, dust, 1)
                        .setMode(InscriberProcessType.INSCRIBE)
                        .save(writer, Ids.of("inscriber/" + material.id() + "_dust"));
            }

            if (material.has(TagPrefix.PLATE)) {
                Item plate = MaterialItems.get(TagPrefix.PLATE, material).get();

                InscriberRecipeBuilder.inscribe(ingotTag, plate, 1)
                        .setTop(Ingredient.of(ModItems.SINGLE_PLATE_PRESS.get()))
                        .setMode(InscriberProcessType.INSCRIBE)
                        .save(writer, Ids.of("inscriber/" + material.id() + "_plate"));
            }
        }
    }
    private static TagKey<Item> forgeTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }
}
