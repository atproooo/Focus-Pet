package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.ProductType;

public enum ShopCategory {
    ALL(null, "All"),
    PET_LOOKS("pet_looks", "Pet Looks"),
    EXPRESSIONS("expressions", "Expressions");

    final String key;
    final String label;

    ShopCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    boolean includes(ShopProductEntity product) {
        ProductType type = ProductType.valueOf(product.productType);
        if (this == ALL) {
            return type == ProductType.PET_LOOK || type == ProductType.PET_EXPRESSION;
        }
        if (this == PET_LOOKS) return type == ProductType.PET_LOOK;
        return type == ProductType.PET_EXPRESSION && key.equals(product.categoryKey);
    }
}
