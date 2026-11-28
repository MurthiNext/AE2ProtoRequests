package com.murthinext.ae2pr.api;

import net.minecraft.resources.ResourceLocation;

public final class Ids {

    public static final String MODID = "ae2pr";

    private Ids() {}

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
