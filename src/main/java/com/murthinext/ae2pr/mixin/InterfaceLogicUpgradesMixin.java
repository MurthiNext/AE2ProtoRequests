package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.networking.IManagedGridNode;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.Item;

// ME 接口：构造函数只做返回/存储槽绑定，槽位追加推迟到使用边界
@Mixin(value = InterfaceLogic.class, priority = 1200, remap = false)
public abstract class InterfaceLogicUpgradesMixin {

    @Shadow
    @Final
    private ConfigInventory storage;

    @Shadow
    @Final
    private ConfigInventory config;

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/InterfaceLogicHost;Lnet/minecraft/world/item/Item;I)V",
            at = @At("TAIL"), require = 0)
    private void ae2pr$bindStorage(IManagedGridNode gridNode, InterfaceLogicHost host, Item is, int slots,
            CallbackInfo ci) {
        SuperStackSupport.bind(this.storage, (InterfaceLogic) (Object) this);
        SuperStackSupport.bind(this.config, (InterfaceLogic) (Object) this);
    }
}
