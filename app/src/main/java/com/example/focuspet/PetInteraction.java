package com.example.focuspet;

/** Free interactions affect pet state only; they never grant focus time or currency. */
public final class PetInteraction {
    public enum Action { PET, PLAY, REST }
    public static final long COOLDOWN_MILLIS = 10_000L;
    static final float TAP_EXPRESSION_CHANCE = 0.20f;

    public static int moodAfter(int mood, Action action) {
        return Math.max(0, Math.min(100, mood + (action == Action.PET ? 5 : action == Action.PLAY ? 10 : 0)));
    }
    public static int energyAfter(int energy, Action action) {
        return Math.max(0, Math.min(100, energy + (action == Action.PLAY ? -5 : action == Action.REST ? 10 : 0)));
    }
    static boolean shouldShowTapExpression(float randomValue) {
        return randomValue >= 0f && randomValue < TAP_EXPRESSION_CHANCE;
    }
    private PetInteraction() { }
}
