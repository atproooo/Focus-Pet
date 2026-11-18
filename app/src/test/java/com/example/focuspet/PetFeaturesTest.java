package com.example.focuspet;

import org.junit.Test;
import java.util.*;
import com.example.focuspet.model.FocusPetModels.*;
import com.example.focuspet.model.FocusPetModels.Currency;
import static org.junit.Assert.*;

public class PetFeaturesTest {
    @Test public void catalogKeepsCurrenciesSeparate() {
        Set<String> ids=new HashSet<>();
        for(ShopProduct p:ShopCatalog.PRODUCTS){assertTrue(ids.add(p.id));assertEquals(p.productType==ProductType.PET_EGG?Currency.ESSENCE:Currency.COINS,p.currency);if(p.productType==ProductType.COSMETIC)assertNotNull(ShopCatalog.cosmetic(p.itemKey));}
    }
    @Test public void interactionsAreBoundedAndRestoresEnergy() {
        assertEquals(100,PetInteraction.moodAfter(98,PetInteraction.Action.PET));
        assertEquals(95,PetInteraction.energyAfter(100,PetInteraction.Action.PLAY));
        assertEquals(100,PetInteraction.energyAfter(95,PetInteraction.Action.REST));
        assertEquals(0,PetInteraction.energyAfter(0,PetInteraction.Action.PLAY));
    }
}
