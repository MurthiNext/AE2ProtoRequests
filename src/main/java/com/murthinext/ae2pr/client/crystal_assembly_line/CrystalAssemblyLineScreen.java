package com.murthinext.ae2pr.client.crystal_assembly_line;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_assembly_line.CrystalAssemblyLineMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;

/**
 * 水晶装配线主机界面。
 */
public class CrystalAssemblyLineScreen extends AbstractMultiblockControllerScreen<CrystalAssemblyLineMenu> {

    private static final int COLOR_TITLE = 0x55FFFF;

    public CrystalAssemblyLineScreen(CrystalAssemblyLineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CRYSTAL_ASSEMBLY_LINE_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.CRYSTAL_ASSEMBLY_LINE.get();
    }

    @Override
    protected int titleColor() {
        return COLOR_TITLE;
    }
}
