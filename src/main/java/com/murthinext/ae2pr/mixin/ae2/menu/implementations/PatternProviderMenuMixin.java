package com.murthinext.ae2pr.mixin.ae2.menu.implementations;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.PatternProviderMenu;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

// 样板供应器菜单：统一按实际生效的升级库存补槽，附属继承的菜单一并覆盖
@Mixin(value = PatternProviderMenu.class, priority = 500, remap = false)
public abstract class PatternProviderMenuMixin extends AEBaseMenu {

    @Shadow
    @Final
    protected PatternProviderLogic logic;

    public PatternProviderMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory,
                                    PatternProviderLogicHost host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method =
                    "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/patternprovider/PatternProviderLogicHost;)V",
            at = @At("TAIL"))
    private void
    ae2pr$setupUpgrades(MenuType<?> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host,
                        CallbackInfo ci) {
        this.setupUpgrades(SuperStackSupport.providerUpgrades(this.logic));
    }
}
