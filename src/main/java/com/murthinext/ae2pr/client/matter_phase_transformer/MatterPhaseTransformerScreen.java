package com.murthinext.ae2pr.client.matter_phase_transformer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.MachineGui;

/**
 * 物质相变仪主机界面。
 */
public class MatterPhaseTransformerScreen extends AbstractMultiblockControllerScreen<MatterPhaseTransformerMenu> {

    private static final int COLOR_TITLE = 0xFFC46B;

    public MatterPhaseTransformerScreen(MatterPhaseTransformerMenu menu, Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.MATTER_PHASE_TRANSFORMER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.MATTER_PHASE_TRANSFORMER.get();
    }

    @Override
    protected int titleColor() {
        return COLOR_TITLE;
    }

    @Override
    protected int separatorAccent() {
        return MachineGui.ACCENT_EMBER;
    }

    @Override
    protected int separatorAccentDark() {
        return MachineGui.ACCENT_EMBER_DARK;
    }
}
