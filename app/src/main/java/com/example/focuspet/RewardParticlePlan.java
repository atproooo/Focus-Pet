package com.example.focuspet;

import java.util.ArrayList;
import java.util.List;

final class RewardParticlePlan {
    private static final int MAX_COIN_PARTICLES = 24;
    private static final int MAX_XP_FOR_INTENSITY = 240;

    enum Kind {
        XP,
        COIN,
        ESSENCE,
        EGG
    }

    static final class Particle {
        final Kind kind;
        final int amount;
        final float intensity;

        private Particle(Kind kind, int amount, float intensity) {
            this.kind = kind;
            this.amount = amount;
            this.intensity = intensity;
        }
    }

    private RewardParticlePlan() { }

    static List<Particle> create(int xp, int coins, int essence, boolean egg) {
        List<Particle> particles = new ArrayList<>();
        if (xp > 0) particles.add(new Particle(Kind.XP, xp, xpIntensity(xp)));
        for (int i = 0; i < Math.min(Math.max(0, coins), MAX_COIN_PARTICLES); i++) {
            particles.add(new Particle(Kind.COIN, 1, 1f));
        }
        for (int i = 0; i < Math.max(0, essence); i++) {
            particles.add(new Particle(Kind.ESSENCE, 1, 1f));
        }
        if (egg) particles.add(new Particle(Kind.EGG, 1, 1f));
        return particles;
    }

    private static float xpIntensity(int xp) {
        float progress = Math.min(Math.max(0, xp), MAX_XP_FOR_INTENSITY)
                / (float) MAX_XP_FOR_INTENSITY;
        return 0.6f + 0.4f * progress;
    }
}
