package com.murthinext.ae2pr.multiblock.module;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ae2pr;

/**
 * 模块功能类型：方块注册名之外的“功能”标识，同类型模块共享安装上限。
 *
 * @param id       功能标识（如 {@code ae2pr:charging}）
 * @param maxCount 单台结构内的安装上限；-1 表示不限
 */
public record ModuleType(ResourceLocation id, int maxCount) {

    public ModuleType {
        if (maxCount < -1) {
            throw new IllegalArgumentException("模块安装上限非法：" + maxCount);
        }
    }

    /** 以本模组命名空间创建功能类型。 */
    public static ModuleType of(String path, int maxCount) {
        return new ModuleType(new ResourceLocation(ae2pr.MODID, path), maxCount);
    }

    /** 语言键（用于配方要求等展示）。 */
    public String translationKey() {
        return "module." + id.getNamespace() + "." + id.getPath();
    }
}
