package com.murthinext.ae2pr.item.cell.collapsed;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.storage.cells.ICellGuiHandler;
import appeng.api.storage.cells.ICellHandler;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import com.murthinext.ae2pr.core.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CollapsedCellGuiHandler implements ICellGuiHandler {

    @Override
    public boolean isSpecializedFor(ItemStack cell) {
        return cell.getItem() == ModItems.COLLAPSED_ITEM_STORAGE_CELL.get();
    }

    @Override
    public void openChestGui(Player player, IChestOrDrive chest, ICellHandler cellHandler, ItemStack cell) {
        MenuOpener.open(MEStorageMenu.TYPE, player, MenuLocators.forBlockEntity((BlockEntity) chest));
    }
}
