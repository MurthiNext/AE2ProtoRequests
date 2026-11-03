package com.murthinext.ae2pr.api;

import java.util.function.Supplier;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ClientHooks {

    private ClientHooks() {
    }

    public static void run(Supplier<Runnable> action) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, action);
    }
}
