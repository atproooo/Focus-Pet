package com.example.focuspet;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PetBubblePolicyTest {
    @Test
    public void ambientBubblesRequireANonNeutralExpression() {
        PetBubblePolicy policy = new PetBubblePolicy();

        assertFalse(policy.canShowAmbient(PetExpression.NEUTRAL, 1_000L));
        assertTrue(policy.canShowAmbient(PetExpression.HAPPY, 1_000L));
    }

    @Test
    public void anyRecentBubbleStartsTheAmbientCooldown() {
        PetBubblePolicy policy = new PetBubblePolicy();
        policy.recordBubble(1_000L);

        assertFalse(policy.canShowAmbient(PetExpression.HAPPY, 45_999L));
        assertTrue(policy.canShowAmbient(PetExpression.HAPPY, 46_000L));
    }

    @Test
    public void ambientBubblesAreCappedAndTheWindowExpires() {
        PetBubblePolicy policy = new PetBubblePolicy();
        policy.recordAmbientBubble(0L);
        policy.recordAmbientBubble(45_000L);
        policy.recordAmbientBubble(90_000L);

        assertFalse(policy.canShowAmbient(PetExpression.HAPPY, 135_000L));
        assertTrue(policy.canShowAmbient(PetExpression.HAPPY, 300_000L));
    }
}
