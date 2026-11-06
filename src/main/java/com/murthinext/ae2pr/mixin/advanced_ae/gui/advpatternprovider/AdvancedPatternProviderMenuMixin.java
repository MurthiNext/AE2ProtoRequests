package com.murthinext.ae2pr.mixin.advanced_ae.gui.advpatternprovider;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;

// AdvancedAE 样板供应器：把追加的升级槽接到界面上
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.gui.advpatternprovider.AdvPatternProviderMenu", remap = false)
public abstract class AdvancedPatternProviderMenuMixin extends AEBaseMenu {

    @Shadow(remap = false)
    @Final
    protected AdvPatternProviderLogic logic;

    public AdvancedPatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory,
            AdvPatternProviderLogicHost host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;)V",
            at = @At("TAIL"), remap = false)
    private void ae2pr$setupUpgrades(MenuType<?> menuType, int id, Inventory playerInventory,
            AdvPatternProviderLogicHost host, CallbackInfo ci) {
        // 同一库存实例由 AEBaseMenu.setupUpgrades 按 identity 去重，这里不做槽位判断
        this.setupUpgrades(SuperStackSupport.providerUpgrades(this.logic));
    }
}
