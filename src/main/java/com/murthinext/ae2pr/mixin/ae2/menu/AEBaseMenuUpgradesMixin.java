package com.murthinext.ae2pr.mixin.ae2.menu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.inventory.Slot;

// 统一在菜单补升级槽：先追加槽位，再按库存索引补齐缺失的槽对象
@Mixin(value = AEBaseMenu.class, priority = 500, remap = false)
public abstract class AEBaseMenuUpgradesMixin {

    @Shadow(remap = false)
    protected abstract Slot addSlot(Slot slot, SlotSemantic semantic);

    @Inject(method = "setupUpgrades", at = @At("HEAD"), cancellable = true)
    private void ae2pr$setupUpgrades(IUpgradeInventory upgrades, CallbackInfo ci) {
        if (upgrades == null || !SuperStackSupport.isSupportedMachine(upgrades.getUpgradableItem())) {
            return;
        }
        SuperStackSupport.appendUpgradeSlot(upgrades);

        var menu = (AEBaseMenu) (Object) this;
        for (int i = 0; i < upgrades.size(); i++) {
            if (ae2pr$hasSlot(menu, upgrades, i)) {
                continue;
            }
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
            slot.setNotDraggable();
            this.addSlot(slot, SlotSemantics.UPGRADE);
        }
        ci.cancel();
    }

    @Unique
    private static boolean ae2pr$hasSlot(AEBaseMenu menu, IUpgradeInventory upgrades, int index) {
        for (var slot : menu.slots) {
            if (slot instanceof AppEngSlot aeSlot
                    && aeSlot.getInventory() == upgrades
                    && aeSlot.getContainerSlot() == index) {
                return true;
            }
        }
        return false;
    }
}
