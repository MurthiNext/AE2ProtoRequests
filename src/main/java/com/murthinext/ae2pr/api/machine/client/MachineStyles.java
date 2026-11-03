package com.murthinext.ae2pr.api.machine.client;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public final class MachineStyles {

    private static Function<Object, MachineStyle> lookup = definition -> {
        throw new IllegalStateException("Machine style lookup not installed");
    };

    private MachineStyles() {
    }

    public static void install(Function<Object, MachineStyle> impl) {
        lookup = impl;
    }

    public static <T> MachineStyle of(T definition) {
        return lookup.apply(definition);
    }

    public static ResourceLocation guidePage(Object definition) {
        return of(definition).guidePage();
    }
}
