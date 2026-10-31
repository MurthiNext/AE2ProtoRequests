package com.murthinext.ae2pr.client.filter_press;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.filter_press.FilterPressMenu;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.ModGuide;

public class FilterPressScreen extends AbstractMultiblockControllerScreen<FilterPressMenu> {

    public FilterPressScreen(FilterPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.FILTER_PRESS_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.FILTER_PRESS.get();
    }

    @Override
    protected int titleColor() {
        return 0x9CD3FF;
    }
}
