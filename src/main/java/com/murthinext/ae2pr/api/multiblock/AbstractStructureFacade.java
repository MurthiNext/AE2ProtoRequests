package com.murthinext.ae2pr.api.multiblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;

public abstract class AbstractStructureFacade {

    protected final MultiblockPattern pattern;
    protected final MachinePartFacade facade;

    protected AbstractStructureFacade(MultiblockPattern pattern, MachinePartFacade facade) {
        this.pattern = pattern;
        this.facade = facade;
    }

    public MultiblockPattern pattern() {
        return pattern;
    }

    public MachinePartFacade facade() {
        return facade;
    }

    public int maxSlices() {
        return pattern.minSlices();
    }

    /**
     * 同步结构内部件的外观。
     *
     * @param previousCells 上一次成型时覆盖的方块，未成型时只撤销这些方块，避免误伤相邻多方块
     * @param active        机器是否处于运行状态，供需要工作外观的结构使用
     * @return 本次成型覆盖的方块，未成型时为空
     */
    public List<BlockPos> updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result,
                                       List<BlockPos> previousCells, boolean active) {
        List<BlockPos> cells = result.formed() ? pattern.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                                                               result.mirrorFront(), result.slices())
                                               : List.of();
        // 撤销已不在当前结构内的旧方块
        if (!previousCells.isEmpty()) {
            Set<BlockPos> current = new HashSet<>(cells);
            List<BlockPos> stale = new ArrayList<>(previousCells.size());
            for (BlockPos pos : previousCells) {
                if (!current.contains(pos)) {
                    stale.add(pos);
                }
            }
            if (!stale.isEmpty()) {
                applyParts(level, stale, false, false);
            }
        }
        if (result.formed()) {
            applyParts(level, cells, true, active);
        }
        return cells;
    }

    public void reapply(Level level, List<BlockPos> cells, boolean active) {
        if (!cells.isEmpty()) {
            applyParts(level, cells, true, active);
        }
    }

    public void reset(Level level, List<BlockPos> cells) {
        if (!cells.isEmpty()) {
            applyParts(level, cells, false, false);
        }
    }

    protected void applyParts(Level level, List<BlockPos> cells, boolean formed, boolean active) {
        int style = facade.style();
        for (BlockPos pos : cells) {
            onPartCell(level, pos, active);
            StructureHooks.applyPartAppearance(level, pos, formed, style, active);
        }
    }

    protected void onPartCell(Level level, BlockPos pos, boolean active) {
    }
}
