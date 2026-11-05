package com.murthinext.ae2pr.mixin.upgrade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.parts.crafting.PatternProviderPart;

// 样板供应器线缆部件：支持 Shift+右键插卡，并在入世后绑定返回槽
@Mixin(value = PatternProviderPart.class, priority = 500, remap = false)
public abstract class PatternProviderPartMixin implements IUpgradeableObject {

    @Inject(method = "addToWorld", at = @At("TAIL"))
    private void ae2pr$bindOnAddToWorld(CallbackInfo ci) {
        var logic = ((PatternProviderPart) (Object) this).getLogic();
        if (logic != null) {
            SuperStackSupport.bind(logic.getReturnInv(), logic);
        }
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return SuperStackSupport.providerUpgrades(((PatternProviderPart) (Object) this).getLogic());
    }
}
