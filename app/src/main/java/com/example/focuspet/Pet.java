package com.example.focuspet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pets")
public class Pet {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String templateId;
    public String name;
    public String species;
    public String rarity;
    public int experience;
    public String imageKey;
    public String appearanceData;
    public String description;
    public long hatchedAt;
    public int mood;
    public int energy;
    public boolean selected;

    public int level() {
        return 1 + experience / 100;
    }
}
