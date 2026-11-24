package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PetLookCatalog {
    public static final List<ShopProduct> PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            product("mint_star", "Mint Star", "A mint-green look with a bright star."),
            product("cloud_wings", "Cloud Wings", "A soft, winged pet look."),
            product("midnight_dj", "Midnight DJ", "A striped cap, headphones, and glasses."),
            product("garden_chill", "Garden Chill", "A relaxed look with a phone and leafy cushion."),
            product("juice_break", "Juice Break", "A cozy beanie and a fruit drink."),
            product("blush_winter", "Blush Winter", "A pink winter hat, scarf, and bows."),
            product("strawberry_night", "Strawberry Night", "A dark look with a star cap and strawberry.")
    ));

    public static ShopProduct product(String productId) {
        for (ShopProduct product : PRODUCTS) {
            if (product.id.equals(productId)) return product;
        }
        return null;
    }

    private static ShopProduct product(String key, String name, String description) {
        return new ShopProduct("look_" + key, name, "pet_looks", ProductType.PET_LOOK,
                key, description, Currency.COINS, 67, 1, true);
    }

    private PetLookCatalog() { }
}
