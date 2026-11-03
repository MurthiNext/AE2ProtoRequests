package com.murthinext.ae2pr.api.multiblock.module;

import net.minecraft.core.BlockPos;

public record ModuleInstance(BlockPos pos, ModuleDefinition definition) {
}
