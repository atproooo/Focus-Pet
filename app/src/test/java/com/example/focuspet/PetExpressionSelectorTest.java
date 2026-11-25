package com.example.focuspet;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PetExpressionSelectorTest {
    @Test
    public void preservesBaselineWhenNoLearnedExpressionIsEnabled() {
        assertEquals(PetExpression.HAPPY, PetExpressionSelector.choose(
                PetExpression.HAPPY, Collections.emptySet(),
                PetExpression.CONTEXT_PET, new Random(1)));
    }

    @Test
    public void neutralAmbientStateCanUseAnEnabledLearnedExpression() {
        Set<String> enabled = Collections.singleton(PetExpression.CURIOUS.key);
        assertEquals(PetExpression.CURIOUS, PetExpressionSelector.choose(
                PetExpression.NEUTRAL, enabled,
                PetExpression.CONTEXT_AMBIENT, new Random(1)));
    }

    @Test
    public void ignoresExpressionsThatDoNotMatchTheCurrentContext() {
        Set<String> enabled = Collections.singleton(PetExpression.SLEEPY_FACE.key);
        assertEquals(PetExpression.HAPPY, PetExpressionSelector.choose(
                PetExpression.HAPPY, enabled,
                PetExpression.CONTEXT_PLAY, new Random(1)));
    }

    @Test
    public void everyLearnedExpressionCanBeSelectedInItsSupportedContext() {
        for (PetExpression expression : PetExpression.LEARNABLE) {
            Set<String> enabled = new HashSet<>(Collections.singleton(expression.key));
            PetExpression selected = PetExpressionSelector.choose(
                    PetExpression.NEUTRAL, enabled,
                    PetExpression.CONTEXT_AMBIENT, new Random(3));
            assertEquals(expression, selected);
        }
    }

    @Test
    public void tapExpressionUsesTwentyPercentThreshold() {
        assertEquals(true, PetInteraction.shouldShowTapExpression(0f));
        assertEquals(true, PetInteraction.shouldShowTapExpression(0.199f));
        assertEquals(false, PetInteraction.shouldShowTapExpression(0.2f));
        assertEquals(false, PetInteraction.shouldShowTapExpression(0.999f));
    }
}
