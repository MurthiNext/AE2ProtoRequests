package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.logic.world_interaction.ComplexWorldInteractions;
import com.murthinext.ae2pr.logic.world_interaction.WorldInteractionEntity;
import com.murthinext.ae2pr.logic.world_interaction.WorldInteractionProgress;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * 物品实体的复杂世界交互与伤害保护。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements WorldInteractionEntity {

    @Unique
    private final WorldInteractionProgress ae2pr$interactionProgress = new WorldInteractionProgress();

    @Override
    public WorldInteractionProgress ae2pr$getWorldInteractionProgress() {
        return ae2pr$interactionProgress;
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void ae2pr$onTick(CallbackInfo ci) {
        ComplexWorldInteractions.onItemTick((ItemEntity) (Object) this);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ae2pr$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity item = (ItemEntity) (Object) this;
        if ((source.is(DamageTypeTags.IS_EXPLOSION) && ComplexWorldInteractions.tryExplosion(item))
                || (source.is(DamageTypeTags.IS_FIRE) && ComplexWorldInteractions.protectFromFire(item))) {
            cir.setReturnValue(false);
        }
    }
}
