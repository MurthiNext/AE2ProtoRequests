package com.murthinext.ae2pr.api.multiblock;

import com.murthinext.ae2pr.api.multiblock.module.ModuleSnapshot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public record StructureResult(
        boolean formed,
        int slices,
        boolean mirrorSide,
        boolean mirrorFront,
        int mismatches,
        @Nullable BlockPos mismatchPos,
        char expected,
        @Nullable Block found,
        List<BlockPos> cells,
        ModuleSnapshot modules,
        boolean fromCache) {

    public static final StructureResult EMPTY =
            new StructureResult(false, 0, false, false, 0, null, ' ', null, List.of(), ModuleSnapshot.EMPTY, false);

    public StructureResult asCached() {
        return new StructureResult(
                formed,
                slices,
                mirrorSide,
                mirrorFront,
                mismatches,
                mismatchPos,
                expected,
                found,
                cells,
                modules,
                true);
    }
}
