package com.murthinext.ae2pr.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.murthinext.ae2pr.api.text.Voltage;

public class CrystalProcessorItem extends Item {

    private final String tier;

    public CrystalProcessorItem(Properties properties, String tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Voltage.circuit(tier));
    }
}
