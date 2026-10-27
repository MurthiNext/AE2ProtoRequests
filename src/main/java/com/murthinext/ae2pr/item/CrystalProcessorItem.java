package com.murthinext.ae2pr.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CrystalProcessorItem extends Item {

    private final String tierKey;

    public CrystalProcessorItem(Properties properties, String tierKey) {
        super(properties);
        this.tierKey = tierKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.circuit." + tierKey));
    }
}
