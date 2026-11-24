package com.murthinext.ae2pr.api.machine.part;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.murthinext.ae2pr.api.recipe.FluidInput;
import com.murthinext.ae2pr.api.recipe.FluidOutput;
import com.murthinext.ae2pr.api.recipe.ItemInput;
import com.murthinext.ae2pr.api.recipe.ItemOutput;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.LongConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 总线与流体仓的事务快照，用于模拟扣料与产物容纳。
 */
public final class PartInventory {

    private static final BigInteger MAX_INPUT = BigInteger.valueOf(Long.MAX_VALUE);

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
        var itemSources = new ArrayList<InputSlot<ItemStack>>(items.size());
        for (ItemSlot slot : items) {
            if (!slot.stack.isEmpty()) {
                itemSources.add(new InputSlot<>(slot.stack, slot.stack.getCount(), n -> slot.stack.shrink((int) n)));
            }
        }
        var fluidSources = new ArrayList<InputSlot<FluidStack>>(fluids.size());
        for (TankSlot slot : fluids) {
            if (!slot.stack.isEmpty()) {
                fluidSources.add(new InputSlot<>(slot.stack, slot.stack.getAmount(), n -> slot.stack.shrink((int) n)));
            }
        }
        return consume(recipe, parallel, itemSources, fluidSources, false, false);
    }

    public static boolean consumeBuffered(
            MultiblockRecipe recipe,
            int parallel,
            Map<AEKey, BigInteger> keys,
            boolean simulate,
            ObjLongConsumer<AEKey> extract) {
        var itemSources = new ArrayList<InputSlot<ItemStack>>();
        var fluidSources = new ArrayList<InputSlot<FluidStack>>();
        for (var entry : keys.entrySet()) {
            AEKey key = entry.getKey();
            long amount = entry.getValue().min(MAX_INPUT).longValueExact();
            if (key instanceof AEItemKey item) {
                itemSources.add(new InputSlot<>(item.toStack(), amount, n -> extract.accept(key, n)));
            } else if (key instanceof AEFluidKey fluid) {
                fluidSources.add(new InputSlot<>(fluid.toStack(1), amount, n -> extract.accept(key, n)));
            }
        }
        return consume(recipe, parallel, itemSources, fluidSources, simulate, true);
    }

    private static boolean consume(
            MultiblockRecipe recipe,
            int parallel,
            List<InputSlot<ItemStack>> items,
            List<InputSlot<FluidStack>> fluids,
            boolean simulate,
            boolean requireConsumption) {
        var deductions = new ArrayList<Runnable>();
        if (!allocate(
                        recipe.getItemInputs(),
                        items,
                        parallel,
                        deductions,
                        ItemInput::catalyst,
                        ItemInput::count,
                        (input, stack) -> input.ingredient().test(stack))
                || !allocate(
                        recipe.getFluidInputs(),
                        fluids,
                        parallel,
                        deductions,
                        FluidInput::catalyst,
                        input -> input.stack().getAmount(),
                        (input, stack) -> input.stack().isFluidEqual(stack))
                || (requireConsumption && deductions.isEmpty())) {
            return false;
        }
        if (!simulate) {
            deductions.forEach(Runnable::run);
        }
        return true;
    }

    // 原料和催化剂共享库存，但只有原料会被扣除。
    private static <I, S> boolean allocate(
            List<I> inputs,
            List<InputSlot<S>> slots,
            int parallel,
            List<Runnable> deductions,
            Predicate<I> catalyst,
            ToIntFunction<I> count,
            BiPredicate<I, S> matches) {
        int requirements = inputs.size();
        if (requirements == 0) {
            return true;
        }
        int source = requirements + slots.size();
        int sink = source + 1;
        var graph = new ArrayList<List<Edge>>(sink + 1);
        for (int i = 0; i <= sink; i++) {
            graph.add(new ArrayList<>());
        }
        boolean[] isCatalyst = new boolean[requirements];
        for (int i = 0; i < requirements; i++) {
            I input = inputs.get(i);
            isCatalyst[i] = catalyst.test(input);
            long amount = (long) count.applyAsInt(input) * (isCatalyst[i] ? 1 : parallel);
            connect(graph, source, i, amount);
            boolean found = false;
            for (int j = 0; j < slots.size(); j++) {
                if (matches.test(input, slots.get(j).stack())) {
                    connect(graph, i, requirements + j, amount);
                    found = true;
                }
            }
            if (!found && amount > 0) {
                return false;
            }
        }
        for (int j = 0; j < slots.size(); j++) {
            connect(graph, requirements + j, sink, slots.get(j).amount());
        }
        int[] queue = new int[sink + 1];
        Edge[] parent = new Edge[sink + 1];
        while (true) {
            Arrays.fill(parent, null);
            queue[0] = source;
            int tail = 1;
            for (int head = 0; head < tail && parent[sink] == null; head++) {
                for (Edge edge : graph.get(queue[head])) {
                    if (edge.to != source && parent[edge.to] == null && edge.capacity > 0) {
                        parent[edge.to] = edge;
                        queue[tail++] = edge.to;
                    }
                }
            }
            if (parent[sink] == null) {
                if (graph.get(source).stream().anyMatch(edge -> edge.capacity > 0)) {
                    return false;
                }
                break;
            }
            long amount = Long.MAX_VALUE;
            for (int to = sink; to != source; to = parent[to].reverse.to) {
                amount = Math.min(amount, parent[to].capacity);
            }
            for (int to = sink; to != source; to = parent[to].reverse.to) {
                parent[to].capacity -= amount;
                parent[to].reverse.capacity += amount;
            }
        }
        for (int j = 0; j < slots.size(); j++) {
            long deducted = 0;
            for (Edge edge : graph.get(requirements + j)) {
                if (edge.to < requirements && !isCatalyst[edge.to]) {
                    deducted += edge.capacity;
                }
            }
            if (deducted > 0) {
                LongConsumer extract = slots.get(j).extract();
                long amount = deducted;
                deductions.add(() -> extract.accept(amount));
            }
        }
        return true;
    }

    private static void connect(List<List<Edge>> graph, int from, int to, long amount) {
        var forward = new Edge(to, amount);
        var reverse = new Edge(from, 0);
        forward.reverse = reverse;
        reverse.reverse = forward;
        graph.get(from).add(forward);
        graph.get(to).add(reverse);
    }

    private static final class Edge {
        private final int to;
        private long capacity;
        private Edge reverse;

        private Edge(int to, long capacity) {
            this.to = to;
            this.capacity = capacity;
        }
    }

    private record InputSlot<S>(S stack, long amount, LongConsumer extract) {}

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
