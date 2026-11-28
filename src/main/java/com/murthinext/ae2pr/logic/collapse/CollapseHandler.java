package com.murthinext.ae2pr.logic.collapse;

import appeng.api.parts.IPart;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.murthinext.ae2pr.core.config.ServerConfig;
import com.murthinext.ae2pr.item.collapsed_storage_cell.CollapsedItemStorageCellItem;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class CollapseHandler {

    private static final BigInteger MAX_CELL_AMOUNT = BigInteger.valueOf(Long.MAX_VALUE);

    private CollapseHandler() {}

    @Nullable
    public static ItemStack tryCollapse(BlockEntity blockEntity) {
        return collapse(CollapseSources.of(blockEntity));
    }

    @Nullable
    public static ItemStack tryCollapse(ResourceLocation id, IItemHandler handler) {
        return CollapseMatcher.allows(id) ? collapse(List.of(CollapseSource.of(id, handler))) : null;
    }

    public static void dropBufferedContents(
            Level level, BlockPos pos, ResourceLocation id, Map<AEKey, BigInteger> keys) {
        boolean collapsed = collapseBufferedItems(id, keys, stack -> Block.popResource(level, pos, stack));
        for (var entry : keys.entrySet()) {
            var key = entry.getKey();
            var amount = entry.getValue();
            if (key instanceof AEItemKey item) {
                if (collapsed) {
                    continue;
                }
                var limit = BigInteger.valueOf(Math.max(1, item.getMaxStackSize()));
                while (amount.signum() > 0) {
                    var taken = amount.min(limit);
                    Block.popResource(level, pos, item.toStack(taken.intValueExact()));
                    amount = amount.subtract(taken);
                }
            } else {
                while (amount.signum() > 0) {
                    var taken = amount.min(MAX_CELL_AMOUNT);
                    var drops = new ArrayList<ItemStack>();
                    key.addDrops(taken.longValueExact(), drops, level, pos);
                    drops.forEach(stack -> Block.popResource(level, pos, stack));
                    amount = amount.subtract(taken);
                }
            }
        }
    }

    private static boolean collapseBufferedItems(
            ResourceLocation id, Map<AEKey, BigInteger> keys, Consumer<ItemStack> drop) {
        if (!ServerConfig.collapseEnabled() || !CollapseMatcher.allows(id)) {
            return false;
        }
        BigInteger total = BigInteger.ZERO;
        for (var entry : keys.entrySet()) {
            if (entry.getKey() instanceof AEItemKey) {
                total = total.add(entry.getValue());
            }
        }
        if (total.compareTo(BigInteger.valueOf(ServerConfig.collapseThreshold())) < 0) {
            return false;
        }
        var contents = new ArrayList<GenericStack>();
        long remaining = Long.MAX_VALUE;
        for (var entry : keys.entrySet()) {
            if (!(entry.getKey() instanceof AEItemKey item)) {
                continue;
            }
            var amount = entry.getValue();
            while (amount.signum() > 0) {
                var taken = amount.min(BigInteger.valueOf(remaining));
                contents.add(new GenericStack(item, taken.longValueExact()));
                amount = amount.subtract(taken);
                remaining -= taken.longValueExact();
                if (remaining == 0) {
                    drop.accept(CollapsedItemStorageCellItem.create(contents));
                    contents.clear();
                    remaining = Long.MAX_VALUE;
                }
            }
        }
        if (!contents.isEmpty()) {
            drop.accept(CollapsedItemStorageCellItem.create(contents));
        }
        return true;
    }

    public static boolean collapseInto(BlockEntity blockEntity, List<ItemStack> drops) {
        var cell = tryCollapse(blockEntity);
        if (cell == null) {
            return false;
        }
        drops.add(cell);
        return true;
    }

    @Nullable
    public static ResourceLocation idOf(@Nullable Object owner) {
        if (owner instanceof IPart part) {
            return BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem());
        }
        if (owner instanceof BlockEntity blockEntity) {
            return BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        }
        return null;
    }

    @Nullable
    private static ItemStack collapse(List<CollapseSource> sources) {
        if (!ServerConfig.collapseEnabled() || sources.isEmpty()) {
            return null;
        }
        var stacks = new ArrayList<GenericStack>();
        var accepted = new ArrayList<CollapseSource>(sources.size());
        for (var source : sources) {
            if (CollapseMatcher.allows(source.id())) {
                accepted.add(source);
                stacks.addAll(source.read());
            }
        }
        var cell = buildCell(stacks);
        if (cell == null) {
            return null;
        }
        for (var source : accepted) {
            source.clear();
        }
        return cell;
    }

    @Nullable
    private static ItemStack buildCell(List<GenericStack> stacks) {
        var merged = mergeItems(stacks);
        long total = 0;
        for (var stack : merged) {
            total += stack.amount();
        }
        return total >= ServerConfig.collapseThreshold() ? CollapsedItemStorageCellItem.create(merged) : null;
    }

    private static List<GenericStack> mergeItems(List<GenericStack> stacks) {
        Map<AEKey, Long> merged = new LinkedHashMap<>();
        for (var stack : stacks) {
            if (stack != null && stack.what() instanceof AEItemKey) {
                merged.merge(stack.what(), stack.amount(), Long::sum);
            }
        }
        var result = new ArrayList<GenericStack>(merged.size());
        merged.forEach((key, amount) -> result.add(new GenericStack(key, amount)));
        return result;
    }
}
