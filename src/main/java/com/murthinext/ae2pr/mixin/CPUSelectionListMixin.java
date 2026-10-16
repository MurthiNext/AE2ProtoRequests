package com.murthinext.ae2pr.mixin;

import com.murthinext.ae2pr.mixin.accessor.CPUSelectionListAccessor;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.client.repeat.ClientRepeatOrderState;

import appeng.client.Point;
import appeng.client.gui.Tooltip;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.menu.me.crafting.CraftingStatusMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 合成状态界面：CPU 条目与 tooltip 追加重复轮次信息。
 */
@Mixin(value = CPUSelectionList.class, remap = false)
public abstract class CPUSelectionListMixin {

    @Inject(method = "getCpuName", at = @At("RETURN"), cancellable = true)
    private void ae2pr$appendRoundToName(CraftingStatusMenu.CraftingCpuListEntry cpu,
            CallbackInfoReturnable<Component> cir) {
        var info = ClientRepeatOrderState.get(cpu.serial());
        if (info == null) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue()
                .copy()
                .append(Component.literal(" (" + info.round() + "/" + info.total() + ")")
                        .withStyle(ChatFormatting.GOLD)));
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void ae2pr$appendRepeatTooltip(int mouseX, int mouseY, CallbackInfoReturnable<Tooltip> cir) {
        var tooltip = cir.getReturnValue();
        if (tooltip == null) {
            return;
        }
        var cpu = ((CPUSelectionListAccessor) this).ae2pr$hitTestCpu(new Point(mouseX, mouseY));
        if (cpu == null) {
            return;
        }
        var info = ClientRepeatOrderState.get(cpu.serial());
        if (info == null) {
            return;
        }

        var lines = new ArrayList<>(tooltip.getContent());
        lines.add(Component.translatable("gui.ae2pr.repeat_order.tooltip.round", info.round(), info.total())
                .withStyle(ChatFormatting.GRAY));

        cir.setReturnValue(new Tooltip(lines));
    }
}
