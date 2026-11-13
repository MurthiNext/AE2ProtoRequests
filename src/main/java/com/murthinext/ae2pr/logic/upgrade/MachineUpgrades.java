package com.murthinext.ae2pr.logic.upgrade;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.api.machine.MachineStat;
import com.murthinext.ae2pr.api.machine.MachineTuning;
import com.murthinext.ae2pr.api.upgrade.MachineUpgradeType;
import com.murthinext.ae2pr.item.upgrade.MachineUpgradeCardItem;

public final class MachineUpgrades {
    private MachineUpgrades() {
    }

    public static double apply(IUpgradeInventory inventory, MachineUpgradeType type, double value) {
        return apply(inventory, type, value, value * MachineTuning.speedPerCard(), null);
    }

    public static double apply(IUpgradeInventory inventory, MachineUpgradeType type, double value, double speedPerCard,
                               @Nullable List<MachineStat.Source> sources) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            boolean formulaCard = stack.getItem() instanceof MachineUpgradeCardItem card &&
                    card.definition().type() == type;
            boolean speedCard = AEItems.SPEED_CARD.isSameAs(stack) && type != MachineUpgradeType.PARALLEL;
            if (!formulaCard && !speedCard) {
                continue;
            }
            double before = value;
            for (int count = 0; count < stack.getCount(); count++) {
                if (formulaCard && stack.getItem() instanceof MachineUpgradeCardItem card) {
                    value = card.definition().apply(value);
                } else if (type == MachineUpgradeType.SPEED) {
                    value = Math.min(Integer.MAX_VALUE, value + speedPerCard);
                } else {
                    value *= MachineTuning.energyMultiplierPerCard();
                }
            }
            if (sources != null) {
                sources.add(new MachineStat.Source(Component.translatable("gui.ae2pr.multiblock.data.slot", slot + 1,
                                                                          stack.getHoverName(), stack.getCount()),
                                                   before, value));
            }
        }
        return value;
    }
}
