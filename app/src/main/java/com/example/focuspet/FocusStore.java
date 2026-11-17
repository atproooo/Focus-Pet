package com.example.focuspet;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Offline MVP store; the repository boundary can later be backed by Room. */
public final class FocusStore {
    private final SharedPreferences preferences;
    private final Clock clock;
    private static final Object CALENDAR_LOCK = new Object();

    public FocusStore(Context context) {
        this(context, Clock.systemDefaultZone());
    }

    FocusStore(Context context, Clock clock) {
        preferences = context.getSharedPreferences("focuspet_progress", Context.MODE_PRIVATE);
        this.clock = clock;
    }

    public int dailyGoal() { return preferences.getInt("daily_goal", 60); }
    public int todayMinutes() {
        synchronized (CALENDAR_LOCK) {
            refreshDay();
            return preferences.getInt("today_minutes", 0);
        }
    }
    public int totalMinutes() { return preferences.getInt("total_minutes", 0); }
    public int sessions() { return preferences.getInt("sessions", 0); }
    public int coins() { return preferences.getInt("coins", 0); }
    public int lifeEssence() { return preferences.getInt("life_essence", 0); }
    public int petEggs() { return preferences.getInt("pet_eggs", 0); }
    public int eggMisses() { return preferences.getInt("egg_misses", 0); }
    public int experience() { return preferences.getInt("experience", 0); }
    public int level() { return 1 + experience() / 100; }
    public int streak() {
        String last = preferences.getString("last_focus_date", "");
        if (last.isEmpty()) return 0;
        LocalDate day = LocalDate.parse(last);
        LocalDate today = LocalDate.now(clock);
        return !day.isAfter(today) && !day.isBefore(today.minusDays(1))
                ? preferences.getInt("streak", 0) : 0;
    }
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
        addClaimedRewards(minutes, minutes, 0, false);
    }

    public void addClaimedRewards(int xp, int coins, int essence, boolean egg) {
        synchronized (CALENDAR_LOCK) {
            refreshDay();
            String today = LocalDate.now(clock).toString();
            String last = preferences.getString("last_focus_date", "");
            int nextStreak = today.equals(last) ? streak()
                    : LocalDate.now(clock).minusDays(1).toString().equals(last) ? streak() + 1 : 1;
            rewardEditor(xp, coins, essence, egg)
                    .putInt("today_minutes", todayMinutes() + xp)
                    .putString("last_focus_date", today).putInt("streak", nextStreak)
                    .putInt("longest_streak", Math.max(longestStreak(), nextStreak)).apply();
        }
    }

    /** Rebuild dates from claimed history so delayed and out-of-order claims stay consistent. */
    public void addClaimedRewards(int xp, int coins, int essence, boolean egg,
                                  List<FocusSession> claimedHistory) {
        synchronized (CALENDAR_LOCK) {
            writeCalendar(rewardEditor(xp, coins, essence, egg), claimedHistory).apply();
        }
    }

    private SharedPreferences.Editor rewardEditor(int xp, int coins, int essence, boolean egg) {
        return preferences.edit().putInt("coins", coins() + coins)
                .putInt("experience", experience() + xp)
                .putInt("life_essence", lifeEssence() + essence)
                .putInt("pet_eggs", petEggs() + (egg ? 1 : 0))
                .putInt("total_minutes", totalMinutes() + xp)
                .putInt("sessions", sessions() + 1);
    }

    boolean needsCalendarInitialization() {
        return !clock.getZone().getId().equals(preferences.getString("calendar_zone", ""));
    }

    void initializeCalendarProgress(List<FocusSession> claimedHistory) {
        synchronized (CALENDAR_LOCK) {
            if (needsCalendarInitialization()) writeCalendar(preferences.edit(), claimedHistory).apply();
        }
    }

    private SharedPreferences.Editor writeCalendar(SharedPreferences.Editor editor,
                                                   List<FocusSession> claimedHistory) {
        LocalDate today = LocalDate.now(clock);
        FocusCalendar.Summary summary = FocusCalendar.summarize(claimedHistory, today, clock.getZone());
        return editor.putString("calendar_zone", clock.getZone().getId())
                .putString("progress_date", today.toString())
                .putString("last_focus_date", summary.lastFocusDate == null ? "" : summary.lastFocusDate.toString())
                .putInt("today_minutes", summary.todayMinutes).putInt("streak", summary.streak)
                .putInt("longest_streak", Math.max(longestStreak(), summary.longestStreak));
    }

    private void refreshDay() {
        String today = LocalDate.now(clock).toString();
        if (!today.equals(preferences.getString("progress_date", ""))) {
            preferences.edit().putString("progress_date", today).putInt("today_minutes", 0).apply();
        }
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
