package com.murthinext.ae2pr.multiblock.module;

/**
 * 模块定义：某个模块方块提供的功能与等级。
 */
public record ModuleDefinition(ModuleType type, int level) {

    public ModuleDefinition {
        if (level < 1) {
            throw new IllegalArgumentException("模块等级必须 >= 1：" + level);
        }
    }

    public static ModuleDefinition of(ModuleType type, int level) {
        return new ModuleDefinition(type, level);
    }
}
