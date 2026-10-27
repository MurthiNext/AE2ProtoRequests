package com.murthinext.ae2pr;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.recipe.ComplexWorldInteractionRecipe;
import com.murthinext.ae2pr.recipe.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.recipe.CrystalReactionChamberRecipe;
import com.murthinext.ae2pr.recipe.CrystalWorkstationRecipe;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;
import com.murthinext.ae2pr.recipe.JigRecipe;
import com.murthinext.ae2pr.recipe.MatterPhaseTransformerRecipe;

public final class ModRecipes {

    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            Registries.RECIPE_TYPE, ae2pr.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, ae2pr.MODID);

    public static final RegistryObject<RecipeType<JigRecipe>> JIG_TYPE = RECIPE_TYPES.register("jig",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "jig")));

    public static final RegistryObject<JigRecipe.Serializer> JIG = SERIALIZERS.register("jig", JigRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<CrystalAssemblyLineRecipe>> CRYSTAL_ASSEMBLY_LINE_TYPE = RECIPE_TYPES
            .register("crystal_assembly_line", () -> new RecipeType<CrystalAssemblyLineRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":crystal_assembly_line";
                }
            });

    public static final RegistryObject<CrystalAssemblyLineRecipe.Serializer> CRYSTAL_ASSEMBLY_LINE = SERIALIZERS
            .register("crystal_assembly_line", CrystalAssemblyLineRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ComplexWorldInteractionRecipe>> COMPLEX_WORLD_INTERACTION_TYPE = RECIPE_TYPES
            .register("complex_world_interaction", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "complex_world_interaction")));

    public static final RegistryObject<ComplexWorldInteractionRecipe.Serializer> COMPLEX_WORLD_INTERACTION = SERIALIZERS
            .register("complex_world_interaction", ComplexWorldInteractionRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<LavaSmelterRecipe>> LAVA_SMELTER_TYPE = RECIPE_TYPES
            .register("lava_smelter", () -> new RecipeType<LavaSmelterRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":lava_smelter";
                }
            });

    public static final RegistryObject<LavaSmelterRecipe.Serializer> LAVA_SMELTER = SERIALIZERS
            .register("lava_smelter", LavaSmelterRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<MatterPhaseTransformerRecipe>> MATTER_PHASE_TRANSFORMER_TYPE = RECIPE_TYPES
            .register("matter_phase_transformer", () -> new RecipeType<MatterPhaseTransformerRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":matter_phase_transformer";
                }
            });

    public static final RegistryObject<MatterPhaseTransformerRecipe.Serializer> MATTER_PHASE_TRANSFORMER = SERIALIZERS
            .register("matter_phase_transformer", MatterPhaseTransformerRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<CrystalReactionChamberRecipe>> CRYSTAL_REACTION_CHAMBER_TYPE = RECIPE_TYPES
            .register("crystal_reaction_chamber", () -> new RecipeType<CrystalReactionChamberRecipe>() {
                @Override
                public String toString() {
                    return ae2pr.MODID + ":crystal_reaction_chamber";
                }
            });

    public static final RegistryObject<CrystalReactionChamberRecipe.Serializer> CRYSTAL_REACTION_CHAMBER = SERIALIZERS
            .register("crystal_reaction_chamber", CrystalReactionChamberRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<CrystalWorkstationRecipe>> CRYSTAL_WORKSTATION_ETCHING_TYPE = RECIPE_TYPES
            .register("crystal_workstation_etching", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_workstation_etching")));

    public static final RegistryObject<RecipeType<CrystalWorkstationRecipe>> CRYSTAL_WORKSTATION_CUTTING_TYPE = RECIPE_TYPES
            .register("crystal_workstation_cutting", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_workstation_cutting")));

    public static final RegistryObject<CrystalWorkstationRecipe.Serializer> CRYSTAL_WORKSTATION_ETCHING = SERIALIZERS
            .register("crystal_workstation_etching",
                    () -> new CrystalWorkstationRecipe.Serializer(CrystalWorkstationMode.ETCHING));

    public static final RegistryObject<CrystalWorkstationRecipe.Serializer> CRYSTAL_WORKSTATION_CUTTING = SERIALIZERS
            .register("crystal_workstation_cutting",
                    () -> new CrystalWorkstationRecipe.Serializer(CrystalWorkstationMode.CUTTING));
}
