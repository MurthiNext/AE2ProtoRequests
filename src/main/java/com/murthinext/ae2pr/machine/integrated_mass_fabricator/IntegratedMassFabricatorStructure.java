package com.murthinext.ae2pr.machine.integrated_mass_fabricator;

import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.api.multiblock.RelativeDirection;
import com.murthinext.ae2pr.api.multiblock.StructurePredicate;
import com.murthinext.ae2pr.api.multiblock.StructureResult;
import com.murthinext.ae2pr.block.casing.StabilityCasingBlock;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModMachines;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class IntegratedMassFabricatorStructure extends AbstractStructureFacade {

    static final int PLANE_SEPARATION = 12;
    static final int FIELD_WIDTH = 11;

    public static final MultiblockPattern PATTERN = MultiblockPattern.builder(
                    RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
            .sliceOrigin(8)
            .origin(1, 6)
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".SSS.....SSS.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "..SEE...EES..",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    "...EEEEEEE...",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".....SSS.....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....E...E....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    "....G...G....",
                    ".....SSS.....",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    "......K......",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    "...EEEEEEE...",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".....GGG.....",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "..SEE...EES..",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".SSS.....SSS.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    ".S.........S.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    ".SS.......SS.",
                    "SSSSSSSSSSSSS")
            .slice(
                    "SSSSSSSSSSSSS",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "SSSSSSSSSSSSS")
            .where('.', StructurePredicate.any())
            .where(
                    'K',
                    StructurePredicate.blocks(
                            ModMachines.integratedMassFabricator().block()))
            .where('S', StructurePredicate.casingWithParts(ModBlocks.STABILITY_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('E', StructurePredicate.blocks(ModBlocks.ENTANGLED_CASING.get()))
            .build();

    public static final IntegratedMassFabricatorStructure INSTANCE = new IntegratedMassFabricatorStructure();

    private IntegratedMassFabricatorStructure() {
        super(PATTERN, MachinePartFacade.STABILITY);
    }

    static BlockPos floorCenter(BlockPos controller, Direction front, boolean mirrorFront) {
        return controller.relative(mirrorFront ? front : front.getOpposite(), 2).below();
    }

    @Override
    public List<BlockPos> updateFormed(
            Level level, StructureResult result, List<BlockPos> previousCells, boolean active) {
        var cells = super.updateFormed(level, result, previousCells, active);
        var floor = cells.isEmpty() ? null : floorCenter(cells);
        if (!previousCells.isEmpty()) {
            var previous = floorCenter(previousCells);
            if (!previous.equals(floor)) {
                lightField(level, previous, false);
            }
        }
        if (floor != null) {
            lightField(level, floor, true);
        }
        return cells;
    }

    @Override
    public void reapply(Level level, List<BlockPos> cells, boolean active) {
        super.reapply(level, cells, active);
        if (!cells.isEmpty()) {
            lightField(level, floorCenter(cells), true);
        }
    }

    @Override
    public void reset(Level level, List<BlockPos> cells) {
        super.reset(level, cells);
        if (!cells.isEmpty()) {
            lightField(level, floorCenter(cells), false);
        }
    }

    private static BlockPos floorCenter(List<BlockPos> cells) {
        // 完整匹配按片、行、列保存，本结构首尾两格正是包围盒的对角。
        var bounds = BoundingBox.fromCorners(cells.get(0), cells.get(cells.size() - 1));
        var center = bounds.getCenter();
        return new BlockPos(center.getX(), bounds.minY(), center.getZ());
    }

    private static void lightField(Level level, BlockPos floor, boolean lit) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            lightPair(level, floor.relative(side, 4), lit);
            lightPair(level, floor.relative(side, 3).relative(side.getClockWise(), 3), lit);
        }
    }

    private static void lightPair(Level level, BlockPos floor, boolean lit) {
        setLight(level, floor, lit);
        setLight(level, floor.above(PLANE_SEPARATION), lit);
    }

    private static void setLight(Level level, BlockPos pos, boolean lit) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(StabilityCasingBlock.FIELD_LIT)
                && state.getValue(StabilityCasingBlock.FIELD_LIT) != lit) {
            level.setBlock(pos, state.setValue(StabilityCasingBlock.FIELD_LIT, lit), Block.UPDATE_CLIENTS);
        }
    }
}
