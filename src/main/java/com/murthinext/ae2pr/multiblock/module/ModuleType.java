package com.murthinext.ae2pr.multiblock.module;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 模块功能类型：同类型模块共享安装上限（-1 表示不限）。
 */
public record ModuleType(ResourceLocation id, int maxCount) {

    public ModuleType {
        if (maxCount < -1) {
            throw new IllegalArgumentException("模块安装上限非法：" + maxCount);
        }
    }

    public static ModuleType of(String path, int maxCount) {
        return new ModuleType(ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path), maxCount);
    }

    public String translationKey() {
        return "module." + id.getNamespace() + "." + id.getPath();
    }
}
