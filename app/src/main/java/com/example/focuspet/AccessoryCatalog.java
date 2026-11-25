package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.CosmeticItem;
import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AccessoryCatalog {
    static final int ACCESSORY_PRICE = 50;

    enum Layer { BEHIND, IN_FRONT }

    static final class Accessory {
        final String key;
        final String name;
        final String description;
        final int drawable;
        final Layer layer;
        final float centerX;
        final float centerY;
        final float widthFraction;
        final float contentLeft;
        final float contentTop;
        final float contentRight;
        final float contentBottom;

        Accessory(String key, String name, int drawable, Layer layer,
                  float centerX, float centerY, float widthFraction,
                  float contentLeft, float contentTop, float contentRight, float contentBottom) {
            this.key = key;
            this.name = name;
            this.description = "A wearable decoration for your pet.";
            this.drawable = drawable;
            this.layer = layer;
            this.centerX = centerX;
            this.centerY = centerY;
            this.widthFraction = widthFraction;
            this.contentLeft = contentLeft;
            this.contentTop = contentTop;
            this.contentRight = contentRight;
            this.contentBottom = contentBottom;
        }

        CosmeticItem cosmeticItem() {
            return new CosmeticItem(key, name, "accessories", "accessory",
                    description, key, true);
        }

        ShopProduct product() {
            return new ShopProduct("accessory_" + key, name, "accessories",
                    ProductType.COSMETIC, key, description, Currency.COINS,
                    ACCESSORY_PRICE, 1, true);
        }
    }

    static final List<Accessory> ACCESSORIES;
    static final List<CosmeticItem> COSMETICS;
    static final List<ShopProduct> PRODUCTS;

    static {
        List<Accessory> accessories = new ArrayList<>();
        accessories.add(new Accessory("wings", "Mint Star Wings", R.drawable.accessory_wings,
                Layer.IN_FRONT, 0.50f, 0.76f, 0.84f,
                0.014f, 0.313f, 0.984f, 0.723f));
        accessories.add(new Accessory("white_wing", "Cloud Wing", R.drawable.accessory_white_wing,
                Layer.IN_FRONT, 0.81f, 0.78f, 0.40f,
                0.118f, 0.270f, 0.883f, 0.730f));
        accessories.add(new Accessory("waterdrop", "Waterdrop", R.drawable.accessory_waterdrop,
                Layer.IN_FRONT, 0.90f, 0.30f, 0.18f,
                0.348f, 0.129f, 0.630f, 0.888f));
        accessories.add(new Accessory("tooth", "Pierced Tooth", R.drawable.accessory_tooth,
                Layer.IN_FRONT, 0.20f, 0.78f, 0.32f,
                0.302f, 0.263f, 0.700f, 0.739f));
        accessories.add(new Accessory("tape", "Polka-dot Tape", R.drawable.accessory_tape,
                Layer.IN_FRONT, 0.50f, 0.56f, 1.0f,
                0.005f, 0.005f, 0.996f, 0.989f));
        accessories.add(new Accessory("rain", "Rain Drops", R.drawable.accessory_rain,
                Layer.IN_FRONT, 0.50f, 0.57f, 0.72f,
                0.197f, 0.085f, 0.805f, 0.917f));
        accessories.add(new Accessory("flower", "Flower", R.drawable.accessory_flower,
                Layer.IN_FRONT, 0.25f, 0.78f, 0.44f,
                0f, 0f, 1f, 1f));
        accessories.add(new Accessory("eyes", "Googly Eyes", R.drawable.accessory_eyes,
                Layer.IN_FRONT, 0.50f, 0.34f, 0.74f,
                0.064f, 0.322f, 0.951f, 0.681f));
        accessories.add(new Accessory("exploding", "Color Burst", R.drawable.accessory_exploding,
                Layer.IN_FRONT, 0.50f, 0.80f, 0.72f,
                0f, 0f, 1f, 1f));
        accessories.add(new Accessory("apple_star", "Starry Apple", R.drawable.accessory_apple_star,
                Layer.IN_FRONT, 0.78f, 0.78f, 0.30f,
                0.201f, 0.256f, 0.762f, 0.709f));
        ACCESSORIES = Collections.unmodifiableList(accessories);

        List<CosmeticItem> cosmetics = new ArrayList<>();
        List<ShopProduct> products = new ArrayList<>();
        for (Accessory accessory : ACCESSORIES) {
            cosmetics.add(accessory.cosmeticItem());
            products.add(accessory.product());
        }
        COSMETICS = Collections.unmodifiableList(cosmetics);
        PRODUCTS = Collections.unmodifiableList(products);
    }

    static Accessory fromKey(String key) {
        for (Accessory accessory : ACCESSORIES) {
            if (accessory.key.equals(key)) return accessory;
        }
        return null;
    }

    static Accessory fromProductId(String productId) {
        for (Accessory accessory : ACCESSORIES) {
            if (("accessory_" + accessory.key).equals(productId)) return accessory;
        }
        return null;
    }

    private AccessoryCatalog() { }
}
