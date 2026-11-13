package com.murthinext.ae2pr.item.proto_terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.api.multiblock.StructureOrientation;
import com.murthinext.ae2pr.api.multiblock.StructurePredicate;

/**
 * 按结构模板补齐多方块：已匹配的方块保留，空位用背包内的对应方块填充。
 * 创造模式不消耗物品，生存模式缺料时能搭多少搭多少。
 */
public final class MultiblockAssembler {

    private MultiblockAssembler() {
    }

    // 一格待处理方块
    private record Placement(BlockPos pos, StructurePredicate predicate, Block block) {}

    // 选定的一组镜像方案，谓词按该方案解释朝向
    private record Layout(MultiblockPattern pattern, List<MultiblockPattern.PatternCell> cells,
                          StructureOrientation orientation) {

        StructurePredicate predicateOf(MultiblockPattern.PatternCell cell) {
            return pattern.predicate(cell.symbol());
        }
    }

    private static final class Plan {

        private final List<Placement> ready = new ArrayList<>();
        private final Map<Block, Integer> missing = new LinkedHashMap<>();
        private int blocked;
        private int misoriented;

        void block() {
            blocked++;
        }

        void misorient() {
            misoriented++;
        }

        void add(Placement placement) {
            ready.add(placement);
        }

        void lack(Block block) {
            missing.merge(block, 1, Integer::sum);
        }
    }

    public static void assemble(ServerPlayer player, BlockPos controllerPos, MachineBlock<?> machine,
                                InteractionHand hand) {
        Level level = player.level();
        if (!level.mayInteract(player, controllerPos)) {
            return;
        }
        MultiblockPattern pattern = machine.definition().structure().pattern();
        Direction facing = level.getBlockState(controllerPos).getValue(MachineBlock.FACING);
        Layout layout = bestLayout(level, pattern, controllerPos, facing);
        Plan plan = plan(player, level, layout);

        BlockPos center = center(layout.cells());
        // 自下而上放置：材料不足时留下的残缺结构更接近完工状态
        plan.ready.sort(Comparator.comparingInt(placement -> placement.pos().getY()));
        List<Placement> placed = new ArrayList<>(plan.ready.size());
        for (Placement placement : plan.ready) {
            if (!player.isCreative() && !consume(player, placement.block().asItem())) {
                continue;
            }
            Direction outward = outward(center, placement.pos(), facing.getOpposite());
            level.setBlock(placement.pos(), stateFor(player, hand, placement, outward, layout.orientation()),
                           Block.UPDATE_ALL);
            placed.add(placement);
        }
        settle(level, placed, layout.orientation());
        if (!placed.isEmpty() &&
            level.getBlockEntity(controllerPos) instanceof AbstractMultiblockControllerBlockEntity controller) {
            controller.validateStructure();
        }
        report(player, machine, plan, placed.size());
    }

    // 逐格挑选谓词的首选方块，机壳格子只放机壳
    private static Plan plan(Player player, Level level, Layout layout) {
        Plan plan = new Plan();
        boolean limited = !player.isCreative();
        Map<Item, Integer> budget = limited ? budget(player) : Map.of();
        for (MultiblockPattern.PatternCell cell : layout.cells()) {
            StructurePredicate predicate = layout.predicateOf(cell);
            BlockState existing = level.getBlockState(cell.pos());
            if (predicate.test(level, cell.pos(), existing, layout.orientation()) != null) {
                continue;
            }
            Block chosen = placeable(predicate);
            if (chosen == null || !existing.canBeReplaced() || occupiedByEntity(level, cell.pos())) {
                // 方块已在位但朝向不符时单独计数，由玩家自己摆正
                if (predicate.testBlock(level, cell.pos(), existing) != null) {
                    plan.misorient();
                } else {
                    plan.block();
                }
                continue;
            }
            if (limited) {
                Item item = chosen.asItem();
                int remaining = budget.getOrDefault(item, 0);
                if (remaining <= 0) {
                    plan.lack(chosen);
                    continue;
                }
                budget.put(item, remaining - 1);
            }
            plan.add(new Placement(cell.pos(), predicate, chosen));
        }
        return plan;
    }

    // 有生物或掉落物占据的位置留空，避免把实体封进结构
    private static boolean occupiedByEntity(Level level, BlockPos pos) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos))) {
            if (!entity.isSpectator() && !(entity instanceof ItemEntity)) {
                return true;
            }
        }
        return false;
    }

    // 已有的部分方块可能按镜像搭建，取命中方块最多的镜像方案
    private static Layout bestLayout(Level level, MultiblockPattern pattern, BlockPos controllerPos, Direction facing) {
        Layout best = null;
        int bestScore = -1;
        for (boolean[] mirror : MultiblockPattern.MIRRORS) {
            int score = pattern.score(level, controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                                      pattern.minSlices());
            if (score > bestScore) {
                bestScore = score;
                best = new Layout(pattern,
                                  pattern.patternCells(controllerPos, facing, Direction.UP, mirror[0], mirror[1],
                                                       pattern.minSlices()),
                                  new StructureOrientation(facing, Direction.UP, mirror[0], mirror[1]));
            }
        }
        return best;
    }

    // 谓词的首选方块，取第一个有对应物品的候选
    @Nullable
    private static Block placeable(StructurePredicate predicate) {
        for (Block block : predicate.previewBlocks()) {
            if (block.asItem() != Items.AIR) {
                return block;
            }
        }
        return null;
    }

    private static Map<Item, Integer> budget(Player player) {
        Map<Item, Integer> budget = new HashMap<>();
        countInto(budget, player.getInventory().items);
        countInto(budget, player.getInventory().offhand);
        return budget;
    }

    private static void countInto(Map<Item, Integer> budget, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                budget.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    private static boolean consume(Player player, Item item) {
        return consume(player.getInventory().items, item) || consume(player.getInventory().offhand, item);
    }

    private static boolean consume(List<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // 先按玩家放置时的上下文决定初始状态，再交给谓词的朝向要求摆成可成型姿态
    private static BlockState stateFor(Player player, InteractionHand hand, Placement placement, Direction outward,
                                       StructureOrientation orientation) {
        ItemStack stack = new ItemStack(placement.block());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(placement.pos()), outward, placement.pos(), false);
        BlockState state = placement.block().getStateForPlacement(new BlockPlaceContext(player, hand, stack, hit));
        if (state == null) {
            state = placement.block().defaultBlockState();
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, outward);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction horizontal = outward.getAxis() == Direction.Axis.Y ? player.getDirection() : outward;
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, horizontal);
        }
        return placement.predicate().orient(state, orientation);
    }

    // 搭建过程中邻居不断变化，补一次形状推导（连通纹理、连接状态等），并保持朝向要求
    private static void settle(Level level, List<Placement> placements, StructureOrientation orientation) {
        for (Placement placement : placements) {
            BlockState current = level.getBlockState(placement.pos());
            BlockState updated = Block.updateFromNeighbourShapes(current, level, placement.pos());
            updated = placement.predicate().orient(updated, orientation);
            if (updated != current) {
                level.setBlock(placement.pos(), updated, Block.UPDATE_ALL);
            }
        }
    }

    private static BlockPos center(List<MultiblockPattern.PatternCell> cells) {
        if (cells.isEmpty()) {
            return BlockPos.ZERO;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (MultiblockPattern.PatternCell cell : cells) {
            BlockPos pos = cell.pos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    // 由结构中心指向该格的方向，用于决定方块的朝向
    private static Direction outward(BlockPos center, BlockPos pos, Direction fallback) {
        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();
        if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        if (dx == 0 && dz == 0) {
            return fallback;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void report(ServerPlayer player, MachineBlock<?> machine, Plan plan, int placed) {
        MutableComponent name = machine.definition().block().getName().copy();
        if (placed == 0 && plan.missing.isEmpty() && plan.blocked == 0 && plan.misoriented == 0) {
            player.displayClientMessage(Component.translatable("message.ae2pr.multiblock.assemble.complete", name),
                                        true);
            return;
        }
        if (placed > 0) {
            player.displayClientMessage(
                    Component.translatable("message.ae2pr.multiblock.assemble.placed", name, placed), true);
        }
        if (!plan.missing.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.ae2pr.multiblock.assemble.missing",
                                                               shortfall(plan.missing)),
                                        false);
        }
        if (plan.blocked > 0) {
            player.displayClientMessage(
                    Component.translatable("message.ae2pr.multiblock.assemble.blocked", plan.blocked), false);
        }
        if (plan.misoriented > 0) {
            player.displayClientMessage(
                    Component.translatable("message.ae2pr.multiblock.assemble.misoriented", plan.misoriented), false);
        }
    }

    private static MutableComponent shortfall(Map<Block, Integer> missing) {
        MutableComponent list = Component.empty();
        boolean first = true;
        for (Map.Entry<Block, Integer> entry : missing.entrySet()) {
            if (!first) {
                list.append(Component.literal(", "));
            }
            first = false;
            list.append(entry.getKey().getName().copy()).append(Component.literal(" x" + entry.getValue()));
        }
        return list;
    }
}
