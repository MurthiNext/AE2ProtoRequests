package com.murthinext.ae2pr.mixin.extendedae.common.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.stacks.AEKey;

// ExtendedAE ME超大接口：自定义 config 不回调父类 getMaxAmount，这里补上装卡后的扩容判定
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.common.inventory.OversizeConfigInv", remap = false)
public abstract class OversizeConfigInventoryMixin {

    @Inject(
            method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void ae2pr$boostMaxAmount(AEKey key, CallbackInfoReturnable<Long> cir) {
        if (key != null
                && (Object) this instanceof SuperStackSupport.Target target
                && target.ae2pr$isBoosted(key.getType())) {
            cir.setReturnValue(SuperStackSupport.BOOSTED_AMOUNT);
        }
    }
}
