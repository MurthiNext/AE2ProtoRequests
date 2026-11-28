package com.murthinext.ae2pr.api.multiblock;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

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

    private record Hooks(
            Supplier<List<Block>> parts,
            @Nullable Supplier<Block> networkAccessHatch,
            @Nullable PartAppearance partAppearance) {}

    private static volatile Hooks hooks;

    private StructureHooks() {}

    // 只允许安装一次，避免结构谓词读取到被覆盖的仓室表
    public static synchronized void install(
            Supplier<List<Block>> parts,
            @Nullable Supplier<Block> networkAccessHatch,
            @Nullable PartAppearance partAppearance) {
        if (hooks != null) {
            throw new IllegalStateException("StructureHooks already installed");
        }
        hooks = new Hooks(Objects.requireNonNull(parts, "parts"), networkAccessHatch, partAppearance);
    }

    public static List<Block> parts() {
        var value = hooks;
        if (value == null) {
            throw new IllegalStateException(
                    "StructureHooks must be installed before any structure pattern is initialized");
        }
        return value.parts().get();
    }

    @Nullable
    public static Block networkAccessHatch() {
        var value = hooks;
        return value == null || value.networkAccessHatch() == null
                ? null
                : value.networkAccessHatch().get();
    }

    public static void applyPartAppearance(Level level, BlockPos pos, boolean formed, int style, boolean active) {
        var value = hooks;
        if (value != null && value.partAppearance() != null) {
            value.partAppearance().apply(level, pos, formed, style, active);
        }
    }
}
