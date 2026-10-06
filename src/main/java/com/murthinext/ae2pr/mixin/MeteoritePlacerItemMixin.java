package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import appeng.debug.MeteoritePlacerItem;

/**
 * 将调试用陨石放置器的半径由 2~8 调整为 8~16。
 */
@Mixin(value = MeteoritePlacerItem.class, remap = false)
public abstract class MeteoritePlacerItemMixin {

    @ModifyConstant(method = "onItemUseFirst", constant = @Constant(floatValue = 6.0F))
    private float ae2pr$enlargeRadiusRange(float original) {
        return 8.0F;
    }

    @ModifyConstant(method = "onItemUseFirst", constant = @Constant(floatValue = 2.0F, ordinal = 0))
    private float ae2pr$raiseRadiusBase(float original) {
        return 8.0F;
    }
}
