package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.CosmeticItem;
import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Initial functional catalog; asset keys and prices can be replaced independently of the UI. */
public final class ShopCatalog {
    public static final int HATCH_ESSENCE_COST = 100;
    public static final List<CosmeticItem> COSMETICS = Collections.unmodifiableList(Arrays.asList(
            new CosmeticItem("collar", "Simple collar", "accessory", "accessory", "A reusable pet accessory.", null, true),
            new CosmeticItem("shirt", "Simple shirt", "clothing", "clothing", "A reusable clothing item.", null, true),
            new CosmeticItem("decoration", "Simple decoration", "decoration", "decoration", "A reusable pet decoration.", null, true)));
    public static final List<ShopProduct> PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            new ShopProduct("egg", "Pet Egg", ProductType.PET_EGG, "standard_egg", Currency.ESSENCE, 100, 1, true),
            new ShopProduct("collar", "Simple collar", ProductType.COSMETIC, "collar", Currency.COINS, 25, 1, true),
            new ShopProduct("shirt", "Simple shirt", ProductType.COSMETIC, "shirt", Currency.COINS, 50, 1, true),
            new ShopProduct("decoration", "Simple decoration", ProductType.COSMETIC, "decoration", Currency.COINS, 75, 1, true)));

    public static ShopProduct product(String id) {
        for (ShopProduct product : PRODUCTS) if (product.id.equals(id)) return product;
        return null;
    }
    public static CosmeticItem cosmetic(String id) {
        for (CosmeticItem item : COSMETICS) if (item.id.equals(id)) return item;
        return null;
    }
    private ShopCatalog() { }
}
