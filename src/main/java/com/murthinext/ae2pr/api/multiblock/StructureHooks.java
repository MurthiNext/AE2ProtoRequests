package com.murthinext.ae2pr.api.multiblock;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 结构模板的内容挂钩：由模组在初始化时注入具体仓室，
 * 使通用结构模板不依赖具体注册表
 */
public final class StructureHooks {

    // 把单个仓室格同步为成型/未成型外观
    @FunctionalInterface
    public interface PartAppearance {
        void apply(Level level, BlockPos pos, boolean formed, int style, boolean active);
    }

    private static Supplier<List<Block>> parts = List::of;
    @Nullable
    private static Supplier<Block> networkAccessHatch;
    @Nullable
    private static PartAppearance partAppearance;

    private StructureHooks() {
    }

    public static void install(Supplier<List<Block>> parts, @Nullable Supplier<Block> networkAccessHatch,
            @Nullable PartAppearance partAppearance) {
        if (parts != null) {
            StructureHooks.parts = parts;
        }
        StructureHooks.networkAccessHatch = networkAccessHatch;
        StructureHooks.partAppearance = partAppearance;
    }

    public static List<Block> parts() {
        return parts.get();
    }

    @Nullable
    public static Block networkAccessHatch() {
        return networkAccessHatch == null ? null : networkAccessHatch.get();
    }

    public static void applyPartAppearance(Level level, BlockPos pos, boolean formed, int style, boolean active) {
        if (partAppearance != null) {
            partAppearance.apply(level, pos, formed, style, active);
        }
    }
}
