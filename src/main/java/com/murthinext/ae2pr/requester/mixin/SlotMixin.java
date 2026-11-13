package com.murthinext.ae2pr.requester.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.Slot;

@SuppressWarnings("ALL")
@Mixin(Slot.class)
public interface SlotMixin {
    @Accessor("x")
    void ae2pr$setX(int x);

    @Accessor("y")
    void ae2pr$setY(int y);
}
