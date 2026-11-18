package com.example.focuspet;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {FocusSession.class, Pet.class}, version = 5, exportSchema = false)
public abstract class FocusDatabase extends RoomDatabase {
    public abstract FocusSessionDao focusSessionDao();
    public abstract PetDao petDao();

    private static volatile FocusDatabase instance;
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE pets ADD COLUMN hatchRequestId TEXT");
            db.execSQL("ALTER TABLE pets ADD COLUMN lastInteractionAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX index_pets_hatchRequestId ON pets (hatchRequestId)");
        }
    };

    public static FocusDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FocusDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            FocusDatabase.class, "focuspet.db")
                            .addMigrations(MIGRATION_4_5)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
