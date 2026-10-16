package com.murthinext.ae2pr.client.gui.level_emitter;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
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
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.client.ModGuide;
import com.murthinext.ae2pr.block.level_emitter.EmitterConfigSlot;
import com.murthinext.ae2pr.block.level_emitter.MultiThresholdLevelEmitterMenu;
import com.murthinext.ae2pr.item.filter_cell.FilterCell;

/**
 * ME 通式阈值发信器界面。
 */
public class MultiThresholdLevelEmitterScreen extends UpgradeableScreen<MultiThresholdLevelEmitterMenu> {

    private final SettingToggleButton<YesNo> craftingMode;
    private final SettingToggleButton<RedstoneMode> redstoneMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final CombineModeButton combineMode;

    private final AETextField upperField;
    private final AETextField lowerField;
    private boolean updatingFields;

    public MultiThresholdLevelEmitterScreen(MultiThresholdLevelEmitterMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.redstoneMode = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.craftingMode = new ServerSettingToggleButton<>(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        this.combineMode = new CombineModeButton(menu::setCombineMode);
        this.addToLeftToolbar(this.redstoneMode);
        this.addToLeftToolbar(this.craftingMode);
        this.addToLeftToolbar(this.fuzzyMode);
        this.addToLeftToolbar(this.combineMode);

        this.upperField = createValueField(menu.getCurrentUpper(), this::onUpperChanged);
        this.lowerField = createValueField(menu.getCurrentLower(), this::onLowerChanged);
    }

    private AETextField createValueField(long value, Consumer<String> responder) {
        var field = new AETextField(this.style, this.font, 0, 0, 120, 12);
        field.setBordered(false);
        field.setMaxLength(18);
        field.setValue(Long.toString(value));
        field.setResponder(responder);
        return field;
    }

    private void onUpperChanged(String text) {
        if (updatingFields) {
            return;
        }
        try {
            long value = Long.parseLong(text.trim());
            if (value >= 0) {
                menu.setUpperValue(value);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void onLowerChanged(String text) {
        if (updatingFields) {
            return;
        }
        try {
            long value = Long.parseLong(text.trim());
            if (value >= 0) {
                menu.setLowerValue(value);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        // 同步并摆放阈值输入框
        this.updatingFields = true;
        if (!upperField.isFocused()) {
            var value = Long.toString(menu.getCurrentUpper());
            if (!upperField.getValue().equals(value)) {
                upperField.setValue(value);
            }
        }
        if (!lowerField.isFocused()) {
            var value = Long.toString(menu.getCurrentLower());
            if (!lowerField.getValue().equals(value)) {
                lowerField.setValue(value);
            }
        }
        this.updatingFields = false;

        if (!this.children().contains(upperField)) {
            this.addRenderableWidget(upperField);
        }
        if (!this.children().contains(lowerField)) {
            this.addRenderableWidget(lowerField);
        }
        // AETextField 内部有 2px 内边距，+2 使文本对齐标签
        upperField.setX(leftPos + 10);
        upperField.setY(topPos + 40);
        lowerField.setX(leftPos + 10);
        lowerField.setY(topPos + 72);

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
    protected void openHelp() {
        ModGuide.openAt(ModGuide.INDEX_PAGE);
    }

    /**
     * 配置槽内过滤元件的 shift 点击改发 QUICK_MOVE。
     */
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

    /**
     * 返回非空即可让 AE2 帮助按钮显示。
     */
    @Override
    protected PageAnchor getHelpTopic() {
        return new PageAnchor(ModGuide.INDEX_PAGE, null);
    }
}
