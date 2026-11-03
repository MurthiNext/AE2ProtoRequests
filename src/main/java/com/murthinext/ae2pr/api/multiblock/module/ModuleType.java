package com.murthinext.ae2pr.api.multiblock.module;

import com.murthinext.ae2pr.api.Ids;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

public record ModuleType(ResourceLocation id, int maxCount) {

    public ModuleType {
        if (maxCount < -1) {
            throw new IllegalArgumentException("Invalid module install limit: " + maxCount);
        }
    }

    public static ModuleType of(String path, int maxCount) {
        return new ModuleType(Ids.of( path), maxCount);
    }

    public String translationKey() {
        return "module." + id.getNamespace() + "." + id.getPath();
    }
}
