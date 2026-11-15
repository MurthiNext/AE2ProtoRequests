package com.murthinext.ae2pr.item;

import com.murthinext.ae2pr.api.text.Voltage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class VoltageBlockItem extends BlockItem {

    public VoltageBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Voltage.name(getDescriptionId(stack));
    }
}
