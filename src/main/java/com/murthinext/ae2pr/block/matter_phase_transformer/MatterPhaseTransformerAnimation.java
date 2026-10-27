package com.murthinext.ae2pr.block.matter_phase_transformer;

// 相变仪特效的快速蓄能与缓慢衰减
public final class MatterPhaseTransformerAnimation {

    private static final float CHARGE_STEP = 1.0F / 20;
    private static final float RELEASE_STEP = 1.0F / 80;
    private static final double MAX_SPEED = 24.0;

    private float activity;
    private float previousActivity;
    private double particleTime;
    private double previousParticleTime;

    public void tick(boolean formed, boolean running) {
        if (!formed) {
            activity = 0.0F;
            previousActivity = 0.0F;
            particleTime = 0.0;
            previousParticleTime = 0.0;
            return;
        }
        previousActivity = activity;
        previousParticleTime = particleTime;
        activity = running ? Math.min(1.0F, activity + CHARGE_STEP)
                : Math.max(0.0F, activity - RELEASE_STEP);
        particleTime += 1.0 + (MAX_SPEED - 1.0) * activity;
    }

    public float getActivity(float partialTick) {
        return previousActivity + (activity - previousActivity) * partialTick;
    }

    public double getParticleTime(float partialTick) {
        return previousParticleTime + (particleTime - previousParticleTime) * partialTick;
    }

    public float getBeamStrength(float partialTick) {
        float strength = Math.max(0.0F, Math.min(1.0F, (getActivity(partialTick) - 0.3F) / 0.7F));
        return strength * strength * (3.0F - 2.0F * strength);
    }
}
