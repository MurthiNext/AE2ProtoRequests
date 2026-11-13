package com.murthinext.ae2pr.api;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public final class BlockProperties {

    private BlockProperties() {
    }

    public static Block.Properties metal() {
        return Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    public static Block.Properties metal(MapColor mapColor) {
        return metal().mapColor(mapColor);
    }

    public static Block.Properties stone() {
        return Block.Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    public static Block.Properties glass(MapColor mapColor) {
        return Block.Properties.of().mapColor(mapColor).strength(0.8F).sound(SoundType.GLASS).noOcclusion();
    }

    public static Block.Properties storageBlock(MapColor mapColor) {
        return Block.Properties.of()
                .mapColor(mapColor)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
