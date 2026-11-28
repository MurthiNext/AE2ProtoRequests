package com.murthinext.ae2pr.api.material;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public record MaterialForm(Form form, Origin origin, int amountMb) {

    public MaterialForm {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(origin, "origin");
        if (amountMb < 1) {
            throw new IllegalArgumentException("Material form amount must be positive");
        }
    }

    public boolean ownsItem() {
        return origin instanceof Origin.Own;
    }

    public boolean usesTag() {
        return !(origin instanceof Origin.ExternalItem);
    }

    public boolean optional() {
        return origin instanceof Origin.ExternalTag tag && tag.optional();
    }

    public ResourceLocation ingredientId() {
        if (origin instanceof Origin.Own own) {
            return own.tag().location();
        }
        if (origin instanceof Origin.ExternalItem item) {
            return item.id();
        }
        return ((Origin.ExternalTag) origin).tag().location();
    }

    public Optional<RegistryObject<? extends Item>> ownItem() {
        return origin instanceof Origin.Own own ? Optional.of(own.item()) : Optional.empty();
    }

    public Optional<TagKey<Item>> ownTag() {
        return origin instanceof Origin.Own own ? Optional.of(own.tag()) : Optional.empty();
    }

    public sealed interface Origin {

        record Own(RegistryObject<? extends Item> item, TagKey<Item> tag) implements Origin {}

        record ExternalItem(ResourceLocation id) implements Origin {}

        record ExternalTag(TagKey<Item> tag, boolean optional) implements Origin {}
    }
}
