package com.murthinext.ae2pr.multiblock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

// 控制器侧的结构验证器
public final class StructureValidator {

    private final MultiblockPattern pattern;
    private final StructureCache cache = new StructureCache();
    @Nullable
    private StructureResult lastFormed;

    public StructureValidator(MultiblockPattern pattern) {
        this.pattern = pattern;
    }

    // 验证结构；{@link StructureResult#fromCache()} 为 true 时无需刷新外观与部件引用
    public StructureResult validate(Level level, BlockPos controllerPos, Direction front, Direction up,
            int maxSlices) {
        if (lastFormed != null && cache.verify(level)) {
            return lastFormed.asCached();
        }
        StructureResult result = pattern.match(level, controllerPos, front, up, maxSlices);
        if (result.formed()) {
            lastFormed = result;
            cache.capture(level, result.cells());
        } else {
            lastFormed = null;
            cache.clear();
        }
        return result;
    }

    public void invalidate() {
        lastFormed = null;
        cache.clear();
    }
}
