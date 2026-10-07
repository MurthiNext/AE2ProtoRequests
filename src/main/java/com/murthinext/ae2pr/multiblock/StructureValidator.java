package com.murthinext.ae2pr.multiblock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 控制器侧的结构验证器：为单个控制器实例绑定模式与缓存。
 * <p>
 * 每次验证先走缓存快速路径（结构未变化时直接复用上次结果）；缓存失效时执行完整匹配并重建缓存。
 */
public final class StructureValidator {

    private final MultiblockPattern pattern;
    private final StructureCache cache = new StructureCache();
    @Nullable
    private StructureResult lastFormed;

    public StructureValidator(MultiblockPattern pattern) {
        this.pattern = pattern;
    }

    /**
     * 验证结构。返回结果的 {@link StructureResult#fromCache()} 为 true 时表示结构未变化，
     * 调用方无需刷新外观与部件引用。
     */
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

    /** 清空缓存（控制器拆除等场景）。 */
    public void invalidate() {
        lastFormed = null;
        cache.clear();
    }
}
