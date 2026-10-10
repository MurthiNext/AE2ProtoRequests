/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 *
 * 输入框骨架改编自 AE2 的 appeng.client.gui.widgets.AETextField（LGPL-3.0）：
 * AE2 1.20.1 自带的输入框底图是旧版灰调，这里改为按 1.21.1 风格自绘
 * （亮描边 + 蓝灰底 + 上沿暗线），以便与新版请求器界面统一。
 */
package com.murthinext.ae2pr.requester.client.widgets;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ITooltip;
import appeng.core.localization.GuiText;
import com.murthinext.ae2pr.requester.RequesterUtils;
import com.murthinext.ae2pr.requester.mixin.EditBoxMixin;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

public class NumberField extends EditBox implements ITooltip {

    /** 视觉盒子的外边距（文本区 = 视觉盒子内缩 PADDING）。 */
    private static final int PADDING = 2;
    /** 视觉盒子高度。 */
    private static final int HEIGHT = 12;

    // 1.21.1 风格输入框配色
    private static final int BORDER_COLOR = 0xFF_F2F2F2;
    private static final int BODY_COLOR = 0xFF_9A9FB4;
    private static final int DISABLED_BODY_COLOR = 0xFF_878FA5;
    private static final int TOP_LINE_COLOR = 0xFF_696D88;
    private static final int SUFFIX_COLOR = 0xFF_4D4D67;
    private static final int ERROR_COLOR = 0xFF_0000;

    private static final int MIN_VALUE = 0;

    private final String name;
    private final int visualWidth;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;
    private final Consumer<Long> onConfirm;

    private List<Component> tooltipMessage = Collections.emptyList();
    private NumberEntryType type = NumberEntryType.UNITLESS;
    private boolean isFluid;

    NumberField(int x, int y, int width, String name, ScreenStyle style, Consumer<Long> onConfirm) {
        super(
            Minecraft.getInstance().font,
            x + PADDING,
            y + PADDING,
            width - 2 * PADDING - Minecraft.getInstance().font.width("_"),
            HEIGHT - 2 * PADDING,
            Component.empty()
        );
        this.name = name;
        this.visualWidth = width;
        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();
        this.onConfirm = onConfirm;

        decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        decimalFormat.setParseBigDecimal(true);
        decimalFormat.setNegativePrefix("-");

        setBordered(false);
        setVisible(true);
        setMaxLength(7);
        setLongValue(0);
        setTextColor(normalTextColor);
        setResponder(text -> validate());
        validate();
    }

    /** 含边框的视觉盒子，用于点击判定与背景绘制。 */
    private Rect2i getVisualBounds() {
        return new Rect2i(getX() - PADDING, getY() - PADDING, visualWidth, HEIGHT);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!isVisible()) {
            return;
        }

        var bounds = getVisualBounds();
        int left = bounds.getX();
        int top = bounds.getY();
        int right = left + bounds.getWidth();
        int bottom = top + bounds.getHeight();

        // 1.21.1 风格底：亮描边 + 上沿暗线 + 蓝灰底
        boolean editable = RequesterUtils.cast(this, EditBoxMixin.class).ae2pr$isEditable();
        guiGraphics.fill(left, top, right, bottom, BORDER_COLOR);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1,
            editable ? BODY_COLOR : DISABLED_BODY_COLOR);
        guiGraphics.fill(left + 1, top + 1, right - 1, top + 2, TOP_LINE_COLOR);

        super.renderWidget(guiGraphics, mouseX, mouseY, partial);

        // 流体以桶为单位显示，右侧补充单位标记
        if (isFluid) {
            var font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, "B", right - PADDING - 2 - font.width("B"), getY(), SUFFIX_COLOR, false);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        var bounds = getVisualBounds();
        return mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
            && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点在边框上时把坐标收进文本区，避免出现"看得见却点不到"的死区
        if (isMouseOver(mouseX, mouseY)) {
            mouseX = Math.max(getX(), Math.min(mouseX, getX() + width - 1));
            mouseY = Math.max(getY(), Math.min(mouseY, getY() + height - 1));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (canConsumeInput() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            getLongValue().ifPresent(value -> {
                onConfirm.accept(value);
                setFocused(false);
            });
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        // 聚焦时吞掉按键（Tab/Esc 除外），避免 e 之类的按键直接关闭界面
        return isFocused() && canConsumeInput() && keyCode != GLFW.GLFW_KEY_TAB && keyCode != GLFW.GLFW_KEY_ESCAPE;
    }

    private void validate() {
        List<Component> validationErrors = new ArrayList<>();
        List<Component> infoMessages = new ArrayList<>();

        var possibleValue = getValueInternal();
        if (possibleValue.isPresent()) {
            if (possibleValue.get().scale() > (isFluid ? 3 : 0)) {
                validationErrors.add(RequesterUtils.translate("tooltip", "whole_number"));
            } else {
                var value = convertToExternalValue(possibleValue.get());
                if (value < MIN_VALUE) {
                    var formatted = decimalFormat.format(convertToInternalValue(MIN_VALUE));
                    validationErrors.add(GuiText.NumberLessThanMinValue.text(formatted));
                } else if (!isNumber()) {
                    infoMessages.add(Component.literal("= " + decimalFormat.format(possibleValue.get())));
                }
            }
        } else {
            validationErrors.add(GuiText.InvalidNumber.text());
        }

        boolean valid = validationErrors.isEmpty();
        var tooltip = valid ? infoMessages : validationErrors;
        setTextColor(valid ? normalTextColor : ERROR_COLOR);
        setTooltipMessage(tooltip);
    }

    private long convertToExternalValue(BigDecimal internalValue) {
        var multiplicand = BigDecimal.valueOf(type.amountPerUnit());
        var value = internalValue.multiply(multiplicand, MathContext.DECIMAL128);
        value = value.setScale(0, RoundingMode.UP);
        return value.longValue();
    }

    private BigDecimal convertToInternalValue(long externalValue) {
        var divisor = BigDecimal.valueOf(type.amountPerUnit());
        return BigDecimal.valueOf(externalValue).divide(divisor, MathContext.DECIMAL128);
    }

    OptionalLong getLongValue() {
        var internalValue = getValueInternal();
        if (internalValue.isEmpty()) {
            return OptionalLong.empty();
        }

        var externalValue = convertToExternalValue(internalValue.get());
        if (externalValue < MIN_VALUE) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(externalValue);
    }

    void setLongValue(long value) {
        var internalValue = convertToInternalValue(Math.max(value, MIN_VALUE));
        setValue(decimalFormat.format(internalValue));
        moveCursorToEnd();
        validate();
    }

    private boolean isNumber() {
        var position = new ParsePosition(0);
        var textValue = getValue().trim();
        decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private Optional<BigDecimal> getValueInternal() {
        return MathExpressionParser.parse(getValue(), decimalFormat);
    }

    public void setTooltipMessage(List<Component> tooltipMessage) {
        tooltipMessage.add(0, RequesterUtils.translate("tooltip", name));
        this.tooltipMessage = tooltipMessage;
        if (!isFocused() || (tooltipMessage.size() > 1 && !tooltipMessage.get(1).getString().startsWith("="))) return;
        tooltipMessage.add(Component.literal("» ").withStyle(ChatFormatting.AQUA)
            .append(RequesterUtils.translate(
                "tooltip",
                "enter_to_submit",
                InputConstants.getKey("key.keyboard.enter").getDisplayName()
            ).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void setFocused(boolean isFocused) {
        if (isFocused && !RequesterUtils.cast(this, EditBoxMixin.class).ae2pr$isEditable()) {
            return;
        }
        super.setFocused(isFocused);
    }

    void adjustToType(@Nullable AEKey key) {
        this.isFluid = key instanceof AEFluidKey;
        this.type = NumberEntryType.of(key);
    }

    @Override
    public List<Component> getTooltipMessage() {
        return tooltipMessage;
    }

    @Override
    public Rect2i getTooltipArea() {
        return getVisualBounds();
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}
