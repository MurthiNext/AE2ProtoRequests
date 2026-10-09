package com.murthinext.ae2pr.client.proto_terminal;

import net.minecraft.util.Mth;

/** 终端标题、剧情块和连线的入场时序。 */
record TerminalAnimation(int nodeCount) {

    static final long OPEN_DURATION = 1050;
    private static final long HEADER_DURATION = 240;
    private static final long NODE_DURATION = 360;
    private static final long NODE_STAGGER = 65;
    private static final long CONNECTION_DURATION = 500;

    float opening(long elapsed) {
        return Mth.clamp(elapsed / (float) OPEN_DURATION, 0, 1);
    }

    float headerOpacity(long elapsed) {
        return CrystalNarrativeGraphics.ease((elapsed - OPEN_DURATION) / (float) HEADER_DURATION);
    }

    float nodeProgress(long elapsed, int index) {
        long start = OPEN_DURATION + HEADER_DURATION + Math.min(index, 6) * NODE_STAGGER;
        return CrystalNarrativeGraphics.ease((elapsed - start) / (float) NODE_DURATION);
    }

    float connectionProgress(long elapsed) {
        long start = OPEN_DURATION + HEADER_DURATION + NODE_DURATION
                + Math.min(Math.max(0, nodeCount - 1), 6) * NODE_STAGGER;
        return Mth.clamp((elapsed - start) / (float) CONNECTION_DURATION, 0, 1);
    }

    static float fadeOut(long elapsed, long duration) {
        return 1 - CrystalNarrativeGraphics.ease(elapsed / (float) Math.max(1, duration));
    }
}
