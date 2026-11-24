package com.example.focuspet;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ShopProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ShopProductEntity product);

    @Query("SELECT * FROM shop_products WHERE available = 1 ORDER BY displayOrder, name")
    List<ShopProductEntity> availableProducts();

    @Query("SELECT * FROM shop_products WHERE id = :id LIMIT 1")
    ShopProductEntity byId(String id);

    @Query("DELETE FROM shop_products")
    void deleteAll();
}
