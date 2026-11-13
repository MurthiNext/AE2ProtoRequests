package com.murthinext.ae2pr.api.machine.client;

import net.minecraft.resources.ResourceLocation;

public final class GuideOpener {

    @FunctionalInterface
    public interface Opener {
        void open(ResourceLocation page);
    }

    private static Opener opener = page -> {
    };

    private GuideOpener() {
    }

    public static void install(Opener impl) {
        opener = impl;
    }

    public static void open(ResourceLocation page) {
        opener.open(page);
    }
}
