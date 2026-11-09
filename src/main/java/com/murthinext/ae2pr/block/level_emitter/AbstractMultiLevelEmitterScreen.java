package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.guidebook.PageAnchor;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.item.filter_cell.FilterCell;

// 多条件发信器界面的公共部分：工具栏、设置同步与过滤槽快捷操作
public abstract class AbstractMultiLevelEmitterScreen<T extends AbstractMultiLevelEmitterMenu<?>>
        extends UpgradeableScreen<T> {

    protected final SettingToggleButton<YesNo> craftingMode;
    protected final SettingToggleButton<RedstoneMode> redstoneMode;
    protected final SettingToggleButton<FuzzyMode> fuzzyMode;
    protected final CombineModeButton combineMode;

    protected AbstractMultiLevelEmitterScreen(T menu, Inventory playerInventory, Component title, ScreenStyle style,
            RedstoneMode defaultRedstoneMode) {
        super(menu, playerInventory, title, style);

        this.redstoneMode = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, defaultRedstoneMode);
        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.craftingMode = new ServerSettingToggleButton<>(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        this.combineMode = new CombineModeButton(menu::setCombineMode);
        this.addToLeftToolbar(this.redstoneMode);
        this.addToLeftToolbar(this.craftingMode);
        this.addToLeftToolbar(this.fuzzyMode);
        this.addToLeftToolbar(this.combineMode);
    }

    // 帮助页锚点
    protected abstract ResourceLocation helpPage();

    protected void updateSettings() {
        this.fuzzyMode.set(menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(menu.supportsFuzzySearch());

        final boolean notCraftingMode = !menu.hasUpgrade(AEItems.CRAFTING_CARD);
        this.redstoneMode.active = notCraftingMode;
        this.redstoneMode.set(menu.getRedStoneMode());
        this.redstoneMode.setVisibility(notCraftingMode);
        this.craftingMode.set(this.menu.getCraftingMode());
        this.craftingMode.setVisibility(!notCraftingMode);

        this.combineMode.setVisibility(true);
        this.combineMode.setMode(menu.combineMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        updateSettings();
    }

    @Override
    protected void openHelp() {
        com.murthinext.ae2pr.registry.ModGuide.openAt(helpPage());
    }

    // 返回非空即可让 AE2 帮助按钮显示
    @Override
    protected PageAnchor getHelpTopic() {
        return new PageAnchor(helpPage(), null);
    }

    // 配置槽内过滤元件的 shift 点击改发 QUICK_MOVE
    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof EmitterConfigSlot && mouseButton == 0
                && slot.getItem().getItem() instanceof FilterCell
                && (clickType == ClickType.QUICK_MOVE || hasShiftDown())) {
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIdx, 0,
                    ClickType.QUICK_MOVE, this.minecraft.player);
            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }
}
