package com.murthinext.ae2pr.data;

import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipeBuilder;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModMaterials;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

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
            var ingot = material.item(Form.INGOT);
            if (ingot.isEmpty()) {
                continue;
            }

            TagKey<Item> ingotTag = Form.INGOT.tag(material.id());

            material.item(Form.DUST).ifPresent(dust -> {
                TagKey<Item> dustTag = Form.DUST.tag(material.id());

                SimpleCookingRecipeBuilder.smelting(
                                Ingredient.of(dustTag),
                                RecipeCategory.MISC,
                                ingot.get().get(),
                                0.7F,
                                200)
                        .unlockedBy("has_dust", has(dustTag))
                        .save(writer, Ids.of("smelting/" + material.id() + "_dust"));
                SimpleCookingRecipeBuilder.blasting(
                                Ingredient.of(dustTag),
                                RecipeCategory.MISC,
                                ingot.get().get(),
                                0.7F,
                                100)
                        .unlockedBy("has_dust", has(dustTag))
                        .save(writer, Ids.of("blasting/" + material.id() + "_dust"));

                InscriberRecipeBuilder.inscribe(ingotTag, dust.get(), 1)
                        .setMode(InscriberProcessType.INSCRIBE)
                        .save(writer, Ids.of("inscriber/" + material.id() + "_dust"));
            });

            material.item(Form.PLATE)
                    .ifPresent(plate -> InscriberRecipeBuilder.inscribe(ingotTag, plate.get(), 1)
                            .setTop(Ingredient.of(ModItems.SINGLE_PLATE_PRESS.get()))
                            .setMode(InscriberProcessType.INSCRIBE)
                            .save(writer, Ids.of("inscriber/" + material.id() + "_plate")));
        }

        // 紫水晶碎片研磨成粉
        InscriberRecipeBuilder.inscribe(
                        Ingredient.of(Items.AMETHYST_SHARD),
                        ModMaterials.AMETHYST.item(Form.DUST).orElseThrow().get(),
                        1)
                .setMode(InscriberProcessType.INSCRIBE)
                .save(writer, Ids.of("inscriber/amethyst_dust"));
    }
}
