package com.murthinext.ae2pr.mixin.ae2cs.common.me.logic;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.util.ConfigInventory;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

// AE2CS 集成接口：独立逻辑不复用 InterfaceLogic，这里补上配置/存储库存的宿主绑定
@Pseudo
@Mixin(targets = "io.github.lounode.ae2cs.common.me.logic.IntegratedInterfaceLogic", remap = false)
public abstract class IntegratedInterfaceLogicMixin {

    @Shadow(remap = false)
    @Final
    private ConfigInventory configInv;

    @Shadow(remap = false)
    @Final
    private ConfigInventory storage;

    @Inject(method =
                    "<init>(Lappeng/api/networking/IManagedGridNode;Lio/github/lounode/ae2cs/common/me/logic/IntegratedInterfaceHost;II)V",
            at = @At("TAIL"), remap = false)
    private void
    ae2pr$bindInventory(CallbackInfo ci) {
        SuperStackSupport.bind(this.configInv, this);
        SuperStackSupport.bind(this.storage, this);
    }
}
