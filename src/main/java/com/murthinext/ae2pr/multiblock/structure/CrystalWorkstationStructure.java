package com.murthinext.ae2pr.multiblock.structure;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractStructureFacade;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

// 晶能工作站结构定义
public final class CrystalWorkstationStructure extends AbstractStructureFacade {

    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT)
            .origin(0, 4)
            .slice("AAAAKAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "C#######C", "C###G###C", "C#######C", "AAAAAAAAA")
            .slice("AAAAAAAAA", "CDDDDDDDC", "CDDDDDDDC", "CDDDDDDDC", "AAAAAAAAA")
            .slice("AAAAAAAAA", "B#######B", "B#######B", "B#######B", "AAAAAAAAA")
            .where('K', StructurePredicate.blocks(ModBlocks.CRYSTAL_WORKSTATION.get()))
            .where('A', StructurePredicate.blocks(ModBlocks.REINFORCED_HIGH_POWER_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get(), ModBlocks.AEV_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get(), ModBlocks.AEV_OUTPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get(), ModBlocks.AEV_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get(), ModBlocks.AEV_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.AEV_DUAL_INPUT_HATCH.get(), ModBlocks.AEV_DUAL_OUTPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.ME_NETWORK_ACCESS_HATCH.get()).maxCount(1)))
            .where('B', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_STABILITY_CASING.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.HIGH_ENERGY_CORE.get()))
            .where('#', StructurePredicate.blocks(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            .build();

    public static final CrystalWorkstationStructure INSTANCE = new CrystalWorkstationStructure();

    public enum PartRole {
        STABILITY, GLASS, CORE
    }

    public static final int PANEL_SIZE = 3;

    public record Part(BlockPos pos, PartRole role, @Nullable Direction outward, @Nullable Direction depthAxis,
            int panelX, int panelY) {
    }

    private CrystalWorkstationStructure() {
        super(PATTERN, MachinePartFacade.HIGH_POWER);
    }

    // 结构深度轴
    public static Direction depthAxis(Direction facing, boolean mirrorSide, boolean mirrorFront) {
        return RelativeDirection.BACK.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
    }

    // 结构左右轴
    public static Direction sideAxis(Direction facing, boolean mirrorSide, boolean mirrorFront) {
        return RelativeDirection.RIGHT.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
    }

    public static List<Part> effectParts(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        Direction side = sideAxis(facing, mirrorSide, mirrorFront);
        Direction depth = depthAxis(facing, mirrorSide, mirrorFront);
        List<MultiblockPattern.PatternCell> cells = PATTERN.patternCells(controllerPos, facing, Direction.UP,
                mirrorSide, mirrorFront, PATTERN.minSlices());
        MultiblockPattern.PatternCell core = null;
        for (MultiblockPattern.PatternCell cell : cells) {
            if (cell.symbol() == 'G') {
                core = cell;
                break;
            }
        }
        if (core == null) {
            return List.of();
        }
        List<Part> parts = new ArrayList<>(cells.size());
        for (MultiblockPattern.PatternCell cell : cells) {
            PartRole role = switch (cell.symbol()) {
                case 'C' -> PartRole.STABILITY;
                case 'D' -> PartRole.GLASS;
                case 'G' -> PartRole.CORE;
                default -> null;
            };
            if (role == null) {
                continue;
            }
            Direction outward = switch (role) {
                // 稳定机械方块按结构左右轴朝外，玻璃按深度轴朝外
                case STABILITY -> cell.col() < core.col() ? side.getOpposite() : side;
                case GLASS -> cell.slice() < core.slice() ? depth.getOpposite() : depth;
                case CORE -> null;
            };
            // 面板内序号：深度 1..3 → 0..2；行号自下而上，贴图行号自上而下，需翻转
            int panelX = cell.slice() - 1;
            int panelY = PANEL_SIZE - cell.row();
            parts.add(new Part(cell.pos(), role, outward, depth, panelX, panelY));
        }
        return parts;
    }

    public static BlockPos corePos(BlockPos controllerPos, Direction facing, boolean mirrorFront) {
        return controllerPos.relative(RelativeDirection.BACK.getFacing(facing, Direction.UP, false, mirrorFront), 2)
                .above(2);
    }
}
