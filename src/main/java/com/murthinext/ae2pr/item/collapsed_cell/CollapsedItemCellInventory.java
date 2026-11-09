package com.murthinext.ae2pr.item.collapsed_cell;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 坍缩元件的内容视图：网络只能读取与抽取，存入一律拒绝。
 */
public class CollapsedItemCellInventory implements StorageCell {

    private final ItemStack cell;
    private final List<GenericStack> contents;
    @Nullable
    private final ISaveProvider host;

    public CollapsedItemCellInventory(ItemStack cell, @Nullable ISaveProvider host) {
        this.cell = cell;
        this.contents = new ArrayList<>(CollapsedItemCellItem.readContents(cell));
        this.host = host;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        for (int i = 0; i < contents.size(); i++) {
            var stack = contents.get(i);
            if (!stack.what().equals(what)) {
                continue;
            }
            long taken = Math.min(stack.amount(), amount);
            if (taken <= 0 || mode != Actionable.MODULATE) {
                return taken;
            }
            if (taken >= stack.amount()) {
                contents.remove(i);
            } else {
                contents.set(i, new GenericStack(what, stack.amount() - taken));
            }
            persist();
            if (host != null) {
                host.saveChanges();
            }
            return taken;
        }
        return 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var stack : contents) {
            out.add(stack.what(), stack.amount());
        }
    }

    @Override
    public Component getDescription() {
        return cell.getHoverName();
    }

    @Override
    public CellState getStatus() {
        return contents.isEmpty() ? CellState.EMPTY : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Override
    public boolean canFitInsideCell() {
        // 空元件可被装进其他元件，有内容时禁止，避免无限嵌套
        return contents.isEmpty();
    }

    @Override
    public void persist() {
        CollapsedItemCellItem.writeContents(cell, contents);
    }
}
