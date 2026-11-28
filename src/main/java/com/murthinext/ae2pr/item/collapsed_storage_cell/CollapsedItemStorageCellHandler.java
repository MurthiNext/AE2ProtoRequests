package com.murthinext.ae2pr.item.collapsed_storage_cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.murthinext.ae2pr.core.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CollapsedItemStorageCellHandler implements ICellHandler {

    @Override
    public boolean isCell(ItemStack is) {
        return is.getItem() == ModItems.COLLAPSED_ITEM_STORAGE_CELL.get();
    }

    @Nullable
    @Override
    public StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host) {
        return isCell(is) ? new CollapsedItemStorageCellInventory(is, host) : null;
    }
}
