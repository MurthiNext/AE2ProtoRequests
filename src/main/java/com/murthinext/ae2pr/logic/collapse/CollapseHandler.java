package com.murthinext.ae2pr.logic.collapse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.parts.IPart;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.item.collapsed_cell.CollapsedItemCellItem;

public final class CollapseHandler {

    private CollapseHandler() {
    }

    @Nullable
    public static ItemStack tryCollapse(BlockEntity blockEntity) {
        return collapse(CollapseSources.of(blockEntity));
    }

    @Nullable
    public static ItemStack tryCollapse(ResourceLocation id, IItemHandler handler) {
        return CollapseMatcher.allows(id) ? collapse(List.of(CollapseSource.of(id, handler))) : null;
    }


    @Nullable
    public static ItemStack tryCollapse(ResourceLocation id, GenericInternalInventory inventory) {
        return CollapseMatcher.allows(id) ? collapse(List.of(CollapseSource.of(id, inventory))) : null;
    }

    public static boolean collapseInto(BlockEntity blockEntity, List<ItemStack> drops) {
        var cell = tryCollapse(blockEntity);
        if (cell == null) {
            return false;
        }
        drops.add(cell);
        return true;
    }

    public static boolean collapseInto(ResourceLocation id, IItemHandler handler, List<ItemStack> drops) {
        var cell = tryCollapse(id, handler);
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
        if (!Config.collapseEnabled() || sources.isEmpty()) {
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
        return total >= Config.collapseThreshold() ? CollapsedItemCellItem.create(merged) : null;
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
