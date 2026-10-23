package com.murthinext.ae2pr.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 世界交互的流体或爆炸触发条件。
 */
public record WorldInteractionCircumstance(@Nullable TagKey<Fluid> fluidTag, boolean sourceOnly) {

    public boolean isExplosion() {
        return fluidTag == null;
    }

    public boolean matches(FluidState state) {
        return fluidTag != null && !state.isEmpty() && state.is(fluidTag) && (!sourceOnly || state.isSource());
    }

    public List<Fluid> getFluids() {
        if (fluidTag == null) {
            return List.of();
        }
        return ForgeRegistries.FLUIDS.tags().getTag(fluidTag).stream()
                .filter(fluid -> fluid != Fluids.EMPTY && (!sourceOnly || fluid.defaultFluidState().isSource()))
                .toList();
    }

    public static WorldInteractionCircumstance fromJson(JsonObject json) {
        return switch (GsonHelper.getAsString(json, "type")) {
            case "fluid" -> new WorldInteractionCircumstance(
                    TagKey.create(Registries.FLUID, ResourceLocation.parse(GsonHelper.getAsString(json, "tag"))),
                    GsonHelper.getAsBoolean(json, "source_only", true));
            case "explosion" -> new WorldInteractionCircumstance(null, false);
            default -> throw new JsonSyntaxException("Unknown world interaction circumstance type");
        };
    }

    public static WorldInteractionCircumstance fromNetwork(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return new WorldInteractionCircumstance(null, false);
        }
        return new WorldInteractionCircumstance(TagKey.create(Registries.FLUID, buffer.readResourceLocation()),
                buffer.readBoolean());
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeBoolean(fluidTag != null);
        if (fluidTag != null) {
            buffer.writeResourceLocation(fluidTag.location());
            buffer.writeBoolean(sourceOnly);
        }
    }
}
