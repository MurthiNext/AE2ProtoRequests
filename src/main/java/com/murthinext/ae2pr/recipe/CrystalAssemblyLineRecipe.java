package com.murthinext.ae2pr.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.multiblock.module.ModuleRequirement;
import com.murthinext.ae2pr.multiblock.module.ModuleType;

/**
 * 水晶装配线配方。
 */
public class CrystalAssemblyLineRecipe extends AbstractMultiblockRecipe {

    public static final int DEFAULT_DURATION = 100;

    private final List<ModuleRequirement> moduleRequirements;

    public CrystalAssemblyLineRecipe(ResourceLocation id, RecipeDraft draft,
            List<ModuleRequirement> moduleRequirements) {
        super(id, draft);
        this.moduleRequirements = List.copyOf(moduleRequirements);
    }

    public List<ModuleRequirement> getModuleRequirements() {
        return moduleRequirements;
    }

    @Override
    protected double defaultEnergyPerParallel() {
        return Config.assemblyEnergyPerParallel();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRYSTAL_ASSEMBLY_LINE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRYSTAL_ASSEMBLY_LINE_TYPE.get();
    }

    public static class Serializer extends AbstractMultiblockRecipeSerializer<CrystalAssemblyLineRecipe> {

        public Serializer() {
            super(DEFAULT_DURATION);
        }

        @Override
        protected void validate(RecipeDraft draft, JsonObject json) {
            super.validate(draft, json);
            if (draft.itemOutputs().isEmpty()) {
                throw new JsonSyntaxException("Recipe requires at least one item output");
            }
            if (!draft.fluidOutputs().isEmpty()) {
                throw new JsonSyntaxException("Crystal assembly line does not support fluid outputs");
            }
        }

        @Override
        protected CrystalAssemblyLineRecipe create(ResourceLocation id, RecipeDraft draft, JsonObject json) {
            return new CrystalAssemblyLineRecipe(id, draft, readModules(json));
        }

        @Override
        protected CrystalAssemblyLineRecipe createFromNetwork(ResourceLocation id, RecipeDraft draft,
                FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<ModuleRequirement> requirements = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ResourceLocation moduleId = buffer.readResourceLocation();
                ModuleType type = ModModules.byId(moduleId);
                if (type == null) {
                    throw new IllegalArgumentException("Unknown module: " + moduleId);
                }
                requirements.add(new ModuleRequirement(type, buffer.readVarInt()));
            }
            return new CrystalAssemblyLineRecipe(id, draft, requirements);
        }

        @Override
        protected void writeExtra(FriendlyByteBuf buffer, CrystalAssemblyLineRecipe recipe) {
            List<ModuleRequirement> requirements = recipe.getModuleRequirements();
            buffer.writeVarInt(requirements.size());
            for (ModuleRequirement requirement : requirements) {
                buffer.writeResourceLocation(requirement.type().id());
                buffer.writeVarInt(requirement.minLevel());
            }
        }

        private static List<ModuleRequirement> readModules(JsonObject json) {
            List<ModuleRequirement> requirements = new ArrayList<>();
            Set<ResourceLocation> seen = new HashSet<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "module_requirements", new JsonArray())) {
                JsonObject entry = element.getAsJsonObject();
                ResourceLocation moduleId = ResourceLocation.parse(GsonHelper.getAsString(entry, "module"));
                ModuleType type = ModModules.byId(moduleId);
                if (type == null) {
                    throw new JsonSyntaxException("Unknown module: " + moduleId);
                }
                if (!seen.add(moduleId)) {
                    throw new JsonSyntaxException("Duplicate module requirement: " + moduleId);
                }
                int minLevel = GsonHelper.getAsInt(entry, "min_level", 1);
                if (minLevel < 1) {
                    throw new JsonSyntaxException("module_requirements min_level must be >= 1");
                }
                requirements.add(new ModuleRequirement(type, minLevel));
            }
            return requirements;
        }
    }
}
