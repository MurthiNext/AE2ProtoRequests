package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import net.minecraft.nbt.CompoundTag;

// 样板供应器方块：支持 Shift+右键插卡，并在就绪/读档后绑定返回槽
@Mixin(value = PatternProviderBlockEntity.class, priority = 500, remap = false)
public abstract class PatternProviderBlockEntityMixin implements IUpgradeableObject {

    @Inject(method = "onReady", at = @At("TAIL"))
    private void ae2pr$bindOnReady(CallbackInfo ci) {
        ae2pr$bind();
    }

    @Inject(method = "loadTag", at = @At("TAIL"))
    private void ae2pr$bindOnLoad(CompoundTag tag, CallbackInfo ci) {
        ae2pr$bind();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return SuperStackSupport.providerUpgrades(((PatternProviderBlockEntity) (Object) this).getLogic());
    }

    private void ae2pr$bind() {
        var logic = ((PatternProviderBlockEntity) (Object) this).getLogic();
        if (logic != null) {
            SuperStackSupport.bind(logic.getReturnInv(), logic);
        }
    }
}
