package com.example.focuspet;

import org.junit.Test;

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

        for (PetExpression expression : PetExpression.values()) {
            for (int i = 0; i < expression.asciiArt.length(); i++) {
                assertTrue(expression.asciiArt.charAt(i) <= 0x7f);
            }
        }
    }

    @Test
    public void petStateChoosesAnExpressionAtTheExpectedThresholds() {
        assertSame(PetExpression.TIRED, PetExpression.forState(100, 20));
        assertSame(PetExpression.HAPPY, PetExpression.forState(75, 21));
        assertSame(PetExpression.NEUTRAL, PetExpression.forState(74, 21));
    }
}
