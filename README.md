# FocusPet

FocusPet is an offline-first Android focus timer presented as a lightweight pet-collection game. Users complete distraction-free focus sessions, earn rewards, grow companion pets, collect Life Essence and Pet Eggs, and review their focus history.

The current project is an Android MVP for a single local user. It does not require an account or network connection.

## Current features

- Focus sessions with selectable durations and labels.
- Foreground-service timer that continues while the app is backgrounded or the screen is locked.
- Ongoing and completion notifications.
- Finish and reward-claim flow with one-time claim protection.
- Early-session rewards based on counted whole minutes, with reduced payout for ending early.
- Randomized coins, Life Essence, and Pet Egg rewards.
- Pet Egg flow with an explicit confirmation before starting customization, then a name and appearance choice before final preview and confirmation; one egg and 100 Life Essence are charged only on final confirmation.
- Persistent pets with names, species, rarity, level, experience, appearance data, hatch date, mood, energy, and selected companion state.
- Companion selection from the home screen.
- Collection screen with owned pets and resource inventory.
- Shop with All, Pet Looks, Expressions, Accessories, and Backgrounds filters. Expression items stack in inventory, support quantity-based repeat purchases, and can be taught to individual pets. Background products cost 50 Coins each, unlock globally, and can be equipped independently by each pet.
- Accessories shop with ten transparent PNG items. Each pet can equip one accessory at a time; the same item may be equipped independently by different pets.
- Pet detail interactions are separate from a unified Customize screen with Accessories, Expressions, and per-pet Background selection.
- Pet detail and Customize screens with per-pet learned expressions, enable checkboxes, and random expression bubbles during focus and pet interactions.
- Hatched pets keep their selected base appearance; owned accessories remain managed separately.
- Pet detail screen with experience progress.
- Statistics summary and clickable focus-history details with the saved background illustration for each session.
- Settings, notification permission handling, local reset, and debug-only resource controls.
- XML-backed reusable pet cards and screen components, with some navigation/UI composition still handled by Java.

## Technology

| Area | Current implementation |
|---|---|
| Language | Java |
| UI | Android Views, XML layouts, and programmatic Java composition |
| Architecture | Feature-oriented prototype with `MainActivity`, repository, store, service, and Room DAOs |
| Local database | Room 2.7.2; stores sessions, pets, hatch drafts, and the shop product catalog |
| Preferences | `SharedPreferences` through `FocusStore`; stores balances, ownership, and purchase receipts |
| Timer | Android foreground service using `SystemClock.elapsedRealtime()` |
| Notifications | Android notification channel and foreground notifications |
| Minimum SDK | API 33 |
| Compile/target SDK | API 37 |
| Java compatibility | Java 11 |
| Build system | Gradle Kotlin DSL, Android Gradle Plugin 9.3.3 |

The original project plan mentioned Jetpack Compose, but the current implementation uses Android Views and XML. Navigation is currently performed by replacing content in `MainActivity` rather than using a complete Navigation Component graph.

## Important files

### Application and navigation

- [`MainActivity.java`](app/src/main/java/com/example/focuspet/MainActivity.java) - Main activity, screen rendering, manual navigation, timer event handling, permission prompts, finish/claim locking, and debug UI.
- [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) - Launcher activity, notification permission, foreground-service permissions, and `FocusService` registration.

### Timer and rewards

- [`FocusService.java`](app/src/main/java/com/example/focuspet/FocusService.java) - Starts, restores, pauses, resumes, and cancels focus sessions; sends timer broadcasts and notifications.
- [`FocusRepository.java`](app/src/main/java/com/example/focuspet/FocusRepository.java) - Coordinates Room sessions and pets, calculates randomized rewards, completes sessions, and atomically claims rewards.
- [`FocusStore.java`](app/src/main/java/com/example/focuspet/FocusStore.java) - Stores lightweight account progress, resources, streak values, and active-session state in `SharedPreferences`.
- [`ShopRepository.java`](app/src/main/java/com/example/focuspet/ShopRepository.java), [`ShopProductEntity.java`](app/src/main/java/com/example/focuspet/ShopProductEntity.java), and [`ShopProductDao.java`](app/src/main/java/com/example/focuspet/ShopProductDao.java) - Seeds and reads the Room-backed shop catalog.
- [`ShopCatalog.java`](app/src/main/java/com/example/focuspet/ShopCatalog.java), [`PetLookCatalog.java`](app/src/main/java/com/example/focuspet/PetLookCatalog.java), [`PetExpressionCatalog.java`](app/src/main/java/com/example/focuspet/PetExpressionCatalog.java), [`BackgroundCatalog.java`](app/src/main/java/com/example/focuspet/BackgroundCatalog.java), and [`ShopCategory.java`](app/src/main/java/com/example/focuspet/ShopCategory.java) - Define products, appearance offers, expression items, backgrounds, and shop categories separately from persistence and UI.
- [`PetLook.java`](app/src/main/java/com/example/focuspet/PetLook.java) - Maps pet appearance keys to localized labels and drawable resources.
- [`PetExpression.java`](app/src/main/java/com/example/focuspet/PetExpression.java) and [`PetExpressionSelector.java`](app/src/main/java/com/example/focuspet/PetExpressionSelector.java) - Define built-in and learnable expressions and choose context-appropriate expressions for bubbles.

### Room data model

- [`FocusDatabase.java`](app/src/main/java/com/example/focuspet/FocusDatabase.java) - Room database containing sessions and pets.
- [`FocusSession.java`](app/src/main/java/com/example/focuspet/FocusSession.java) - Session entity, timing fields, status, early-ending state, reward values, and the pet-visual snapshot used by history illustrations.
- [`FocusSessionDao.java`](app/src/main/java/com/example/focuspet/FocusSessionDao.java) - Session lifecycle queries and atomic reward-claim update.
- [`Pet.java`](app/src/main/java/com/example/focuspet/Pet.java) - Pet entity and level calculation.
- [`PetDao.java`](app/src/main/java/com/example/focuspet/PetDao.java) - Pet listing, selection, insertion, and experience updates.

### XML UI

- [`item_pet_card.xml`](app/src/main/res/layout/item_pet_card.xml) - Reusable pet card used by the home companion card, collection, and companion chooser. It defines the artwork placeholder, aligned text column, one-line name, metadata, EXP label, and progress bar.
- [`screen_collection.xml`](app/src/main/res/layout/screen_collection.xml) - Resource banner and owned-pet collection container.
- [`screen_pet_detail.xml`](app/src/main/res/layout/screen_pet_detail.xml) - Pet detail framework and experience display.
- [`screen_companion_chooser.xml`](app/src/main/res/layout/screen_companion_chooser.xml) - Companion selection screen.
- [`screen_statistics.xml`](app/src/main/res/layout/screen_statistics.xml) - Statistics summary and focus-history containers.
- [`item_statistics_summary.xml`](app/src/main/res/layout/item_statistics_summary.xml) - Combined time, sessions, current streak, and longest streak card.
- [`item_session_history.xml`](app/src/main/res/layout/item_session_history.xml) and [`screen_history_detail.xml`](app/src/main/res/layout/screen_history_detail.xml) - History list rows and detail view.
- [`screen_hatching.xml`](app/src/main/res/layout/screen_hatching.xml) and `screen_hatch_*.xml` - Pet Egg entry, name, appearance, and preview screens.
- [`HatchDraft.java`](app/src/main/java/com/example/focuspet/HatchDraft.java) and [`HatchDraftDao.java`](app/src/main/java/com/example/focuspet/HatchDraftDao.java) - Persisted, resumable Pet Egg customization drafts.

### Build configuration

- [`app/build.gradle.kts`](app/build.gradle.kts) - Android module configuration, SDK levels, Java compatibility, Room compiler, and test dependencies.
- [`gradle/libs.versions.toml`](gradle/libs.versions.toml) - Dependency and plugin version catalog.
- [`settings.gradle.kts`](settings.gradle.kts) - Project name, repositories, and module inclusion.
- [`gradle.properties`](gradle.properties) - Gradle JVM configuration.

## Project structure

```text
FocusPet/
├── app/
│   ├── src/main/java/com/example/focuspet/
│   │   ├── MainActivity.java
│   │   ├── FocusService.java
│   │   ├── FocusRepository.java
│   │   ├── FocusStore.java
│   │   ├── FocusSession*.java
│   │   └── Pet*.java
│   ├── src/main/res/layout/
│   ├── src/main/res/drawable/
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Generated files under `app/build/` and `build/` are build outputs, not source files. `local.properties` contains machine-specific Android SDK configuration and should remain local.

## Build and verify

From the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Create a release APK with:

```powershell
.\gradlew.bat :app:assembleRelease
```

The debug build currently compiles successfully. The project includes JUnit and Espresso dependencies, but automated coverage is still limited; manual device testing remains important for timer, notification, and reward-flow behavior.

## Reward and progression overview

1. A focus session is created in Room when the timer starts.
2. `FocusService` keeps the timer reliable in the background and sends completion or cancellation events.
3. `FocusRepository` calculates rewards and stores them on the session as pending.
4. The finish screen shows the result without applying it immediately.
5. The user claims the reward once.
6. `FocusStore` updates coins, Life Essence, Eggs, total focus time, sessions, and streak progress.
7. Claimed XP is also added to the selected Room pet.
8. Pet Egg customization stores a draft without creating a collection pet or consuming resources. Confirmation creates the named pet with the chosen appearance and consumes one egg and 100 Life Essence.

For an early ending, counted focus time is floored to whole minutes. Sessions shorter than one minute do not create a claimable early reward. Early rewards use approximately 30% of the normal numeric rewards, representing an approximate 70% loss.

## Known limitations

- `MainActivity` is still a large controller containing UI, navigation, and event logic.
- `FocusStore` and Room divide progress data between two persistence systems.
- Room schema changes use explicit migrations; unsupported database versions fail rather than silently deleting user data.
- Several starter XML fragments and navigation dependencies come from the original Android Studio template and are not part of the current primary flow.
- Pet artwork, animations, sound, vibration settings, and pet customization are placeholders or incomplete.
- The reward economy and streak rules need more balancing and dedicated tests.
- No account synchronization or cloud backup exists.

## What to do next (Possibly)

### Must

1. Add a pet customize system, letting users choose the appearance of their new pets from a sequence of choices. Better do it with artistic resources.
2. Better user setting interface. Allow editing daily focusing goals.
3. Goal setting & focus session naming system. Need a data structure to store information of every focus session. The goal system might be complicated.
4. Shop for some game items. Need to be carefully designed.
5. Better focus history display. The main screen should only show some recent records. Add a 'more..' card to click into for details.
6. Fonts and format should be adjusted for better visual, and the text hints in the whole app should be manually edited to get rid of unpleasant AI feels.
7. Game art and possibly sound effects.
8. To be added...

### Optional

1. Split `MainActivity` into screen components and ViewModels so UI rendering, navigation, and business logic are easier to test.
2. Move all persistent progress into Room or define a clear synchronization boundary between Room and `FocusStore`.
3. Replace destructive Room migration fallback with explicit migrations and backup/restore tests.
4. Add unit tests for reward probabilities, early-session rounding, streak updates, hatching costs, and one-time claims.
5. Add instrumentation tests for background timing, rotation/process recreation, notification actions, and locked finish/claim screens.
6. Replace manual screen swapping with a consistent Navigation Component structure, or migrate deliberately to Compose rather than maintaining a mixed UI strategy.
7. Add real pet assets, accessibility content descriptions, responsive layouts, and visual regression checks for different screen sizes.
8. Profile startup and database calls, remove unused template dependencies, and close repository executors with lifecycle-safe ownership.
9. Balance the economy using observed session data while ensuring rewards encourage healthy, bounded focus rather than excessive screen time.

## Data model design update (2026-11-14)

The standalone Java 11 prototype in [`FocusPetModels.java`](data-model/src/main/java/com/example/focuspet/model/FocusPetModels.java) describes data structures for the planned features. It is not connected to the Android app or its database.

- **Goals and sessions:** named goals have a target duration and an extensible period; a session can optionally reference a goal and retain its own label. User settings include a daily focus target. Recent and complete history can be built from the session records.
- **Pet customization:** cosmetic items represent additions such as clothing and decorations, separate from a pet's base appearance. Item categories and equipment slots use extensible keys. Ownership and each pet's equipped items are modeled separately; the exact categories, assets, and sharing rules remain to be decided.
- **Shop and inventory:** products, currency balances, stackable inventory, and purchase snapshots have separate models. Cosmetic products use Coins; Pet Egg products use Life Essence. The model validates these currency assignments, but does not prescribe products or prices.
- **Persistence and safety:** existing pets, sessions, balances, and eggs should be preserved during any future migration. A future implementation should make purchases atomic and idempotent, including the balance deduction, item grant, and purchase record. It should define a single source of truth for progress currently split between Room and `SharedPreferences`.

These are data structures and basic field validations only. Room entities and migrations, purchase transactions, goal and history screens, customization UI, and artwork have not yet been implemented. The existing app still uses its current reward and hatching rules; whether to change random egg rewards or the Essence cost of hatching remains undecided.

## Pet and shop implementation update (2026-11-18)

The app includes a shop with All and Pet Looks filters, product cards, previews, and a confirmation dialog. Seven additional pet appearances are stored in the Room product catalog and can be purchased for 67 Coins each; ownership remains global in `SharedPreferences` and is granted atomically with the balance deduction and purchase receipt. Pet Egg customization requires a name, appearance choice, and final preview confirmation; resources are charged and the pet is added to the collection only at confirmation. The draft can resume after interruption. Insufficient-resource hatch buttons are disabled and gray. The extra four-minute warning was removed, leaving the normal single confirmation.

Purchases are persisted with duplicate-request protection. Hatching uses a resumable Room draft and a request journal to recover interrupted confirmations without charging twice. Room v4-to-v7 migrations preserve existing pets and history. Product and cosmetic types from `data-model` are used by the Android app; the other standalone model types are still prototypes. See [implementation notes and validation](docs/pet-shop-2026-11-18.md) for initial prices, storage behavior and testing scope.

## Pet expression implementation update (2026-11-24)

The shop offers eight A-H expression items for 50 Coins each. Items are repeatable and stack in `SharedPreferences`; teaching one to a pet consumes one item, records that expression for the pet, and enables it by default. Customize lists only that pet's learned expressions with per-expression Use checkboxes, while Expression inventory lists only positive stock and supports teaching directly to the current pet. Expression selection is shared by focus-session and pet-detail bubbles, with the existing ambient rate limits retained. Base appearance remains fixed after hatching; accessory equipment stays on a separate screen. No Room schema change or data reset is required.

The Accessories category offers the ten `accessory_*.png` assets for 50 Coins each. Ownership is global, while the equipped accessory key is stored per pet in the existing `SharedPreferences` equipment entry; all accessories share one `accessory` slot, so equipping another replaces the current one. Static accessory definitions map each key to its drawable, front/back layer, and normalized position over the pet artwork. The renderer composes the accessory on the pet in collection, detail, session, wardrobe, and shop previews. No Room schema migration or app-data reset is required.
