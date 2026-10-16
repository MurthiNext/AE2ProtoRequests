package com.murthinext.ae2pr.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * ME计算组件。
 */
public class ComputationCellComponentItem extends Item {

    public static final int AEV_TIER = 0;

    private final int tier;

    public ComputationCellComponentItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(circuitTranslationKey()));
    }

    // AEV 级电路直接取 aev，降级变体取 aev_dN
    private String circuitTranslationKey() {
        return tier == AEV_TIER ? "tooltip.ae2pr.circuit.aev" : "tooltip.ae2pr.circuit.aev_d" + tier;
    }
}
