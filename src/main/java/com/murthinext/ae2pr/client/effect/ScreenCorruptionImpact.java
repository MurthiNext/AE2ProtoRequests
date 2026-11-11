package com.murthinext.ae2pr.client.effect;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.registry.ModEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenCorruptionImpact {
    private static final double DURATION_SECONDS = 1.05;
    private static LocalPlayer player;
    private static ClientLevel level;
    private static long lastFrameNanos;
    private static double elapsed;
    private static float seed;

    private ScreenCorruptionImpact() {
    }

    public static void trigger() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level == null
                || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getCameraEntity() != minecraft.player || minecraft.getOverlay() != null) {
            return;
        }
        player = minecraft.player;
        level = minecraft.level;
        elapsed = 0;
        lastFrameNanos = 0;
        seed = player.getRandom().nextFloat() * 256;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || player == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != player || minecraft.level != level || !player.isAlive()
                || !player.hasEffect(ModEffects.SCREEN_CORRUPTION.get())
                || !minecraft.options.getCameraType().isFirstPerson() || minecraft.getCameraEntity() != player
                || minecraft.getOverlay() != null) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (!minecraft.isPaused() && lastFrameNanos != 0) {
            elapsed += Math.max(0, now - lastFrameNanos) * 1.0e-9;
        }
        lastFrameNanos = now;
        if (elapsed >= DURATION_SECONDS) {
            clear();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float strength = strength() * Minecraft.getInstance().options.damageTiltStrength().get().floatValue();
        if (strength <= 0) {
            return;
        }
        double envelope = Math.exp(-elapsed * 7) * (1 - progress()) * strength;
        float direction = seed < 128 ? -1 : 1;
        event.setPitch(event.getPitch() - (float) (Math.sin(elapsed * 31) * envelope * 4.2));
        event.setYaw(event.getYaw() + (float) (Math.sin(elapsed * 23) * envelope * 1.3) * direction);
        event.setRoll(event.getRoll() + (float) (Math.sin(elapsed * 36) * envelope * 3.4) * direction);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        float strength = strength() * Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        if (strength <= 0 || !event.usedConfiguredFov()) {
            return;
        }
        double kick = Math.sin((elapsed - 0.035) * 19) * Math.exp(-elapsed * 8) * (1 - progress());
        event.setFOV(Mth.clamp(event.getFOV() * (1 + kick * strength * 0.18), 1, 179));
    }

    static float progress() {
        return (float) Math.min(1, elapsed / DURATION_SECONDS);
    }

    static float strength() {
        var minecraft = Minecraft.getInstance();
        return player != null && minecraft.screen == null && minecraft.getOverlay() == null
                ? minecraft.options.screenEffectScale().get().floatValue() : 0;
    }

    static float seed() {
        return seed;
    }

    static void clear() {
        player = null;
        level = null;
        elapsed = 0;
        lastFrameNanos = 0;
    }
}
