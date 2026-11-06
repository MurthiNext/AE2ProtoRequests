package com.murthinext.ae2pr.mixin.ae2.blockentity;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.collapse.CollapseHandler;

import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = AEBaseBlockEntity.class, priority = 500, remap = false)
public abstract class AEBaseBlockEntityDropsMixin {

    @Inject(method = "addAdditionalDrops", at = @At("HEAD"), remap = false)
    private void ae2pr$collapseContents(Level level, BlockPos pos, List<ItemStack> drops, CallbackInfo ci) {
        CollapseHandler.collapseInto((AEBaseBlockEntity) (Object) this, drops);
    }
}
