/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * ME Requester（LGPL-3.0）兼容层，纳入本仓库 LGPL-3.0 声明范围。
 * See licenses/me-requester in this repository.
 */
package com.murthinext.ae2pr.requester.compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.almostreliable.merequester.client.abstraction.RequesterReference;
import com.almostreliable.merequester.client.widgets.NumberField;
import com.almostreliable.merequester.client.widgets.RequestWidget;
import com.almostreliable.merequester.requester.Requests.Request;

import appeng.client.gui.widgets.IResizableWidget;

/**
 * 请求器终端中的本模组请求器行：去掉 ME Requester 的“并行大小”栏。
 * <p>
 * 本模组的红石请求器只有合成数量、不存在重复下单，所以隐藏并行输入框，
 * 并把数量输入框拉宽到覆盖它原来的位置，使行外观与请求器自身界面一致。
 * 终端的一行控件会被滚动到的不同请求器复用，因此两个方向都要还原。
 */
@Mixin(value = RequestWidget.class, remap = false)
public abstract class RequestWidgetMixin {

    // ME Requester 自身的数量框宽度（构造参数）
    @Unique
    private static final int ae2pr$BATCHED_AMOUNT_WIDTH = 52;
    // 合并并行栏后的宽度：左沿不变，右沿一直伸到提交按钮之前
    @Unique
    private static final int ae2pr$MERGED_AMOUNT_WIDTH = 106;
    @Unique
    private static final int ae2pr$FIELD_HEIGHT = 12;

    @Shadow(remap = false)
    private NumberField amountField;

    @Shadow(remap = false)
    private NumberField batchField;

    @Inject(method = "applyRequest", at = @At("RETURN"), remap = false)
    private void ae2pr$mergeBatchIntoAmount(Request request, CallbackInfo ci) {
        // 本模组请求器的 id 由方块坐标导出，恒为非负；ME Requester 自身的 id 从 Long.MIN_VALUE 递增
        boolean ours = ((RequesterReference) request.getRequesterReference()).getRequesterId() >= 0;
        ((IResizableWidget) this.amountField).resize(
                ours ? ae2pr$MERGED_AMOUNT_WIDTH : ae2pr$BATCHED_AMOUNT_WIDTH, ae2pr$FIELD_HEIGHT);

        // applyRequest 每次都会把所有子控件设为可见，隐藏要重新施加
        this.batchField.visible = !ours;
        if (ours) {
            this.batchField.setFocused(false);
        }
    }
}
