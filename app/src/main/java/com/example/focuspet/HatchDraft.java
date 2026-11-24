package com.example.focuspet;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "hatch_drafts")
public class HatchDraft {
    public static final int STEP_NAME = 0;
    public static final int STEP_APPEARANCE = 1;
    public static final int STEP_PREVIEW = 2;
    public static final int STEP_COMPLETED = 3;
    public static final int MAX_NAME_CODE_POINTS = 20;

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    @NonNull
    @ColumnInfo(defaultValue = "'classic_cat'")
    public String appearanceKey;
    @ColumnInfo(defaultValue = "0")
    public int step;
    @ColumnInfo(defaultValue = "0")
    public long createdAt;
}
