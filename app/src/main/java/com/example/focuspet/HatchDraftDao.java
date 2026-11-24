package com.example.focuspet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HatchDraftDao {
    @Insert
    void insert(HatchDraft draft);

    @Query("SELECT * FROM hatch_drafts WHERE id = :id LIMIT 1")
    HatchDraft byId(String id);

    @Query("SELECT * FROM hatch_drafts ORDER BY createdAt ASC")
    List<HatchDraft> all();

    @Query("SELECT * FROM hatch_drafts WHERE step != 3 ORDER BY createdAt DESC LIMIT 1")
    HatchDraft active();

    @Query("UPDATE hatch_drafts SET name = :name WHERE id = :id AND step = 0")
    int updateName(String id, String name);

    @Query("UPDATE hatch_drafts SET name = :name, step = 1 WHERE id = :id AND step = 0")
    int saveNameAndAdvance(String id, String name);

    @Query("UPDATE hatch_drafts SET appearanceKey = :appearanceKey WHERE id = :id AND step = 1")
    int updateAppearance(String id, String appearanceKey);

    @Query("UPDATE hatch_drafts SET step = 2 WHERE id = :id AND step = 1")
    int advanceToPreview(String id);

    @Query("UPDATE hatch_drafts SET step = 0 WHERE id = :id AND step = 1")
    int backToName(String id);

    @Query("UPDATE hatch_drafts SET step = 1 WHERE id = :id AND step = 2")
    int backToAppearance(String id);

    @Query("UPDATE hatch_drafts SET step = 3 WHERE id = :id")
    int markCompleted(String id);

    @Query("DELETE FROM hatch_drafts WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM hatch_drafts WHERE step = 3")
    void deleteCompleted();

    @Query("DELETE FROM hatch_drafts")
    void deleteAll();
}
