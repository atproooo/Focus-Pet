package com.example.focuspet;

import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class PetExpressionContext {
    static final int AMBIENT = 1;
    static final int PET = 1 << 1;
    static final int PLAY = 1 << 2;
    static final int REST = 1 << 3;

    private PetExpressionContext() { }
}

enum PetExpression {
    NEUTRAL("neutral", "", 0, 0, false, 0),
    HAPPY("happy", "(^_^)", R.string.pet_expression_happy, 0, false, 0),
    EXCITED("excited", "(^o^)", R.string.pet_expression_excited, 0, false, 0),
    SLEEPY("sleepy", "(-_-) zzz", R.string.pet_expression_sleepy, 0, false, 0),
    TIRED("tired", "(>_<)", R.string.pet_expression_tired, 0, false, 0),
    CURIOUS("a_curious", "( .  , .)", R.string.pet_expression_curious,
            R.string.pet_expression_name_curious, true, PetExpressionContext.AMBIENT),
    SURPRISED("b_surprised", "(> . <)", R.string.pet_expression_surprised,
            R.string.pet_expression_name_surprised, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.PLAY),
    CALM_CAT("c_calm_cat", "/\\_/\\\n(='_' )", R.string.pet_expression_calm_cat,
            R.string.pet_expression_name_calm_cat, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.REST),
    HAPPY_FACE("d_happy", "꒰ˊᗜˋ* ꒱", R.string.pet_expression_happy_face,
            R.string.pet_expression_name_happy_face, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.PET),
    EXCITED_FACE("e_excited", "(>⩊<)", R.string.pet_expression_excited_face,
            R.string.pet_expression_name_excited_face, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.PLAY),
    SLEEPY_FACE("f_sleepy", "꒰っ˕ -｡꒱", R.string.pet_expression_sleepy_face,
            R.string.pet_expression_name_sleepy_face, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.REST),
    SAD_FACE("g_sad", "꒰ ◞ ‸ ◟ ꒱", R.string.pet_expression_sad_face,
            R.string.pet_expression_name_sad_face, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.REST),
    POUT("h_pout", "૮꒰⩌’︿’⩌｡꒱ა", R.string.pet_expression_pout,
            R.string.pet_expression_name_pout, true,
            PetExpressionContext.AMBIENT | PetExpressionContext.PET | PetExpressionContext.PLAY);

    static final int LOW_ENERGY_THRESHOLD = 20;
    static final int HAPPY_MOOD_THRESHOLD = 75;
    static final int CONTEXT_AMBIENT = PetExpressionContext.AMBIENT;
    static final int CONTEXT_PET = PetExpressionContext.PET;
    static final int CONTEXT_PLAY = PetExpressionContext.PLAY;
    static final int CONTEXT_REST = PetExpressionContext.REST;
    static final List<PetExpression> LEARNABLE = Collections.unmodifiableList(Arrays.asList(
            CURIOUS, SURPRISED, CALM_CAT, HAPPY_FACE, EXCITED_FACE, SLEEPY_FACE, SAD_FACE, POUT));

    final String key;
    final String asciiArt;
    @StringRes final int descriptionRes;
    @StringRes final int nameRes;
    final boolean learnable;
    private final int contexts;

    PetExpression(String key, String asciiArt, @StringRes int descriptionRes,
                  @StringRes int nameRes, boolean learnable, int contexts) {
        this.key = key;
        this.asciiArt = asciiArt;
        this.descriptionRes = descriptionRes;
        this.nameRes = nameRes;
        this.learnable = learnable;
        this.contexts = contexts;
    }

    boolean supportsContext(int context) {
        return (contexts & context) != 0;
    }

    static PetExpression fromKey(String key) {
        if (key == null) return null;
        for (PetExpression expression : values()) {
            if (expression.key.equals(key)) return expression;
        }
        return null;
    }

    static int contextForAction(PetInteraction.Action action) {
        switch (action) {
            case PET: return CONTEXT_PET;
            case PLAY: return CONTEXT_PLAY;
            case REST: return CONTEXT_REST;
            default: throw new IllegalArgumentException("Unsupported pet action: " + action);
        }
    }

    static PetExpression forState(int mood, int energy) {
        if (energy <= LOW_ENERGY_THRESHOLD) return TIRED;
        if (mood >= HAPPY_MOOD_THRESHOLD) return HAPPY;
        return NEUTRAL;
    }

    static PetExpression forAction(PetInteraction.Action action) {
        switch (action) {
            case PET: return HAPPY;
            case PLAY: return EXCITED;
            case REST: return SLEEPY;
            default: throw new IllegalArgumentException("Unsupported pet action: " + action);
        }
    }
}
