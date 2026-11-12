package com.murthinext.ae2pr.item.upgrade;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import appeng.items.materials.UpgradeCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.api.upgrade.MachineUpgradeDefinition;

public class MachineUpgradeCardItem extends UpgradeCardItem {
    private final MachineUpgradeDefinition definition;

    public MachineUpgradeCardItem(Properties properties, MachineUpgradeDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public MachineUpgradeDefinition definition() {
        return definition;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.ae2pr.machine_upgrade.inscription").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.ae2pr.machine_upgrade.formula", definition.formula().source())
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, lines, flag);
    }
}
