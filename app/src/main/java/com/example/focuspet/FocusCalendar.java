package com.example.focuspet;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeSet;

/** Daily goal minutes use actual time; streaks continue to use claimed sessions. */
final class FocusCalendar {
    static final class Summary {
        int todayMinutes;
        int streak;
        int longestStreak;
        LocalDate lastFocusDate;
    }

    static Summary summarize(List<FocusSession> sessions, LocalDate today, ZoneId zone) {
        Summary result = new Summary();
        TreeSet<LocalDate> days = new TreeSet<>();
        for (FocusSession session : sessions) {
            boolean claimed = FocusSession.STATUS_CLAIMED.equals(session.status);
            boolean finished = claimed
                    || FocusSession.STATUS_COMPLETED.equals(session.status)
                    || FocusSession.STATUS_COMPLETED_NO_REWARD.equals(session.status)
                    || FocusSession.STATUS_CANCELLED.equals(session.status);
            if (!finished) continue;
            long timestamp = session.endedAt > 0 ? session.endedAt : session.startedAt;
            if (timestamp <= 0) continue;
            LocalDate day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate();
            if (day.isAfter(today)) continue;
            if (claimed) days.add(day);
            if (today.equals(day)) result.todayMinutes += Math.max(0, session.actualMinutes);
        }
        LocalDate previous = null;
        int run = 0;
        for (LocalDate day : days) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            result.longestStreak = Math.max(result.longestStreak, run);
            previous = day;
        }
        result.lastFocusDate = previous;
        if (previous != null && !previous.isBefore(today.minusDays(1))) result.streak = run;
        return result;
    }
}
