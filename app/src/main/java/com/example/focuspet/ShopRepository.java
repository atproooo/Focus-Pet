package com.example.focuspet;

import android.content.Context;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ShopRepository {
    private static volatile ShopRepository instance;
    private final FocusDatabase database;
    private final ShopProductDao products;
    private final FocusStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static ShopRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ShopRepository.class) {
                if (instance == null) instance = new ShopRepository(context);
            }
        }
        return instance;
    }

    private ShopRepository(Context context) {
        database = FocusDatabase.getInstance(context);
        products = database.shopProductDao();
        store = new FocusStore(context);
        seedCatalog();
    }

    public List<ShopProductEntity> availableProducts() {
        return run(products::availableProducts);
    }

    public FocusStore.PurchaseResult purchase(String productId, String requestId) {
        return purchase(productId, 1, requestId);
    }

    public FocusStore.PurchaseResult purchase(String productId, int purchaseCount,
                                              String requestId) {
        return run(() -> {
            ShopProductEntity product = products.byId(productId);
            if (product == null || !product.available) {
                return FocusStore.PurchaseResult.INVALID_PRODUCT;
            }
            return store.purchase(product.toProduct(), purchaseCount, requestId);
        });
    }

    private void seedCatalog() {
        run(() -> {
            List<ShopProduct> catalog = new ArrayList<>(ShopCatalog.PRODUCTS);
            catalog.addAll(PetLookCatalog.PRODUCTS);
            catalog.addAll(PetExpressionCatalog.PRODUCTS);
            catalog.addAll(AccessoryCatalog.PRODUCTS);
            catalog.addAll(BackgroundCatalog.PRODUCTS);
            database.runInTransaction(() -> {
                for (int i = 0; i < catalog.size(); i++) {
                    products.upsert(ShopProductEntity.from(catalog.get(i), i));
                }
            });
            return null;
        });
    }

    private <T> T run(Callable<T> operation) {
        try {
            return executor.submit(operation).get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while accessing the shop catalog", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("Unable to access the shop catalog", error);
        }
    }
}
