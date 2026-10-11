package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.client.ClientRepeatOrderFailures;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 终端界面：失败物品红闪/常亮，重新打开终端清除标记。
 */
@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2pr$onTerminalOpened(MEStorageMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style, CallbackInfo ci) {
        if (!menu.isReturnedFromSubScreen()) {
            ClientRepeatOrderFailures.onTerminalOpenedFresh();
        }
    }

    // renderSlot 是原版方法重写，保留重映射
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void ae2pr$renderFailedRepeatMark(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!(slot instanceof RepoSlot repoSlot)) {
            return;
        }
        var entry = repoSlot.getEntry();
        if (entry == null || entry.getWhat() == null) {
            return;
        }
        ClientRepeatOrderFailures.renderMark(guiGraphics, slot.x, slot.y, entry.getWhat());
    }
}
