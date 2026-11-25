package com.example.focuspet;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {FocusSession.class, Pet.class, HatchDraft.class, ShopProductEntity.class},
        version = 8, exportSchema = false)
public abstract class FocusDatabase extends RoomDatabase {
    public abstract FocusSessionDao focusSessionDao();
    public abstract PetDao petDao();
    public abstract HatchDraftDao hatchDraftDao();
    public abstract ShopProductDao shopProductDao();

    private static volatile FocusDatabase instance;
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE pets ADD COLUMN hatchRequestId TEXT");
            db.execSQL("ALTER TABLE pets ADD COLUMN lastInteractionAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX index_pets_hatchRequestId ON pets (hatchRequestId)");
        }
    };
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS hatch_drafts ("
                    + "id TEXT NOT NULL, "
                    + "name TEXT, "
                    + "appearanceKey TEXT NOT NULL DEFAULT 'classic_cat', "
                    + "step INTEGER NOT NULL DEFAULT 0, "
                    + "createdAt INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(id))");
        }
    };
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS shop_products ("
                    + "id TEXT NOT NULL, "
                    + "name TEXT NOT NULL, "
                    + "categoryKey TEXT NOT NULL, "
                    + "productType TEXT NOT NULL, "
                    + "itemKey TEXT NOT NULL, "
                    + "description TEXT NOT NULL, "
                    + "currency TEXT NOT NULL, "
                    + "price INTEGER NOT NULL, "
                    + "quantityGranted INTEGER NOT NULL, "
                    + "available INTEGER NOT NULL, "
                    + "displayOrder INTEGER NOT NULL, "
                    + "PRIMARY KEY(id))");
        }
    };
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN petId INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN petName TEXT");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN petSpecies TEXT");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN petImageKey TEXT");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN petAppearanceData TEXT");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN accessoryKey TEXT");
            db.execSQL("ALTER TABLE focus_sessions ADD COLUMN backgroundKey TEXT NOT NULL DEFAULT 'plain'");
        }
    };

    public static FocusDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FocusDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            FocusDatabase.class, "focuspet.db")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                                    MIGRATION_7_8)
                            .build();
                }
            }
        }
        return instance;
    }
}
