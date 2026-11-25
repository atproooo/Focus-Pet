package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BackgroundCatalog {
    static final String DEFAULT_KEY = "plain";
    static final int BACKGROUND_PRICE = 50;

    static final class Background {
        final String key;
        final String name;
        final int drawable;

        Background(String key, String name, int drawable) {
            this.key = key;
            this.name = name;
            this.drawable = drawable;
        }

        ShopProduct product() {
            return new ShopProduct("background_" + key, name, "backgrounds",
                    ProductType.PET_BACKGROUND, key, "A background for your pet.",
                    Currency.COINS, BACKGROUND_PRICE, 1, true);
        }
    }

    static final Background DEFAULT =
            new Background(DEFAULT_KEY, "Plain", R.drawable.bg_plain);
    static final List<Background> BACKGROUNDS;
    static final List<ShopProduct> PRODUCTS;

    static {
        List<Background> backgrounds = new ArrayList<>();
        backgrounds.add(DEFAULT);
        backgrounds.add(new Background("bg_1", "Dotted Wings", R.drawable.bg_1));
        backgrounds.add(new Background("bg_2", "Cloud Wings", R.drawable.bg_2));
        backgrounds.add(new Background("bg_3", "Starburst", R.drawable.bg_3));
        backgrounds.add(new Background("bg_4", "Olive Stars", R.drawable.bg_4));
        backgrounds.add(new Background("bg_5", "Blue Dots", R.drawable.bg_5));
        backgrounds.add(new Background("bg_6", "Silver Stars", R.drawable.bg_6));
        backgrounds.add(new Background("bg_7", "Lavender Burst", R.drawable.bg_7));
        backgrounds.add(new Background("bg_8", "Prism Waves", R.drawable.bg_8));
        backgrounds.add(new Background("bg_9", "Cloud Glow", R.drawable.bg_9));
        backgrounds.add(new Background("bg_10", "Aqua Burst", R.drawable.bg_10));
        backgrounds.add(new Background("bg_11", "Icy Abstract", R.drawable.bg_11));
        backgrounds.add(new Background("bg_12", "Starlight Spiral", R.drawable.bg_12));
        backgrounds.add(new Background("bg_13", "Blue Flow", R.drawable.bg_13));
        backgrounds.add(new Background("bg_14", "Aqua Splash", R.drawable.bg_14));
        BACKGROUNDS = Collections.unmodifiableList(backgrounds);

        List<ShopProduct> products = new ArrayList<>();
        for (Background background : BACKGROUNDS) {
            if (background != DEFAULT) products.add(background.product());
        }
        PRODUCTS = Collections.unmodifiableList(products);
    }

    static Background fromKey(String key) {
        if (key == null) return null;
        for (Background background : BACKGROUNDS) {
            if (background.key.equals(key)) return background;
        }
        return null;
    }

    static ShopProduct product(String productId) {
        for (ShopProduct product : PRODUCTS) {
            if (product.id.equals(productId)) return product;
        }
        return null;
    }

    private BackgroundCatalog() { }
}
