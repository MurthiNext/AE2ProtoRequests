package com.murthinext.ae2pr.logic.upgrade;

import appeng.api.upgrades.IUpgradeInventory;
import net.minecraft.world.item.ItemStack;

import com.murthinext.ae2pr.api.upgrade.MachineUpgradeType;
import com.murthinext.ae2pr.item.upgrade.MachineUpgradeCardItem;

public final class MachineUpgrades {
    private MachineUpgrades() {
    }

    public static double apply(IUpgradeInventory inventory, MachineUpgradeType type, double value) {
        for (ItemStack stack : inventory) {
            if (stack.getItem() instanceof MachineUpgradeCardItem card && card.definition().type() == type) {
                for (int count = 0; count < stack.getCount(); count++) {
                    value = card.definition().apply(value);
                }
            }
        }
        return value;
    }
}
