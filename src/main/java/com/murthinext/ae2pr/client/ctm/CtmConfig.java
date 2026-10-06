package com.murthinext.ae2pr.client.ctm;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;

/**
 * 连接纹理的逐方块配置。
 * <p>
 * 规则：默认<strong>只有同种方块才互相连接</strong>（跨方块种类不连接），
 * 且方块必须在此登记才启用连接纹理；未登记的方块（控制器、装配线外壳、控制外壳）完全不参与连接。
 * 例外：<strong>水晶强化复合机械方块 / 赛特斯石英水晶输入总线 / 输入仓 / 输出总线</strong>归为同一"机身族"，
 * 彼此相邻（无论是否成型）都会连接，因为它们最终使用同一张机壳贴图。
 */
public final class CtmConfig {

    /**
     * 十二条棱（对角）方向对，{@link #worldMask} 中占用 bit 6+i。
     * 棱邻居 = 两个相邻面方向交叠处的方块，用于 47-tile 连接纹理的内角判定。
     */
    private static final Direction[][] EDGES = {
            { Direction.EAST, Direction.UP }, { Direction.EAST, Direction.DOWN },
            { Direction.WEST, Direction.UP }, { Direction.WEST, Direction.DOWN },
            { Direction.EAST, Direction.NORTH }, { Direction.EAST, Direction.SOUTH },
            { Direction.WEST, Direction.NORTH }, { Direction.WEST, Direction.SOUTH },
            { Direction.UP, Direction.NORTH }, { Direction.UP, Direction.SOUTH },
            { Direction.DOWN, Direction.NORTH }, { Direction.DOWN, Direction.SOUTH },
    };

    /** 方向对 -> 棱位序号（-1 = 同轴/相反，非棱组合） */
    private static final int[][] EDGE_BIT = new int[6][6];

    static {
        for (int[] row : EDGE_BIT) {
            Arrays.fill(row, -1);
        }
        for (int i = 0; i < EDGES.length; i++) {
            EDGE_BIT[EDGES[i][0].ordinal()][EDGES[i][1].ordinal()] = i;
            EDGE_BIT[EDGES[i][1].ordinal()][EDGES[i][0].ordinal()] = i;
        }
    }

    private CtmConfig() {
    }

    /** 该方块是否启用连接纹理。 */
    public static boolean enabled(BlockState state) {
        return familyKey(state) != 0;
    }

    /** 邻居是否与本方块可连接（同种外观）。 */
    public static boolean connects(BlockState neighbor, BlockState self) {
        int key = familyKey(self);
        return key != 0 && key == familyKey(neighbor);
    }

    /**
     * 连接族标识：同一标识之间才连接。
     * <ul>
     * <li>{@code 1} 机身族：水晶强化复合机械方块 + 三种仓室 + 能源仓（未成型用赛特斯机壳贴图，也与机壳相连）</li>
     * <li>{@code 2} 装配线格栅（同种相连）</li>
     * <li>{@code 3} 水晶玻璃（同种相连）</li>
     * <li>{@code 4} 锆刚玉砖块（同种相连）</li>
     * <li>{@code 0} 不参与连接纹理（控制器、装配线外壳、控制外壳）</li>
     * </ul>
     */
    private static int familyKey(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()
                || block == ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()) {
            return 1;
        }
        if (block == ModBlocks.CRYSTAL_ASSEMBLY_LINE_GRATING.get()) {
            return 2;
        }
        if (block == ModBlocks.CRYSTAL_GLASS.get()) {
            return 3;
        }
        if (block == ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()) {
            return 4;
        }
        return 0;
    }

    /**
     * 世界空间连接掩码：bit 0-5 = 六面方向（位 = Direction#ordinal），bit 6-17 = 十二棱（对角）方向。
     * 棱邻居只在两条邻面都已连接时才可能影响内角，此时才查询以省去无谓的取块。
     */
    public static int worldMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (connects(level.getBlockState(pos.relative(dir)), state)) {
                mask |= 1 << dir.ordinal();
            }
        }
        for (int i = 0; i < EDGES.length; i++) {
            if (!connected(mask, EDGES[i][0]) || !connected(mask, EDGES[i][1])) {
                continue;
            }
            if (connects(level.getBlockState(pos.relative(EDGES[i][0]).relative(EDGES[i][1])), state)) {
                mask |= 1 << (6 + i);
            }
        }
        return mask;
    }

    public static boolean connected(int worldMask, Direction dir) {
        return (worldMask & (1 << dir.ordinal())) != 0;
    }

    /** 棱（对角）方向是否连接：a、b 必须为不同轴的两个面方向。 */
    public static boolean connected(int worldMask, Direction a, Direction b) {
        int bit = EDGE_BIT[a.ordinal()][b.ordinal()];
        return bit >= 0 && (worldMask & (1 << (6 + bit))) != 0;
    }
}
