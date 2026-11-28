package com.murthinext.ae2pr.mixin.ae2.parts;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.parts.CableBusContainer;
import com.murthinext.ae2pr.cable_part.sky_dome.SkyDomeAnnihilationPlanePart;
import com.murthinext.ae2pr.cable_part.sky_dome.SkyDomeAnnihilationPlanePartItem;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CableBusContainer.class, remap = false)
public abstract class CableBusContainerMixin {

    @Shadow
    public abstract BlockEntity getBlockEntity();

    @Inject(method = "canAddPart", at = @At("HEAD"), cancellable = true)
    private void ae2pr$checkSkyDomePlacement(ItemStack stack, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof SkyDomeAnnihilationPlanePartItem) {
            var host = getBlockEntity();
            if (!SkyDomeAnnihilationPlanePart.canPlaceAt(host.getLevel(), host.getBlockPos(), side)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "addPart", at = @At("HEAD"), cancellable = true)
    private <T extends IPart> void ae2pr$restrictSkyDomePlacement(
            IPartItem<T> item, Direction side, Player player, CallbackInfoReturnable<T> cir) {
        if (item instanceof SkyDomeAnnihilationPlanePartItem) {
            var host = getBlockEntity();
            if (!SkyDomeAnnihilationPlanePart.canPlaceAt(host.getLevel(), host.getBlockPos(), side)) {
                cir.setReturnValue(null);
            }
        }
    }
}
