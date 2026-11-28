package com.murthinext.ae2pr.core.datagen;

import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipeBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.core.registry.ModItems;
import com.murthinext.ae2pr.core.registry.ModMachines;
import com.murthinext.ae2pr.core.registry.ModMaterials;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerMode;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;

/**
 * 材料配方：
 * - 有锭 + 粉的材料：粉可熔炼/高炉烧成锭，锭可在压印器中直接压成粉；
 * - 有锭 + 板的材料：锭配合单层板压印模板压成板；
 * - 有存储方块的材料：按块内物品数生成 2×2 / 3×3 与分解配方。
 */
public class MaterialRecipeProvider extends RecipeProvider {

    private static final int MB_PER_UNIT = 144;
    private static final int ENERGY_PER_UNIT = 50_000;
    private static final MatterPhaseTransformerMode MELTING = MatterPhaseTransformerMode.MELTING;

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

        for (Material material : ModMaterials.ALL) {
            storageBlockRecipes(writer, material);
            if (!material.hasFluid()) {
                continue;
            }
            for (MaterialForm form : material.forms()) {
                writer.accept(new MeltingRecipe(material, form));
            }
        }

        InscriberRecipeBuilder.inscribe(
                        Ingredient.of(Items.AMETHYST_SHARD),
                        ModMaterials.AMETHYST.item(Form.DUST).orElseThrow().get(),
                        1)
                .setMode(InscriberProcessType.INSCRIBE)
                .save(writer, Ids.of("inscriber/amethyst_dust"));
    }

    private static void storageBlockRecipes(Consumer<FinishedRecipe> writer, Material material) {
        MaterialForm blockForm = material.form(Form.BLOCK).orElse(null);
        if (blockForm == null || !blockForm.ownsItem()) {
            return;
        }
        MaterialForm unitForm = material.form(Form.GEM)
                .filter(MaterialForm::ownsItem)
                .or(() -> material.form(Form.INGOT).filter(MaterialForm::ownsItem))
                .orElse(null);
        if (unitForm == null || blockForm.amountMb() % unitForm.amountMb() != 0) {
            return;
        }
        int itemsPerBlock = blockForm.amountMb() / unitForm.amountMb();
        if (itemsPerBlock != 4 && itemsPerBlock != 9) {
            return;
        }

        Item blockItem = blockForm.ownItem().orElseThrow().get();
        Item unitItem = unitForm.ownItem().orElseThrow().get();
        TagKey<Item> unitTag = unitForm.ownTag().orElseThrow();

        ShapedRecipeBuilder packer = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockItem)
                .define('A', unitTag)
                .unlockedBy("has_" + unitForm.form().name(), has(unitTag));
        if (itemsPerBlock == 4) {
            packer.pattern("AA").pattern("AA");
        } else {
            packer.pattern("AAA").pattern("AAA").pattern("AAA");
        }
        packer.save(writer, Ids.of("crafting_shaped/" + material.id() + "_block"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, unitItem, itemsPerBlock)
                .requires(blockItem)
                .unlockedBy("has_block", has(blockItem))
                .save(writer, Ids.of("crafting_shapeless/" + itemPath(unitItem) + "_from_block"));
    }

    private static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    // 材料形态 → 流体，由 material.fluid() 推导
    private record MeltingRecipe(Material material, MaterialForm form) implements FinishedRecipe {

        @Override
        public void serializeRecipeData(JsonObject recipe) {
            recipe.addProperty("energy", (long) form.amountMb() * ENERGY_PER_UNIT / MB_PER_UNIT);
            if (form.optional()) {
                var condition = new NotCondition(new TagEmptyCondition(form.ingredientId()));
                recipe.add("conditions", array(CraftingHelper.serialize(condition)));
            }

            JsonObject input = new JsonObject();
            input.addProperty(
                    form.usesTag() ? "tag" : "item", form.ingredientId().toString());
            input.addProperty("count", 1);
            recipe.add("item_inputs", array(input));

            JsonObject output = new JsonObject();
            output.addProperty(
                    "fluid", Ids.of(material.fluid().orElseThrow().id()).toString());
            output.addProperty("amount", form.amountMb());
            recipe.add("fluid_outputs", array(output));
        }

        @Override
        public ResourceLocation getId() {
            String fluid = material.fluid().orElseThrow().id();
            return MELTING.recipeId(fluid + "_from_" + form.form().name());
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModMachines.matterPhaseTransformer().recipe(MELTING).serializer();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }

        private static JsonArray array(JsonObject entry) {
            JsonArray array = new JsonArray();
            array.add(entry);
            return array;
        }
    }
}
