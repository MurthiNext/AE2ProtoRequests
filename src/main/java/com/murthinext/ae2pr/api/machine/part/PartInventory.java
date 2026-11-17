package com.murthinext.ae2pr.api.machine.part;

import com.murthinext.ae2pr.api.recipe.FluidInput;
import com.murthinext.ae2pr.api.recipe.FluidOutput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 总线与流体仓的事务快照，用于模拟扣料与产物容纳。
 */
public final class PartInventory {

    private final List<ItemSlot> items = new ArrayList<>();
    private final List<TankSlot> fluids = new ArrayList<>();

    public PartInventory(List<MachinePart.ItemPart> buses, List<MachinePart.FluidPart> hatches) {
        for (MachinePart.ItemPart bus : buses) {
            for (int slot = 0; slot < bus.slotCount(); slot++) {
                items.add(new ItemSlot(bus.storage(), slot));
            }
        }
        for (MachinePart.FluidPart hatch : hatches) {
            for (int tank = 0; tank < hatch.tankCount(); tank++) {
                fluids.add(new TankSlot(hatch, hatch.tank(tank)));
            }
        }
    }

    // 按配方扣除输入；原料与催化剂共享同一最大流，仅非催化剂节点会实际扣除
    public boolean consume(MultiblockRecipe recipe, int parallel) {
        return consumeItems(recipe, parallel) && consumeFluids(recipe, parallel);
    }

    // 原料按并行放大，催化剂按条目数量固定；同物品重叠时由最大流统一分配，仅原料会被扣除
    private boolean consumeItems(MultiblockRecipe recipe, int parallel) {
        List<ItemInput> inputs = recipe.getItemInputs();
        int requirements = inputs.size();
        int source = requirements + items.size();
        int sink = source + 1;
        long[][] capacity = new long[sink + 1][sink + 1];
        long required = 0;
        for (int i = 0; i < requirements; i++) {
            ItemInput input = inputs.get(i);
            long amount = input.catalyst() ? input.count() : (long) input.count() * parallel;
            capacity[source][i] = amount;
            required += amount;
            for (int j = 0; j < items.size(); j++) {
                if (input.ingredient().test(items.get(j).stack)) {
                    capacity[i][requirements + j] = amount;
                }
            }
        }
        for (int j = 0; j < items.size(); j++) {
            capacity[requirements + j][sink] = items.get(j).stack.getCount();
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
        // 由反向边读取已分配量，仅累加原料节点；催化剂占用但不变动库存
        for (int j = 0; j < items.size(); j++) {
            int slotNode = requirements + j;
            long deducted = 0;
            for (int i = 0; i < requirements; i++) {
                if (!inputs.get(i).catalyst()) {
                    deducted += capacity[slotNode][i];
                }
            }
            items.get(j).stack.shrink((int)
                    Math.min(deducted, items.get(j).stack.getCount()));
        }
        return true;
    }

    // 流体原料按并行扣除，催化剂只检查存在；同种流体合并后与总存量比较
    private boolean consumeFluids(MultiblockRecipe recipe, int parallel) {
        List<FluidInput> inputs = recipe.getFluidInputs();
        if (!hasFluids(inputs, parallel)) {
            return false;
        }
        for (FluidInput input : inputs) {
            if (input.catalyst()) {
                continue;
            }
            long remaining = (long) input.stack().getAmount() * parallel;
            for (TankSlot slot : fluids) {
                if (remaining <= 0) {
                    break;
                }
                if (slot.stack.isFluidEqual(input.stack())) {
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

    private boolean hasFluids(List<FluidInput> inputs, int parallel) {
        List<FluidStack> requirements = new ArrayList<>(inputs.size());
        for (FluidInput input : inputs) {
            long amount = input.catalyst()
                    ? input.stack().getAmount()
                    : (long) input.stack().getAmount() * parallel;
            mergeFluid(requirements, input.stack(), amount);
        }
        for (FluidStack requirement : requirements) {
            if (available(requirement) < requirement.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private static void mergeFluid(List<FluidStack> requirements, FluidStack fluid, long amount) {
        for (FluidStack existing : requirements) {
            if (existing.isFluidEqual(fluid)) {
                long total = Math.min(Integer.MAX_VALUE, (long) existing.getAmount() + amount);
                existing.setAmount((int) total);
                return;
            }
        }
        FluidStack merged = fluid.copy();
        merged.setAmount((int) Math.min(Integer.MAX_VALUE, amount));
        requirements.add(merged);
    }

    private long available(FluidStack fluid) {
        long amount = 0;
        for (TankSlot slot : fluids) {
            if (slot.stack.isFluidEqual(fluid)) {
                amount += slot.stack.getAmount();
            }
        }
        return amount;
    }

    // 模拟写入产物，返回全部放得下时为 true；快照需 {@link #commit()} 才生效
    public boolean insert(MultiblockRecipe recipe, int parallel) {
        // 概率产物按全量预留，避免实际产出时溢出掉落
        for (ItemOutput output : recipe.getItemOutputs()) {
            ItemStack stack = output.stack();
            long remaining = (long) stack.getCount() * parallel;
            // 先在同类物品槽位堆叠，再占用空槽位
            for (boolean sameItem : new boolean[] {true, false}) {
                for (ItemSlot slot : items) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (sameItem ? !ItemStack.isSameItemSameTags(slot.stack, stack) : !slot.stack.isEmpty()) {
                        continue;
                    }
                    int capacity = slot.storage.getSlotLimit(slot.index);
                    int moved = (int) Math.min(remaining, Math.max(0, capacity - slot.stack.getCount()));
                    if (moved > 0) {
                        slot.stack = stack.copyWithCount(slot.stack.getCount() + moved);
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        for (FluidOutput output : recipe.getFluidOutputs()) {
            FluidStack stack = output.stack();
            long remaining = (long) stack.getAmount() * parallel;
            for (boolean sameFluid : new boolean[] {true, false}) {
                for (TankSlot slot : fluids) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (sameFluid ? !slot.stack.isFluidEqual(stack) : !slot.stack.isEmpty()) {
                        continue;
                    }
                    int moved =
                            (int) Math.min(remaining, Math.max(0, slot.tank.getCapacity() - slot.stack.getAmount()));
                    if (moved > 0) {
                        int amount = slot.stack.getAmount() + moved;
                        slot.stack = stack.copy();
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
            slot.hatch.markChanged();
        }
    }

    private static final class ItemSlot {

        private final IItemHandlerModifiable storage;
        private final int index;
        private ItemStack stack;

        private ItemSlot(IItemHandlerModifiable storage, int index) {
            this.storage = storage;
            this.index = index;
            this.stack = storage.getStackInSlot(index).copy();
        }
    }

    private static final class TankSlot {

        private final MachinePart.FluidPart hatch;
        private final FluidTank tank;
        private FluidStack stack;

        private TankSlot(MachinePart.FluidPart hatch, FluidTank tank) {
            this.hatch = hatch;
            this.tank = tank;
            this.stack = tank.getFluid().copy();
        }
    }
}
