package com.murthinext.ae2pr.requester.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("ALL")
@Mixin(EditBox.class)
public interface EditBoxMixin {
    @Accessor("isEditable")
    boolean ae2pr$isEditable();
}
