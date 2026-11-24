package com.example.focuspet;

import android.content.SharedPreferences;

import org.junit.Test;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpressionInventoryTest {
    @Test
    public void purchasesStackAndDuplicateReceiptsDoNotGrantTwice() {
        TestPreferences preferences = new TestPreferences();
        FocusStore store = new FocusStore(preferences, Clock.systemUTC());
        store.addDebugResources(200, 0, 0);
        com.example.focuspet.model.FocusPetModels.ShopProduct product =
                PetExpressionCatalog.PRODUCTS.get(0);

        assertEquals(FocusStore.PurchaseResult.PURCHASED,
                store.purchase(product, "purchase-one"));
        assertEquals(FocusStore.PurchaseResult.PURCHASED,
                store.purchase(product, "purchase-two"));
        assertEquals(FocusStore.PurchaseResult.PURCHASED,
                store.purchase(product, "purchase-two"));

        assertEquals(2, store.expressionStock(product.itemKey));
        assertEquals(100, store.coins());
    }

    @Test
    public void teachingConsumesOneItemAndStoresPerPetEnabledState() {
        TestPreferences preferences = new TestPreferences();
        FocusStore store = new FocusStore(preferences, Clock.systemUTC());
        store.addDebugResources(100, 0, 0);
        com.example.focuspet.model.FocusPetModels.ShopProduct product =
                PetExpressionCatalog.PRODUCTS.get(0);
        store.purchase(product, "purchase-one");
        store.purchase(product, "purchase-two");

        assertEquals(FocusStore.ExpressionUseResult.LEARNED,
                store.teachExpression(7, product.itemKey));
        assertEquals(1, store.expressionStock(product.itemKey));
        assertTrue(store.learnedPetExpressions(7).contains(product.itemKey));
        assertTrue(store.enabledPetExpressions(7).contains(product.itemKey));

        assertEquals(FocusStore.ExpressionUseResult.ALREADY_LEARNED,
                store.teachExpression(7, product.itemKey));
        assertEquals(1, store.expressionStock(product.itemKey));
        assertTrue(store.setPetExpressionEnabled(7, product.itemKey, false));
        assertFalse(store.enabledPetExpressions(7).contains(product.itemKey));
        assertTrue(store.learnedPetExpressions(7).contains(product.itemKey));

        assertEquals(FocusStore.ExpressionUseResult.LEARNED,
                store.teachExpression(8, product.itemKey));
        assertEquals(0, store.expressionStock(product.itemKey));
        assertTrue(store.enabledPetExpressions(8).contains(product.itemKey));
        assertFalse(store.enabledPetExpressions(7).contains(product.itemKey));
    }

    private static final class TestPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override public synchronized Map<String, ?> getAll() {
            return Collections.unmodifiableMap(new HashMap<>(values));
        }

        @Override public synchronized String getString(String key, String defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (String) value;
        }

        @SuppressWarnings("unchecked")
        @Override public synchronized Set<String> getStringSet(
                String key, Set<String> defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : new HashSet<>((Set<String>) value);
        }

        @Override public synchronized int getInt(String key, int defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Integer) value;
        }

        @Override public synchronized long getLong(String key, long defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Long) value;
        }

        @Override public synchronized float getFloat(String key, float defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Float) value;
        }

        @Override public synchronized boolean getBoolean(String key, boolean defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : (Boolean) value;
        }

        @Override public synchronized boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override public Editor edit() {
            return new TestEditor();
        }

        @Override public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        @Override public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        private final class TestEditor implements Editor {
            private final Map<String, Object> updates = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clear;

            private Editor put(String key, Object value) {
                removals.remove(key);
                updates.put(key, value);
                return this;
            }

            @Override public Editor putString(String key, String value) {
                return put(key, value);
            }

            @Override public Editor putStringSet(String key, Set<String> value) {
                return put(key, value == null ? null : new HashSet<>(value));
            }

            @Override public Editor putInt(String key, int value) {
                return put(key, value);
            }

            @Override public Editor putLong(String key, long value) {
                return put(key, value);
            }

            @Override public Editor putFloat(String key, float value) {
                return put(key, value);
            }

            @Override public Editor putBoolean(String key, boolean value) {
                return put(key, value);
            }

            @Override public Editor remove(String key) {
                updates.remove(key);
                removals.add(key);
                return this;
            }

            @Override public Editor clear() {
                clear = true;
                updates.clear();
                removals.clear();
                return this;
            }

            @Override public boolean commit() {
                synchronized (TestPreferences.this) {
                    if (clear) values.clear();
                    for (String key : removals) values.remove(key);
                    values.putAll(updates);
                }
                return true;
            }

            @Override public void apply() {
                commit();
            }
        }
    }
}
