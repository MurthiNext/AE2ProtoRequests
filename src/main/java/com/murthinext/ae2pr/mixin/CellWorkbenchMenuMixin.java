package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;

import appeng.blockentity.misc.CellWorkbenchBlockEntity;
import appeng.menu.implementations.CellWorkbenchMenu;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

// 元件工作台菜单：注册白/黑名单表达式同步用的 client action
@Mixin(value = CellWorkbenchMenu.class, remap = false)
public abstract class CellWorkbenchMenuMixin extends UpgradeableMenu<CellWorkbenchBlockEntity> {

    private static final String ae2pr$ACTION_SET_WHITELIST = AdvancedFilterCellItem.ACTION_SET_WHITELIST;
    private static final String ae2pr$ACTION_SET_BLACKLIST = AdvancedFilterCellItem.ACTION_SET_BLACKLIST;

    public CellWorkbenchMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory,
            CellWorkbenchBlockEntity host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2pr$init(int id, Inventory playerInventory, CellWorkbenchBlockEntity host, CallbackInfo ci) {
        registerClientAction(ae2pr$ACTION_SET_WHITELIST, String.class, this::ae2pr$setWhitelist);
        registerClientAction(ae2pr$ACTION_SET_BLACKLIST, String.class, this::ae2pr$setBlacklist);
    }

    @Unique
    private void ae2pr$setWhitelist(String value) {
        var cell = ae2pr$workbenchItem();
        if (!(cell.getItem() instanceof AdvancedFilterCellItem)) {
            return;
        }
        AdvancedFilterCellItem.setWhitelist(cell, value);
        getHost().saveChanges();
    }

    @Unique
    private void ae2pr$setBlacklist(String value) {
        var cell = ae2pr$workbenchItem();
        if (!(cell.getItem() instanceof AdvancedFilterCellItem)) {
            return;
        }
        AdvancedFilterCellItem.setBlacklist(cell, value);
        getHost().saveChanges();
    }

    @Unique
    private net.minecraft.world.item.ItemStack ae2pr$workbenchItem() {
        return ((CellWorkbenchMenu) (Object) this).getWorkbenchItem();
    }
}
