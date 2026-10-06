package com.murthinext.ae2pr.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import appeng.worldgen.meteorite.MeteoriteStructure;

/**
 * 将陨石半径由 2~8 调整为 8~16。
 */
@Mixin(value = MeteoriteStructure.class, remap = false)
public abstract class MeteoriteStructureMixin {

    @ModifyConstant(method = "generatePieces", constant = @Constant(floatValue = 6.0F))
    private static float ae2pr$enlargeRadiusRange(float original) {
        return 8.0F;
    }

    @ModifyConstant(method = "generatePieces", constant = @Constant(floatValue = 2.0F, ordinal = 0))
    private static float ae2pr$raiseRadiusBase(float original) {
        return 8.0F;
    }
}
