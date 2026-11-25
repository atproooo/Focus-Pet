package com.example.focuspet;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import com.example.focuspet.model.FocusPetModels.CosmeticItem;

/** Offline MVP store; the repository boundary can later be backed by Room. */
public final class FocusStore {
    private static final int CALENDAR_METRICS_VERSION = 2;
    private final SharedPreferences preferences;
    private final Clock clock;
    /** Guards the whole check-balance -> deduct -> grant -> receipt critical section across all instances. */
    static final Object CALENDAR_LOCK = new Object();

    public FocusStore(Context context) {
        this(context, Clock.systemDefaultZone());
    }

    FocusStore(Context context, Clock clock) {
        this(context.getSharedPreferences("focuspet_progress", Context.MODE_PRIVATE), clock);
    }

    FocusStore(SharedPreferences preferences, Clock clock) {
        this.clock = clock;
        this.preferences = preferences;
    }

    public int dailyGoal() { return preferences.getInt("daily_goal", 60); }
    public int todayMinutes() {
        synchronized (CALENDAR_LOCK) {
            refreshDay();
            return preferences.getInt("today_minutes", 0);
        }
    }
    public int totalMinutes() { return preferences.getInt("total_minutes", 0); }
    public int sessions() { return preferences.getInt("sessions", 0); }
    public int coins() { return preferences.getInt("coins", 0); }
    public int lifeEssence() { return preferences.getInt("life_essence", 0); }
    public int petEggs() { return preferences.getInt("pet_eggs", 0); }
    public int eggMisses() { return preferences.getInt("egg_misses", 0); }
    public int experience() { return preferences.getInt("experience", 0); }
    public int level() { return 1 + experience() / 100; }
    public int streak() {
        String last = preferences.getString("last_focus_date", "");
        if (last.isEmpty()) return 0;
        LocalDate day = LocalDate.parse(last);
        LocalDate today = LocalDate.now(clock);
        return !day.isAfter(today) && !day.isBefore(today.minusDays(1))
                ? preferences.getInt("streak", 0) : 0;
    }
    public int longestStreak() { return preferences.getInt("longest_streak", 0); }
    public boolean hasActiveSession() { return preferences.getBoolean("active", false); }
    public long activeEndAt() { return preferences.getLong("active_end_at", 0); }
    public int activeMinutes() { return preferences.getInt("active_minutes", 0); }
    public long activeSessionId() { return preferences.getLong("active_session_id", 0); }
    public boolean activePaused() { return preferences.getBoolean("active_paused", false); }
    public long activeRemaining() { return preferences.getLong("active_remaining", 0); }
    public void saveActiveSession(long endAt, int minutes) {
        saveActiveSessionState(endAt, minutes, false, minutes * 60_000L);
    }
    public void saveActiveSessionState(long endAt, int minutes, boolean paused, long remaining) {
        preferences.edit().putBoolean("active", true).putLong("active_end_at", endAt)
                .putInt("active_minutes", minutes).putBoolean("active_paused", paused)
                .putLong("active_remaining", remaining).apply();
    }
    public void setActiveSessionId(long id) {
        preferences.edit().putLong("active_session_id", id).apply();
    }
    public void clearActiveSession() {
        preferences.edit().remove("active").remove("active_end_at").remove("active_minutes")
                .remove("active_paused").remove("active_remaining").remove("active_session_id").apply();
    }
    public void setDailyGoal(int value) {
        preferences.edit().putInt("daily_goal", Math.max(15, Math.min(240, value))).apply();
    }

    public void recordCompleted(int minutes) {
        addClaimedRewards(minutes, minutes, 0, false);
    }

    public void addClaimedRewards(int xp, int coins, int essence, boolean egg) {
        synchronized (CALENDAR_LOCK) {
            refreshDay();
            String today = LocalDate.now(clock).toString();
            String last = preferences.getString("last_focus_date", "");
            int nextStreak = today.equals(last) ? streak()
                    : LocalDate.now(clock).minusDays(1).toString().equals(last) ? streak() + 1 : 1;
            rewardEditor(xp, coins, essence, egg)
                    .putInt("today_minutes", todayMinutes() + xp)
                    .putString("last_focus_date", today).putInt("streak", nextStreak)
                    .putInt("longest_streak", Math.max(longestStreak(), nextStreak)).apply();
        }
    }

    /** Rebuild the calendar from session history so delayed claims stay consistent. */
    public void addClaimedRewards(int xp, int coins, int essence, boolean egg,
                                  List<FocusSession> sessionHistory) {
        synchronized (CALENDAR_LOCK) {
            writeCalendar(rewardEditor(xp, coins, essence, egg), sessionHistory).apply();
        }
    }

    private SharedPreferences.Editor rewardEditor(int xp, int coins, int essence, boolean egg) {
        return preferences.edit().putInt("coins", coins() + coins)
                .putInt("experience", experience() + xp)
                .putInt("life_essence", lifeEssence() + essence)
                .putInt("pet_eggs", petEggs() + (egg ? 1 : 0))
                .putInt("total_minutes", totalMinutes() + xp)
                .putInt("sessions", sessions() + 1);
    }

    boolean needsCalendarInitialization() {
        return !clock.getZone().getId().equals(preferences.getString("calendar_zone", ""))
                || preferences.getInt("calendar_metrics_version", 0)
                < CALENDAR_METRICS_VERSION
                || !LocalDate.now(clock).toString().equals(
                preferences.getString("progress_date", ""));
    }

    void initializeCalendarProgress(List<FocusSession> sessionHistory) {
        synchronized (CALENDAR_LOCK) {
            if (needsCalendarInitialization()) {
                writeCalendar(preferences.edit(), sessionHistory).apply();
            }
        }
    }

    void refreshCalendarProgress(List<FocusSession> sessionHistory) {
        synchronized (CALENDAR_LOCK) {
            writeCalendar(preferences.edit(), sessionHistory).apply();
        }
    }

    private SharedPreferences.Editor writeCalendar(SharedPreferences.Editor editor,
                                                   List<FocusSession> sessionHistory) {
        LocalDate today = LocalDate.now(clock);
        FocusCalendar.Summary summary = FocusCalendar.summarize(
                sessionHistory, today, clock.getZone());
        return editor.putString("calendar_zone", clock.getZone().getId())
                .putInt("calendar_metrics_version", CALENDAR_METRICS_VERSION)
                .putString("progress_date", today.toString())
                .putString("last_focus_date", summary.lastFocusDate == null ? "" : summary.lastFocusDate.toString())
                .putInt("today_minutes", summary.todayMinutes).putInt("streak", summary.streak)
                .putInt("longest_streak", Math.max(longestStreak(), summary.longestStreak));
    }

    private void refreshDay() {
        String today = LocalDate.now(clock).toString();
        if (!today.equals(preferences.getString("progress_date", ""))) {
            preferences.edit().putString("progress_date", today).putInt("today_minutes", 0).apply();
        }
    }

    public void setEggMisses(int misses) {
        preferences.edit().putInt("egg_misses", misses).apply();
    }

    public boolean canHatch() {
        return petEggs() > 0 && lifeEssence() >= ShopCatalog.HATCH_ESSENCE_COST;
    }

    static final class HatchRequest {
        final String id;
        final long createdAt;
        HatchRequest(String id, long createdAt) { this.id = id; this.createdAt = createdAt; }
    }

    HatchRequest pendingHatch() {
        String id = preferences.getString("pending_hatch_id", null);
        return id == null ? null : new HatchRequest(id, preferences.getLong("pending_hatch_at", 0));
    }

    /** Persist the deduction and a recovery journal together before creating the Room pet. */
    HatchRequest beginHatch(String requestId) {
        synchronized (CALENDAR_LOCK) {
            HatchRequest pending = pendingHatch();
            if (pending != null) {
                if (!pending.id.equals(requestId)) {
                    throw new IllegalStateException("A different hatch request is already pending");
                }
                return pending;
            }
            if (!canHatch()) return null;
            long now = System.currentTimeMillis();
            if (!preferences.edit().putInt("pet_eggs", petEggs() - 1)
                    .putInt("life_essence", lifeEssence() - ShopCatalog.HATCH_ESSENCE_COST)
                    .putString("pending_hatch_id", requestId).putLong("pending_hatch_at", now).commit()) {
                throw new IllegalStateException("Unable to save hatching request");
            }
            return new HatchRequest(requestId, now);
        }
    }

    void finishHatch(long petId) {
        if (!preferences.edit().remove("pending_hatch_id").remove("pending_hatch_at")
                .putLong("hatch_result_pet_id", petId).commit()) {
            throw new IllegalStateException("Unable to save hatch result");
        }
    }

    public long hatchResultPetId() { return preferences.getLong("hatch_result_pet_id", 0); }
    public void acknowledgeHatchResult() { preferences.edit().remove("hatch_result_pet_id").apply(); }

    public enum PurchaseResult { PURCHASED, ALREADY_OWNED, INSUFFICIENT_RESOURCES, INVALID_PRODUCT, INVALID_REQUEST, STORAGE_ERROR }

    public Set<String> ownedCosmeticIds() {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet("owned_cosmetics", Collections.emptySet())));
    }

    public Set<String> ownedPetLookKeys() {
        return Collections.unmodifiableSet(new HashSet<>(
                preferences.getStringSet("owned_pet_looks", Collections.emptySet())));
    }

    public Set<String> ownedBackgroundKeys() {
        Set<String> owned = new HashSet<>(preferences.getStringSet(
                "owned_backgrounds", Collections.emptySet()));
        owned.add(BackgroundCatalog.DEFAULT_KEY);
        return Collections.unmodifiableSet(owned);
    }

    public String petBackgroundKey(long petId) {
        if (petId <= 0) return BackgroundCatalog.DEFAULT_KEY;
        String key = preferences.getString(
                equippedItemPreference(petId, "background"), BackgroundCatalog.DEFAULT_KEY);
        if (BackgroundCatalog.fromKey(key) == null) {
            throw new IllegalStateException("Pet references an unknown background: " + key);
        }
        return key;
    }

    public boolean setPetBackground(long petId, String backgroundKey) {
        synchronized (CALENDAR_LOCK) {
            if (petId <= 0 || BackgroundCatalog.fromKey(backgroundKey) == null
                    || !ownedBackgroundKeys().contains(backgroundKey)) return false;
            return preferences.edit()
                    .putString(equippedItemPreference(petId, "background"), backgroundKey)
                    .commit();
        }
    }

    public int expressionStock(String expressionKey) {
        PetExpression expression = PetExpression.fromKey(expressionKey);
        return expression != null && expression.learnable
                ? preferences.getInt(expressionStockPreference(expressionKey), 0) : 0;
    }

    public Set<String> learnedPetExpressions(long petId) {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet(
                learnedExpressionsPreference(petId), Collections.emptySet())));
    }

    public Set<String> enabledPetExpressions(long petId) {
        Set<String> enabled = new HashSet<>(preferences.getStringSet(
                enabledExpressionsPreference(petId), Collections.emptySet()));
        enabled.retainAll(learnedPetExpressions(petId));
        return Collections.unmodifiableSet(enabled);
    }

    public boolean setPetExpressionEnabled(long petId, String expressionKey, boolean enabled) {
        synchronized (CALENDAR_LOCK) {
            PetExpression expression = PetExpression.fromKey(expressionKey);
            if (petId <= 0 || expression == null || !expression.learnable) return false;
            Set<String> learned = new HashSet<>(learnedPetExpressions(petId));
            if (!learned.contains(expressionKey)) return false;
            Set<String> active = new HashSet<>(enabledPetExpressions(petId));
            if (enabled) active.add(expressionKey);
            else active.remove(expressionKey);
            return preferences.edit()
                    .putStringSet(enabledExpressionsPreference(petId), active)
                    .commit();
        }
    }

    public enum ExpressionUseResult { LEARNED, ALREADY_LEARNED, NO_STOCK, INVALID_EXPRESSION, STORAGE_ERROR }

    public ExpressionUseResult teachExpression(long petId, String expressionKey) {
        synchronized (CALENDAR_LOCK) {
            PetExpression expression = PetExpression.fromKey(expressionKey);
            if (petId <= 0 || expression == null || !expression.learnable) {
                return ExpressionUseResult.INVALID_EXPRESSION;
            }
            Set<String> learned = new HashSet<>(learnedPetExpressions(petId));
            if (learned.contains(expressionKey)) return ExpressionUseResult.ALREADY_LEARNED;
            int stock = expressionStock(expressionKey);
            if (stock <= 0) return ExpressionUseResult.NO_STOCK;
            learned.add(expressionKey);
            Set<String> active = new HashSet<>(enabledPetExpressions(petId));
            active.add(expressionKey);
            boolean saved = preferences.edit()
                    .putInt(expressionStockPreference(expressionKey), stock - 1)
                    .putStringSet(learnedExpressionsPreference(petId), learned)
                    .putStringSet(enabledExpressionsPreference(petId), active)
                    .commit();
            return saved ? ExpressionUseResult.LEARNED : ExpressionUseResult.STORAGE_ERROR;
        }
    }

    public boolean isPetLookUnlocked(String lookKey) {
        PetLook look = PetLook.fromKey(lookKey);
        return look != null && (look == PetLook.ORIGINAL || look.isFree()
                || ownedPetLookKeys().contains(lookKey));
    }

    /** Balance deduction, item grant and receipt use one atomic preference edit. */
    public PurchaseResult purchase(String productId, String requestId) {
        return purchase(productId, 1, requestId);
    }

    public PurchaseResult purchase(String productId, int purchaseCount, String requestId) {
        ShopProduct product = ShopCatalog.product(productId);
        if (product == null) product = PetLookCatalog.product(productId);
        if (product == null) product = PetExpressionCatalog.product(productId);
        if (product == null) {
            AccessoryCatalog.Accessory accessory = AccessoryCatalog.fromProductId(productId);
            if (accessory != null) product = accessory.product();
        }
        if (product == null) product = BackgroundCatalog.product(productId);
        return purchase(product, purchaseCount, requestId);
    }

    PurchaseResult purchase(ShopProduct product, String requestId) {
        return purchase(product, 1, requestId);
    }

    PurchaseResult purchase(ShopProduct product, int purchaseCount, String requestId) {
        synchronized (CALENDAR_LOCK) {
            if (product == null || !product.available) return PurchaseResult.INVALID_PRODUCT;
            if (purchaseCount < 1 || requestId == null || requestId.trim().isEmpty()) {
                return PurchaseResult.INVALID_REQUEST;
            }
            String receipt = preferences.getString("purchase_" + requestId, null);
            if (receipt != null) {
                if (!receipt.startsWith(product.id + "|")) return PurchaseResult.INVALID_REQUEST;
                String[] receiptParts = receipt.split("\\|", -1);
                int previousPurchaseCount = 1;
                if (receiptParts.length >= 6) {
                    try {
                        previousPurchaseCount = Integer.parseInt(receiptParts[5]);
                    } catch (NumberFormatException error) {
                        return PurchaseResult.INVALID_REQUEST;
                    }
                }
                return previousPurchaseCount == purchaseCount
                        ? PurchaseResult.PURCHASED : PurchaseResult.INVALID_REQUEST;
            }
            if (product.productType != ProductType.PET_EXPRESSION && purchaseCount != 1) {
                return PurchaseResult.INVALID_REQUEST;
            }
            if (product.price > Long.MAX_VALUE / purchaseCount) {
                return PurchaseResult.INVALID_REQUEST;
            }
            long totalPrice = product.price * purchaseCount;
            long quantityToGrant = (long) product.quantityGranted * purchaseCount;
            Set<String> owned = new HashSet<>(ownedCosmeticIds());
            Set<String> ownedLooks = new HashSet<>(ownedPetLookKeys());
            Set<String> ownedBackgrounds = new HashSet<>(
                    preferences.getStringSet("owned_backgrounds", Collections.emptySet()));
            if (product.productType == ProductType.COSMETIC && owned.contains(product.itemKey)) {
                return PurchaseResult.ALREADY_OWNED;
            }
            if (product.productType == ProductType.PET_LOOK && ownedLooks.contains(product.itemKey)) {
                return PurchaseResult.ALREADY_OWNED;
            }
            if (product.productType == ProductType.PET_BACKGROUND) {
                BackgroundCatalog.Background background =
                        BackgroundCatalog.fromKey(product.itemKey);
                if (background == null || background == BackgroundCatalog.DEFAULT) {
                    return PurchaseResult.INVALID_PRODUCT;
                }
                if (ownedBackgrounds.contains(product.itemKey)) {
                    return PurchaseResult.ALREADY_OWNED;
                }
            }
            if (product.productType == ProductType.PET_EXPRESSION) {
                PetExpression expression = PetExpression.fromKey(product.itemKey);
                if (expression == null || !expression.learnable) {
                    return PurchaseResult.INVALID_PRODUCT;
                }
                if ((long) expressionStock(product.itemKey) + quantityToGrant > Integer.MAX_VALUE) {
                    return PurchaseResult.INVALID_REQUEST;
                }
            }
            if (product.productType == ProductType.COSMETIC
                    && AccessoryCatalog.fromKey(product.itemKey) == null
                    && ShopCatalog.cosmetic(product.itemKey) == null) {
                return PurchaseResult.INVALID_PRODUCT;
            }
            int balance = product.currency == Currency.COINS ? coins() : lifeEssence();
            if (balance < totalPrice) return PurchaseResult.INSUFFICIENT_RESOURCES;
            if (product.productType == ProductType.PET_EGG
                    && (long) petEggs() + product.quantityGranted > Integer.MAX_VALUE) return PurchaseResult.INVALID_REQUEST;
            SharedPreferences.Editor editor = preferences.edit()
                    .putInt(product.currency == Currency.COINS ? "coins" : "life_essence",
                            balance - (int) totalPrice);
            if (product.productType == ProductType.COSMETIC) {
                owned.add(product.itemKey);
                editor.putStringSet("owned_cosmetics", owned);
            } else if (product.productType == ProductType.PET_LOOK) {
                ownedLooks.add(product.itemKey);
                editor.putStringSet("owned_pet_looks", ownedLooks);
            } else if (product.productType == ProductType.PET_BACKGROUND) {
                ownedBackgrounds.add(product.itemKey);
                editor.putStringSet("owned_backgrounds", ownedBackgrounds);
            } else if (product.productType == ProductType.PET_EXPRESSION) {
                editor.putInt(expressionStockPreference(product.itemKey),
                        expressionStock(product.itemKey) + (int) quantityToGrant);
            } else if (product.productType == ProductType.PET_EGG) {
                editor.putInt("pet_eggs", petEggs() + product.quantityGranted);
            } else {
                return PurchaseResult.INVALID_PRODUCT;
            }
            editor.putString("purchase_" + requestId, product.id + "|" + product.currency + "|" + product.price
                    + "|" + product.quantityGranted + "|" + System.currentTimeMillis()
                    + "|" + purchaseCount);
            return editor.commit() ? PurchaseResult.PURCHASED : PurchaseResult.STORAGE_ERROR;
        }
    }

    private String expressionStockPreference(String expressionKey) {
        return "expression_stock_" + expressionKey;
    }

    private String learnedExpressionsPreference(long petId) {
        return "pet_" + petId + "_learned_expressions";
    }

    private String enabledExpressionsPreference(long petId) {
        return "pet_" + petId + "_enabled_expressions";
    }

    boolean equip(long petId, String itemId) {
        synchronized (CALENDAR_LOCK) {
            if (petId <= 0 || !ownedCosmeticIds().contains(itemId)) return false;
            AccessoryCatalog.Accessory accessory = AccessoryCatalog.fromKey(itemId);
            if (accessory != null) {
                return preferences.edit()
                        .putString(equippedItemPreference(petId, "accessory"), itemId)
                        .commit();
            }
            CosmeticItem item = ShopCatalog.cosmetic(itemId);
            return item != null && preferences.edit()
                    .putString(equippedItemPreference(petId, item.slotKey), itemId).commit();
        }
    }

    public String equippedItemId(long petId, String slot) {
        return preferences.getString(equippedItemPreference(petId, slot), null);
    }

    public String equippedAccessoryId(long petId) {
        return equippedItemId(petId, "accessory");
    }

    public void unequip(long petId, String slot) {
        synchronized (CALENDAR_LOCK) {
            preferences.edit().remove(equippedItemPreference(petId, slot)).apply();
        }
    }

    private String equippedItemPreference(long petId, String slot) {
        return "pet_equipped_" + petId + "_" + slot;
    }

    public void addDebugResources(int coins, int essence, int eggs) {
        synchronized (CALENDAR_LOCK) {
            preferences.edit().putInt("coins", (int) Math.min(Integer.MAX_VALUE, (long) coins() + Math.max(0, coins)))
                    .putInt("life_essence", (int) Math.min(Integer.MAX_VALUE, (long) lifeEssence() + Math.max(0, essence)))
                    .putInt("pet_eggs", (int) Math.min(Integer.MAX_VALUE, (long) petEggs() + Math.max(0, eggs))).apply();
        }
    }

    public void reset() { synchronized (CALENDAR_LOCK) { preferences.edit().clear().apply(); } }
}
