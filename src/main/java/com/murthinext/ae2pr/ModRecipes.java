package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.recipe.AlienLavaRecipe;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.recipe.CrystalReactionChamberRecipe;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;
import com.murthinext.ae2pr.recipe.MatterPhaseRecipe;

/**
 * 配方注册入口。
 */
public final class ModRecipes {

    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            Registries.RECIPE_TYPE, ae2pr.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, ae2pr.MODID);

    public static final RegistryObject<RecipeType<CrystalAssemblyLineRecipe>> CRYSTAL_ASSEMBLY_LINE_TYPE = RECIPE_TYPES
            .register("crystal_assembly_line", () -> new RecipeType<CrystalAssemblyLineRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":crystal_assembly_line";
                }
            });

    public static final RegistryObject<CrystalAssemblyLineRecipe.Serializer> CRYSTAL_ASSEMBLY_LINE = SERIALIZERS
            .register("crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<AlienLavaRecipe>> ALIEN_LAVA_TYPE = RECIPE_TYPES
            .register("alien_lava", () -> new RecipeType<AlienLavaRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":alien_lava";
                }
            });

    public static final RegistryObject<AlienLavaRecipe.Serializer> ALIEN_LAVA = SERIALIZERS
            .register("alien_lava", AlienLavaRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<LavaSmelterRecipe>> LAVA_SMELTER_TYPE = RECIPE_TYPES
            .register("lava_smelter", () -> new RecipeType<LavaSmelterRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":lava_smelter";
                }
            });

    public static final RegistryObject<LavaSmelterRecipe.Serializer> LAVA_SMELTER = SERIALIZERS
            .register("lava_smelter", LavaSmelterRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<MatterPhaseRecipe>> MATTER_PHASE_TYPE = RECIPE_TYPES
            .register("matter_phase", () -> new RecipeType<MatterPhaseRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":matter_phase";
                }
            });

    public static final RegistryObject<MatterPhaseRecipe.Serializer> MATTER_PHASE = SERIALIZERS
            .register("matter_phase", MatterPhaseRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<CrystalReactionChamberRecipe>> CRYSTAL_REACTION_CHAMBER_TYPE = RECIPE_TYPES
            .register("crystal_reaction_chamber", () -> new RecipeType<CrystalReactionChamberRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":crystal_reaction_chamber";
                }
            });

    public static final RegistryObject<CrystalReactionChamberRecipe.Serializer> CRYSTAL_REACTION_CHAMBER = SERIALIZERS
            .register("crystal_reaction_chamber", CrystalReactionChamberRecipe.Serializer::new);
}
