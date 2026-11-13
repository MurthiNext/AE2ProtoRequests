package com.murthinext.ae2pr.mixin.ae2.items.materials;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.items.materials.UpgradeCardItem;
import appeng.util.InteractionUtil;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

// 升级卡首次 Shift 右键时先补出槽位，保证未开界面/未读档也能插卡
@Mixin(UpgradeCardItem.class)
public abstract class UpgradeCardItemUseMixin {

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), remap = false)
    private void ae2pr$appendUpgradeSlot(ItemStack stack, UseOnContext context,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        var player = context.getPlayer();
        if (player == null || !InteractionUtil.isInAlternateUseMode(player)) {
            return;
        }
        var host = context.getLevel().getBlockEntity(context.getClickedPos());
        IUpgradeableObject upgradable = null;
        if (host instanceof IPartHost partHost) {
            SelectedPart selected = partHost.selectPartWorld(context.getClickLocation());
            if (selected != null && selected.part instanceof IUpgradeableObject part) {
                upgradable = part;
            }
        } else if (host instanceof IUpgradeableObject blockEntity) {
            upgradable = blockEntity;
        }
        if (upgradable != null) {
            SuperStackSupport.appendUpgradeSlot(upgradable.getUpgrades());
        }
    }
}
