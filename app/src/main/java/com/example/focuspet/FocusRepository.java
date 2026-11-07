package com.example.focuspet;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Owns session history and completion rewards. Methods are called from the service worker.
 */
public final class FocusRepository {
    private final FocusSessionDao sessions;
    private final FocusStore progress;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public FocusRepository(Context context) {
        sessions = FocusDatabase.getInstance(context).focusSessionDao();
        progress = new FocusStore(context);
    }

    public long startSession(int plannedMinutes, String label) {
        try {
            Future<Long> result = databaseExecutor.submit(() -> {
                FocusSession session = new FocusSession();
                session.startedAt = System.currentTimeMillis();
                session.plannedMinutes = plannedMinutes;
                session.label = label == null ? "" : label;
                session.status = FocusSession.STATUS_RUNNING;
                session.rewardGranted = false;
                return sessions.insert(session);
            });
            return result.get();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create focus session", error);
        }
    }

    public void completeSession(long id, int actualMinutes) {
        run(() -> {
            FocusSession session = sessions.findById(id);
            if (session == null || !FocusSession.STATUS_RUNNING.equals(session.status)) return null;
            int baseEssence = Math.max(1, Math.min(5, Math.round(actualMinutes * 0.08f)));
            int essence = Math.max(1, Math.min(5, baseEssence + random.nextInt(3) - 1));
            int coins = actualMinutes + random.nextInt(4);
            int streak = progress.streak();
            int eggChance = Math.min(100, eggChance(actualMinutes) + Math.min(3, streak / 4));
            int misses = progress.eggMisses() + 1;
            boolean egg = misses >= 10 || random.nextInt(100) < eggChance;
            progress.setEggMisses(egg ? 0 : misses);
            sessions.finish(id, FocusSession.STATUS_COMPLETED, System.currentTimeMillis(), actualMinutes,
                    actualMinutes, coins, essence, egg);
            return null;
        });
    }

    private final java.util.Random random = new java.util.Random();

    private int eggChance(int minutes) {
        if (minutes >= 60) return 6;
        if (minutes >= 45) return 4;
        if (minutes >= 25) return 2;
        return 1;
    }

    public FocusSession pendingSession(long id) {
        return run(() -> sessions.findById(id));
    }

    public boolean claimRewards(long id) {
        return run(() -> {
            FocusSession session = sessions.findById(id);
            if (session == null || !FocusSession.STATUS_COMPLETED.equals(session.status)
                    || sessions.claim(id) != 1) return false;
            progress.addClaimedRewards(session.xpReward, session.coinReward,
                    session.essenceReward, session.eggReward);
            return true;
        });
    }

    public void cancelSession(long id, int actualMinutes) {
        if (id > 0) {
            run(() -> {
                sessions.cancel(id, FocusSession.STATUS_CANCELLED, System.currentTimeMillis(), actualMinutes);
                return null;
            });
        }
    }

    private <T> T run(java.util.concurrent.Callable<T> work) {
        try {
            return databaseExecutor.submit(work).get();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to update focus session", error);
        }
    }

    public List<FocusSession> history() {
        return run(sessions::history);
    }

    public FocusSession latestPending() {
        return run(sessions::latestPending);
    }
}
