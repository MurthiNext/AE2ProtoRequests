package com.murthinext.ae2pr.item.collapsed_cell;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.registry.ModItems;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.world.item.ItemStack;

public class CollapsedItemCellHandler implements ICellHandler {

    @Override
    public boolean isCell(ItemStack is) {
        return is.getItem() == ModItems.COLLAPSED_ITEM_CELL.get();
    }

    @Nullable
    @Override
    public StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host) {
        return isCell(is) ? new CollapsedItemCellInventory(is, host) : null;
    }
}
