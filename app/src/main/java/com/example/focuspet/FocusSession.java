package com.example.focuspet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED_UNCLAIMED";
    public static final String STATUS_CLAIMED = "CLAIMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startedAt;
    public long endedAt;
    public int plannedMinutes;
    public int actualMinutes;
    public String label;
    public String status;
    public boolean rewardGranted;
    public int xpReward;
    public int coinReward;
    public int essenceReward;
    public boolean eggReward;
}
