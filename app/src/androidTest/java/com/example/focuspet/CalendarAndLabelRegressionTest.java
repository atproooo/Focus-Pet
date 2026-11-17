package com.example.focuspet;

import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CalendarAndLabelRegressionTest {
    Context context; SharedPreferences prefs; Map<String,?> backup;
    @Before public void prepare() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs=context.getSharedPreferences("focuspet_progress",0);backup=new HashMap<>(prefs.getAll());prefs.edit().clear().commit();
    }
    @After public void restore() {
        SharedPreferences.Editor e=prefs.edit().clear();
        for(Map.Entry<String,?> p:backup.entrySet()) {
            Object v=p.getValue();String k=p.getKey();
            if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Float)e.putFloat(k,(Float)v);
        }e.commit();
    }
    @Test public void midnightClearsTodayAndConsecutiveDaysIncrementOnce() {
        Clock day1=Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"),ZoneOffset.UTC);
        FocusStore first=new FocusStore(context,day1);first.addClaimedRewards(3,3,0,false);first.addClaimedRewards(2,2,0,false);
        assertEquals(5,first.todayMinutes());assertEquals(1,first.streak());
        FocusStore next=new FocusStore(context,Clock.offset(day1,Duration.ofDays(1)));
        assertEquals(0,next.todayMinutes());assertEquals(1,next.streak());
        next.addClaimedRewards(3,3,0,false);assertEquals(3,next.todayMinutes());assertEquals(2,next.streak());assertEquals(2,next.longestStreak());assertEquals(8,next.totalMinutes());
        FocusStore gap=new FocusStore(context,Clock.offset(day1,Duration.ofDays(3)));
        assertEquals(0,gap.todayMinutes());assertEquals(0,gap.streak());gap.addClaimedRewards(1,1,0,false);assertEquals(1,gap.streak());assertEquals(2,gap.longestStreak());
    }
    @Test public void legacyHistoryMigrationPreservesBalancesAndTotals() {
        prefs.edit().putInt("coins",42).putInt("total_minutes",60).putInt("today_minutes",60).commit();
        FocusSession s=new FocusSession();s.status=FocusSession.STATUS_CLAIMED;s.xpReward=30;s.endedAt=Instant.parse("2026-11-16T12:00:00Z").toEpochMilli();
        FocusStore store=new FocusStore(context,Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"),ZoneOffset.UTC));
        store.initializeCalendarProgress(Collections.singletonList(s));assertEquals(0,store.todayMinutes());assertEquals(1,store.streak());assertEquals(42,store.coins());assertEquals(60,store.totalMinutes());
        store.initializeCalendarProgress(Collections.emptyList());assertEquals(1,store.streak());
    }
    @Test public void delayedClaimUsesCompletionDayAndDiscountedXp() {
        FocusSession old=new FocusSession();old.status=FocusSession.STATUS_CLAIMED;old.xpReward=3;old.endedAt=Instant.parse("2026-11-16T12:00:00Z").toEpochMilli();
        FocusStore store=new FocusStore(context,Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"),ZoneOffset.UTC));
        store.addClaimedRewards(3,3,0,false,Collections.singletonList(old));assertEquals(0,store.todayMinutes());assertEquals(3,store.totalMinutes());assertEquals(1,store.streak());
    }
    @Test public void unicodeLabelsKeepCompleteGraphemes() {
        assertEquals("short",SessionLabels.abbreviate("short",20));
        assertEquals("ABCDEFGHIJKLMNOPQRST...",SessionLabels.abbreviate("ABCDEFGHIJKLMNOPQRSTUVWXYZ",20));
        String prefix="1234567890123456789";
        for(String emoji:new String[]{"\uD83D\uDE00","\uD83D\uDC69\u200D\uD83D\uDCBB","e\u0301","\uD83C\uDDE8\uD83C\uDDF3"}) {
            assertEquals(prefix+emoji+"...",SessionLabels.abbreviate(prefix+emoji+"tail",20));
        }
    }
}
