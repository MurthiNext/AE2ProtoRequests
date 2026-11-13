package com.murthinext.ae2pr.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.effect.ScreenCorruptionEffect;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
                                                                                      ae2pr.MODID);

    public static final RegistryObject<ScreenCorruptionEffect> SCREEN_CORRUPTION = EFFECTS.register(
            "screen_corruption", ScreenCorruptionEffect::new);

    private ModEffects() {
    }
}
