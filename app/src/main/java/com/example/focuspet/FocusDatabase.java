package com.example.focuspet;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FocusSession.class, Pet.class}, version = 4, exportSchema = false)
public abstract class FocusDatabase extends RoomDatabase {
    public abstract FocusSessionDao focusSessionDao();
    public abstract PetDao petDao();

    private static volatile FocusDatabase instance;

    public static FocusDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FocusDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            FocusDatabase.class, "focuspet.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
