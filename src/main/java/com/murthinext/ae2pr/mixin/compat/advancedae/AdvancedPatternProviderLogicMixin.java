package com.murthinext.ae2pr.mixin.compat.advancedae;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;

// AdvancedAE 样板供应器：补一个升级槽，宿主装卡时返回槽扩容
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic", remap = false)
public abstract class AdvancedPatternProviderLogicMixin implements SuperStackSupport.ProviderUpgrades {

    @Shadow(remap = false)
    @Final
    private AdvPatternProviderLogicHost host;

    /** 目标即 AE2 的返回库存类型，避免在方法体里做 AAE 类型的强转。 */
    @Shadow(remap = false)
    public abstract PatternProviderReturnInventory getReturnInv();

    @Unique
    private IUpgradeInventory ae2pr$upgrades = UpgradeInventories.empty();
    @Unique
    private boolean ae2pr$upgradesCreated;

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;I)V",
            at = @At("TAIL"), remap = false)
    private void ae2pr$initUpgrades(CallbackInfo ci) {
        SuperStackSupport.bind(this.getReturnInv(), this);
    }

    @Override
    public IUpgradeInventory ae2pr$getProviderUpgrades() {
        if (!this.ae2pr$upgradesCreated && this.host != null) {
            this.ae2pr$upgradesCreated = true;
            this.ae2pr$upgrades = UpgradeInventories.forMachine(
                    this.host.getTerminalIcon().getItem(), 0, this.host::saveChanges);
        }
        return this.ae2pr$upgrades;
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"), remap = false)
    private void ae2pr$loadUpgrades(CompoundTag tag, CallbackInfo ci) {
        // 先解析生效库存，确保本模组的兜底库存已按追加规则就位
        SuperStackSupport.providerUpgrades(this);
        if (this.ae2pr$upgrades.size() > 0) {
            this.ae2pr$upgrades.readFromNBT(tag, SuperStackSupport.NBT_PROVIDER_UPGRADES);
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = false)
    private void ae2pr$saveUpgrades(CompoundTag tag, CallbackInfo ci) {
        if (this.ae2pr$upgrades.size() > 0) {
            this.ae2pr$upgrades.writeToNBT(tag, SuperStackSupport.NBT_PROVIDER_UPGRADES);
        }
    }

    @Inject(method = "addDrops", at = @At("TAIL"), remap = false)
    private void ae2pr$dropUpgrades(List<ItemStack> drops, CallbackInfo ci) {
        for (var stack : this.ae2pr$upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"), remap = false)
    private void ae2pr$clearUpgrades(CallbackInfo ci) {
        this.ae2pr$upgrades.clear();
    }
}
