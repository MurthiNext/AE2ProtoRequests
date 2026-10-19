package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;

import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.MultiblockRecipe;

/**
 * 总线与流体仓的事务快照，用于模拟扣料与产物容纳。
 */
public final class PartInventory {

    private final List<ItemSlot> items = new ArrayList<>();
    private final List<TankSlot> fluids = new ArrayList<>();

    public PartInventory(List<ItemBusBlockEntity> buses, List<FluidHatchBlockEntity> hatches) {
        for (ItemBusBlockEntity bus : buses) {
            for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                items.add(new ItemSlot(bus.getStorage(), slot));
            }
        }
        for (FluidHatchBlockEntity hatch : hatches) {
            for (int tank = 0; tank < hatch.getTankCount(); tank++) {
                fluids.add(new TankSlot(hatch, hatch.getTank(tank)));
            }
        }
    }

    /**
     * 按配方扣除输入；原料重叠时按最大流分配，失败时不修改任何实际存储。
     */
    public boolean consume(MultiblockRecipe recipe, int parallel) {
        if (!consumeItems(recipe.getItemInputs(), parallel)) {
            return false;
        }
        for (FluidStack input : recipe.getFluidInputs()) {
            long remaining = (long) input.getAmount() * parallel;
            for (TankSlot slot : fluids) {
                if (remaining <= 0) {
                    break;
                }
                if (slot.stack.isFluidEqual(input)) {
                    int taken = (int) Math.min(remaining, slot.stack.getAmount());
                    slot.stack.shrink(taken);
                    remaining -= taken;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    // 按容量流分配重叠原料，支持同一标签匹配多个槽位。
    private boolean consumeItems(List<CountedIngredient> inputs, int parallel) {
        int source = inputs.size() + items.size();
        int sink = source + 1;
        long[][] capacity = new long[sink + 1][sink + 1];
        long required = 0;
        for (int i = 0; i < inputs.size(); i++) {
            CountedIngredient input = inputs.get(i);
            long amount = (long) input.count() * parallel;
            capacity[source][i] = amount;
            required += amount;
            for (int j = 0; j < items.size(); j++) {
                if (input.ingredient().test(items.get(j).stack)) {
                    capacity[i][inputs.size() + j] = amount;
                }
            }
        }
        for (int j = 0; j < items.size(); j++) {
            capacity[inputs.size() + j][sink] = items.get(j).stack.getCount();
        }
        long matched = 0;
        int[] queue = new int[sink + 1];
        while (matched < required) {
            int[] parent = new int[sink + 1];
            Arrays.fill(parent, -1);
            parent[source] = source;
            queue[0] = source;
            int tail = 1;
            for (int head = 0; head < tail && parent[sink] == -1; head++) {
                int from = queue[head];
                for (int to = 0; to <= sink; to++) {
                    if (parent[to] == -1 && capacity[from][to] > 0) {
                        parent[to] = from;
                        queue[tail++] = to;
                    }
                }
            }
            if (parent[sink] == -1) {
                return false;
            }
            long amount = required - matched;
            for (int to = sink; to != source; to = parent[to]) {
                amount = Math.min(amount, capacity[parent[to]][to]);
            }
            for (int to = sink; to != source; to = parent[to]) {
                capacity[parent[to]][to] -= amount;
                capacity[to][parent[to]] += amount;
            }
            matched += amount;
        }
        for (int j = 0; j < items.size(); j++) {
            items.get(j).stack.setCount((int) capacity[inputs.size() + j][sink]);
        }
        return true;
    }

    /**
     * 模拟写入产物，返回全部放得下时为 true；快照需 {@link #commit()} 才生效。
     */
    public boolean insert(MultiblockRecipe recipe, int parallel) {
        for (ItemStack output : recipe.getItemOutputs()) {
            long remaining = (long) output.getCount() * parallel;
            // 先在同类物品槽位堆叠，再占用空槽位
            for (boolean sameItem : new boolean[] { true, false }) {
                for (ItemSlot slot : items) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (sameItem ? !ItemStack.isSameItemSameTags(slot.stack, output) : !slot.stack.isEmpty()) {
                        continue;
                    }
                    int capacity = slot.storage.getSlotLimit(slot.index);
                    int moved = (int) Math.min(remaining, Math.max(0, capacity - slot.stack.getCount()));
                    if (moved > 0) {
                        slot.stack = output.copyWithCount(slot.stack.getCount() + moved);
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        for (FluidStack output : recipe.getFluidOutputs()) {
            long remaining = (long) output.getAmount() * parallel;
            for (boolean sameFluid : new boolean[] { true, false }) {
                for (TankSlot slot : fluids) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (sameFluid ? !slot.stack.isFluidEqual(output) : !slot.stack.isEmpty()) {
                        continue;
                    }
                    int moved = (int) Math.min(remaining,
                            Math.max(0, slot.tank.getCapacity() - slot.stack.getAmount()));
                    if (moved > 0) {
                        int amount = slot.stack.getAmount() + moved;
                        slot.stack = output.copy();
                        slot.stack.setAmount(amount);
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public void commit() {
        for (ItemSlot slot : items) {
            slot.storage.setStackInSlot(slot.index, slot.stack);
        }
        for (TankSlot slot : fluids) {
            slot.tank.setFluid(slot.stack);
            slot.hatch.setChanged();
        }
    }

    private static final class ItemSlot {

        private final ItemStackHandler storage;
        private final int index;
        private ItemStack stack;

        private ItemSlot(ItemStackHandler storage, int index) {
            this.storage = storage;
            this.index = index;
            this.stack = storage.getStackInSlot(index).copy();
        }
    }

    private static final class TankSlot {

        private final FluidHatchBlockEntity hatch;
        private final FluidTank tank;
        private FluidStack stack;

        private TankSlot(FluidHatchBlockEntity hatch, FluidTank tank) {
            this.hatch = hatch;
            this.tank = tank;
            this.stack = tank.getFluid().copy();
        }
    }
}
