/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.client.widgets;

import appeng.client.gui.style.ScreenStyle;
import com.murthinext.ae2pr.requester.client.abstraction.RequestDisplay;
import com.murthinext.ae2pr.requester.client.abstraction.RequesterReference;
import com.murthinext.ae2pr.requester.platform.RequesterPlatform;
import com.murthinext.ae2pr.requester.Requests.Request;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.murthinext.ae2pr.requester.RequesterUtils.f;

@OnlyIn(Dist.CLIENT)
public class RequestWidget {

    /** 数量输入框的视觉宽度（行内长条输入框）。 */
    private static final int AMOUNT_FIELD_WIDTH = 106;
    /** 数量输入框相对行左端的偏移（与面板贴图内的长条输入框位置对应）。 */
    private static final int AMOUNT_FIELD_X = 36;
    /** 状态条宽度（覆盖输入框与提交按钮下方）。 */
    private static final int STATUS_WIDTH = 123;

    private final RequestDisplay host;
    private final int index;
    private final int x;
    private final int y;
    private final ScreenStyle style;
    private final Map<String, AbstractWidget> subWidgets;

    private StateBox stateBox;
    private NumberField amountField;
    private SubmitButton submitButton;
    private StatusDisplay statusDisplay;

    public RequestWidget(RequestDisplay host, int index, int x, int y, ScreenStyle style) {
        this.host = host;
        this.index = index;
        this.x = x;
        this.y = y;
        this.style = style;
        this.subWidgets = new HashMap<>();
    }

    /**
     * Has to be called from a {@link RequestDisplay} implementation before
     * the super call to the init method.
     * <p>
     * This removes all sub-widgets from the widget container so the super init
     * doesn't throw an error because the widgets are styleless.
     *
     * @param widgetContainer the widget container
     */
    public void preInit(Map<String, AbstractWidget> widgetContainer) {
        subWidgets.forEach(widgetContainer::remove);
    }

    /**
     * Has to be called from a {@link RequestDisplay} implementation after
     * the super call to the init method.
     * <p>
     * This adds all sub-widgets to the widget container manually so no
     * style is required. This is necessary because the widgets are styleless.
     */
    public void postInit() {
        stateBox = new StateBox(x, y, style, () -> stateBoxChanged(host.getTargetRequest(index)));
        host.addSubWidget(f("request_state_{}", index), stateBox, subWidgets);

        amountField = new NumberField(x + AMOUNT_FIELD_X, y + 3, AMOUNT_FIELD_WIDTH, "amount", style,
            amount -> amountFieldSubmitted(host.getTargetRequest(index), amount)
        );
        host.addSubWidget(f("request_amount_{}", index), amountField, subWidgets);

        submitButton = new SubmitButton(x + 145, y + 3, style, () -> submitButtonClicked(host.getTargetRequest(index)));
        host.addSubWidget(f("request_submit_{}", index), submitButton, subWidgets);

        statusDisplay = new StatusDisplay(x + AMOUNT_FIELD_X, y + 16, STATUS_WIDTH,
            () -> isInactive(host.getTargetRequest(index)));
        host.addSubWidget(f("request_status_{}", index), statusDisplay, subWidgets);
    }

    public void hide() {
        subWidgets.values().forEach(w -> w.visible = false);
    }

    public void applyRequest(Request request) {
        subWidgets.values().forEach(w -> w.visible = true);
        stateBox.setSelected(request.getState());
        var status = request.getClientStatus();
        statusDisplay.setStatus(status);
        amountField.adjustToType(request.getKey());
        if (status.locksRequest()) {
            amountField.setEditable(false);
        } else {
            amountField.setEditable(true);
        }
        if (amountField.isFocused() || submitButton.isFocused()) return;
        amountField.setLongValue(request.getAmount());
    }

    private void stateBoxChanged(@Nullable Request request) {
        if (request == null) return;
        var newState = stateBox.isSelected();
        request.updateState(newState); // prevent jittery animation before server information is received
        var requesterId = ((RequesterReference) request.getRequesterReference()).getRequesterId();
        RequesterPlatform.sendRequestState(requesterId, request.getIndex(), newState);
    }

    private void amountFieldSubmitted(@Nullable Request request, long amount) {
        if (request == null) return;
        var oldValue = request.getAmount();
        request.updateAmount(amount);
        if (oldValue == request.getAmount()) {
            amountField.setLongValue(oldValue);
        } else {
            submitButtonClicked(request);
        }
    }

    private void submitButtonClicked(@Nullable Request request) {
        if (request == null) return;
        long amount = amountField.getLongValue().orElse(0);
        var requesterId = ((RequesterReference) request.getRequesterReference()).getRequesterId();
        RequesterPlatform.sendRequestAmount(requesterId, request.getIndex(), amount);
    }

    private boolean isInactive(@Nullable Request request) {
        return request == null || !request.isRequesting() || request.getAmount() == 0;
    }
}
