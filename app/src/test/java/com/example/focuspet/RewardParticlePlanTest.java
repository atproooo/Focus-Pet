package com.example.focuspet;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RewardParticlePlanTest {
    @Test
    public void createsParticlesFromActualRewardAmounts() {
        List<RewardParticlePlan.Particle> particles =
                RewardParticlePlan.create(16, 30, 4, true);

        assertEquals(1, count(particles, RewardParticlePlan.Kind.XP));
        assertEquals(24, count(particles, RewardParticlePlan.Kind.COIN));
        assertEquals(4, count(particles, RewardParticlePlan.Kind.ESSENCE));
        assertEquals(1, count(particles, RewardParticlePlan.Kind.EGG));
        assertEquals(16, first(particles, RewardParticlePlan.Kind.XP).amount);
    }

    @Test
    public void omitsParticlesForZeroRewardsAndMissingEggs() {
        assertTrue(RewardParticlePlan.create(0, 0, 0, false).isEmpty());
        assertEquals(0, count(
                RewardParticlePlan.create(10, 2, 1, false), RewardParticlePlan.Kind.EGG));
    }

    @Test
    public void xpRewardIncreasesParticleIntensity() {
        float small = first(RewardParticlePlan.create(5, 0, 0, false),
                RewardParticlePlan.Kind.XP).intensity;
        float large = first(RewardParticlePlan.create(200, 0, 0, false),
                RewardParticlePlan.Kind.XP).intensity;

        assertTrue(large > small);
        assertFalse(large > 1f);
    }

    private int count(List<RewardParticlePlan.Particle> particles,
                      RewardParticlePlan.Kind kind) {
        int count = 0;
        for (RewardParticlePlan.Particle particle : particles) {
            if (particle.kind == kind) count++;
        }
        return count;
    }

    private RewardParticlePlan.Particle first(List<RewardParticlePlan.Particle> particles,
                                              RewardParticlePlan.Kind kind) {
        for (RewardParticlePlan.Particle particle : particles) {
            if (particle.kind == kind) return particle;
        }
        throw new AssertionError("No particle of kind " + kind);
    }
}
