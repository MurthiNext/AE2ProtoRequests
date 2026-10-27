package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.repeat.IRepeatRoundsHolder;
import com.murthinext.ae2pr.logic.repeat.RepeatOrderConstants;
import com.murthinext.ae2pr.logic.repeat.ServerRepeatRegistry;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

// 数量选择菜单：轮数同步与回填，登记移至 {@code MenuOpenerMixin} 以兼容 GTLCore
@Mixin(value = CraftAmountMenu.class, remap = false)
public abstract class CraftAmountMenuMixin extends AEBaseMenu implements IRepeatRoundsHolder {

    @Unique
    @GuiSync(30)
    public int ae2pr$repeatRounds = 1;

    public CraftAmountMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lappeng/api/storage/ISubMenuHost;)V",
            at = @At("TAIL"))
    private void ae2pr$initRepeatRounds(int id, Inventory playerInventory, ISubMenuHost host, CallbackInfo ci) {
        this.registerClientAction(RepeatOrderConstants.ACTION_SET_REPEAT_ROUNDS, Integer.class,
                this::ae2pr$setRepeatRounds);

        // 从确认界面返回时恢复轮数（确认菜单关闭时写入恢复队列）
        if (getPlayer() instanceof ServerPlayer serverPlayer) {
            var restored = ServerRepeatRegistry.consumeRestore(serverPlayer.getUUID());
            if (restored != null && restored > 1) {
                this.ae2pr$repeatRounds = restored;
            }
        }
    }

    @Override
    public int ae2pr$getRepeatRounds() {
        return this.ae2pr$repeatRounds;
    }

    @Override
    public void ae2pr$setRepeatRounds(int rounds) {
        this.ae2pr$repeatRounds = Math.max(1, rounds);
    }
}
