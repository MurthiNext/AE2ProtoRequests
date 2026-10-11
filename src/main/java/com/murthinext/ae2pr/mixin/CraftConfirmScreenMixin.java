package com.murthinext.ae2pr.mixin;

import java.text.NumberFormat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.client.ClientConfirmRounds;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.core.localization.GuiText;
import appeng.menu.me.crafting.CraftConfirmMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 确认界面：在标题追加重复轮数信息（每帧重设以覆盖 AE2 原始标题）。
 */
@Mixin(value = CraftConfirmScreen.class, remap = false)
public abstract class CraftConfirmScreenMixin extends AEBaseScreen<CraftConfirmMenu> {

    public CraftConfirmScreenMixin(CraftConfirmMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void ae2pr$appendRoundsToTitle(CallbackInfo ci) {
        var rounds = ClientConfirmRounds.getFor(this.menu.containerId);
        if (rounds <= 1) {
            return;
        }

        var plan = this.menu.getPlan();
        Component planDetails;
        if (plan != null) {
            var byteUsed = NumberFormat.getInstance().format(plan.getUsedBytes());
            planDetails = GuiText.BytesUsed.text(byteUsed);
        } else {
            planDetails = GuiText.CalculatingWait.text();
        }

        var title = GuiText.CraftingPlan.text(planDetails)
                .append(Component.translatable("gui.ae2pr.repeat_order.title_suffix", rounds));
        this.setTextContent(AEBaseScreen.TEXT_ID_DIALOG_TITLE, title);
    }
}
