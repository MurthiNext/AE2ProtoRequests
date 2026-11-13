package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.RedstoneMode;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;

import com.murthinext.ae2pr.registry.ModGuide;

public class MultiLevelEmitterScreen extends AbstractMultiLevelEmitterScreen<MultiLevelEmitterMenu> {

    private final NumberEntryWidget level;

    public MultiLevelEmitterScreen(MultiLevelEmitterMenu menu, Inventory playerInventory, Component title,
                                   ScreenStyle style) {
        super(menu, playerInventory, title, style, RedstoneMode.LOW_SIGNAL);

        this.level = widgets.addNumberEntryWidget("level", NumberEntryType.of(menu.getConfiguredFilter()));
        this.level.setTextFieldStyle(style.getWidget("levelInput"));
        this.level.setLongValue(this.menu.getCurrentValue());
        this.level.setOnChange(this::saveReportingValue);
        this.level.setOnConfirm(this::onClose);
    }

    @Override
    protected ResourceLocation helpPage() {
        return ModGuide.EMITTER_PAGE;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.level.setType(NumberEntryType.of(menu.getConfiguredFilter()));
        this.level.setActive(!menu.hasUpgrade(appeng.core.definitions.AEItems.CRAFTING_CARD));
    }

    private void saveReportingValue() {
        this.level.getLongValue().ifPresent(menu::setValue);
    }
}
