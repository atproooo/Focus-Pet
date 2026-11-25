package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.ProductType;

public enum ShopCategory {
    ALL(null, "All"),
    PET_LOOKS("pet_looks", "Pet Looks"),
    EXPRESSIONS("expressions", "Expressions"),
    ACCESSORIES("accessories", "Accessories"),
    BACKGROUNDS("backgrounds", "Backgrounds");

    final String key;
    final String label;

    ShopCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    boolean includes(ShopProductEntity product) {
        ProductType type = ProductType.valueOf(product.productType);
        boolean accessory = type == ProductType.COSMETIC
                && AccessoryCatalog.fromKey(product.itemKey) != null;
        if (this == ALL) {
            return type == ProductType.PET_LOOK || type == ProductType.PET_EXPRESSION
                    || type == ProductType.PET_BACKGROUND || accessory;
        }
        if (this == PET_LOOKS) return type == ProductType.PET_LOOK;
        if (this == EXPRESSIONS) {
            return type == ProductType.PET_EXPRESSION && key.equals(product.categoryKey);
        }
        if (this == ACCESSORIES) return accessory && key.equals(product.categoryKey);
        return type == ProductType.PET_BACKGROUND && key.equals(product.categoryKey);
    }
}
