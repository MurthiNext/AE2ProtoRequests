package com.murthinext.ae2pr.logic.world_interaction;

import java.util.Arrays;

/**
 * 按原料匹配关系分配物品数量并计算最大并行数。
 */
public record WorldInteractionInputPlan(int batches, int[] consumed) {

    public static WorldInteractionInputPlan find(int[] available, int[] required, boolean[][] matches, int limit) {
        long totalAvailable = Arrays.stream(available).asLongStream().sum();
        long totalRequired = Arrays.stream(required).asLongStream().sum();
        int low = 0;
        int high = totalRequired <= 0 ? 0 : (int) Math.min(limit, totalAvailable / totalRequired);
        int[] consumed = new int[available.length];
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            int[] plan = allocate(available, required, matches, middle);
            if (plan == null) {
                high = middle - 1;
            } else {
                low = middle;
                consumed = plan;
            }
        }
        return new WorldInteractionInputPlan(low, consumed);
    }

    private static int[] allocate(int[] available, int[] required, boolean[][] matches, int batches) {
        int sink = available.length + required.length + 1;
        long[][] residual = new long[sink + 1][sink + 1];
        for (int item = 0; item < available.length; item++) {
            residual[0][item + 1] = available[item];
            for (int ingredient = 0; ingredient < required.length; ingredient++) {
                if (matches[item][ingredient]) {
                    residual[item + 1][available.length + ingredient + 1] = available[item];
                }
            }
        }
        long needed = 0;
        for (int ingredient = 0; ingredient < required.length; ingredient++) {
            long count = (long) required[ingredient] * batches;
            residual[available.length + ingredient + 1][sink] = count;
            needed += count;
        }
        int[] parent = new int[sink + 1];
        int[] queue = new int[sink + 1];
        while (needed > 0) {
            Arrays.fill(parent, -1);
            parent[0] = 0;
            queue[0] = 0;
            int tail = 1;
            for (int head = 0; head < tail && parent[sink] == -1; head++) {
                int from = queue[head];
                for (int to = 1; to <= sink; to++) {
                    if (parent[to] == -1 && residual[from][to] > 0) {
                        parent[to] = from;
                        queue[tail++] = to;
                    }
                }
            }
            if (parent[sink] == -1) {
                return null;
            }
            long amount = needed;
            for (int to = sink; to != 0; to = parent[to]) {
                amount = Math.min(amount, residual[parent[to]][to]);
            }
            for (int to = sink; to != 0; to = parent[to]) {
                int from = parent[to];
                residual[from][to] -= amount;
                residual[to][from] += amount;
            }
            needed -= amount;
        }
        int[] consumed = new int[available.length];
        for (int item = 0; item < available.length; item++) {
            consumed[item] = available[item] - (int) residual[0][item + 1];
        }
        return consumed;
    }
}
