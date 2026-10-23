package com.murthinext.ae2pr.client.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.murthinext.ae2pr.ModSounds;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

/**
 * 客户端加工循环音效与卸载清理。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT)
public final class MachineProcessingSounds {

    private static final double RANGE_SQUARED = 16.0 * 16.0;
    private static final int SUBTITLE_INTERVAL = 40;
    private static final Map<BlockEntity, ProcessingSound> SOUNDS = new HashMap<>();

    public enum Type {
        ASSEMBLY_LINE(ModSounds.ASSEMBLY_LINE, 1.0F, 1.0F, 0),
        LAVA_SMELTER(() -> SoundEvents.LAVA_AMBIENT, 0.4F, 0.8F, 0, ModSounds.LAVA_SMELTER),
        MATTER_PHASE_TRANSFORMER(ModSounds.MATTER_PHASE_TRANSFORMER, 0.65F, 1.0F, 0),
        CRYSTAL_REACTION(ModSounds.CRYSTAL_REACTION, 0.7F, 1.0F, 0),
        CRYSTAL_WORKSTATION_ETCHING(ModSounds.CRYSTAL_WORKSTATION_ETCHING, 0.7F, 1.0F, 0),
        CRYSTAL_WORKSTATION_CUTTING(ModSounds.CRYSTAL_WORKSTATION_CUTTING, 0.7F, 1.0F, 0);

        private final Supplier<SoundEvent> sound;
        private final float volume;
        private final float pitch;
        private final int delay;
        private final Supplier<SoundEvent> subtitleSource;

        Type(Supplier<SoundEvent> sound, float volume, float pitch, int delay) {
            this(sound, volume, pitch, delay, sound);
        }

        Type(Supplier<SoundEvent> sound, float volume, float pitch, int delay,
                Supplier<SoundEvent> subtitleSource) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.delay = delay;
            this.subtitleSource = subtitleSource;
        }

        private boolean isRunning(BlockEntity controller) {
            BlockState state = controller.getBlockState();
            return state.hasProperty(AbstractMultiblockControllerBlock.FORMED)
                    && state.hasProperty(AbstractMultiblockControllerBlock.RUNNING)
                    && state.getValue(AbstractMultiblockControllerBlock.FORMED)
                    && state.getValue(AbstractMultiblockControllerBlock.RUNNING);
        }
    }

    private MachineProcessingSounds() {
    }

    public static void tick(BlockEntity controller, Type type) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!type.isRunning(controller)
                || !isAudible(minecraft, controller)) {
            return;
        }
        ProcessingSound sound = SOUNDS.get(controller);
        if (sound != null && sound.type != type) {
            // 工作模式切换后换成对应音效
            sound.release();
            minecraft.getSoundManager().stop(sound);
            SOUNDS.remove(controller);
            sound = null;
        }
        if (sound == null) {
            sound = new ProcessingSound(controller, type);
            SOUNDS.put(controller, sound);
            minecraft.getSoundManager().play(sound);
        }
        sound.keepSubtitle(minecraft);
        if (type == Type.LAVA_SMELTER) {
            sound.tickSplashes(minecraft);
        }
    }

    private static boolean isAudible(Minecraft minecraft, BlockEntity controller) {
        return minecraft.level != null && minecraft.level == controller.getLevel()
                && minecraft.player != null && !controller.isRemoved()
                && minecraft.level.hasChunkAt(controller.getBlockPos())
                && controller.getBlockPos().distToCenterSqr(minecraft.player.position()) < RANGE_SQUARED
                && minecraft.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS) > 0.0F;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.isPaused()) {
            return;
        }
        var soundManager = minecraft.getSoundManager();
        SOUNDS.values().removeIf(sound -> {
            // 为异步音频加载留出时间，资源重载后允许重新播放
            sound.inactiveTicks = soundManager.isActive(sound) ? 0 : sound.inactiveTicks + 1;
            boolean inactive = sound.inactiveTicks > 20;
            if (sound.isStopped() || !sound.canPlaySound() || inactive) {
                sound.release();
                soundManager.stop(sound);
                return true;
            }
            return false;
        });
    }

    private static final class ProcessingSound extends AbstractTickableSoundInstance {

        private final BlockEntity controller;
        private final Type type;
        private final List<LavaSplashSound> splashes = new ArrayList<>();
        private int inactiveTicks;
        private int splashTicks;
        private int subtitleTicks;

        private ProcessingSound(BlockEntity controller, Type type) {
            super(type.sound.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.controller = controller;
            this.type = type;
            this.looping = true;
            this.delay = type.delay;
            this.volume = type.volume;
            this.pitch = type.pitch;
            this.attenuation = Attenuation.LINEAR;
            this.x = controller.getBlockPos().getX() + 0.5;
            this.y = controller.getBlockPos().getY() + 0.5;
            this.z = controller.getBlockPos().getZ() + 0.5;
        }

        @Override
        public boolean canPlaySound() {
            Minecraft minecraft = Minecraft.getInstance();
            return !isStopped() && type.isRunning(controller) && isAudible(minecraft, controller)
                    && minecraft.level.getBlockEntity(controller.getBlockPos()) == controller;
        }

        // 刷新字幕
        private void keepSubtitle(Minecraft minecraft) {
            if (!minecraft.options.showSubtitles().get() || ++subtitleTicks < SUBTITLE_INTERVAL) {
                return;
            }
            subtitleTicks = 0;
            WeighedSoundEvents events = minecraft.getSoundManager()
                    .getSoundEvent(type.subtitleSource.get().getLocation());
            if (events == null || events.getSubtitle() == null) {
                return;
            }
            minecraft.gui.subtitleOverlay.onPlaySound(this, events);
        }

        private void release() {
            stop();
            var soundManager = Minecraft.getInstance().getSoundManager();
            for (LavaSplashSound splash : splashes) {
                splash.release();
                soundManager.stop(splash);
            }
            splashes.clear();
        }

        private void tickSplashes(Minecraft minecraft) {
            if (!canPlaySound()) {
                return;
            }
            var soundManager = minecraft.getSoundManager();
            splashes.removeIf(splash -> splash.isStopped()
                    || (++splash.age > 20 && !soundManager.isActive(splash)));
            if (--splashTicks > 0) {
                return;
            }
            // 每 20～40 tick 叠加随机音调的熔岩迸溅
            splashTicks = 20 + random.nextInt(21);
            if (splashes.size() < 8) {
                LavaSplashSound splash = new LavaSplashSound(this);
                splashes.add(splash);
                soundManager.play(splash);
            }
        }

        @Override
        public void tick() {
            if (!canPlaySound()) {
                stop();
            }
        }
    }

    private static final class LavaSplashSound extends AbstractTickableSoundInstance {

        private final ProcessingSound owner;
        private int age;

        private LavaSplashSound(ProcessingSound owner) {
            super(ModSounds.LAVA_SMELTER.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.owner = owner;
            this.volume = 0.22F + random.nextFloat() * 0.18F;
            this.pitch = 0.6F + random.nextFloat() * 0.55F;
            this.attenuation = Attenuation.LINEAR;
            this.x = owner.getX() + (random.nextDouble() - 0.5) * 1.8;
            this.y = owner.getY();
            this.z = owner.getZ() + (random.nextDouble() - 0.5) * 1.8;
        }

        @Override
        public boolean canPlaySound() {
            return !isStopped() && owner.canPlaySound();
        }

        private void release() {
            stop();
        }

        @Override
        public void tick() {
            if (!canPlaySound()) {
                stop();
            }
        }
    }
}
