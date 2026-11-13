package com.murthinext.ae2pr.api.ctm;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

// 连接掩码：bit 0-5 六面，bit 6-17 十二棱
public final class CtmMask {

    public static final int NONE = 0;

    // 棱：两条不同轴的面方向
    private record Edge(Direction a, Direction b) {}

    // 邻居连接判定
    @FunctionalInterface
    public interface Joiner {
        boolean test(@Nullable Direction direction, BlockPos neighbor, BlockState neighborState);
    }

    private static final List<Edge> EDGES = List.of(
            new Edge(Direction.EAST, Direction.UP), new Edge(Direction.EAST, Direction.DOWN),
            new Edge(Direction.WEST, Direction.UP), new Edge(Direction.WEST, Direction.DOWN),
            new Edge(Direction.EAST, Direction.NORTH), new Edge(Direction.EAST, Direction.SOUTH),
            new Edge(Direction.WEST, Direction.NORTH), new Edge(Direction.WEST, Direction.SOUTH),
            new Edge(Direction.UP, Direction.NORTH), new Edge(Direction.UP, Direction.SOUTH),
            new Edge(Direction.DOWN, Direction.NORTH), new Edge(Direction.DOWN, Direction.SOUTH));

    private static final int[][] EDGE_BIT = new int[6][6];

    static {
        for (int[] row : EDGE_BIT) {
            Arrays.fill(row, -1);
        }
        for (int i = 0; i < EDGES.size(); i++) {
            Edge edge = EDGES.get(i);
            EDGE_BIT[edge.a().ordinal()][edge.b().ordinal()] = i;
            EDGE_BIT[edge.b().ordinal()][edge.a().ordinal()] = i;
        }
    }

    private CtmMask() {
    }

    public static int face(Direction direction) {
        return 1 << direction.ordinal();
    }

    public static int edge(Direction a, Direction b) {
        int bit = EDGE_BIT[a.ordinal()][b.ordinal()];
        return bit < 0 ? NONE : 1 << (6 + bit);
    }

    public static boolean connected(int mask, Direction direction) {
        return (mask & face(direction)) != 0;
    }

    // 棱方向是否连接，a、b 需为不同轴的面方向
    public static boolean connected(int mask, Direction a, Direction b) {
        return (mask & edge(a, b)) != 0;
    }

    // 六面 + 十二棱的连接掩码，棱仅当两条邻面均已连接时判定
    public static int compute(BlockAndTintGetter level, BlockPos pos, Joiner join) {
        int mask = NONE;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (join.test(direction, neighbor, level.getBlockState(neighbor))) {
                mask |= face(direction);
            }
        }
        for (Edge edge : EDGES) {
            if (!connected(mask, edge.a()) || !connected(mask, edge.b())) {
                continue;
            }
            BlockPos neighbor = pos.relative(edge.a()).relative(edge.b());
            if (join.test(null, neighbor, level.getBlockState(neighbor))) {
                mask |= edge(edge.a(), edge.b());
            }
        }
        return mask;
    }
}
