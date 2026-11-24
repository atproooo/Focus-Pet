package com.example.focuspet;

import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class PetExpressionCatalog {
    static final long EXPRESSION_PRICE = 50L;
    static final List<ShopProduct> PRODUCTS;

    static {
        List<ShopProduct> products = new ArrayList<>();
        for (PetExpression expression : PetExpression.LEARNABLE) {
            String label = expression.key.substring(2).replace('_', ' ');
            String name = expression.key.substring(0, 1).toUpperCase(Locale.ROOT)
                    + " · " + Character.toUpperCase(label.charAt(0)) + label.substring(1)
                    + " expression";
            products.add(new ShopProduct("expression_" + expression.key,
                    name, "expressions", ProductType.PET_EXPRESSION,
                    expression.key, "A one-use item that teaches this expression to one pet.",
                    Currency.COINS, EXPRESSION_PRICE, 1, true));
        }
        PRODUCTS = Collections.unmodifiableList(products);
    }

    static ShopProduct product(String productId) {
        if (productId == null) return null;
        for (ShopProduct product : PRODUCTS) {
            if (product.id.equals(productId)) return product;
        }
        return null;
    }

    private PetExpressionCatalog() { }
}
