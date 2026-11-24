package com.example.focuspet;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.focuspet.model.FocusPetModels.Currency;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.ShopProduct;

@Entity(tableName = "shop_products")
public class ShopProductEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String name;
    @NonNull
    public String categoryKey;
    @NonNull
    public String productType;
    @NonNull
    public String itemKey;
    @NonNull
    public String description;
    @NonNull
    public String currency;
    public long price;
    public int quantityGranted;
    public boolean available;
    public int displayOrder;

    public ShopProductEntity(@NonNull String id, @NonNull String name,
                             @NonNull String categoryKey, @NonNull String productType,
                             @NonNull String itemKey, @NonNull String description,
                             @NonNull String currency, long price, int quantityGranted,
                             boolean available, int displayOrder) {
        this.id = id;
        this.name = name;
        this.categoryKey = categoryKey;
        this.productType = productType;
        this.itemKey = itemKey;
        this.description = description;
        this.currency = currency;
        this.price = price;
        this.quantityGranted = quantityGranted;
        this.available = available;
        this.displayOrder = displayOrder;
    }

    static ShopProductEntity from(ShopProduct product, int displayOrder) {
        return new ShopProductEntity(product.id, product.name, product.categoryKey,
                product.productType.name(), product.itemKey, product.description,
                product.currency.name(), product.price, product.quantityGranted,
                product.available, displayOrder);
    }

    ShopProduct toProduct() {
        return new ShopProduct(id, name, categoryKey, ProductType.valueOf(productType),
                itemKey, description, Currency.valueOf(currency), price, quantityGranted, available);
    }
}
