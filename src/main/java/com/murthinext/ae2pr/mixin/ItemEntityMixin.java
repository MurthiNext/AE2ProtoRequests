package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.logic.alien_lava.AlienLavaInteractions;
import com.murthinext.ae2pr.logic.alien_lava.AlienLavaTransformTimer;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * 物品实体：挂载异星熔岩转化计时器，并处理岩浆伤害前的充能转化。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements AlienLavaTransformTimer {

    @Unique
    private int ae2pr$transformTicks;

    @Override
    public int ae2pr$getTransformTicks() {
        return ae2pr$transformTicks;
    }

    @Override
    public void ae2pr$setTransformTicks(int ticks) {
        this.ae2pr$transformTicks = ticks;
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void ae2pr$onTick(CallbackInfo ci) {
        AlienLavaInteractions.onItemTick((ItemEntity) (Object) this);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ae2pr$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.LAVA) && AlienLavaInteractions.tryChargeLava((ItemEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
