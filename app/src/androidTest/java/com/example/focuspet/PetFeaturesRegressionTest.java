package com.example.focuspet;

import android.app.Activity;
import android.content.*;
import android.os.SystemClock;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PetFeaturesRegressionTest {
    Context c;Activity a;FocusDatabase db;FocusRepository repo;FocusStore store;
    Map<String,?> saved;List<Pet> pets;List<FocusSession> sessions;List<HatchDraft> drafts;
    @Before public void prepare(){
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
        db=FocusDatabase.getInstance(c);repo=new FocusRepository(c);store=new FocusStore(c);
        saved=new HashMap<>(c.getSharedPreferences("focuspet_progress",0).getAll());pets=db.petDao().all();sessions=db.focusSessionDao().history();drafts=db.hatchDraftDao().all();
        c.stopService(new Intent(c,FocusService.class));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        clear();c.getSharedPreferences("focuspet_progress",0).edit().clear().commit();
    }
    void clear(){db.runInTransaction(()->{db.focusSessionDao().deleteAll();db.petDao().deleteAll();db.hatchDraftDao().deleteAll();});}
    @After public void restore(){
        if(a!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);
        c.stopService(new Intent(c,FocusService.class));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        clear();db.runInTransaction(()->{for(Pet p:pets)db.petDao().insert(p);for(FocusSession s:sessions)db.focusSessionDao().insert(s);for(HatchDraft draft:drafts)db.hatchDraftDao().insert(draft);});
        SharedPreferences.Editor e=c.getSharedPreferences("focuspet_progress",0).edit().clear();
        for(Map.Entry<String,?> item:saved.entrySet()){String k=item.getKey();Object v=item.getValue();if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}e.commit();
    }
    void resources(int coins,int essence,int eggs){c.getSharedPreferences("focuspet_progress",0).edit().putInt("coins",coins).putInt("life_essence",essence).putInt("pet_eggs",eggs).commit();}
    void launch(){a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    void reopen(){InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);InstrumentationRegistry.getInstrumentation().waitForIdleSync();launch();}
    void hatchScreen(){onView(withText("Collection")).perform(click());onView(withId(R.id.pet_eggs_chip)).perform(click());}
    void assertResourceChipIcon(String label, int iconResource) {
        onView(withText(label)).check((view, error) -> {
            ImageView image = ((ViewGroup) view.getParent())
                    .findViewById(R.id.resource_chip_icon);
            BitmapDrawable actual = (BitmapDrawable) image.getDrawable();
            BitmapDrawable expected = (BitmapDrawable) androidx.core.content.ContextCompat
                    .getDrawable(c, iconResource);
            assertNotNull(actual);
            assertNotNull(expected);
            assertTrue(actual.getBitmap().sameAs(expected.getBitmap()));
        });
    }
    Pet addPet(String name){Pet pet=new Pet();pet.templateId="test_pet";pet.name=name;pet.species="Pet";pet.rarity="Common";pet.imageKey=PetLook.CLASSIC.key;pet.appearanceData=PetLook.CLASSIC.key;pet.description="Test companion";pet.hatchedAt=System.currentTimeMillis();pet.mood=100;pet.energy=100;pet.id=db.petDao().insert(pet);return repo.pet(pet.id);}
    HatchDraft readyDraft(String requestId,String name,String look){HatchDraft draft=new HatchDraft();draft.id=requestId;draft.name=name;draft.appearanceKey=look;draft.step=HatchDraft.STEP_PREVIEW;draft.createdAt=System.currentTimeMillis();db.hatchDraftDao().insert(draft);return draft;}
    void collect(View v,int id,List<View> result){if(v.getId()==id)result.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),id,result);}
    View view(int id){List<View> result=new ArrayList<>();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->collect(a.getWindow().getDecorView(),id,result));assertEquals(1,result.size());return result.get(0);}
    int topOfTaggedView(String tag){int[] location=new int[2];onView(withTagValue(is((Object)tag))).check((target,error)->target.getLocationOnScreen(location));return location[1];}

    @Test public void starterUsesPetNameAndClassicArtworkAndMigratesLegacyDefaults() {
        Pet legacy = new Pet();
        legacy.templateId = "starter_fox";
        legacy.name = "Mochi";
        legacy.species = "Fox";
        legacy.rarity = "Common";
        legacy.imageKey = "pet_fox";
        legacy.appearanceData = "default_fox";
        legacy.hatchedAt = System.currentTimeMillis();
        legacy.mood = 100;
        legacy.energy = 100;
        legacy.selected = true;
        db.petDao().insert(legacy);

        Pet starter = repo.ensureStarterPet();
        assertEquals("Toast", starter.name);
        assertEquals("classic_cat", starter.appearanceData);
        assertEquals("classic_cat", starter.imageKey);
        assertEquals(0, starter.experience);

        launch();
        onView(withId(R.id.pet_name)).check(matches(withText("Toast")));
        checkPetArtwork(R.drawable.pet_look_classic);
        onView(withText("Collection")).perform(click());
        onView(withId(R.id.pet_name)).check(matches(withText("Toast")));

        Pet companion = addPet("Second");
        assertEquals(0, companion.experience);
        assertEquals(100, companion.mood);
        assertEquals(100, companion.energy);
        assertFalse(companion.selected);
    }

    @Test public void collectionResourcesShowTheirDrawableIcons() {
        resources(12, 34, 5);
        launch();
        onView(withText("Collection")).perform(click());
        assertResourceChipIcon("Coins 12", R.drawable.icon_coin);
        assertResourceChipIcon("Essence 34", R.drawable.icon_essence);
        assertResourceChipIcon("Eggs 5", R.drawable.icon_eggs);
    }

    @Test public void hatchRequiresNameAndAppearanceAndAddsPetOnlyAfterConfirmation() {
        resources(0, 100, 1);
        launch();
        hatchScreen();
        onView(withId(R.id.hatch_button)).perform(scrollTo(), click());
        onView(withId(R.id.hatch_name_input)).perform(typeText("Mallow"));
        onView(withText("Continue")).perform(scrollTo(), click());
        assertEquals(1, repo.pets().size());
        assertEquals(1, store.petEggs());
        assertEquals(100, store.lifeEssence());
        onView(withText(R.string.pet_look_headphones)).perform(scrollTo(), click());
        onView(withText("Preview pet")).perform(scrollTo(), click());
        onView(withId(R.id.hatch_preview_name)).check(matches(withText("Mallow")));
        onView(withId(R.id.pet_animation_image))
                .check((view, error) -> assertPetArtwork(view, R.drawable.pet_look_headphones));
        assertEquals(1, repo.pets().size());
        assertEquals(1, store.petEggs());
        assertEquals(100, store.lifeEssence());

        onView(withId(R.id.hatching_back)).perform(click());
        onView(withId(R.id.hatching_back)).perform(click());
        onView(withId(R.id.hatch_name_input)).check(matches(withText("Mallow")));
        onView(withText("Continue")).perform(scrollTo(), click());
        onView(withText(R.string.pet_look_headphones)).perform(scrollTo(), click());
        onView(withText("Preview pet")).perform(scrollTo(), click());
        onView(withText("Confirm and add to collection")).perform(scrollTo(), click());
        long petId = store.hatchResultPetId();
        assertEquals(0, store.petEggs());
        assertEquals(0, store.lifeEssence());
        assertEquals("Mallow", repo.pet(petId).name);
        assertEquals("headphones_cat", repo.pet(petId).appearanceData);
        assertEquals(2, repo.pets().size());

        reopen();
        onView(withText("Hatched successfully!")).check(matches(isDisplayed()));
        onView(withText("Mallow has been added to your Collection.")).check(matches(isDisplayed()));
        onView(withText("OK")).perform(click());
        assertEquals(0, store.hatchResultPetId());
        assertEquals("headphones_cat", repo.pet(petId).appearanceData);
        onView(withText("Pet Collection")).check(matches(isDisplayed()));
        onView(withText("Mallow")).check(matches(isDisplayed()));
    }

    @Test public void hatchDraftResumesAndSystemBackCannotExitBeforeConfirmation() {
        resources(0, 100, 1);
        launch();
        hatchScreen();
        onView(withId(R.id.hatch_button)).perform(scrollTo(), click());
        onView(withId(R.id.hatch_name_input)).perform(typeText("Cloud"));
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withId(R.id.hatch_name_input)).check(matches(isDisplayed()));

        reopen();
        onView(withId(R.id.hatch_name_input)).check(matches(withText("Cloud")));
        onView(withText("Continue")).perform(scrollTo(), click());
        onView(withText(R.string.pet_look_winter)).perform(scrollTo(), click());
        onView(withText("Preview pet")).perform(scrollTo(), click());
        reopen();
        onView(withId(R.id.hatch_preview_name)).check(matches(withText("Cloud")));
        assertEquals(1, repo.pets().size());
        assertEquals(1, store.petEggs());
        assertEquals(100, store.lifeEssence());
    }

    @Test public void shopPurchasesPetLookAndUnlocksItForHatching() {
        resources(67, 100, 1);
        launch();
        onView(withText("Collection")).perform(click());
        onView(withId(R.id.open_shop)).perform(scrollTo(), click());
        assertEquals(10, ShopRepository.getInstance(c).availableProducts().size());
        onView(withText("Pet Looks")).perform(click());
        onView(withTagValue(is((Object) "buy_look_mint_star"))).perform(scrollTo(), click());
        onView(withText("Unlock Mint Star?")).check(matches(isDisplayed()));
        onView(withText("A mint-green look with a bright star.")).check(doesNotExist());
        onView(withTagValue(is((Object) "pet_look_dialog_art_mint_star")))
                .check(matches(withContentDescription("Unrevealed pet appearance")));
        onView(withText("Buy")).perform(click());
        onView(withTagValue(is((Object) "buy_look_mint_star"))).check(matches(withText("Owned")));
        assertTrue(store.isPetLookUnlocked("mint_star"));
        onView(withTagValue(is((Object) "pet_look_art_mint_star")))
                .check(matches(withContentDescription("Mint Star")));
        assertTrue(topOfTaggedView("buy_look_mint_star")
                > topOfTaggedView("buy_look_cloud_wings"));
        assertEquals(FocusStore.PurchaseResult.ALREADY_OWNED,
                store.purchase("look_mint_star", "duplicate-look-purchase"));
        assertEquals(0, store.coins());

        onView(withText("Back to collection")).perform(scrollTo(), click());
        onView(withId(R.id.pet_eggs_chip)).perform(click());
        onView(withId(R.id.hatch_button)).perform(scrollTo(), click());
        onView(withId(R.id.hatch_name_input)).perform(typeText("Minty"));
        onView(withText("Continue")).perform(scrollTo(), click());
        onView(withText("Mint Star")).check(matches(isDisplayed()));
    }

    @Test public void invalidHatchNamesKeepTheFlowAtTheNameStep() {
        resources(0, 100, 1);
        launch();
        hatchScreen();
        onView(withId(R.id.hatch_button)).perform(scrollTo(), click());
        onView(withId(R.id.hatch_name_input)).perform(typeText("   "));
        onView(withText("Continue")).perform(scrollTo(), click());
        onView(withId(R.id.hatch_name_input)).check(matches(hasErrorText("Enter a name for your pet")));
        assertEquals(1, repo.pets().size());
        assertEquals(1, store.petEggs());
        assertEquals(100, store.lifeEssence());
    }

    private void checkPetArtwork(int expectedResource) {
        onView(withId(R.id.pet_animation_image))
                .check((view, error) -> assertPetArtwork(view, expectedResource));
    }

    private void assertPetArtwork(View view, int expectedResource) {
        assertNotNull(view);
        ImageView image = (ImageView) view;
        assertEquals(View.VISIBLE, image.getVisibility());
        android.graphics.drawable.BitmapDrawable actual =
                (android.graphics.drawable.BitmapDrawable) image.getDrawable();
        android.graphics.drawable.BitmapDrawable expected =
                (android.graphics.drawable.BitmapDrawable) c.getDrawable(expectedResource);
        assertNotNull(actual);
        assertTrue(expected.getBitmap().sameAs(actual.getBitmap()));
    }
    @Test public void purchaseCurrenciesOwnershipAndIdempotency(){
        resources(100,200,0);assertEquals(FocusStore.PurchaseResult.PURCHASED,store.purchase("collar","c1"));assertEquals(75,store.coins());assertEquals(200,store.lifeEssence());
        assertEquals(FocusStore.PurchaseResult.PURCHASED,store.purchase("shirt","s1"));assertEquals(25,store.coins());assertEquals(200,store.lifeEssence());
        assertEquals(FocusStore.PurchaseResult.PURCHASED,new FocusStore(c).purchase("collar","c1"));assertEquals(25,store.coins());
        assertEquals(FocusStore.PurchaseResult.ALREADY_OWNED,store.purchase("collar","c2"));assertEquals(25,store.coins());
        assertEquals(FocusStore.PurchaseResult.INVALID_REQUEST,store.purchase("shirt","c1"));assertEquals(25,store.coins());assertTrue(new FocusStore(c).ownedCosmeticIds().contains("collar"));
        assertEquals(FocusStore.PurchaseResult.INVALID_PRODUCT,store.purchase("egg","e1"));assertEquals(0,store.petEggs());
    }
    @Test public void insufficientAndInvalidPurchasesDoNotChangeInventory(){assertEquals(FocusStore.PurchaseResult.INSUFFICIENT_RESOURCES,store.purchase("collar","c"));assertEquals(FocusStore.PurchaseResult.INVALID_PRODUCT,store.purchase("egg","e"));assertEquals(FocusStore.PurchaseResult.INVALID_PRODUCT,store.purchase("missing","x"));assertEquals(FocusStore.PurchaseResult.INVALID_REQUEST,store.purchase("collar",""));assertEquals(0,store.petEggs());assertTrue(store.ownedCosmeticIds().isEmpty());}
    @Test public void concurrentPurchasesCannotOverspend() throws Exception {
        resources(25,0,0);ExecutorService executor=Executors.newFixedThreadPool(2);
        try{Future<FocusStore.PurchaseResult> one=executor.submit(()->new FocusStore(c).purchase("collar","one"));Future<FocusStore.PurchaseResult> two=executor.submit(()->new FocusStore(c).purchase("collar","two"));Set<FocusStore.PurchaseResult> results=new HashSet<>(Arrays.asList(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS)));assertEquals("exactly one purchase must succeed",1,Collections.frequency(results,FocusStore.PurchaseResult.PURCHASED));assertTrue("the losing thread is rejected as already owned",results.contains(FocusStore.PurchaseResult.ALREADY_OWNED));assertEquals(0,store.coins());assertTrue(store.ownedCosmeticIds().contains("collar"));}finally{executor.shutdownNow();}
    }
    @Test public void cosmeticEquipRequiresOwnershipAndIsPerPet(){Pet first=repo.ensureStarterPet();Pet second=addPet("Second");assertFalse(repo.equipCosmetic(first.id,"collar"));resources(100,0,0);store.purchase("collar","c");assertTrue(repo.equipCosmetic(first.id,"collar"));assertTrue(repo.equipCosmetic(second.id,"collar"));assertEquals("collar",new FocusStore(c).equippedItemId(first.id,"accessory"));store.unequip(first.id,"accessory");assertNull(store.equippedItemId(first.id,"accessory"));assertEquals("collar",store.equippedItemId(second.id,"accessory"));assertFalse(repo.equipCosmetic(99999,"collar"));}
    @Test public void hatchCostsOnlyAtConfirmationAndDuplicateConfirmationDoesNotDuplicatePet(){
        repo.ensureStarterPet();resources(7,99,1);assertNull(repo.startHatchDraft());
        assertEquals(99,store.lifeEssence());assertEquals(1,store.petEggs());
        resources(7,100,1);HatchDraft draft=repo.startHatchDraft();
        repo.advanceHatchName(draft.id,"Toast");repo.updateHatchAppearance(draft.id,PetLook.CLASSIC.key);repo.advanceHatchAppearance(draft.id);
        assertEquals(1,repo.pets().size());assertEquals(100,store.lifeEssence());assertEquals(1,store.petEggs());
        Pet pet=repo.confirmHatch(draft.id);assertNotNull(pet);assertEquals("Toast",pet.name);
        assertEquals(7,store.coins());assertEquals(0,store.lifeEssence());assertEquals(0,store.petEggs());
        assertEquals(2,repo.pets().size());assertEquals(pet.id,repo.confirmHatch(draft.id).id);
        assertEquals(2,repo.pets().size());assertEquals(pet.id,store.hatchResultPetId());
    }
    @Test public void pendingHatchRecoversAfterConfirmationBeforePetInsert(){
        repo.ensureStarterPet();resources(0,100,1);HatchDraft draft=readyDraft("interrupted","Sprout",PetLook.WINTER.key);
        store.beginHatch(draft.id);assertEquals(0,store.petEggs());
        FocusRepository recovered=new FocusRepository(c);Pet pet=recovered.recoverPendingHatch();
        assertNotNull(pet);assertEquals(2,recovered.pets().size());assertNull(store.pendingHatch());
        assertEquals(0,store.lifeEssence());assertEquals("interrupted",pet.hatchRequestId);
        assertEquals("Sprout",pet.name);assertEquals(PetLook.WINTER.key,pet.appearanceData);
    }
    @Test public void pendingHatchRecoversAfterPetInsertBeforePreferenceFinish(){
        repo.ensureStarterPet();resources(0,100,1);HatchDraft draft=readyDraft("inserted","Recovered",PetLook.HEADPHONES.key);
        store.beginHatch(draft.id);Pet p=new Pet();p.name=draft.name;p.templateId="hatched_pet";
        p.hatchRequestId=draft.id;p.appearanceData=draft.appearanceKey;p.imageKey=draft.appearanceKey;
        p.species="Pet";p.rarity="Common";p.hatchedAt=draft.createdAt;p.mood=100;p.energy=100;
        long id=db.petDao().insert(p);db.hatchDraftDao().markCompleted(draft.id);
        FocusRepository recovered=new FocusRepository(c);assertEquals(id,recovered.recoverPendingHatch().id);
        assertEquals(2,recovered.pets().size());assertEquals(id,store.hatchResultPetId());
        assertEquals(0,store.lifeEssence());assertNull(store.pendingHatch());
    }
    @Test public void legacyChargedHatchResumesCustomizationWithoutChargingAgain(){
        repo.ensureStarterPet();resources(0,100,1);store.beginHatch("legacy-pending");
        resources(0,0,0);
        FocusRepository recovered=new FocusRepository(c);assertNull(recovered.recoverPendingHatch());
        HatchDraft draft=recovered.activeHatchDraft();assertNotNull(draft);
        assertEquals(HatchDraft.STEP_NAME,draft.step);assertEquals(1,recovered.pets().size());
        recovered.advanceHatchName(draft.id,"Legacy Toast");
        recovered.advanceHatchAppearance(draft.id);
        Pet pet=recovered.confirmHatch(draft.id);assertEquals("Legacy Toast",pet.name);
        assertEquals(0,store.petEggs());assertEquals(0,store.lifeEssence());
        assertEquals(2,recovered.pets().size());
    }
    @Test public void interactionsPersistClampAndThrottle(){Pet p=repo.ensureStarterPet();long at=System.currentTimeMillis();assertEquals(FocusRepository.InteractionResult.UPDATED,repo.interact(p.id,PetInteraction.Action.PLAY,at));assertEquals(95,repo.pet(p.id).energy);assertEquals(100,repo.pet(p.id).mood);assertEquals(FocusRepository.InteractionResult.COOLDOWN,repo.interact(p.id,PetInteraction.Action.PET,at+1));assertEquals(FocusRepository.InteractionResult.UPDATED,repo.interact(p.id,PetInteraction.Action.REST,at+10000));assertEquals(100,new FocusRepository(c).pet(p.id).energy);db.petDao().updateInteraction(p.id,40,4,0);assertEquals(FocusRepository.InteractionResult.LOW_ENERGY,repo.interact(p.id,PetInteraction.Action.PLAY,at+20000));assertEquals(4,repo.pet(p.id).energy);assertEquals(0,store.coins());assertEquals(0,store.experience());}
    @Test public void hatchButtonDisabledGrayAndEnabledAtExactCost(){resources(0,100,0);launch();hatchScreen();onView(withId(R.id.hatch_button)).check(matches(not(isEnabled())));resources(0,99,1);onView(withId(R.id.hatching_back)).perform(scrollTo(),click());onView(withId(R.id.pet_eggs_chip)).perform(click());onView(withId(R.id.hatch_button)).check(matches(not(isEnabled())));Button b=(Button)view(R.id.hatch_button);assertEquals(Color.rgb(105,105,105),b.getCurrentTextColor());assertEquals(Color.rgb(218,218,218),b.getBackgroundTintList().getDefaultColor());resources(0,100,1);onView(withId(R.id.hatching_back)).perform(scrollTo(),click());onView(withId(R.id.pet_eggs_chip)).perform(click());onView(withId(R.id.hatch_button)).check(matches(isEnabled()));}
    @Test public void shopShowsOnlyPetLooksAndCoinBalance() {
        resources(100, 200, 0);
        launch();
        onView(withText("Collection")).perform(click());
        onView(withId(R.id.open_shop)).perform(click());
        onView(withId(R.id.shop_balance)).check(matches(withText("Coins: 100")));
        onView(withId(R.id.shop_balance_icon)).check(matches(isDisplayed()));
        onView(withText("Essence: 200")).check(doesNotExist());
        onView(withText("Accessories")).check(doesNotExist());
        onView(withText("Clothing")).check(doesNotExist());
        onView(withText("Decorations")).check(doesNotExist());
        onView(withText("A soft, winged pet look.")).check(doesNotExist());
        onView(withTagValue(is((Object) "buy_collar"))).check(doesNotExist());
        onView(withTagValue(is((Object) "buy_look_mint_star"))).check(matches(isDisplayed()));
        onView(withTagValue(is((Object) "pet_look_art_mint_star")))
                .check(matches(withContentDescription("Unrevealed pet appearance")));
    }

    @Test public void cancelledPurchaseDoesNotCharge() {
        resources(67, 0, 0);
        launch();
        onView(withText("Collection")).perform(click());
        onView(withId(R.id.open_shop)).perform(click());
        onView(withTagValue(is((Object) "buy_look_mint_star"))).perform(scrollTo(), click());
        onView(withText("Cancel")).perform(click());
        assertEquals(67, store.coins());
        assertTrue(store.ownedPetLookKeys().isEmpty());
    }
    @Test public void petDetailHasWorkingInteractions(){launch();onView(withText("Collection")).perform(click());onView(withId(R.id.pet_card)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_play)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_feedback)).perform(scrollTo()).check(matches(withText(containsString("enjoyed playing"))));assertEquals(95,repo.selectedPet().energy);onView(withId(R.id.pet_interaction_rest)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_feedback)).perform(scrollTo()).check(matches(withText(containsString("wait"))));}
    @Test public void focusAnimationUsesSelectedPetAndEndingNeedsOneConfirmation(){Pet p=repo.ensureStarterPet();Pet second=addPet("Second");repo.selectPet(second.id);launch();onView(withText("Start focus")).perform(scrollTo(),click());onView(withText("Begin session")).perform(scrollTo(),click());SystemClock.sleep(1200);assertEquals(second.id,((Long)view(R.id.pet_animation_container).getTag()).longValue());assertEquals(second.imageKey,view(R.id.pet_animation_image).getTag());long id=store.activeSessionId();store.saveActiveSessionState(SystemClock.elapsedRealtime()+20*60000L,25,false,20*60000L);c.startService(new Intent(c,FocusService.class).setAction(FocusService.ACTION_RESTORE));SystemClock.sleep(300);onView(withText("End session")).perform(scrollTo(),click());onView(withText("Yes, end session")).perform(click());SystemClock.sleep(300);assertFalse(store.hasActiveSession());assertEquals(FocusSession.STATUS_COMPLETED,repo.pendingSession(id).status);assertEquals(5,repo.pendingSession(id).actualMinutes);onView(withText("Continue to claim")).perform(scrollTo()).check(matches(isDisplayed()));}
    @Test public void resetClearsPurchasesEquipmentAndResources(){resources(25,0,0);Pet p=repo.ensureStarterPet();store.purchase("collar","c");repo.equipCosmetic(p.id,"collar");repo.clearAllData();assertTrue(store.ownedCosmeticIds().isEmpty());assertNull(store.equippedItemId(p.id,"accessory"));assertEquals(0,store.coins());assertTrue(repo.pets().isEmpty());}

    @Test public void hatchWithFocusDroppedEggChargesExplicitCosts(){repo.ensureStarterPet();resources(17,100,1);assertEquals(FocusStore.PurchaseResult.INVALID_PRODUCT,store.purchase("egg","no-egg-sale"));HatchDraft draft=repo.startHatchDraft();repo.advanceHatchName(draft.id,"Focus");repo.advanceHatchAppearance(draft.id);assertNotNull(repo.confirmHatch(draft.id));assertEquals(0,store.lifeEssence());assertEquals(0,store.petEggs());assertEquals(17,store.coins());assertEquals(2,repo.pets().size());}
}
