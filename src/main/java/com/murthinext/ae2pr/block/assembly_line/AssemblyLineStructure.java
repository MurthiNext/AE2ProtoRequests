package com.murthinext.ae2pr.block.assembly_line;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 水晶装配线结构定义与外观同步。
 * <p>
 * 模式基于通用多方块模块（{@code com.murthinext.ae2pr.multiblock}）声明：
 * <ul>
 * <li><b>片（slice）</b>：沿控制器<b>右侧</b>延伸，首尾片固定，中间 3 片起可重复，总片数上限由配置决定</li>
 * <li><b>行（string）</b>：沿<b>上</b>递增（第 0 行是最底行）</li>
 * <li><b>列（char）</b>：沿控制器<b>背面</b>递增（前→后深度）</li>
 * </ul>
 * 匹配支持左右 / 前后镜像，任一布局成立即成型。
 * <p>
 * 每片横截面（3 深 × 4 高，自下而上、前→后）：
 *
 * <pre>
 * 顶  # Y #
 *     S A G      S=控制器(最前) A=装配线外壳 G=装配线格栅(最后)
 *     R T R      R=夹层玻璃 T=装配线控制外壳
 * 底  F I F      F=水晶机壳 I=输入总线
 * </pre>
 *
 * 机壳位 'F' 可用输入仓或能源仓替代，其中<b>能源仓整结构最多 1 个</b>。
 */
public final class AssemblyLineStructure {

    /** 结构模式：控制器位于第 2 行、第 0 列。 */
    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(2, 0)
            .slice("FIF", "RTR", "SAG", "#Y#")
            .sliceRepeatable(3, Integer.MAX_VALUE, "FIF", "RTR", "DAG", "#Y#")
            .slice("FOF", "RTR", "DAG", "#Y#")
            .where('S', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()))
            // 机壳位允许输入仓替代；能源仓同样可替代机壳，但整结构最多 1 个
            .where('F', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()))
                    .or(StructurePredicate.blocks(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()).maxCount(1)))
            .where('Y', StructurePredicate.blocks(ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()))
            .where('I', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
            .where('O', StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))
            .where('A', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_CASING.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
            .where('D', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()))
            .where('R', StructurePredicate.blocks(ModBlocks.CRYSTAL_GLASS.get()))
            .where('T', StructurePredicate.blocks(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get()))
            .where('#', StructurePredicate.any())
            .build();

    /** 整体最小片数（首尾固定 + 最少中间片） */
    public static final int MIN_SLICES = PATTERN.minSlices();

    private AssemblyLineStructure() {
    }

    /** 当前配置允许的最大片数（下限为最小片数）。 */
    public static int maxSlices() {
        return Math.max(MIN_SLICES, Config.assemblyLineMaxSlices());
    }

    /**
     * 同步结构内“状态型”方块的外观：
     * <ul>
     * <li>成型：按匹配到的镜像方向，部件（总线/仓）切换为成型贴图；控制外壳仅在运行配方时才点亮工作态</li>
     * <li>失活：逐一复位全部镜像布局的可能位置（含结构损坏或控制器被拆后的残留状态）</li>
     * </ul>
     */
    public static void updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result,
            boolean running) {
        if (result.formed()) {
            applyState(level, PATTERN.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                    result.mirrorFront(), result.slices()), true, running);
            return;
        }
        // 未成型：不确定此前是哪种镜像布局，逐一复位全部组合
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyState(level, PATTERN.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1], maxSlices()),
                    false, false);
        }
    }

    /**
     * @param partsFormed 部件（总线/仓）是否切换成型贴图
     * @param unitActive  控制外壳是否点亮工作态贴图（仅运行配方时为 true）
     */
    private static void applyState(Level level, List<BlockPos> cells, boolean partsFormed, boolean unitActive) {
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            BlockState updated = null;
            if (state.is(ModBlocks.CRYSTAL_ASSEMBLY_LINE_UNIT.get())) {
                if (state.getValue(AssemblyLineUnitBlock.ACTIVE) != unitActive) {
                    updated = state.setValue(AssemblyLineUnitBlock.ACTIVE, unitActive);
                }
            } else if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())) {
                if (state.getValue(FluixCrystalEnergyHatchBlock.FORMED) != partsFormed) {
                    updated = state.setValue(FluixCrystalEnergyHatchBlock.FORMED, partsFormed);
                }
            } else if (isPart(state)) {
                BlockState candidate = state;
                if (candidate.getValue(CertusQuartzCrystalMachinePartBlock.FORMED) != partsFormed) {
                    candidate = candidate.setValue(CertusQuartzCrystalMachinePartBlock.FORMED, partsFormed);
                }
                // 由装配线成型时外观为水晶机壳（style=1）
                if (candidate.getValue(CertusQuartzCrystalMachinePartBlock.STYLE) != 1) {
                    candidate = candidate.setValue(CertusQuartzCrystalMachinePartBlock.STYLE, 1);
                }
                if (candidate != state) {
                    updated = candidate;
                }
            }
            if (updated != null) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean isPart(BlockState state) {
        return state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get())
                || state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get())
                || state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get());
    }
}
