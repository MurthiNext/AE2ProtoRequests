package com.murthinext.ae2pr.api.machine.client;

import com.murthinext.nextui.client.IconButton;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AutoTransferButton extends IconButton {

    public enum Type {
        PULL,
        PUSH
    }

    public AutoTransferButton(int x, int y, Type type, BooleanSupplier state, Runnable onPress) {
        super(
                x,
                y,
                Component.translatable(
                        type == Type.PULL ? "gui.ae2pr.machine_part.auto.pull" : "gui.ae2pr.machine_part.auto.push"),
                GuiTextures.BUTTON_SKIN,
                () -> icon(type, state.getAsBoolean()),
                () -> false,
                onPress);
    }

    private static ResourceLocation icon(Type type, boolean enabled) {
        if (type == Type.PULL) {
            return enabled ? GuiTextures.PULL_ON : GuiTextures.PULL_OFF;
        }
        return enabled ? GuiTextures.PUSH_ON : GuiTextures.PUSH_OFF;
    }
}
