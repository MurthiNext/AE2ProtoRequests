package com.murthinext.ae2pr.api.collapse;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.item.collapsed_cell.CollapsedItemCellItem;
import com.murthinext.ae2pr.logic.collapse.CollapseHandler;

public final class Collapses {

    private Collapses() {
    }

    @Nullable
    public static ItemStack tryCollapse(BlockEntity blockEntity) {
        return CollapseHandler.tryCollapse(blockEntity);
    }

    @Nullable
    public static ItemStack tryCollapse(ResourceLocation id, IItemHandler handler) {
        return CollapseHandler.tryCollapse(id, handler);
    }

    public static boolean isEnabled() {
        return ModConfig.collapseEnabled();
    }

    public static long threshold() {
        return ModConfig.collapseThreshold();
    }

    public static ItemStack createCell(List<GenericStack> contents) {
        return CollapsedItemCellItem.create(contents.stream()
                .filter(stack -> stack.what() instanceof AEItemKey)
                .toList());
    }
}
