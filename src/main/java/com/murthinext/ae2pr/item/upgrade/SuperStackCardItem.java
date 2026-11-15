package com.murthinext.ae2pr.item.upgrade;

import appeng.items.materials.UpgradeCardItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SuperStackCardItem extends UpgradeCardItem {

    public SuperStackCardItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.ae2pr.super_stack_card.desc"));
        super.appendHoverText(stack, level, lines, flag);
    }
}
