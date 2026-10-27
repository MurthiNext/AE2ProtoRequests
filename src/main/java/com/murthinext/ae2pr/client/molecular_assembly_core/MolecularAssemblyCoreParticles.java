package com.murthinext.ae2pr.client.molecular_assembly_core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import appeng.client.render.effects.ParticleTypes;
import appeng.core.AppEngClient;

public final class MolecularAssemblyCoreParticles {

    private static final float SPAWN_CHANCE = 0.55F;
    private static final int MIN_PER_SPAWN = 1;
    private static final int MAX_PER_SPAWN = 3;
    private static final double RANGE_SQUARED = 32.0 * 32.0;

    private MolecularAssemblyCoreParticles() {
    }

    public static void tick(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel client) || !isVisible(client, pos)) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= SPAWN_CHANCE || !AppEngClient.instance().shouldAddParticles(random)) {
            return;
        }
        int count = MIN_PER_SPAWN + random.nextInt(MAX_PER_SPAWN - MIN_PER_SPAWN + 1);
        for (int i = 0; i < count; i++) {
            spawn(client, pos, random);
        }
    }

    private static void spawn(ClientLevel level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double y = pos.getY() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        double speed = 0.01 + random.nextDouble() * 0.02;
        level.addParticle(ParticleTypes.CRAFTING, x, y, z,
                (random.nextDouble() - 0.5) * speed,
                (random.nextDouble() - 0.5) * speed,
                (random.nextDouble() - 0.5) * speed);
    }

    private static boolean isVisible(ClientLevel level, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == level && !minecraft.isPaused()
                && minecraft.player != null
                && minecraft.player.position().distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < RANGE_SQUARED;
    }
}
