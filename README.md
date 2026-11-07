## Project structure

The project is a single-module Android application:

```text
Final-trial-2/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/focuspet/
        │   ├── res/
        │   └── keepRules/
        ├── test/
        └── androidTest/
```

The `app/build/` directory contains generated build output, APKs, compiled classes, intermediates, and reports. It should not be treated as application source.

---

## Main application files

### `MainActivity.java`

This is currently the main application controller and UI implementation.

It is responsible for:

- Creating the entire UI programmatically
- Building the app header
- Building the bottom navigation bar
- Showing the Focus home screen
- Showing the focus setup screen
- Showing the active timer screen
- Showing the completion screen
- Showing the pet collection screen
- Showing the statistics screen
- Showing the settings screen
- Requesting notification permission
- Registering the timer broadcast receiver
- Starting and controlling `FocusService`
- Updating the timer and progress bar
- Handling session locking while focus is active

Current screen flow:

```text
Home
 └── Focus setup
      └── Active focus timer
           ├── Completion screen
           └── Home after cancellation
```

The class currently contains most of the application logic, so it is doing the work of:

- Activity
- Navigation controller
- UI renderer
- Part of a ViewModel
- Part of a repository
- Session event handler

This is workable for a prototype, but it is the largest structural issue in the current project.

---

### `FocusService.java`

This is the foreground service for active focus sessions.

It handles:

- Starting a focus timer
- Running the timer in the background
- Pause and resume
- Cancelling a session
- Sending timer tick broadcasts
- Sending completion broadcasts
- Showing the ongoing notification
- Showing the completion notification
- Saving and clearing active session state

Important actions:

```java
ACTION_START
ACTION_PAUSE
ACTION_RESUME
ACTION_CANCEL
ACTION_TICK
ACTION_COMPLETE
ACTION_CANCELLED
```

The service is responsible for preserving the timer while:

- The app is backgrounded
- The screen is locked
- The user switches applications

The current timer uses `SystemClock.elapsedRealtime()`, which is appropriate for elapsed-duration timing.

---

### `FocusStore.java`

This is the current local persistence class.

It uses `SharedPreferences` to store:

- Daily goal
- Today’s focus minutes
- Total focus minutes
- Completed session count
- Coins
- Experience
- Pet level
- Current streak
- Longest streak
- Active session information

Current responsibilities include:

```text
Reading progress
Writing progress
Saving active timer state
Clearing active timer state
Recording completed sessions
Resetting local data
```

This is acting as a lightweight repository, although it is not formally named or structured as one.

---

### `FirstFragment.java`

This is an unused fragment from the original Android Studio template.

It currently belongs to the old XML/Fragment navigation prototype and is not used by the current `MainActivity`.

---

### `SecondFragment.java`

This is also an unused Android Studio template fragment.

It is not part of the current FocusPet UI flow.

---

## Android manifest

### `AndroidManifest.xml`

Declares:

- Main launcher activity
- Foreground focus timer service
- Notification permission
- Foreground service permission
- Special-use foreground service type

Important declarations:

```text
MainActivity
FocusService
POST_NOTIFICATIONS
FOREGROUND_SERVICE
FOREGROUND_SERVICE_SPECIAL_USE
```

The special-use foreground service declaration is currently used for the focus timer.

---

## Gradle and build files

### `app/build.gradle.kts`

Defines:

- Android application module
- Application ID
- Minimum SDK: API 33
- Target SDK: API 37
- Compile SDK: API 37
- Java 11 compatibility
- Debug/release configuration
- Dependencies
- View Binding

Current dependencies include:

- AppCompat
- Material
- ConstraintLayout
- AndroidX Activity
- Navigation Fragment
- Navigation UI
- JUnit
- Espresso

Some of these dependencies are no longer needed by the current implementation.

---

### `libs.versions.toml`

Centralizes:

- Android Gradle Plugin version
- AndroidX versions
- Material version
- Navigation versions
- Test library versions

It is the project’s dependency version catalog.

---

### `settings.gradle.kts`

Defines:

- Project name: `FocusPet`
- The `app` module
- Google and Maven Central repositories
- Gradle plugin management

---

### `build.gradle.kts`

Currently contains the top-level Android application plugin declaration.

---

## Resources

The `res/` directory still contains many files from the original Android Studio template.

### Layout resources

These are mostly obsolete now:

```text
activity_main.xml
content_main.xml
fragment_first.xml
fragment_second.xml
```

The current application UI is built programmatically in `MainActivity.java`, so these layouts are not used by the current screen implementation.

### Navigation resources

```text
navigation/nav_graph.xml
```

This belongs to the previous Fragment Navigation implementation and is not currently used by the active UI.

### Values resources

Important files include:

- `themes.xml`
- `strings.xml`
- `colors.xml`
- `dimens.xml`

`strings.xml` still contains template strings such as:

- `First Fragment`
- `Second Fragment`
- `Lorem ipsum`

These should eventually be removed and replaced with real FocusPet resources.

### Drawable and launcher resources

The launcher resources are still the default generated Android Studio assets:

```text
ic_launcher_background.xml
ic_launcher_foreground.xml
ic_launcher.xml
ic_launcher_round.xml
```

They should eventually be replaced with a real FocusPet icon.

---

## Tests

### `ExampleUnitTest.java`

Currently only tests:

```java
2 + 2 == 4
```

This is a placeholder test and does not test application behavior.

### `ExampleInstrumentedTest.java`

Currently only verifies that the application package name is:

```text
com.example.focuspet
```

This is also a placeholder test.

---

# Current architecture

The current architecture is effectively:

```text
MainActivity
 ├── Builds UI
 ├── Handles navigation
 ├── Reads/writes FocusStore
 ├── Registers timer receiver
 ├── Starts FocusService
 └── Updates progress and rewards

FocusService
 ├── Runs timer
 ├── Sends broadcasts
 └── Shows notifications

FocusStore
 └── SharedPreferences persistence
```

It is currently closer to a prototype architecture than the original requested MVVM architecture.

The planned architecture was:

```text
Screen
  ↓
ViewModel
  ↓
Use case / Repository
  ↓
Room / DataStore
```

The project currently does not yet have:

- ViewModels
- Repository interfaces
- Use-case classes
- Room database
- DataStore
- Navigation Compose
- Jetpack Compose
- Session history entities
- Proper domain models

---

# Most important improvements next

## 1. Fix active-session recovery

This is the highest-priority reliability issue.

`FocusStore` saves:

```text
active_end_at
active_minutes
```

However, when the service is recreated, `FocusService` does not currently restore its in-memory fields from those stored values.

That means a process/service restart could leave the app believing a session is active while the service no longer has the correct timer state.

Recommended improvement:

```text
On FocusService.onCreate or onStartCommand:
- Read active_end_at from FocusStore
- Calculate remaining time
- Restore duration and end timestamp
- Resume notification and tick updates
```

---

## 2. Move rewards into the service or repository

Currently, rewards are granted from `MainActivity` after receiving `ACTION_COMPLETE`.

That creates a reliability problem:

- If the activity is destroyed when the timer completes, it may not receive the broadcast.
- The completion notification may appear, but rewards may not be recorded.
- Reopening the app could show an incomplete reward state.

Better design:

```text
FocusService detects completion
        ↓
Repository records completed session exactly once
        ↓
Activity observes updated progress
```

Reward recording should not depend on the activity being open.

---

## 3. Add a real focus-session history

The current store only stores totals:

```text
total minutes
session count
current streak
longest streak
```

It does not store individual sessions.

A proper history model should store:

```text
FocusSession
- id
- startedAt
- endedAt
- plannedMinutes
- actualMinutes
- label
- status
- rewardGranted
```

This is needed for:

- Daily history
- Weekly charts
- Most-used duration
- Session labels
- Accurate streak calculations
- Reward deduplication

---

## 4. Replace SharedPreferences with Room and DataStore

The original product plan requested:

- Room for structured data
- DataStore for preferences

Recommended split:

### Room

Use Room for:

- Focus sessions
- Pets
- Pet experience
- Achievements
- Player progress
- Reward records

### DataStore

Use DataStore for:

- Daily goal
- Default session duration
- Notification preference
- Sound preference
- Vibration preference
- Theme selection
- Onboarding completion

`FocusStore` can then be replaced with:

```text
FocusRepository
```

---

## 5. Separate UI from application logic

`MainActivity.java` is currently approximately 400+ lines and contains nearly all behavior.

Recommended classes:

```text
ui/
    MainActivity.java
    HomeView.java
    SetupView.java
    ActiveFocusView.java
    SettingsView.java

viewmodel/
    HomeViewModel.java
    FocusSetupViewModel.java
    ActiveFocusViewModel.java
    SettingsViewModel.java

data/
    FocusRepository.java
    FocusStore.java
    FocusDatabase.java
    FocusSessionDao.java

domain/
    RewardCalculator.java
    StreakCalculator.java
    FocusSession.java
    Pet.java
```

Even if the project stays in Java and programmatic views, this separation will make it much easier to test and extend.

---

## 6. Improve timer state modeling

The timer currently uses several booleans and mutable fields:

```text
paused
stopped
workerGeneration
remaining
duration
endAt
```

A clearer model would be:

```java
enum FocusState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}
```

The service and UI should communicate a state object containing:

```text
state
remainingMillis
elapsedMillis
durationMillis
```

This would simplify:

- Pause/resume behavior
- Screen restoration
- Notification content
- Progress rendering
- Testing

---

## 7. Implement calendar-aware streaks

The current streak logic effectively increments to at least one:

```java
int nextStreak = Math.max(1, streak());
```

It does not yet correctly handle:

- Consecutive calendar days
- Missed days
- Multiple sessions on one day
- Yesterday versus older dates
- Time zone changes
- Daily progress reset

A proper implementation should store:

```text
lastCompletedDate
currentStreak
longestStreak
```

Then compare the current local date with the last completed date.

---

## 8. Add real tests

The current tests are only templates.

The next test suite should include:

### Timer tests

- Timer completes at the correct time
- Pause preserves remaining duration
- Resume continues from the paused value
- Cancellation does not grant rewards
- Service restoration works
- Completion is recorded only once

### Reward tests

- XP is calculated correctly
- Coins are calculated correctly
- Cancelled sessions receive no full reward
- Duplicate completion events do not duplicate rewards

### Progress tests

- Daily goal updates
- Daily totals reset correctly
- Streak increments correctly
- Missed days reset correctly
- Pet levels are calculated correctly

### UI tests

- Home screen displays progress
- Setup accepts preset durations
- Setup accepts custom duration
- Timer screen hides navigation
- End session returns to home
- Settings reset works
- Active bottom tab changes correctly

---

## 9. Remove obsolete template code

The following should be cleaned up once the current implementation is stable:

- `FirstFragment.java`
- `SecondFragment.java`
- `activity_main.xml`
- `content_main.xml`
- `fragment_first.xml`
- `fragment_second.xml`
- `nav_graph.xml`
- Template menu resources
- Template strings
- Unused Fragment Navigation dependencies
- Unused ConstraintLayout dependency
- Unused View Binding configuration

This will reduce confusion and make the project easier to maintain.

---

## 10. Improve the UI implementation

The current UI is manually built with Java views. This is acceptable for a prototype, but it creates layout risks:

- Manual spacing
- Manual density calculations
- Hard-coded colors
- Hard-coded text
- Difficult responsive behavior
- More chance of overlapping content
- Less reusable components

The long-term options are:

### Option A: Migrate to Jetpack Compose

This matches the original product plan and would simplify:

- State-driven UI
- Navigation
- Responsive layouts
- Bottom navigation
- Progress bars
- Theming
- Previewing screens

### Option B: Stay with Java views temporarily

If Java must remain the priority:

- Move repeated styles into XML drawables
- Use XML layouts rather than constructing every view manually
- Create reusable custom view helpers
- Use resource colors, dimensions, and strings
- Use `ConstraintLayout` or `MaterialCardView`
- Keep screen state outside the Activity

---

# Recommended next milestone

The best next implementation step is:

```text
Milestone: Reliable local focus sessions
```

It should include:

1. Extract timer state into a testable class.
2. Restore active sessions after service recreation.
3. Record completion in the service/repository instead of the Activity.
4. Prevent duplicate rewards.
5. Add individual `FocusSession` records.
6. Add timer and reward unit tests.
7. Only then migrate persistence from SharedPreferences to Room/DataStore.

After that, the project can safely expand into:

```text
Room history
→ streak calculation
→ pet progression
→ achievements
→ statistics charts
→ onboarding
→ visual pet assets
```