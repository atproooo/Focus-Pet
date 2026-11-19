package com.example.focuspet;

import android.app.Activity;
import android.content.*;
import android.os.SystemClock;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
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
    Map<String,?> saved;List<Pet> pets;List<FocusSession> sessions;
    @Before public void prepare(){
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
        db=FocusDatabase.getInstance(c);repo=new FocusRepository(c);store=new FocusStore(c);
        saved=new HashMap<>(c.getSharedPreferences("focuspet_progress",0).getAll());pets=db.petDao().all();sessions=db.focusSessionDao().history();
        c.stopService(new Intent(c,FocusService.class));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        clear();c.getSharedPreferences("focuspet_progress",0).edit().clear().commit();
    }
    void clear(){db.runInTransaction(()->{db.focusSessionDao().deleteAll();db.petDao().deleteAll();});}
    @After public void restore(){
        if(a!=null)InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);
        c.stopService(new Intent(c,FocusService.class));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        clear();db.runInTransaction(()->{for(Pet p:pets)db.petDao().insert(p);for(FocusSession s:sessions)db.focusSessionDao().insert(s);});
        SharedPreferences.Editor e=c.getSharedPreferences("focuspet_progress",0).edit().clear();
        for(Map.Entry<String,?> item:saved.entrySet()){String k=item.getKey();Object v=item.getValue();if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}e.commit();
    }
    void resources(int coins,int essence,int eggs){c.getSharedPreferences("focuspet_progress",0).edit().putInt("coins",coins).putInt("life_essence",essence).putInt("pet_eggs",eggs).commit();}
    void launch(){a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    void reopen(){InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);InstrumentationRegistry.getInstrumentation().waitForIdleSync();launch();}
    void hatchScreen(){onView(withText("Collection")).perform(click());onView(withText(containsString("Eggs"))).perform(click());}
    void collect(View v,int id,List<View> result){if(v.getId()==id)result.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),id,result);}
    View view(int id){List<View> result=new ArrayList<>();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->collect(a.getWindow().getDecorView(),id,result));assertEquals(1,result.size());return result.get(0);}

    @Test public void hatchAppearancePreviewsCancelsAndPersistsAcrossScreens() {
        resources(0, 100, 1);
        launch();
        hatchScreen();
        onView(withId(R.id.hatch_button)).perform(scrollTo(), click());
        long petId = store.hatchResultPetId();
        assertEquals(0, store.petEggs());
        assertEquals(0, store.lifeEssence());
        checkPetArtwork(R.drawable.pet_look_classic);

        onView(withText(R.string.pet_look_choose)).perform(scrollTo(), click());
        onView(withText(R.string.pet_look_headphones)).perform(scrollTo(), click());
        onView(allOf(withId(R.id.pet_animation_image), isDescendantOfA(withId(R.id.pet_look_picker))))
                .check((view, error) -> assertPetArtwork(view, R.drawable.pet_look_headphones));
        assertEquals("default", repo.pet(petId).appearanceData);
        onView(withText(R.string.pet_look_save)).perform(click());
        assertEquals("headphones_cat", repo.pet(petId).appearanceData);
        checkPetArtwork(R.drawable.pet_look_headphones);

        onView(withText(R.string.pet_look_choose)).perform(scrollTo(), click());
        onView(withText(R.string.pet_look_winter)).perform(scrollTo(), click());
        onView(withText(android.R.string.cancel)).perform(click());
        assertEquals("headphones_cat", repo.pet(petId).appearanceData);
        checkPetArtwork(R.drawable.pet_look_headphones);

        reopen();
        checkPetArtwork(R.drawable.pet_look_headphones);
        onView(withId(R.id.hatch_result_select)).perform(scrollTo(), click());
        checkPetArtwork(R.drawable.pet_look_headphones);
        onView(withText("Start focus")).perform(scrollTo(), click());
        onView(withText("Begin session")).perform(scrollTo(), click());
        checkPetArtwork(R.drawable.pet_look_headphones);
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
    @Test public void cosmeticEquipRequiresOwnershipAndIsPerPet(){Pet first=repo.ensureStarterPet();Pet second=repo.hatchDefaultPet();assertFalse(repo.equipCosmetic(first.id,"collar"));resources(100,0,0);store.purchase("collar","c");assertTrue(repo.equipCosmetic(first.id,"collar"));assertTrue(repo.equipCosmetic(second.id,"collar"));assertEquals("collar",new FocusStore(c).equippedItemId(first.id,"accessory"));store.unequip(first.id,"accessory");assertNull(store.equippedItemId(first.id,"accessory"));assertEquals("collar",store.equippedItemId(second.id,"accessory"));assertFalse(repo.equipCosmetic(99999,"collar"));}
    @Test public void hatchCostsAndDuplicateRequest(){repo.ensureStarterPet();resources(7,99,1);assertNull(repo.hatchPet("h1"));assertEquals(99,store.lifeEssence());resources(7,100,1);Pet pet=repo.hatchPet("h1");assertNotNull(pet);assertEquals(7,store.coins());assertEquals(0,store.lifeEssence());assertEquals(0,store.petEggs());assertEquals(2,repo.pets().size());assertEquals(pet.id,repo.hatchPet("h1").id);assertEquals(2,repo.pets().size());assertEquals(pet.id,store.hatchResultPetId());}
    @Test public void pendingHatchRecoversBeforePetInsert(){repo.ensureStarterPet();resources(0,100,1);store.beginHatch("interrupted");assertEquals(0,store.petEggs());FocusRepository recovered=new FocusRepository(c);assertEquals(2,recovered.pets().size());assertNull(store.pendingHatch());assertEquals(0,store.lifeEssence());assertEquals("interrupted",recovered.pet(store.hatchResultPetId()).hatchRequestId);}
    @Test public void pendingHatchRecoversAfterPetInsert(){repo.ensureStarterPet();resources(0,100,1);store.beginHatch("inserted");Pet p=new Pet();p.name="Recovered";p.hatchRequestId="inserted";long id=db.petDao().insert(p);FocusRepository recovered=new FocusRepository(c);assertEquals(2,recovered.pets().size());assertEquals(id,store.hatchResultPetId());assertEquals(0,store.lifeEssence());assertNull(store.pendingHatch());}
    @Test public void interactionsPersistClampAndThrottle(){Pet p=repo.ensureStarterPet();long at=System.currentTimeMillis();assertEquals(FocusRepository.InteractionResult.UPDATED,repo.interact(p.id,PetInteraction.Action.PLAY,at));assertEquals(95,repo.pet(p.id).energy);assertEquals(100,repo.pet(p.id).mood);assertEquals(FocusRepository.InteractionResult.COOLDOWN,repo.interact(p.id,PetInteraction.Action.PET,at+1));assertEquals(FocusRepository.InteractionResult.UPDATED,repo.interact(p.id,PetInteraction.Action.REST,at+10000));assertEquals(100,new FocusRepository(c).pet(p.id).energy);db.petDao().updateInteraction(p.id,40,4,0);assertEquals(FocusRepository.InteractionResult.LOW_ENERGY,repo.interact(p.id,PetInteraction.Action.PLAY,at+20000));assertEquals(4,repo.pet(p.id).energy);assertEquals(0,store.coins());assertEquals(0,store.experience());}
    @Test public void hatchButtonDisabledGrayAndEnabledAtExactCost(){resources(0,100,0);launch();hatchScreen();onView(withId(R.id.hatch_button)).check(matches(not(isEnabled())));resources(0,99,1);onView(withId(R.id.hatching_back)).perform(scrollTo(),click());onView(withText(containsString("Eggs"))).perform(click());onView(withId(R.id.hatch_button)).check(matches(not(isEnabled())));Button b=(Button)view(R.id.hatch_button);assertEquals(Color.rgb(105,105,105),b.getCurrentTextColor());assertEquals(Color.rgb(218,218,218),b.getBackgroundTintList().getDefaultColor());resources(0,100,1);onView(withId(R.id.hatching_back)).perform(scrollTo(),click());onView(withText(containsString("Eggs"))).perform(click());onView(withId(R.id.hatch_button)).check(matches(isEnabled()));}
    @Test public void hatchShowsPersistentResultAndCanChooseCompanion(){resources(0,100,1);launch();hatchScreen();onView(withId(R.id.hatch_button)).perform(scrollTo(),click());onView(withId(R.id.hatch_result_name)).check(matches(withText("Freshman the Cat")));long id=store.hatchResultPetId();reopen();onView(withId(R.id.hatch_result_name)).check(matches(withText("Freshman the Cat")));onView(withId(R.id.hatch_result_select)).perform(scrollTo(),click());assertEquals(id,repo.selectedPet().id);assertEquals(0,store.hatchResultPetId());}
    @Test public void shopPurchaseAndWardrobeUi(){resources(25,200,0);launch();onView(withText("Collection")).perform(click());onView(withId(R.id.open_shop)).perform(scrollTo(),click());onView(withTagValue(is((Object)"buy_collar"))).perform(scrollTo(),click());onView(withText("Buy")).perform(click());assertEquals(0,store.coins());onView(withTagValue(is((Object)"buy_collar"))).check(matches(not(isEnabled())));onView(withText("Customize companion")).perform(scrollTo(),click());onView(withTagValue(is((Object)"equip_collar"))).perform(scrollTo(),click());assertEquals("collar",store.equippedItemId(repo.selectedPet().id,"accessory"));onView(withTagValue(is((Object)"equip_collar"))).check(matches(withText("Remove Simple collar")));}
    @Test public void cancelledPurchaseDoesNotCharge(){resources(25,0,0);launch();onView(withText("Collection")).perform(click());onView(withId(R.id.open_shop)).perform(scrollTo(),click());onView(withTagValue(is((Object)"buy_collar"))).perform(scrollTo(),click());onView(withText("Cancel")).perform(click());assertEquals(25,store.coins());assertTrue(store.ownedCosmeticIds().isEmpty());}
    @Test public void petDetailHasWorkingInteractions(){launch();onView(withText("Collection")).perform(click());onView(withId(R.id.pet_card)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_play)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_feedback)).perform(scrollTo()).check(matches(withText(containsString("enjoyed playing"))));assertEquals(95,repo.selectedPet().energy);onView(withId(R.id.pet_interaction_rest)).perform(scrollTo(),click());onView(withId(R.id.pet_interaction_feedback)).perform(scrollTo()).check(matches(withText(containsString("wait"))));}
    @Test public void focusAnimationUsesSelectedPetAndEndingNeedsOneConfirmation(){Pet p=repo.ensureStarterPet();Pet second=repo.hatchDefaultPet();repo.selectPet(second.id);launch();onView(withText("Start focus")).perform(scrollTo(),click());onView(withText("Begin session")).perform(scrollTo(),click());SystemClock.sleep(1200);assertEquals(second.id,((Long)view(R.id.pet_animation_container).getTag()).longValue());assertEquals(second.imageKey,view(R.id.pet_animation_image).getTag());long id=store.activeSessionId();store.saveActiveSessionState(SystemClock.elapsedRealtime()+20*60000L,25,false,20*60000L);c.startService(new Intent(c,FocusService.class).setAction(FocusService.ACTION_RESTORE));SystemClock.sleep(300);onView(withText("End session")).perform(scrollTo(),click());onView(withText("Yes, end session")).perform(click());SystemClock.sleep(300);assertFalse(store.hasActiveSession());assertEquals(FocusSession.STATUS_COMPLETED,repo.pendingSession(id).status);assertEquals(5,repo.pendingSession(id).actualMinutes);onView(withText("Continue to claim")).perform(scrollTo()).check(matches(isDisplayed()));}
    @Test public void resetClearsPurchasesEquipmentAndResources(){resources(25,0,0);Pet p=repo.ensureStarterPet();store.purchase("collar","c");repo.equipCosmetic(p.id,"collar");repo.clearAllData();assertTrue(store.ownedCosmeticIds().isEmpty());assertNull(store.equippedItemId(p.id,"accessory"));assertEquals(0,store.coins());assertTrue(repo.pets().isEmpty());}

    @Test public void hatchWithFocusDroppedEggChargesExplicitCosts(){repo.ensureStarterPet();resources(17,100,1);assertEquals(FocusStore.PurchaseResult.INVALID_PRODUCT,store.purchase("egg","no-egg-sale"));assertNotNull(repo.hatchPet("hatch-egg"));assertEquals(0,store.lifeEssence());assertEquals(0,store.petEggs());assertEquals(17,store.coins());assertEquals(2,repo.pets().size());}
}
