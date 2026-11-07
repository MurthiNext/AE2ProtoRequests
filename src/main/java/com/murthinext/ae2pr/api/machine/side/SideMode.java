package com.murthinext.ae2pr.api.machine.side;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum SideMode {
    NONE,
    INPUT,
    OUTPUT,
    BOTH;

    private static final SideMode[] VALUES = values();

    public boolean allowsInsert() {
        return this == INPUT || this == BOTH;
    }

    public boolean allowsExtract() {
        return this == OUTPUT || this == BOTH;
    }

    public SideMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public Component displayName() {
        return Component.translatable("gui.ae2pr.side_config.mode." + name().toLowerCase(Locale.ROOT));
    }
}
