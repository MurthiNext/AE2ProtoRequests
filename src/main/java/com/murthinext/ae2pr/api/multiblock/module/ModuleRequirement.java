package com.murthinext.ae2pr.api.multiblock.module;

public record ModuleRequirement(ModuleType type, int minLevel) {

    public ModuleRequirement {
        if (minLevel < 1) {
            throw new IllegalArgumentException("Module minimum level must be >= 1: " + minLevel);
        }
    }
}
