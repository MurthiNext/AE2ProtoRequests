package com.murthinext.ae2pr.block.lava_smelter;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.assembly_line.CertusQuartzCrystalMachinePartBlock;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;
import com.murthinext.ae2pr.multiblock.StructureResult;

/**
 * 高反应性熔岩冶炼炉结构定义与外观同步。
 * <p>
 * 模式按 {@code misc/high_reactivity_lava_smelter.nbt} 逐片照抄（木桶位替换为主机），
 * 参考坐标系与装配线指南 NBT 相同：主机朝东时，NBT 的 +z 为"主机右侧"、-x 为"主机后方"。
 * <ul>
 * <li><b>片（slice）</b>：沿控制器右侧延伸，共 4 片，第 0 片含主机</li>
 * <li><b>行（string）</b>：沿上递增（第 0 行是最底行），共 8 行</li>
 * <li><b>列（char）</b>：沿控制器背面递增（前→后深度），共 4 列</li>
 * </ul>
 * 字符含义：{@code C}=主机（原 NBT 木桶位） {@code B}=锆刚玉砖块（可替换为输入/输出总线）
 * {@code G}=耐火水晶玻璃 {@code P}=陨钢管道（NBT 中炉膛顶朝下、烟囱朝上，结构只校验方块类型）
 * {@code L}=物流控制机械方块 {@code #}=任意（炉膛内部与烟囱周围留空）。
 * <p>
 * 成型后，结构内的输入/输出总线切换为锆刚玉砖块外观（style=2，仓口保留），
 * 与周围的砖块使用同一连接纹理族。
 */
public final class LavaSmelterStructure {

    /** 结构模式：主机位于第 0 行、第 0 列。 */
    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(0, 0)
            .slice("CBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#PB#", "#P##", "#P##")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#BB#", "####", "####")
            .slice("BBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()))
            .where('B', StructurePredicate.blocks(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get())
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()))
                    .or(StructurePredicate.blocks(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get())))
            .where('G', StructurePredicate.blocks(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()))
            .where('P', StructurePredicate.blocks(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('#', StructurePredicate.any())
            .build();

    /** 炉膛内部空腔的尺寸（2×2×2） */
    public static final int CAVITY_SIZE = 2;

    private LavaSmelterStructure() {
    }

    /**
     * 同步结构内输入/输出总线的外观：
     * <ul>
     * <li>成型：切换为锆刚玉砖块外壳（style=2），仓口保留</li>
     * <li>失活：逐一复位全部镜像布局的可能位置（含结构损坏或控制器被拆后的残留状态）</li>
     * </ul>
     */
    public static void updateFormed(Level level, BlockPos controllerPos, Direction facing, StructureResult result) {
        if (result.formed()) {
            applyParts(level, PATTERN.cells(controllerPos, facing, Direction.UP, result.mirrorSide(),
                    result.mirrorFront(), result.slices()), true);
            return;
        }
        // 未成型：不确定此前是哪种镜像布局，逐一复位全部组合
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            applyParts(level, PATTERN.cells(controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                    PATTERN.minSlices()), false);
        }
    }

    private static void applyParts(Level level, List<BlockPos> cells, boolean formed) {
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            if (!(state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get())
                    || state.is(ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()))) {
                continue;
            }
            BlockState updated = state;
            if (updated.getValue(CertusQuartzCrystalMachinePartBlock.FORMED) != formed) {
                updated = updated.setValue(CertusQuartzCrystalMachinePartBlock.FORMED, formed);
            }
            int style = formed ? 2 : 1;
            if (updated.getValue(CertusQuartzCrystalMachinePartBlock.STYLE) != style) {
                updated = updated.setValue(CertusQuartzCrystalMachinePartBlock.STYLE, style);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    /** 炉膛底面 2×2 池面（空腔最底层）的世界坐标：用于假熔岩渲染与伤害判定。 */
    public static List<BlockPos> poolCells(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        Direction sliceFacing = RelativeDirection.RIGHT.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        Direction charFacing = RelativeDirection.BACK.getFacing(facing, Direction.UP, mirrorSide, mirrorFront);
        List<BlockPos> cells = new ArrayList<>(CAVITY_SIZE * CAVITY_SIZE);
        for (int slice = 1; slice <= CAVITY_SIZE; slice++) {
            for (int col = 1; col <= CAVITY_SIZE; col++) {
                cells.add(controllerPos.relative(sliceFacing, slice).relative(Direction.UP, 1)
                        .relative(charFacing, col));
            }
        }
        return cells;
    }

    /** 炉膛空腔（底面 2×2、高 2）的世界包围盒：用于空腔内的熔岩伤害判定。 */
    public static AABB cavityAabb(BlockPos controllerPos, Direction facing, boolean mirrorSide,
            boolean mirrorFront) {
        List<BlockPos> cells = poolCells(controllerPos, facing, mirrorSide, mirrorFront);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = cells.get(0).getY();
        for (BlockPos cell : cells) {
            minX = Math.min(minX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        return new AABB(minX, y, minZ, maxX + 1, y + CAVITY_SIZE, maxZ + 1);
    }
}
