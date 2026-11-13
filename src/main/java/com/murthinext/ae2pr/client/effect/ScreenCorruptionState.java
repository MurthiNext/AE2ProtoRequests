package com.murthinext.ae2pr.client.effect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

final class ScreenCorruptionState {
    private final List<Region> regions = new ArrayList<>();
    private final List<Region> view = Collections.unmodifiableList(regions);
    private UUID playerId;
    private Random random;
    private double startedAt;
    private double time;

    void update(UUID playerId, int count, double now) {
        if (!playerId.equals(this.playerId)) {
            clear();
            this.playerId = playerId;
            random = new Random(playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits());
            startedAt = now;
        }
        time = Math.max(time, now - startedAt);
        count = Math.max(0, Math.min(Byte.MAX_VALUE + 1, count));
        if (regions.size() > count) {
            regions.subList(count, regions.size()).clear();
        }
        while (regions.size() < count) {
            regions.add(Region.create(random, time));
        }
    }

    List<Region> regions() {
        return view;
    }

    double time() {
        return time;
    }

    void clear() {
        regions.clear();
        playerId = null;
        random = null;
        startedAt = 0;
        time = 0;
    }

    record Region(double x, double y, double width, double height, float seed, double bornAt) {
        private static final double REVEAL_SECONDS = 0.72;

        private static Region create(Random random, double time) {
            double area = 0.10 + random.nextDouble() * 0.10;
            double width = Math.sqrt(area) * (0.8 + random.nextDouble() * 0.4);
            double height = area / width;
            return new Region(random.nextDouble() * (1 - width), random.nextDouble() * (1 - height), width, height,
                              random.nextFloat() * 256, time);
        }

        double areaProgress(double time) {
            double progress = Math.max(0, Math.min(1, (time - bornAt) / REVEAL_SECONDS));
            return progress * progress * (3 - 2 * progress);
        }

        double scale(double time) {
            return Math.sqrt(areaProgress(time));
        }
    }
}
