package com.example.focuspet;

import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class FocusCalendarTest {
    private FocusSession claimed(String endedAt, int xp) {
        FocusSession s=new FocusSession();s.status=FocusSession.STATUS_CLAIMED;
        s.endedAt=Instant.parse(endedAt).toEpochMilli();s.xpReward=xp;
        s.actualMinutes=xp;return s;
    }
    @Test public void dailyProgressUsesActualMinutesOnCompletionDate() {
        FocusSession old=claimed("2026-11-16T12:00:00Z",30);
        FocusSession early=claimed("2026-11-17T12:00:00Z",3);
        early.actualMinutes=16;
        FocusCalendar.Summary s=FocusCalendar.summarize(Arrays.asList(old,early),LocalDate.of(2026,11,17),ZoneOffset.UTC);
        assertEquals(16,s.todayMinutes);assertEquals(2,s.streak);assertEquals(2,s.longestStreak);
    }
    @Test public void sameDayDoesNotDoubleCountStreakAndGapsResetIt() {
        List<FocusSession> sessions=Arrays.asList(claimed("2026-11-13T12:00:00Z",1),claimed("2026-11-14T12:00:00Z",1),claimed("2026-11-17T12:00:00Z",1),claimed("2026-11-17T14:00:00Z",1));
        FocusCalendar.Summary s=FocusCalendar.summarize(sessions,LocalDate.of(2026,11,17),ZoneOffset.UTC);
        assertEquals(1,s.streak);assertEquals(2,s.longestStreak);assertEquals(2,s.todayMinutes);
    }
    @Test public void streakSurvivesYesterdayButExpiresAfterMissingDay() {
        List<FocusSession> sessions=Arrays.asList(claimed("2026-11-15T12:00:00Z",1),claimed("2026-11-16T12:00:00Z",1));
        assertEquals(2,FocusCalendar.summarize(sessions,LocalDate.of(2026,11,17),ZoneOffset.UTC).streak);
        FocusCalendar.Summary s=FocusCalendar.summarize(sessions,LocalDate.of(2026,11,18),ZoneOffset.UTC);
        assertEquals(0,s.streak);assertEquals(2,s.longestStreak);
    }
    @Test public void localMidnightAndYearBoundaryAreCalendarBased() {
        List<FocusSession> sessions=Arrays.asList(claimed("2026-12-31T15:00:00Z",3),claimed("2026-12-31T16:00:00Z",4));
        FocusCalendar.Summary s=FocusCalendar.summarize(sessions,LocalDate.of(2027,1,1),ZoneId.of("Asia/Shanghai"));
        assertEquals(4,s.todayMinutes);assertEquals(2,s.streak);
    }
    @Test public void daylightSavingTransitionStillCountsConsecutiveDays() {
        List<FocusSession> sessions=Arrays.asList(claimed("2026-03-07T17:00:00Z",1),claimed("2026-03-08T16:00:00Z",1));
        assertEquals(2,FocusCalendar.summarize(sessions,LocalDate.of(2026,3,8),ZoneId.of("America/New_York")).streak);
    }
    @Test public void allFinishedSessionsCountActualMinutesButOnlyClaimsCountForStreak() {
        FocusSession pending=claimed("2026-11-17T12:00:00Z",99);pending.status=FocusSession.STATUS_COMPLETED;
        pending.actualMinutes=4;
        FocusSession noReward=claimed("2026-11-17T13:00:00Z",0);
        noReward.status=FocusSession.STATUS_COMPLETED_NO_REWARD;
        noReward.actualMinutes=5;
        FocusSession cancelled=claimed("2026-11-17T14:00:00Z",99);
        cancelled.status=FocusSession.STATUS_CANCELLED;cancelled.actualMinutes=2;
        FocusSession running=claimed("2026-11-17T15:00:00Z",99);
        running.status=FocusSession.STATUS_RUNNING;running.actualMinutes=20;
        FocusSession future=claimed("2026-11-18T12:00:00Z",99);
        FocusCalendar.Summary s=FocusCalendar.summarize(
                Arrays.asList(pending,noReward,cancelled,running,future),
                LocalDate.of(2026,11,17),ZoneOffset.UTC);
        assertEquals(11,s.todayMinutes);assertEquals(0,s.streak);
    }

    @Test public void lateClaimCanFillAGapRegardlessOfHistoryOrder() {
        List<FocusSession> sessions=new ArrayList<>(Arrays.asList(claimed("2026-11-17T12:00:00Z",3),claimed("2026-11-15T12:00:00Z",1)));
        assertEquals(1,FocusCalendar.summarize(sessions,LocalDate.of(2026,11,17),ZoneOffset.UTC).streak);
        sessions.add(claimed("2026-11-16T12:00:00Z",1));
        FocusCalendar.Summary s=FocusCalendar.summarize(sessions,LocalDate.of(2026,11,17),ZoneOffset.UTC);
        assertEquals(3,s.streak);assertEquals(3,s.longestStreak);assertEquals(3,s.todayMinutes);
    }
}
