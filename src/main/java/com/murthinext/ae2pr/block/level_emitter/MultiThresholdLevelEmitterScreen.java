package com.murthinext.ae2pr.block.level_emitter;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.RedstoneMode;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;

import com.murthinext.ae2pr.registry.ModGuide;

public class MultiThresholdLevelEmitterScreen
        extends AbstractMultiLevelEmitterScreen<MultiThresholdLevelEmitterMenu> {

    private final AETextField upperField;
    private final AETextField lowerField;
    private boolean updatingFields;

    public MultiThresholdLevelEmitterScreen(MultiThresholdLevelEmitterMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style, RedstoneMode.HIGH_SIGNAL);

        this.upperField = createValueField(menu.getCurrentUpper(), this::onUpperChanged);
        this.lowerField = createValueField(menu.getCurrentLower(), this::onLowerChanged);
    }

    @Override
    protected ResourceLocation helpPage() {
        return ModGuide.INDEX_PAGE;
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
        parseThreshold(text, menu::setUpperValue);
    }

    private void onLowerChanged(String text) {
        parseThreshold(text, menu::setLowerValue);
    }

    private void parseThreshold(String text, Consumer<Long> setter) {
        if (updatingFields) {
            return;
        }
        try {
            long value = Long.parseLong(text.trim());
            if (value >= 0) {
                setter.accept(value);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        // 同步并摆放阈值输入框
        this.updatingFields = true;
        syncField(upperField, menu.getCurrentUpper());
        syncField(lowerField, menu.getCurrentLower());
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
    }

    private void syncField(AETextField field, long value) {
        if (field.isFocused()) {
            return;
        }
        var text = Long.toString(value);
        if (!field.getValue().equals(text)) {
            field.setValue(text);
        }
    }
}
