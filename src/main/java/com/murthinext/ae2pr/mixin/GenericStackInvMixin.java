package com.murthinext.ae2pr.mixin;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;

// 装卡后所有 AEKey 的堆叠上限统一为 INT MAX，移卡后保留已有超量存量
@Mixin(value = GenericStackInv.class, priority = 500, remap = false)
public abstract class GenericStackInvMixin implements SuperStackSupport.Target {

    @Unique
    private static final int AE2PR_MAX_RESOLVE_ATTEMPTS = 8;

    @Shadow
    @Final
    protected GenericStack[] stacks;

    @Unique
    @Nullable
    private BooleanSupplier ae2pr$check;
    @Unique
    private int ae2pr$resolveAttempts;

    @Override
    public void ae2pr$setOwner(@Nullable Object owner) {
        this.ae2pr$check = owner == null ? null : () -> SuperStackSupport.hasCard(owner);
    }

    @Override
    public boolean ae2pr$isManaged() {
        return ae2pr$resolveOwner();
    }

    @Override
    public void ae2pr$setSyncedStack(int slot, @Nullable GenericStack stack) {
        if (slot >= 0 && slot < this.stacks.length) {
            this.stacks[slot] = stack;
        }
    }

    @Inject(method = "getMaxAmount", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$boostMaxAmount(AEKey key, CallbackInfoReturnable<Long> cir) {
        if (key != null && ae2pr$isBoosted(key.getType())) {
            cir.setReturnValue(SuperStackSupport.BOOSTED_AMOUNT);
        }
    }

    @Inject(method = "getCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2pr$boostCapacity(AEKeyType type, CallbackInfoReturnable<Long> cir) {
        if (ae2pr$isBoosted(type)) {
            cir.setReturnValue(SuperStackSupport.BOOSTED_AMOUNT);
        }
    }

    /** setStack 的两处容量判定改为 max(真实上限, 同槽同 key 的原存量)，移卡后不把超量截回原值。 */
    @Redirect(method = "setStack", at = @At(value = "INVOKE",
            target = "Lappeng/helpers/externalstorage/GenericStackInv;getMaxAmount(Lappeng/api/stacks/AEKey;)J"),
            remap = false)
    private long ae2pr$limitForMove(GenericStackInv inventory, AEKey key, int slot,
            @Nullable GenericStack replacement) {
        var limit = inventory.getMaxAmount(key);
        if (key == null || !ae2pr$isManaged()) {
            return limit;
        }
        if (slot < 0 || slot >= this.stacks.length) {
            return limit;
        }
        var previous = this.stacks[slot];
        if (previous == null || !key.equals(previous.what())) {
            return limit;
        }
        return Math.max(limit, previous.amount());
    }

    /** 宿主显式绑定优先，否则从监听器反查；构造期可能拿不到宿主，重试有限次后放弃。 */
    @Unique
    private boolean ae2pr$resolveOwner() {
        if (this.ae2pr$check != null) {
            return true;
        }
        if (this.ae2pr$resolveAttempts >= AE2PR_MAX_RESOLVE_ATTEMPTS) {
            return false;
        }
        this.ae2pr$resolveAttempts++;
        var owner = SuperStackSupport.resolveStorageOwner((GenericStackInv) (Object) this);
        if (owner == null) {
            return false;
        }
        ae2pr$setOwner(owner);
        return true;
    }

    @Override
    public boolean ae2pr$isBoosted(AEKeyType type) {
        return type != null && ae2pr$resolveOwner() && this.ae2pr$check.getAsBoolean();
    }
}
