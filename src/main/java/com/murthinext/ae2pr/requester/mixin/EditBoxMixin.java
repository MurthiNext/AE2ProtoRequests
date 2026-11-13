package com.murthinext.ae2pr.requester.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.EditBox;

@SuppressWarnings("ALL")
@Mixin(EditBox.class)
public interface EditBoxMixin {
    @Accessor("isEditable")
    boolean ae2pr$isEditable();
}
