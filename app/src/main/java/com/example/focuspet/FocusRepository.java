package com.example.focuspet;

import android.content.Context;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Owns session history and completion rewards using a serialized database executor.
 */
public final class FocusRepository {
    private static final String STARTER_TEMPLATE_ID = "starter_fox";
    private static final String HATCH_TEMPLATE_ID = "hatched_pet";
    private final FocusSessionDao sessions;
    private final PetDao pets;
    private final HatchDraftDao hatchDrafts;
    private final FocusStore progress;
    private final FocusDatabase database;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public FocusRepository(Context context) {
        database = FocusDatabase.getInstance(context);
        sessions = database.focusSessionDao();
        pets = database.petDao();
        hatchDrafts = database.hatchDraftDao();
        progress = new FocusStore(context);
        if (progress.needsCalendarInitialization()) {
            run(() -> {
                progress.initializeCalendarProgress(sessions.claimedHistory());
                return null;
            });
        }
    }

    /** Erases all persisted sessions, pets, and preferences in one serialized operation. */
    public void clearAllData() {
        run(() -> {
            synchronized (FocusStore.CALENDAR_LOCK) {
                sessions.deleteAll();
                pets.deleteAll();
                hatchDrafts.deleteAll();
                progress.reset();
            }
            return null;
        });
    }

    public long startSession(int plannedMinutes, String label) {
        try {
            Future<Long> result = databaseExecutor.submit(() -> {
                FocusSession session = new FocusSession();
                session.startedAt = System.currentTimeMillis();
                session.plannedMinutes = plannedMinutes;
                session.label = label == null ? "" : label;
                session.status = FocusSession.STATUS_RUNNING;
                session.rewardGranted = false;
                return sessions.insert(session);
            });
            return result.get();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create focus session", error);
        }
    }

    public void completeSession(long id, int actualMinutes) {
        completeSession(id, actualMinutes, false);
    }

    public void completeEarlySession(long id, int actualMinutes) {
        if (actualMinutes > 0) completeSession(id, actualMinutes, true);
        else cancelSession(id, 0);
    }

    private void completeSession(long id, int actualMinutes, boolean endedEarly) {
        run(() -> {
            FocusSession session = sessions.findById(id);
            if (session == null || !FocusSession.STATUS_RUNNING.equals(session.status)) return null;
            int baseEssence = Math.max(1, Math.min(5, (int) (actualMinutes * 0.08f)));
            int essence = Math.max(1, Math.min(5, baseEssence + random.nextInt(3) - 1));
            int coins = actualMinutes + random.nextInt(4);
            int xp = actualMinutes;
            int streak = progress.streak();
            int eggChance = Math.min(100, eggChance(actualMinutes) + Math.min(3, streak / 4));
            if (endedEarly) {
                xp = (int) (xp * 0.30f);
                essence = (int) (essence * 0.30f);
                coins = (int) (coins * 0.30f);
                eggChance = (int) (eggChance * 0.30f);
            }
            int misses = progress.eggMisses() + 1;
            boolean egg = !endedEarly && (misses >= 10 || random.nextInt(100) < eggChance);
            progress.setEggMisses(egg ? 0 : misses);
            sessions.finish(id, FocusSession.STATUS_COMPLETED, System.currentTimeMillis(), actualMinutes,
                    xp, coins, essence, egg, endedEarly);
            return null;
        });
    }

    private final java.util.Random random = new java.util.Random();

    private int eggChance(int minutes) {
        if (minutes >= 60) return 6;
        if (minutes >= 45) return 4;
        if (minutes >= 25) return 2;
        return 1;
    }

    public FocusSession pendingSession(long id) {
        return run(() -> sessions.findById(id));
    }

    public boolean claimRewards(long id) {
        return run(() -> {
            FocusSession session = sessions.findById(id);
            if (session == null || !FocusSession.STATUS_COMPLETED.equals(session.status)
                    || sessions.claim(id) != 1) return false;
            progress.addClaimedRewards(session.xpReward, session.coinReward,
                    session.essenceReward, session.eggReward, sessions.claimedHistory());
            Pet selected = pets.selected();
            if (selected != null && session.xpReward > 0) {
                pets.addExperience(selected.id, session.xpReward);
            }
            return true;
        });
    }

    public void cancelSession(long id, int actualMinutes) {
        if (id > 0) {
            run(() -> {
                sessions.cancel(id, FocusSession.STATUS_CANCELLED, System.currentTimeMillis(), actualMinutes);
                return null;
            });
        }
    }

    private <T> T run(java.util.concurrent.Callable<T> work) {
        try {
            return databaseExecutor.submit(work).get();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to update focus session", error);
        }
    }

    public List<FocusSession> history() {
        return run(sessions::history);
    }

    public List<FocusSession> recentHistory(int limit) {
        return run(() -> sessions.recentHistory(limit));
    }

    public FocusSession latestPending() {
        return run(sessions::latestPending);
    }

    public List<Pet> pets() {
        return run(pets::all);
    }

    public Pet selectedPet() {
        return run(pets::selected);
    }

    public Pet pet(long id) { return run(() -> pets.byId(id)); }

    boolean savePetLook(long petId, PetLook look) {
        if (look == null || !progress.isPetLookUnlocked(look.key)) return false;
        return run(() -> pets.updateAppearance(petId, look.key) == 1);
    }

    public Pet ensureStarterPet() {
        return run(() -> {
            Pet selected = pets.selected();
            Pet starter = pets.byTemplateId(STARTER_TEMPLATE_ID);
            if (starter != null) {
                pets.updateNameIfMatches(starter.id, "Mochi", "Toast");
                pets.updateAppearanceIfMatches(starter.id, "default_fox",
                        PetLook.CLASSIC.key, PetLook.CLASSIC.key);
                starter = pets.byId(starter.id);
                if (selected != null && selected.id == starter.id) selected = starter;
            }
            if (pets.count() > 0) return selected != null ? selected : pets.all().get(0);
            return insertPet(STARTER_TEMPLATE_ID, "Toast", "Fox", PetLook.CLASSIC.key,
                    PetLook.CLASSIC.key, "Smelling nice?", System.currentTimeMillis(),
                    null, true);
        });
    }

    private Pet insertPet(String templateId, String name, String species, String imageKey,
                          String appearanceData, String description, long createdAt,
                          String hatchRequestId, boolean selected) {
        Pet pet = new Pet();
        pet.templateId = templateId;
        pet.name = name;
        pet.species = species;
        pet.rarity = "Common";
        pet.experience = 0;
        pet.imageKey = imageKey;
        pet.appearanceData = appearanceData;
        pet.description = description;
        pet.hatchedAt = createdAt;
        pet.hatchRequestId = hatchRequestId;
        pet.mood = 100;
        pet.energy = 100;
        pet.selected = selected;
        pet.id = pets.insert(pet);
        return pet;
    }

    public HatchDraft activeHatchDraft() {
        return run(() -> {
            hatchDrafts.deleteCompleted();
            return hatchDrafts.active();
        });
    }

    public HatchDraft startHatchDraft() {
        return run(() -> {
            synchronized (FocusStore.CALENDAR_LOCK) {
                HatchDraft active = hatchDrafts.active();
                FocusStore.HatchRequest pending = progress.pendingHatch();
                if (active != null) {
                    if (pending != null && !pending.id.equals(active.id)) {
                        throw new IllegalStateException("A different hatch customization is already active");
                    }
                    return active;
                }
                if (pending != null) {
                    HatchDraft interrupted = hatchDrafts.byId(pending.id);
                    if (interrupted != null) return interrupted;
                    HatchDraft legacy = newHatchDraft(pending.id, pending.createdAt);
                    hatchDrafts.insert(legacy);
                    return legacy;
                }

                if (!progress.canHatch()) return null;
                HatchDraft draft = newHatchDraft(UUID.randomUUID().toString(),
                        System.currentTimeMillis());
                hatchDrafts.insert(draft);
                return draft;
            }
        });
    }

    private HatchDraft newHatchDraft(String id, long createdAt) {
        HatchDraft draft = new HatchDraft();
        draft.id = id;
        draft.appearanceKey = PetLook.CLASSIC.key;
        draft.step = HatchDraft.STEP_NAME;
        draft.createdAt = createdAt;
        return draft;
    }

    public boolean updateHatchDraftName(String draftId, String name) {
        return run(() -> hatchDrafts.updateName(draftId, name == null ? "" : name) == 1);
    }

    public HatchDraft advanceHatchName(String draftId, String name) {
        String normalized = name == null ? "" : name.trim();
        validateHatchName(normalized);
        return run(() -> {
            if (hatchDrafts.saveNameAndAdvance(draftId, normalized) != 1) {
                throw new IllegalStateException("Hatch name step is no longer active");
            }
            return hatchDrafts.byId(draftId);
        });
    }

    public HatchDraft updateHatchAppearance(String draftId, String appearanceKey) {
        if (!isSelectableHatchLook(appearanceKey)) {
            throw new IllegalArgumentException("Unsupported pet appearance");
        }
        return run(() -> {
            if (hatchDrafts.updateAppearance(draftId, appearanceKey) != 1) {
                throw new IllegalStateException("Hatch appearance step is no longer active");
            }
            return hatchDrafts.byId(draftId);
        });
    }

    public HatchDraft advanceHatchAppearance(String draftId) {
        return run(() -> {
            HatchDraft draft = hatchDrafts.byId(draftId);
            if (draft == null || draft.step != HatchDraft.STEP_APPEARANCE
                    || !isSelectableHatchLook(draft.appearanceKey)
                    || hatchDrafts.advanceToPreview(draftId) != 1) {
                throw new IllegalStateException("Choose a pet appearance before continuing");
            }
            return hatchDrafts.byId(draftId);
        });
    }

    public HatchDraft moveHatchDraftBack(String draftId) {
        return run(() -> {
            HatchDraft draft = hatchDrafts.byId(draftId);
            if (draft == null) throw new IllegalStateException("Hatch customization is unavailable");
            if (draft.step == HatchDraft.STEP_APPEARANCE) {
                hatchDrafts.backToName(draftId);
            } else if (draft.step == HatchDraft.STEP_PREVIEW) {
                hatchDrafts.backToAppearance(draftId);
            }
            return hatchDrafts.byId(draftId);
        });
    }

    public Pet confirmHatch(String draftId) {
        if (draftId == null || draftId.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing hatch draft");
        }
        return run(() -> {
            synchronized (FocusStore.CALENDAR_LOCK) {
                Pet existing = pets.byHatchRequest(draftId);
                if (existing != null) {
                    FocusStore.HatchRequest pending = progress.pendingHatch();
                    if (pending != null && pending.id.equals(draftId)) {
                        progress.finishHatch(existing.id);
                    }
                    hatchDrafts.deleteById(draftId);
                    return existing;
                }
                HatchDraft draft = hatchDrafts.byId(draftId);
                if (draft == null || draft.step != HatchDraft.STEP_PREVIEW) {
                    throw new IllegalStateException("Review the pet before confirming");
                }
                validateHatchName(draft.name);
                if (!isSelectableHatchLook(draft.appearanceKey)) {
                    throw new IllegalStateException("Choose a valid pet appearance");
                }

                FocusStore.HatchRequest pending = progress.pendingHatch();
                if (pending == null) {
                    pending = progress.beginHatch(draftId);
                    if (pending == null) return null;
                } else if (!pending.id.equals(draftId)) {
                    throw new IllegalStateException("A different hatch request is already pending");
                }
                return finishPendingHatch(draft, pending);
            }
        });
    }

    /** Resume only requests that had passed the user's final confirmation step. */
    public Pet recoverPendingHatch() {
        return run(() -> {
            synchronized (FocusStore.CALENDAR_LOCK) {
                FocusStore.HatchRequest pending = progress.pendingHatch();
                if (pending == null) return null;

                Pet existing = pets.byHatchRequest(pending.id);
                if (existing != null) {
                    progress.finishHatch(existing.id);
                    hatchDrafts.deleteById(pending.id);
                    return existing;
                }

                HatchDraft draft = hatchDrafts.byId(pending.id);
                if (draft == null) {
                    draft = newHatchDraft(pending.id, pending.createdAt);
                    hatchDrafts.insert(draft);
                    return null;
                }
                if (draft.step != HatchDraft.STEP_PREVIEW) return null;
                return finishPendingHatch(draft, pending);
            }
        });
    }

    private Pet finishPendingHatch(HatchDraft draft, FocusStore.HatchRequest request) {
        Pet pet = database.runInTransaction(() -> {
            Pet existing = pets.byHatchRequest(request.id);
            if (existing != null) return existing;

            HatchDraft latest = hatchDrafts.byId(draft.id);
            if (latest == null || latest.step != HatchDraft.STEP_PREVIEW) {
                throw new IllegalStateException("Confirmed hatch draft is unavailable");
            }
            validateHatchName(latest.name);
            if (!isSelectableHatchLook(latest.appearanceKey)) {
                throw new IllegalStateException("Confirmed pet appearance is invalid");
            }
            Pet created = insertPet(HATCH_TEMPLATE_ID, latest.name, "Pet",
                    latest.appearanceKey, latest.appearanceKey,
                    "A newly hatched pet waiting to grow.", request.createdAt,
                    request.id, false);
            hatchDrafts.markCompleted(latest.id);
            return created;
        });
        progress.finishHatch(pet.id);
        return pet;
    }

    private boolean isSelectableHatchLook(String key) {
        PetLook look = PetLook.fromKey(key);
        return look != null && look != PetLook.ORIGINAL && progress.isPetLookUnlocked(key);
    }

    private static void validateHatchName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a name for your pet");
        }
        String normalized = name.trim();
        if (normalized.codePointCount(0, normalized.length()) > HatchDraft.MAX_NAME_CODE_POINTS) {
            throw new IllegalArgumentException("Pet names must be 20 characters or fewer");
        }
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException("Pet names cannot contain control characters");
            }
            offset += Character.charCount(codePoint);
        }
    }

    public boolean equipCosmetic(long petId, String itemId) {
        return run(() -> pets.byId(petId) != null && progress.equip(petId, itemId));
    }

    public enum InteractionResult { UPDATED, COOLDOWN, LOW_ENERGY, NOT_FOUND }

    public InteractionResult interact(long petId, PetInteraction.Action action) {
        return interact(petId, action, System.currentTimeMillis());
    }

    InteractionResult interact(long petId, PetInteraction.Action action, long now) {
        if (action == null) throw new IllegalArgumentException("Missing interaction");
        return run(() -> database.runInTransaction(() -> {
            Pet pet = pets.byId(petId);
            if (pet == null) return InteractionResult.NOT_FOUND;
            if (pet.lastInteractionAt > 0 && now >= pet.lastInteractionAt
                    && now - pet.lastInteractionAt < PetInteraction.COOLDOWN_MILLIS) return InteractionResult.COOLDOWN;
            if (action == PetInteraction.Action.PLAY && pet.energy < 5) return InteractionResult.LOW_ENERGY;
            pets.updateInteraction(petId, PetInteraction.moodAfter(pet.mood, action),
                    PetInteraction.energyAfter(pet.energy, action), now);
            return InteractionResult.UPDATED;
        }));
    }

    public void selectPet(long id) {
        run(() -> {
            pets.clearSelection();
            pets.select(id);
            return null;
        });
    }
}
