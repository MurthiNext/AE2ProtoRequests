package com.murthinext.ae2pr.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.items.materials.UpgradeCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// 升级卡的适用机器列表仅在按住 Shift 时显示
@Mixin(UpgradeCardItem.class)
public abstract class UpgradeCardItemMixin {

    @Inject(method = "appendHoverText", at = @At(value = "FIELD",
            target = "Lappeng/core/localization/ButtonToolTips;SupportedBy:Lappeng/core/localization/ButtonToolTips;",
            remap = false), cancellable = true)
    private void ae2pr$collapseSupportedBy(ItemStack stack, Level level, List<Component> lines,
            TooltipFlag tooltipFlag, CallbackInfo ci) {
        if (!Screen.hasShiftDown()) {
            lines.add(Component.translatable("tooltip.ae2pr.upgrade_card.shift_for_supported")
                    .withStyle(ChatFormatting.GRAY));
            ci.cancel();
        }
    }
}
