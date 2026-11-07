package com.example.focuspet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FocusSessionDao {
    @Insert
    long insert(FocusSession session);

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    FocusSession findById(long id);

    @Query("UPDATE focus_sessions SET status = :status, endedAt = :endedAt, actualMinutes = :actualMinutes, xpReward = :xp, coinReward = :coins, essenceReward = :essence, eggReward = :egg, endedEarly = :endedEarly WHERE id = :id")
    void finish(long id, String status, long endedAt, int actualMinutes, int xp, int coins, int essence, boolean egg, boolean endedEarly);

    @Query("UPDATE focus_sessions SET status = :status, endedAt = :endedAt, actualMinutes = :actualMinutes, rewardGranted = 0 WHERE id = :id")
    void cancel(long id, String status, long endedAt, int actualMinutes);

    @Query("UPDATE focus_sessions SET rewardGranted = 1 WHERE id = :id AND rewardGranted = 0")
    int markRewardGranted(long id);

    @Query("UPDATE focus_sessions SET rewardGranted = 1, status = 'CLAIMED' WHERE id = :id AND rewardGranted = 0")
    int claim(long id);

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    List<FocusSession> history();

    @Query("SELECT * FROM focus_sessions WHERE status = 'COMPLETED_UNCLAIMED' ORDER BY endedAt DESC LIMIT 1")
    FocusSession latestPending();
}
