package com.murthinext.ae2pr.mixin.ae2.menu.slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import net.minecraft.world.item.ItemStack;

// 客户端按服务器权威值写入返回/存储槽，避免本地未同步的升级卡状态把超量存量截断
@Mixin(value = AppEngSlot.class, priority = 500, remap = false)
public abstract class AppEngSlotSuperStackMixin {

    @Shadow
    protected abstract AEBaseMenu getMenu();

    @Shadow
    public abstract void resetCachedValidation();

    @Inject(method = "set(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true, remap = true)
    private void ae2pr$applySyncedOnSet(ItemStack stack, CallbackInfo ci) {
        if (ae2pr$applySynced(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$applySyncedOnInitialize(ItemStack stack, CallbackInfo ci) {
        if (ae2pr$applySynced(stack)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean ae2pr$applySynced(ItemStack stack) {
        var menu = this.getMenu();
        if (menu == null || !menu.isClientSide()) {
            return false;
        }
        var slot = (AppEngSlot) (Object) this;
        if (!(slot.getInventory() instanceof ConfigMenuInventory menuInventory)) {
            return false;
        }
        var delegate = menuInventory.getDelegate();
        var mode = delegate.getMode();
        if (mode != GenericStackInv.Mode.STORAGE && mode != GenericStackInv.Mode.CONFIG_STACKS) {
            return false;
        }
        if (!(delegate instanceof SuperStackSupport.Target target) || !target.ae2pr$isManaged()) {
            return false;
        }
        GenericStack converted = menuInventory.convertToSuitableStack(stack);
        if (converted == null && !stack.isEmpty()) {
            // 无法转换时交回原逻辑
            return false;
        }
        // 槽位号取真实库存索引，Slot.index 是菜单顺序
        target.ae2pr$setSyncedStack(slot.getContainerSlot(), converted);
        this.resetCachedValidation();
        return true;
    }
}
