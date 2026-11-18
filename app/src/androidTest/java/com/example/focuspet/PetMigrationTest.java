package com.example.focuspet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PetMigrationTest {
    @Test public void versionFourPetsAndHistorySurviveUpgrade() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="migration-"+UUID.randomUUID()+".db";
        FocusDatabase migrated=null;
        try {
            SQLiteDatabase old=c.openOrCreateDatabase(name,0,null);
            old.execSQL("CREATE TABLE focus_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, plannedMinutes INTEGER NOT NULL, actualMinutes INTEGER NOT NULL, label TEXT, status TEXT, rewardGranted INTEGER NOT NULL, endedEarly INTEGER NOT NULL, xpReward INTEGER NOT NULL, coinReward INTEGER NOT NULL, essenceReward INTEGER NOT NULL, eggReward INTEGER NOT NULL)");
            old.execSQL("CREATE TABLE pets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, templateId TEXT, name TEXT, species TEXT, rarity TEXT, experience INTEGER NOT NULL, imageKey TEXT, appearanceData TEXT, description TEXT, hatchedAt INTEGER NOT NULL, mood INTEGER NOT NULL, energy INTEGER NOT NULL, selected INTEGER NOT NULL)");
            old.execSQL("INSERT INTO pets VALUES (7,'starter','Legacy','Fox','Common',80,'fox','default','Saved pet',1,70,60,1)");
            old.execSQL("INSERT INTO focus_sessions VALUES (9,1,2,25,25,'Legacy session','CLAIMED',1,0,25,25,2,0)");
            old.setVersion(4);old.close();
            migrated=Room.databaseBuilder(c,FocusDatabase.class,name).addMigrations(FocusDatabase.MIGRATION_4_5).build();
            Pet pet=migrated.petDao().byId(7);assertNotNull(pet);assertEquals("Legacy",pet.name);assertEquals(80,pet.experience);assertEquals(70,pet.mood);assertTrue(pet.selected);assertNull(pet.hatchRequestId);assertEquals(0,pet.lastInteractionAt);
            assertEquals("Legacy session",migrated.focusSessionDao().findById(9).label);
            migrated.petDao().updateInteraction(7,75,55,100);assertEquals(100,migrated.petDao().byId(7).lastInteractionAt);
        } finally {if(migrated!=null)migrated.close();c.deleteDatabase(name);}
    }
}
