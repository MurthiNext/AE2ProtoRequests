package com.murthinext.ae2pr.mixin.upgrade;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

// 受支持机器：对已存在的升级库存原地追加一个槽位，保留原对象、过滤与回调
@Mixin(value = AppEngInternalInventory.class, priority = 500, remap = false)
public abstract class AppEngInternalInventoryUpgradesMixin implements SuperStackSupport.AppendableUpgrades {

    @Mutable
    @Final
    @Shadow
    private NonNullList<ItemStack> stacks;

    @Mutable
    @Final
    @Shadow
    private int[] maxStack;

    @Unique
    private boolean ae2pr$appended;

    @Override
    public void ae2pr$appendUpgradeSlot() {
        if (this.ae2pr$appended) {
            return;
        }
        if (!((Object) this instanceof IUpgradeInventory upgrades)) {
            return;
        }
        if (!SuperStackSupport.isSupportedMachine(upgrades.getUpgradableItem())) {
            return;
        }
        var grown = NonNullList.withSize(this.stacks.size() + 1, ItemStack.EMPTY);
        for (int i = 0; i < this.stacks.size(); i++) {
            grown.set(i, this.stacks.get(i));
        }
        this.stacks = grown;

        var limits = Arrays.copyOf(this.maxStack, this.maxStack.length + 1);
        limits[limits.length - 1] = 1;
        this.maxStack = limits;

        this.ae2pr$appended = true;
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void ae2pr$appendBeforeRead(CompoundTag data, String name, CallbackInfo ci) {
        ae2pr$appendUpgradeSlot();
    }

    @Inject(method = "writeToNBT", at = @At("HEAD"))
    private void ae2pr$appendBeforeWrite(CompoundTag data, String name, CallbackInfo ci) {
        ae2pr$appendUpgradeSlot();
    }
}
