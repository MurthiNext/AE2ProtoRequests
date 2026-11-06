package com.murthinext.ae2pr.item.collapsed_cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.registry.ModItems;

import appeng.api.stacks.GenericStack;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CollapsedItemCellItem extends AEBaseItem {

    private static final String CONTENT_TAG = "content";
    private static final int TOOLTIP_LINES = 6;

    public CollapsedItemCellItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(List<GenericStack> contents) {
        var cell = new ItemStack(ModItems.COLLAPSED_ITEM_CELL.get());
        writeContents(cell, contents);
        return cell;
    }

    public static List<GenericStack> readContents(ItemStack cell) {
        var tag = cell.getTag();
        if (tag == null || !tag.contains(CONTENT_TAG, Tag.TAG_LIST)) {
            return List.of();
        }
        var entries = tag.getList(CONTENT_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<GenericStack>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var stack = GenericStack.readTag(entries.getCompound(i));
            if (stack != null) {
                contents.add(stack);
            }
        }
        return contents;
    }

    public static void writeContents(ItemStack cell, List<GenericStack> contents) {
        if (contents.isEmpty()) {
            cell.removeTagKey(CONTENT_TAG);
            return;
        }
        var entries = new ListTag();
        for (var stack : contents) {
            entries.add(GenericStack.writeTag(stack));
        }
        cell.getOrCreateTag().put(CONTENT_TAG, entries);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var contents = readContents(stack);
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        var sorted = new ArrayList<>(contents);
        sorted.sort((left, right) -> Long.compare(right.amount(), left.amount()));
        boolean hasMore = sorted.size() > TOOLTIP_LINES;
        return Optional.of(new StorageCellTooltipComponent(List.of(),
                hasMore ? new ArrayList<>(sorted.subList(0, TOOLTIP_LINES)) : sorted, hasMore, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.collapsed_item_cell.desc"));
    }
}
