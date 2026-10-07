package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.recipe.AlienLavaRecipe;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;

/**
 * 配方注册入口：配方类型与序列化器都通过注册事件注册，避免注册表冻结问题。
 */
public final class ModRecipes {

    private ModRecipes() {
    }

    /** 配方类型 */
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            Registries.RECIPE_TYPE, ae2pr.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, ae2pr.MODID);

    /** 水晶装配线配方类型 */
    public static final RegistryObject<RecipeType<CrystalAssemblyLineRecipe>> CRYSTAL_ASSEMBLY_LINE_TYPE = RECIPE_TYPES
            .register("crystal_assembly_line", () -> new RecipeType<CrystalAssemblyLineRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":crystal_assembly_line";
                }
            });

    /** 水晶装配线配方序列化器 */
    public static final RegistryObject<CrystalAssemblyLineRecipe.Serializer> CRYSTAL_ASSEMBLY_LINE = SERIALIZERS
            .register("crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new);

    /** 异星熔岩世界交互配方类型 */
    public static final RegistryObject<RecipeType<AlienLavaRecipe>> ALIEN_LAVA_TYPE = RECIPE_TYPES
            .register("alien_lava", () -> new RecipeType<AlienLavaRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":alien_lava";
                }
            });

    /** 异星熔岩世界交互配方序列化器 */
    public static final RegistryObject<AlienLavaRecipe.Serializer> ALIEN_LAVA = SERIALIZERS
            .register("alien_lava", AlienLavaRecipe.Serializer::new);

    /** 熔岩冶炼炉配方类型 */
    public static final RegistryObject<RecipeType<LavaSmelterRecipe>> LAVA_SMELTER_TYPE = RECIPE_TYPES
            .register("lava_smelter", () -> new RecipeType<LavaSmelterRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":lava_smelter";
                }
            });

    /** 熔岩冶炼炉配方序列化器 */
    public static final RegistryObject<LavaSmelterRecipe.Serializer> LAVA_SMELTER = SERIALIZERS
            .register("lava_smelter", LavaSmelterRecipe.Serializer::new);
}
