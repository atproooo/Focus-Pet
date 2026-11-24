package com.example.focuspet;

import androidx.annotation.StringRes;

enum PetExpression {
    NEUTRAL("", 0),
    HAPPY("(^_^)", R.string.pet_expression_happy),
    EXCITED("(^o^)", R.string.pet_expression_excited),
    SLEEPY("(-_-) zzz", R.string.pet_expression_sleepy),
    TIRED("(>_<)", R.string.pet_expression_tired);

    static final int LOW_ENERGY_THRESHOLD = 20;
    static final int HAPPY_MOOD_THRESHOLD = 75;

    final String asciiArt;
    @StringRes final int descriptionRes;

    PetExpression(String asciiArt, @StringRes int descriptionRes) {
        this.asciiArt = asciiArt;
        this.descriptionRes = descriptionRes;
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
