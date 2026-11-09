package com.murthinext.ae2pr.api;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

public final class Ids {

    private Ids() {
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path);
    }
}
