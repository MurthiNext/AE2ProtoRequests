package com.murthinext.ae2pr.client.jig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.jig.JigMenu;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.ModGuide;

public class JigScreen extends AbstractMultiblockControllerScreen<JigMenu> {

    public JigScreen(JigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.JIG_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.JIG.get();
    }

    @Override
    protected int titleColor() {
        return 0x9CD3FF;
    }
}
