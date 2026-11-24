package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.ProductType;

public enum ShopCategory {
    ALL(null, "All"),
    PET_LOOKS("pet_looks", "Pet Looks");

    final String key;
    final String label;

    ShopCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    boolean includes(ShopProductEntity product) {
        if (!ProductType.PET_LOOK.name().equals(product.productType)) return false;
        return this == ALL || key.equals(product.categoryKey);
    }
}
