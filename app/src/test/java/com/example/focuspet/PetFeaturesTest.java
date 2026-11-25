package com.example.focuspet;

import org.junit.Test;
import java.util.*;
import com.example.focuspet.model.FocusPetModels.*;
import com.example.focuspet.model.FocusPetModels.Currency;
import static org.junit.Assert.*;

public class PetFeaturesTest {
    @Test public void catalogKeepsCurrenciesSeparate() {
        Set<String> ids=new HashSet<>();
        List<ShopProduct> products=new ArrayList<>(ShopCatalog.PRODUCTS);
        products.addAll(PetLookCatalog.PRODUCTS);
        products.addAll(PetExpressionCatalog.PRODUCTS);
        products.addAll(AccessoryCatalog.PRODUCTS);
        products.addAll(BackgroundCatalog.PRODUCTS);
        for(ShopProduct p:products){assertTrue(ids.add(p.id));assertEquals(p.productType==ProductType.PET_EGG?Currency.ESSENCE:Currency.COINS,p.currency);if(p.productType==ProductType.COSMETIC)assertTrue(ShopCatalog.cosmetic(p.itemKey)!=null||AccessoryCatalog.fromKey(p.itemKey)!=null);}
        assertEquals(7, PetLookCatalog.PRODUCTS.size());
        for(ShopProduct product:PetLookCatalog.PRODUCTS){assertEquals(ProductType.PET_LOOK,product.productType);assertEquals("pet_looks",product.categoryKey);assertEquals(67,product.price);assertNotNull(PetLook.fromKey(product.itemKey));}
        assertEquals(8, PetExpressionCatalog.PRODUCTS.size());
        for (ShopProduct product : PetExpressionCatalog.PRODUCTS) {
            assertEquals(ProductType.PET_EXPRESSION, product.productType);
            assertEquals("expressions", product.categoryKey);
            assertEquals(PetExpressionCatalog.EXPRESSION_PRICE, product.price);
            PetExpression expression = PetExpression.fromKey(product.itemKey);
            assertNotNull(expression);
            assertTrue(expression.learnable);
        }
        assertEquals(10, AccessoryCatalog.PRODUCTS.size());
        for (ShopProduct product : AccessoryCatalog.PRODUCTS) {
            assertEquals(ProductType.COSMETIC, product.productType);
            assertEquals("accessories", product.categoryKey);
            assertEquals(AccessoryCatalog.ACCESSORY_PRICE, product.price);
            assertNotNull(AccessoryCatalog.fromKey(product.itemKey));
        }
        assertEquals(14, BackgroundCatalog.PRODUCTS.size());
        for (ShopProduct product : BackgroundCatalog.PRODUCTS) {
            assertEquals(ProductType.PET_BACKGROUND, product.productType);
            assertEquals("backgrounds", product.categoryKey);
            assertEquals(BackgroundCatalog.BACKGROUND_PRICE, product.price);
            assertNotNull(BackgroundCatalog.fromKey(product.itemKey));
        }
        assertNull("Pet Eggs are focus-drop only and must not be sold", ShopCatalog.product("egg"));
    }
    @Test public void shopCategoriesIncludePetLooksAndExpressions() {
        assertArrayEquals(new ShopCategory[]{ShopCategory.ALL, ShopCategory.PET_LOOKS,
                        ShopCategory.EXPRESSIONS, ShopCategory.ACCESSORIES,
                        ShopCategory.BACKGROUNDS},
                ShopCategory.values());
        for (ShopProduct product : ShopCatalog.PRODUCTS) {
            assertFalse(ShopCategory.ALL.includes(ShopProductEntity.from(product, 0)));
        }
        for (ShopProduct product : PetLookCatalog.PRODUCTS) {
            ShopProductEntity entity = ShopProductEntity.from(product, 0);
            assertTrue(ShopCategory.ALL.includes(entity));
            assertTrue(ShopCategory.PET_LOOKS.includes(entity));
            assertFalse(ShopCategory.EXPRESSIONS.includes(entity));
        }
        for (ShopProduct product : PetExpressionCatalog.PRODUCTS) {
            ShopProductEntity entity = ShopProductEntity.from(product, 0);
            assertTrue(ShopCategory.ALL.includes(entity));
            assertTrue(ShopCategory.EXPRESSIONS.includes(entity));
            assertFalse(ShopCategory.PET_LOOKS.includes(entity));
        }
        for (ShopProduct product : AccessoryCatalog.PRODUCTS) {
            ShopProductEntity entity = ShopProductEntity.from(product, 0);
            assertTrue(ShopCategory.ALL.includes(entity));
            assertTrue(ShopCategory.ACCESSORIES.includes(entity));
            assertFalse(ShopCategory.EXPRESSIONS.includes(entity));
        }
        for (ShopProduct product : BackgroundCatalog.PRODUCTS) {
            ShopProductEntity entity = ShopProductEntity.from(product, 0);
            assertTrue(ShopCategory.ALL.includes(entity));
            assertTrue(ShopCategory.BACKGROUNDS.includes(entity));
            assertFalse(ShopCategory.ACCESSORIES.includes(entity));
        }
    }
    @Test public void interactionsAreBoundedAndRestoresEnergy() {
        assertEquals(100,PetInteraction.moodAfter(98,PetInteraction.Action.PET));
        assertEquals(95,PetInteraction.energyAfter(100,PetInteraction.Action.PLAY));
        assertEquals(100,PetInteraction.energyAfter(95,PetInteraction.Action.REST));
        assertEquals(0,PetInteraction.energyAfter(0,PetInteraction.Action.PLAY));
    }
}
