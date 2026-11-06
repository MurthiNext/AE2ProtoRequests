package com.murthinext.ae2pr.api.collapse;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface CollapseProvider {

    @Nullable
    Collapsible create(BlockEntity blockEntity);
}
