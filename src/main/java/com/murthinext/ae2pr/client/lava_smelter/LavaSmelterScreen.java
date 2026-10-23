package com.murthinext.ae2pr.client.lava_smelter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterMenu;
import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.client.AbstractMultiblockControllerScreen;
import com.murthinext.ae2pr.client.MachineGui;
import com.murthinext.ae2pr.client.MultiblockInfoLayout;

/**
 * 高反应性熔岩冶炼炉主机界面。
 */
public class LavaSmelterScreen extends AbstractMultiblockControllerScreen<LavaSmelterMenu> {

    private static final int COLOR_TITLE = 0xFFC46B;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_DURABILITY = 0xFFC46B;

    private static final int DUST_SLOT_X = 151;
    private static final int DUST_SLOT_Y = 95;

    public LavaSmelterScreen(LavaSmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.LAVA_SMELTER_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get();
    }

    @Override
    protected int titleColor() {
        return COLOR_TITLE;
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX,
            int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        MachineGui.drawSlot(graphics, leftPos + DUST_SLOT_X, topPos + DUST_SLOT_Y);
    }

    @Override
    protected int separatorAccent() {
        return MachineGui.ACCENT_EMBER;
    }

    @Override
    protected int separatorAccentDark() {
        return MachineGui.ACCENT_EMBER_DARK;
    }

    @Override
    protected void renderExtraLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        int durability = menu.getDurability();
        graphics.drawString(font, Component.translatable("gui.ae2pr.lava_smelter.durability",
                durability, menu.getMaxDurability()),
                MultiblockInfoLayout.TEXT_X, MultiblockInfoLayout.EXTRA_Y,
                durability > 0 ? COLOR_DURABILITY : COLOR_FAIL, false);
    }
}
