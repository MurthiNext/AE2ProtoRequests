package com.murthinext.ae2pr.client.gui.crystal_reaction_chamber;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.gui.AbstractMultiblockControllerScreen;

/**
 * 水晶反应仓主机界面。
 */
public class CrystalReactionChamberScreen extends AbstractMultiblockControllerScreen<CrystalReactionChamberMenu> {

    private static final int COLOR_TITLE = 0x8EE6E0;

    public CrystalReactionChamberScreen(CrystalReactionChamberMenu menu, Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CRYSTAL_REACTION_CHAMBER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.CRYSTAL_REACTION_CHAMBER.get();
    }

    @Override
    protected int titleColor() {
        return COLOR_TITLE;
    }
}
