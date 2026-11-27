package com.murthinext.ae2pr.api.material;

import net.minecraft.resources.ResourceLocation;

public record MaterialForm(TagPrefix prefix, ResourceLocation ingredientId, int units, Source source) {

    public MaterialForm {
        if (units < 1) {
            throw new IllegalArgumentException("Material form units must be positive");
        }
    }

    public boolean registersItem() {
        return source == Source.REGISTERED_ITEM;
    }

    public boolean usesTag() {
        return source != Source.EXISTING_ITEM;
    }

    public boolean optionalTag() {
        return source == Source.OPTIONAL_TAG;
    }

    public enum Source {
        REGISTERED_ITEM,
        EXISTING_ITEM,
        ITEM_TAG,
        OPTIONAL_TAG
    }
}
