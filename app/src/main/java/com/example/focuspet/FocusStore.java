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
    public int experience() { return preferences.getInt("experience", 0); }
    public int level() { return 1 + experience() / 100; }
    public int streak() { return preferences.getInt("streak", 0); }
    public int longestStreak() { return preferences.getInt("longest_streak", 0); }
    public boolean hasActiveSession() { return preferences.getBoolean("active", false); }
    public long activeEndAt() { return preferences.getLong("active_end_at", 0); }
    public int activeMinutes() { return preferences.getInt("active_minutes", 0); }
    public void saveActiveSession(long endAt, int minutes) {
        preferences.edit().putBoolean("active", true).putLong("active_end_at", endAt)
                .putInt("active_minutes", minutes).apply();
    }
    public void clearActiveSession() {
        preferences.edit().remove("active").remove("active_end_at").remove("active_minutes").apply();
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

    public void reset() { preferences.edit().clear().apply(); }
}
