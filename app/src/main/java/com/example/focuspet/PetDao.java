package com.example.focuspet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PetDao {
    @Insert
    long insert(Pet pet);

    @Query("DELETE FROM pets")
    void deleteAll();

    @Query("SELECT * FROM pets ORDER BY id ASC")
    List<Pet> all();

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    Pet byId(long id);

    @Query("SELECT * FROM pets WHERE templateId = :templateId LIMIT 1")
    Pet byTemplateId(String templateId);

    @Query("UPDATE pets SET name = :name WHERE id = :id AND name = :oldName")
    int updateNameIfMatches(long id, String oldName, String name);

    @Query("UPDATE pets SET imageKey = :imageKey, appearanceData = :appearanceData WHERE id = :id AND appearanceData = :oldAppearance")
    int updateAppearanceIfMatches(long id, String oldAppearance, String imageKey, String appearanceData);

    @Query("UPDATE pets SET appearanceData = :lookKey WHERE id = :id")
    int updateAppearance(long id, String lookKey);

    @Query("SELECT * FROM pets WHERE hatchRequestId = :requestId LIMIT 1")
    Pet byHatchRequest(String requestId);

    @Query("UPDATE pets SET mood = :mood, energy = :energy, lastInteractionAt = :at WHERE id = :id")
    void updateInteraction(long id, int mood, int energy, long at);

    @Query("SELECT * FROM pets WHERE selected = 1 LIMIT 1")
    Pet selected();

    @Query("SELECT COUNT(*) FROM pets")
    int count();

    @Query("UPDATE pets SET selected = 0")
    void clearSelection();

    @Query("UPDATE pets SET selected = 1 WHERE id = :id")
    void select(long id);

    @Query("UPDATE pets SET experience = experience + :xp WHERE id = :id")
    void addExperience(long id, int xp);
}
