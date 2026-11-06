package com.murthinext.ae2pr.mixin.advanced_ae.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;

// AdvancedAE 样板供应器：对外的升级库存统一走本模组的解析
@Pseudo
@Mixin(targets = {
        "net.pedroksl.advanced_ae.common.entities.AdvPatternProviderEntity",
        "net.pedroksl.advanced_ae.common.parts.AdvPatternProviderPart"
}, remap = false)
public abstract class AdvancedPatternProviderHostMixin implements IUpgradeableObject {

    @Shadow(remap = false)
    public abstract AdvPatternProviderLogic getLogic();

    @Override
    public IUpgradeInventory getUpgrades() {
        return SuperStackSupport.providerUpgrades(this.getLogic());
    }
}
