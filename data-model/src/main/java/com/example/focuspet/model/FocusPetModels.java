package com.example.focuspet.model;

import java.util.Objects;

/**
 * Standalone value objects for the proposed FocusPet data model.
 * They do not read or write Android or Room data. All times are Unix milliseconds.
 */
public final class FocusPetModels {
    private FocusPetModels() { }

    public enum Currency { COINS, ESSENCE }
    public enum ProductType { COSMETIC, PET_EGG }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return value;
    }

    private static long nonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }

    private static int positive(int value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
        return value;
    }

    private static void checkProductCurrency(ProductType type, Currency currency) {
        Objects.requireNonNull(type, "productType");
        Objects.requireNonNull(currency, "currency");
        if ((type == ProductType.COSMETIC && currency != Currency.COINS)
                || (type == ProductType.PET_EGG && currency != Currency.ESSENCE)) {
            throw new IllegalArgumentException("Cosmetics use coins and pet eggs use essence");
        }
    }

    public static final class FocusGoal {
        public final long id;
        public final String name;
        public final int targetMinutes;
        /** Extensible key, e.g. DAILY or WEEKLY. */
        public final String periodType;
        public final long createdAt;
        public final long updatedAt;
        public final Long archivedAt;

        public FocusGoal(long id, String name, int targetMinutes, String periodType,
                         long createdAt, long updatedAt, Long archivedAt) {
            this.id = nonNegative(id, "id");
            this.name = required(name, "name");
            this.targetMinutes = positive(targetMinutes, "targetMinutes");
            this.periodType = required(periodType, "periodType");
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.archivedAt = archivedAt;
        }
    }

    /** A proposed session snapshot, including all fields in the existing session model. */
    public static final class FocusSession {
        public final long id;
        public final long startedAt;
        public final long endedAt;
        public final int plannedMinutes;
        public final int actualMinutes;
        public final String label;
        /** Existing status values may be retained without changing the running app. */
        public final String status;
        public final boolean rewardGranted;
        public final boolean endedEarly;
        public final int xpReward;
        public final int coinReward;
        public final int essenceReward;
        public final boolean eggReward;
        public final Long goalId;

        public FocusSession(long id, long startedAt, long endedAt, int plannedMinutes,
                            int actualMinutes, String label, String status,
                            boolean rewardGranted, boolean endedEarly, int xpReward,
                            int coinReward, int essenceReward, boolean eggReward, Long goalId) {
            this.id = nonNegative(id, "id");
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.plannedMinutes = positive(plannedMinutes, "plannedMinutes");
            this.actualMinutes = (int) nonNegative(actualMinutes, "actualMinutes");
            this.label = label == null ? "" : label;
            this.status = required(status, "status");
            this.rewardGranted = rewardGranted;
            this.endedEarly = endedEarly;
            this.xpReward = (int) nonNegative(xpReward, "xpReward");
            this.coinReward = (int) nonNegative(coinReward, "coinReward");
            this.essenceReward = (int) nonNegative(essenceReward, "essenceReward");
            this.eggReward = eggReward;
            this.goalId = goalId;
        }
    }

    public static final class UserSettings {
        public final int dailyTargetMinutes;
        public final long updatedAt;
        public UserSettings(int dailyTargetMinutes, long updatedAt) {
            this.dailyTargetMinutes = positive(dailyTargetMinutes, "dailyTargetMinutes");
            this.updatedAt = updatedAt;
        }
    }

    /** Clothing and accessories extend a pet's base appearance; keys are not fixed categories. */
    public static final class CosmeticItem {
        public final String id;
        public final String name;
        public final String categoryKey;
        public final String slotKey;
        public final String description;
        public final String assetKey;
        public final boolean available;
        public CosmeticItem(String id, String name, String categoryKey, String slotKey,
                            String description, String assetKey, boolean available) {
            this.id = required(id, "id");
            this.name = required(name, "name");
            this.categoryKey = required(categoryKey, "categoryKey");
            this.slotKey = required(slotKey, "slotKey");
            this.description = description;
            this.assetKey = assetKey;
            this.available = available;
        }
    }

    public static final class UserCosmetic {
        public final String itemId;
        public final long acquiredAt;
        public final Long sourcePurchaseId;
        public UserCosmetic(String itemId, long acquiredAt, Long sourcePurchaseId) {
            this.itemId = required(itemId, "itemId");
            this.acquiredAt = acquiredAt;
            this.sourcePurchaseId = sourcePurchaseId;
        }
    }

    /** Storage should enforce uniqueness of (petId, slotKey). */
    public static final class PetEquipment {
        public final long petId;
        public final String slotKey;
        public final String itemId;
        public final long equippedAt;
        public PetEquipment(long petId, String slotKey, String itemId, long equippedAt) {
            this.petId = nonNegative(petId, "petId");
            this.slotKey = required(slotKey, "slotKey");
            this.itemId = required(itemId, "itemId");
            this.equippedAt = equippedAt;
        }
    }

    public static final class WalletBalance {
        public final Currency currency;
        public final long amount;
        public final long updatedAt;
        public WalletBalance(Currency currency, long amount, long updatedAt) {
            this.currency = Objects.requireNonNull(currency, "currency");
            this.amount = nonNegative(amount, "amount");
            this.updatedAt = updatedAt;
        }
    }

    public static final class ShopProduct {
        public final String id;
        public final String name;
        public final ProductType productType;
        public final String itemKey;
        public final Currency currency;
        public final long price;
        public final int quantityGranted;
        public final boolean available;
        public ShopProduct(String id, String name, ProductType productType, String itemKey,
                           Currency currency, long price, int quantityGranted, boolean available) {
            checkProductCurrency(productType, currency);
            this.id = required(id, "id");
            this.name = required(name, "name");
            this.productType = productType;
            this.itemKey = required(itemKey, "itemKey");
            this.currency = currency;
            this.price = nonNegative(price, "price");
            this.quantityGranted = positive(quantityGranted, "quantityGranted");
            this.available = available;
        }
    }

    /** Stackable items, such as eggs; uniqueness is (itemType, itemKey). */
    public static final class InventoryItem {
        public final String itemType;
        public final String itemKey;
        public final long quantity;
        public final long updatedAt;
        public InventoryItem(String itemType, String itemKey, long quantity, long updatedAt) {
            this.itemType = required(itemType, "itemType");
            this.itemKey = required(itemKey, "itemKey");
            this.quantity = nonNegative(quantity, "quantity");
            this.updatedAt = updatedAt;
        }
    }

    /** Purchase-time snapshot; atomic storage and idempotency belong to the future repository. */
    public static final class PurchaseRecord {
        public final long id;
        public final String requestId;
        public final String productId;
        public final ProductType productType;
        public final String itemKey;
        public final Currency currency;
        public final long unitPrice;
        public final int quantity;
        public final long grantedQuantity;
        public final long createdAt;
        public PurchaseRecord(long id, String requestId, String productId, ProductType productType,
                              String itemKey, Currency currency, long unitPrice, int quantity,
                              long grantedQuantity, long createdAt) {
            checkProductCurrency(productType, currency);
            this.id = nonNegative(id, "id");
            this.requestId = required(requestId, "requestId");
            this.productId = required(productId, "productId");
            this.productType = productType;
            this.itemKey = required(itemKey, "itemKey");
            this.currency = currency;
            this.unitPrice = nonNegative(unitPrice, "unitPrice");
            this.quantity = positive(quantity, "quantity");
            if (grantedQuantity <= 0) throw new IllegalArgumentException("grantedQuantity must be positive");
            this.grantedQuantity = grantedQuantity;
            this.createdAt = createdAt;
        }
    }
}
