package com.murthinext.ae2pr.machine;

import net.minecraft.network.chat.Component;

public interface Mode {
    String id();

    Component displayName();
}
