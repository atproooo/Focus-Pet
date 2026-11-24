package com.example.focuspet;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PetExpressionTest {
    @Test
    public void interactionsMapToAsciiExpressions() {
        assertSame(PetExpression.HAPPY,
                PetExpression.forAction(PetInteraction.Action.PET));
        assertSame(PetExpression.EXCITED,
                PetExpression.forAction(PetInteraction.Action.PLAY));
        assertSame(PetExpression.SLEEPY,
                PetExpression.forAction(PetInteraction.Action.REST));

        for (PetExpression expression : new PetExpression[]{
                PetExpression.HAPPY, PetExpression.EXCITED,
                PetExpression.SLEEPY, PetExpression.TIRED}) {
            for (int i = 0; i < expression.asciiArt.length(); i++) {
                assertTrue(expression.asciiArt.charAt(i) <= 0x7f);
            }
        }
    }

    @Test
    public void allEightLearnableExpressionsHaveStableKeysAndArtwork() {
        assertEquals(8, PetExpression.LEARNABLE.size());
        Set<String> keys = new HashSet<>();
        for (PetExpression expression : PetExpression.LEARNABLE) {
            assertTrue(expression.learnable);
            assertTrue(keys.add(expression.key));
            assertFalse(expression.asciiArt.trim().isEmpty());
            assertSame(expression, PetExpression.fromKey(expression.key));
        }
    }

    @Test
    public void petStateChoosesAnExpressionAtTheExpectedThresholds() {
        assertSame(PetExpression.TIRED, PetExpression.forState(100, 20));
        assertSame(PetExpression.HAPPY, PetExpression.forState(75, 21));
        assertSame(PetExpression.NEUTRAL, PetExpression.forState(74, 21));
    }
}
