package com.example.focuspet;

import android.content.Context;
import android.content.SharedPreferences;

/** Offline MVP store; the repository boundary can later be backed by Room. */
public final class FocusStore {
    private final SharedPreferences preferences;

    public FocusStore(Context context) {
        preferences = context.getSharedPreferences("focuspet_progress", Context.MODE_PRIVATE);
    }

    public int dailyGoal() { return preferences.getInt("daily_goal", 60); }
    public int todayMinutes() { return preferences.getInt("today_minutes", 0); }
    public int totalMinutes() { return preferences.getInt("total_minutes", 0); }
    public int sessions() { return preferences.getInt("sessions", 0); }
    public int coins() { return preferences.getInt("coins", 0); }
    public int lifeEssence() { return preferences.getInt("life_essence", 0); }
    public int petEggs() { return preferences.getInt("pet_eggs", 0); }
    public int eggMisses() { return preferences.getInt("egg_misses", 0); }
    public int experience() { return preferences.getInt("experience", 0); }
    public int level() { return 1 + experience() / 100; }
    public int streak() { return preferences.getInt("streak", 0); }
    public int longestStreak() { return preferences.getInt("longest_streak", 0); }
    public boolean hasActiveSession() { return preferences.getBoolean("active", false); }
    public long activeEndAt() { return preferences.getLong("active_end_at", 0); }
    public int activeMinutes() { return preferences.getInt("active_minutes", 0); }
    public long activeSessionId() { return preferences.getLong("active_session_id", 0); }
    public boolean activePaused() { return preferences.getBoolean("active_paused", false); }
    public long activeRemaining() { return preferences.getLong("active_remaining", 0); }
    public void saveActiveSession(long endAt, int minutes) {
        saveActiveSessionState(endAt, minutes, false, minutes * 60_000L);
    }
    public void saveActiveSessionState(long endAt, int minutes, boolean paused, long remaining) {
        preferences.edit().putBoolean("active", true).putLong("active_end_at", endAt)
                .putInt("active_minutes", minutes).putBoolean("active_paused", paused)
                .putLong("active_remaining", remaining).apply();
    }
    public void setActiveSessionId(long id) {
        preferences.edit().putLong("active_session_id", id).apply();
    }
    public void clearActiveSession() {
        preferences.edit().remove("active").remove("active_end_at").remove("active_minutes")
                .remove("active_paused").remove("active_remaining").remove("active_session_id").apply();
    }
    public void setDailyGoal(int value) {
        preferences.edit().putInt("daily_goal", Math.max(15, Math.min(240, value))).apply();
    }

    public void recordCompleted(int minutes) {
        int nextStreak = Math.max(1, streak());
        preferences.edit().putInt("today_minutes", todayMinutes() + minutes)
                .putInt("total_minutes", totalMinutes() + minutes)
                .putInt("sessions", sessions() + 1).putInt("coins", coins() + minutes)
                .putInt("experience", experience() + minutes).putInt("streak", nextStreak)
                .putInt("longest_streak", Math.max(longestStreak(), nextStreak)).apply();
    }

    public void addClaimedRewards(int xp, int coins, int essence, boolean egg) {
        preferences.edit().putInt("coins", coins() + coins)
                .putInt("experience", experience() + xp)
                .putInt("life_essence", lifeEssence() + essence)
                .putInt("pet_eggs", petEggs() + (egg ? 1 : 0))
                .putInt("today_minutes", todayMinutes() + xp)
                .putInt("total_minutes", totalMinutes() + xp)
                .putInt("sessions", sessions() + 1)
                .putInt("streak", Math.max(1, streak()))
                .putInt("longest_streak", Math.max(longestStreak(), Math.max(1, streak())))
                .apply();
    }

    public void setEggMisses(int misses) {
        preferences.edit().putInt("egg_misses", misses).apply();
    }

    public boolean consumeHatchingResources() {
        if (petEggs() < 1 || lifeEssence() < 100) return false;
        preferences.edit().putInt("pet_eggs", petEggs() - 1)
                .putInt("life_essence", lifeEssence() - 100).apply();
        return true;
    }

    public void addDebugResources(int coins, int essence, int eggs) {
        preferences.edit().putInt("coins", coins() + Math.max(0, coins))
                .putInt("life_essence", lifeEssence() + Math.max(0, essence))
                .putInt("pet_eggs", petEggs() + Math.max(0, eggs)).apply();
    }

    public void reset() { preferences.edit().clear().apply(); }
}
