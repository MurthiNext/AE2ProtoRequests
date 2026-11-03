package com.murthinext.ae2pr.api.multiblock.module;

public record ModuleDefinition(ModuleType type, int level) {

    public ModuleDefinition {
        if (level < 1) {
            throw new IllegalArgumentException("Module level must be >= 1: " + level);
        }
    }

    public static ModuleDefinition of(ModuleType type, int level) {
        return new ModuleDefinition(type, level);
    }
}
