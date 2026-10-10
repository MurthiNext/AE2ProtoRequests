package com.murthinext.ae2pr.multiblock.module;

/**
 * 配方对模块的要求。
 */
public record ModuleRequirement(ModuleType type, int minLevel) {

    public ModuleRequirement {
        if (minLevel < 1) {
            throw new IllegalArgumentException("模块最低等级必须 >= 1：" + minLevel);
        }
    }
}
