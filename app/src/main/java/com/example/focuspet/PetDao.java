package com.example.focuspet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PetDao {
    @Insert
    long insert(Pet pet);

    @Query("SELECT * FROM pets ORDER BY id ASC")
    List<Pet> all();

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    Pet byId(long id);

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
