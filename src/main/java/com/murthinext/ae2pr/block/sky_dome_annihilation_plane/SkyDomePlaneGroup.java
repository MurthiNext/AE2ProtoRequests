package com.murthinext.ae2pr.block.sky_dome_annihilation_plane;

import java.util.ArrayDeque;
import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import appeng.api.parts.IPartHost;

import com.murthinext.ae2pr.ServerConfig;

final class SkyDomePlaneGroup {

    static final int OPTIMAL_PLANES = 16;
    static final int MAX_PLANES = 64;

    private static final double TERMINAL_RATIO = 0.75;
    private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    private SkyDomePlaneGroup() {
    }

    // 相连的板超过 MAX_PLANES 后效率已经饱和，只需收集到这个数量。
    static double efficiency(SkyDomeAnnihilationPlanePart origin) {
        double min = ServerConfig.skyDomeMinEfficiency();
        double max = Math.max(min, ServerConfig.skyDomeMaxEfficiency());
        double terminal = Math.max(min, max * TERMINAL_RATIO);

        var level = origin.getLevel();
        var side = origin.getSide();
        var start = origin.getBlockEntity().getBlockPos();
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            if (visited.size() >= MAX_PLANES) {
                return terminal;
            }
            var pos = queue.poll();
            for (var direction : HORIZONTAL) {
                var next = pos.relative(direction);
                if (visited.contains(next) || !(level.getBlockEntity(next) instanceof IPartHost host) ||
                    !(host.getPart(side) instanceof SkyDomeAnnihilationPlanePart)) {
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }

        if (visited.size() >= MAX_PLANES) {
            return terminal;
        }

        int count = visited.size();
        int perimeter = 0;
        for (var pos : visited) {
            perimeter += 4;
            if (visited.contains(pos.east())) {
                perimeter -= 2;
            }
            if (visited.contains(pos.north())) {
                perimeter -= 2;
            }
        }

        // 2D 等周不等式：周长固定的形状中正方形最紧凑，16n/P² 在正方形时为 1。
        double shape = Math.min(1.0, 16.0 * count / ((double) perimeter * perimeter));
        double curve;
        if (count <= 1) {
            curve = min;
        } else if (count <= OPTIMAL_PLANES) {
            curve = min + (max - min) * (count - 1.0) / (OPTIMAL_PLANES - 1);
        } else {
            curve = max - (max - terminal) * (count - OPTIMAL_PLANES) / (MAX_PLANES - OPTIMAL_PLANES);
        }
        return min + (curve - min) * shape;
    }
}
