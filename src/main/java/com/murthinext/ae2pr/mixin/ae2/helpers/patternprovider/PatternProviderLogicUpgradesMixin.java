package com.murthinext.ae2pr.mixin.ae2.helpers.patternprovider;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import appeng.api.networking.IManagedGridNode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

// 样板供应器：构造期只绑定返回槽并保存宿主，fallback 升级库存按需创建
@Mixin(value = PatternProviderLogic.class, priority = 500, remap = false)
public abstract class PatternProviderLogicUpgradesMixin implements SuperStackSupport.ProviderUpgrades {

    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Unique
    @Nullable
    private IUpgradeInventory ae2pr$upgrades;

    @Inject(method =
                    "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",
            at = @At("TAIL"))
    private void
    ae2pr$bindReturnInv(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternSize, CallbackInfo ci) {
        var logic = (PatternProviderLogic) (Object) this;
        SuperStackSupport.bind(logic.getReturnInv(), logic);
    }

    @Override
    public IUpgradeInventory ae2pr$getProviderUpgrades() {
        // 未登记的机器不提供本模组 fallback，避免无谓库存与存档
        if (!SuperStackSupport.isSupportedMachine(this.host.getTerminalIcon().getItem())) {
            return UpgradeInventories.empty();
        }
        if (this.ae2pr$upgrades == null) {
            this.ae2pr$upgrades = UpgradeInventories.forMachine(this.host.getTerminalIcon().getItem(), 0,
                                                                this.host::saveChanges);
        }
        return this.ae2pr$upgrades;
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void ae2pr$loadUpgrades(CompoundTag tag, CallbackInfo ci) {
        // 未登记机器不碰任何升级数据
        if (!SuperStackSupport.isSupportedMachine(this.host.getTerminalIcon().getItem())) {
            return;
        }
        var logic = (PatternProviderLogic) (Object) this;
        var inventory = SuperStackSupport.upgradesOf(logic);
        // 只有解析结果就是本模组 fallback 时才读自己的键，第三方库存自行处理
        if (inventory != null && inventory == this.ae2pr$upgrades) {
            SuperStackSupport.appendUpgradeSlot(inventory);
            inventory.readFromNBT(tag, SuperStackSupport.NBT_PROVIDER_UPGRADES);
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2pr$saveUpgrades(CompoundTag tag, CallbackInfo ci) {
        if (ae2pr$fallbackActive()) {
            SuperStackSupport.appendUpgradeSlot(this.ae2pr$upgrades);
            this.ae2pr$upgrades.writeToNBT(tag, SuperStackSupport.NBT_PROVIDER_UPGRADES);
        }
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    private void ae2pr$dropUpgrades(List<ItemStack> drops, CallbackInfo ci) {
        if (!ae2pr$fallbackActive()) {
            return;
        }
        for (var stack : this.ae2pr$upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void ae2pr$clearUpgrades(CallbackInfo ci) {
        if (ae2pr$fallbackActive()) {
            this.ae2pr$upgrades.clear();
        }
    }

    /** 只有生效库存就是本模组 fallback 时才读写/掉落自己的数据。 */
    @Unique
    private boolean ae2pr$fallbackActive() {
        return this.ae2pr$upgrades != null &&
                SuperStackSupport.upgradesOf((PatternProviderLogic) (Object) this) == this.ae2pr$upgrades;
    }
}
