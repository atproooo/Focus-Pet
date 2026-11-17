package com.example.focuspet;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST = 7;
    private static final int PURPLE = Color.rgb(108, 80, 174);
    private static final int PURPLE_DARK = Color.rgb(82, 57, 145);
    private static final int SURFACE = Color.rgb(255, 248, 255);
    private static final int CARD = Color.rgb(232, 226, 234);
    private static final int LAVENDER = Color.rgb(238, 224, 255);
    private FocusStore store;
    private FocusRepository repository;
    private LinearLayout content;
    private LinearLayout navigation;
    private TextView title;
    private TextView debugButton;
    private BroadcastReceiver timerReceiver;
    private final List<LinearLayout> navigationItems = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FocusStore(this);
        repository = new FocusRepository(this);
        repository.ensureStarterPet();
        buildShell();
        requestNotifications();
        if (store.hasActiveSession()) {
            ContextCompat.startForegroundService(this,
                    new Intent(this, FocusService.class).setAction(FocusService.ACTION_RESTORE));
            FocusSession active = repository.pendingSession(store.activeSessionId());
            showActive(store.activeMinutes(), active == null ? "" : active.label);
        } else {
            FocusSession pending = repository.latestPending();
            if (pending != null) {
                showFinish(pending.id, pending.actualMinutes, pending.endedEarly);
            } else {
                showHome();
            }
        }
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SURFACE);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        title = text("Focus Grove", 25, Color.rgb(35, 29, 38));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(12), dp(22), dp(8), dp(18));
        header.setBackgroundColor(SURFACE);
        TextView iconPlaceholder = text("✦", 20, PURPLE_DARK);
        iconPlaceholder.setBackground(roundColor(LAVENDER, 16));
        iconPlaceholder.setGravity(Gravity.CENTER);
        iconPlaceholder.setContentDescription("App icon placeholder");
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.setMargins(dp(20), dp(12), dp(4), dp(8));
        header.addView(iconPlaceholder, iconParams);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        shell.addView(header);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), 0, dp(20), dp(32));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        navigation.setBackgroundColor(Color.rgb(248, 241, 250));
        addNavigationButton(navigation, "🏠", "Focus", v -> showHome());
        addNavigationButton(navigation, "🐾", "Collection", v -> showCollection());
        addNavigationButton(navigation, "📊", "Statistics", v -> showStats());
        addNavigationButton(navigation, "⚙️", "Settings", v -> showSettings());
        navigation.setPadding(dp(4), dp(6), dp(4), dp(6));
        shell.addView(navigation, new LinearLayout.LayoutParams(-1, dp(92)));
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        if (BuildConfig.DEBUG) {
            debugButton = text("⚙", 22, Color.WHITE);
            debugButton.setGravity(Gravity.CENTER);
            debugButton.setBackgroundResource(R.drawable.bg_focuspet_debug_button);
            debugButton.setContentDescription("Development resource controls");
            debugButton.setOnClickListener(v -> showDebugResourceDialog());
            FrameLayout.LayoutParams debugParams =
                    new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.END);
            debugParams.setMargins(0, 0, dp(16), dp(104));
            root.addView(debugButton, debugParams);
        }
        setContentView(root);
    }

    private void setSessionLock(boolean locked) {
        navigation.setVisibility(locked ? View.GONE : View.VISIBLE);
        if (debugButton != null) debugButton.setVisibility(locked ? View.GONE : View.VISIBLE);
    }

    private void showDebugResourceDialog() {
        if (!BuildConfig.DEBUG) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(4), dp(20), 0);
        panel.addView(text("Presets", 15, PURPLE_DARK));
        LinearLayout presets = new LinearLayout(this);
        presets.setGravity(Gravity.CENTER);
        addDebugPreset(presets, "+10", 10);
        addDebugPreset(presets, "+100", 100);
        addDebugPreset(presets, "+1000", 1000);
        panel.addView(presets);
        EditText coins = debugAmount("Coins");
        EditText essence = debugAmount("Life Essence");
        EditText eggs = debugAmount("Pet Eggs");
        panel.addView(coins);
        panel.addView(essence);
        panel.addView(eggs);
        new AlertDialog.Builder(this).setTitle("Debug resources")
                .setMessage("Development-only controls. These are removed from release builds.")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add resources", (dialog, which) -> {
                    store.addDebugResources(parseAmount(coins), parseAmount(essence), parseAmount(eggs));
                    recreate();
                }).show();
    }

    private void addDebugPreset(LinearLayout parent, String label, int amount) {
        Button preset = outlineButton(label, v -> {
            store.addDebugResources(amount, amount, amount);
            recreate();
        });
        preset.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        parent.addView(preset, params);
    }

    private EditText debugAmount(String label) {
        EditText input = new EditText(this);
        input.setHint(label + " amount");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        return input;
    }

    private int parseAmount(EditText input) {
        try {
            return Math.max(0, Integer.parseInt(input.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override public void onBackPressed() {
        if (store != null && store.hasActiveSession()) {
            android.widget.Toast.makeText(this, "Finish or end the current focus session first.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (repository != null && repository.latestPending() != null) {
            android.widget.Toast.makeText(this, "Claim the pending reward first.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    private void addNavigationButton(LinearLayout navigation, String icon, String label,
                                     View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView iconView = text(icon, 19, Color.rgb(45, 39, 50));
        iconView.setPadding(0, 0, 0, 0);
        iconView.setGravity(Gravity.CENTER);
        TextView labelView = text(label, 14, Color.rgb(45, 39, 50));
        labelView.setPadding(0, 0, 0, 0);
        labelView.setGravity(Gravity.CENTER);
        item.addView(iconView, new LinearLayout.LayoutParams(-1, dp(25)));
        item.addView(labelView, new LinearLayout.LayoutParams(-1, dp(26)));
        item.setBackground(statefulBackground(Color.TRANSPARENT, LAVENDER, Color.LTGRAY));
        item.setOnClickListener(listener);
        navigationItems.add(item);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(5), dp(2), dp(5), dp(2));
        navigation.addView(item, params);
    }

    private void setActiveTab(int index) {
        for (int i = 0; i < navigationItems.size(); i++) {
            navigationItems.get(i).setBackground(roundColor(i == index ? LAVENDER : Color.TRANSPARENT, 28));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundColor(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable outline(int color, int width, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dp(width), color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private StateListDrawable statefulBackground(int normal, int pressed, int disabled) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, roundColor(disabled, 28));
        states.addState(new int[]{android.R.attr.state_pressed}, roundColor(pressed, 28));
        states.addState(new int[]{}, roundColor(normal, 28));
        return states;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        return view;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(statefulBackground(PURPLE, PURPLE_DARK, Color.rgb(190, 190, 190)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.setMargins(0, dp(18), 0, dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private Button outlineButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(65, 58, 70));
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setBackground(statefulOutline(Color.rgb(190, 179, 198), PURPLE, 28));
        button.setOnClickListener(listener);
        return button;
    }

    private StateListDrawable statefulOutline(int border, int pressedColor, int radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, outline(pressedColor, 2, radius));
        states.addState(new int[]{}, outline(border, 1, radius));
        return states;
    }

    private void reset(String screenTitle) {
        title.setText(screenTitle);
        content.removeAllViews();
        content.setPadding(dp(20), dp(8), dp(20), dp(32));
    }

    private void showHome() {
        reset("Focus Grove");
        setSessionLock(false);
        setActiveTab(0);
        content.addView(text("Start a session with your pets today! ", 18, Color.rgb(35, 29, 38)));
        View companion = companionCard();
        companion.setOnClickListener(v -> showCompanionChooser());
        content.addView(companion);
        TextView today = text("Today", 16, PURPLE);
        today.setTypeface(null, Typeface.BOLD);
        today.setPadding(dp(0), dp(24), dp(0), dp(0));
        content.addView(today);
        content.addView(text(store.todayMinutes() + " min focused", 28, Color.rgb(35, 29, 38)));

        int todayMinutes = store.todayMinutes();
        int goal = store.dailyGoal();
        int remaining = Math.max(0, goal - todayMinutes);
        ProgressBar goalProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        goalProgress.setMax(goal);
        goalProgress.setProgress(Math.min(todayMinutes, goal));
        goalProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        goalProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LAVENDER));
        LinearLayout.LayoutParams goalProgressParams = new LinearLayout.LayoutParams(-1, dp(12));
        goalProgressParams.setMargins(0, dp(8), 0, dp(4));
        content.addView(goalProgress, goalProgressParams);
        content.addView(text(remaining == 0
                ? "Daily goal reached — nice work today!"
                : remaining + " min to reach your " + goal + "-min daily goal",
                14, Color.rgb(75, 67, 80)));

        content.addView(text(store.sessions() + " sessions completed.  Keep showing up!",
                16, Color.rgb(45, 39, 50)));
        content.addView(button("Start focus", v -> showSetup()));

        TextView recentTitle = text("Recent sessions", 16, PURPLE);
        recentTitle.setTypeface(null, Typeface.BOLD);
        recentTitle.setPadding(0, dp(24), 0, dp(8));
        content.addView(recentTitle);

        List<FocusSession> recent = repository.recentHistory(3);
        if (recent.isEmpty()) {
            content.addView(text("No sessions yet. Your recent focus sessions will appear here.",
                    16, Color.DKGRAY));
        } else {
            for (FocusSession session : recent) {
                addHistoryRow(content, session);
            }
        }

        TextView moreCard = text("More...", 15, PURPLE_DARK);
        moreCard.setBackgroundResource(R.drawable.bg_focuspet_card);
        moreCard.setGravity(Gravity.CENTER);
        moreCard.setTypeface(null, Typeface.BOLD);
        moreCard.setPadding(dp(12), dp(14), dp(12), dp(14));
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(-1, -2);
        moreParams.setMargins(0, dp(12), 0, dp(8));
        moreCard.setOnClickListener(v -> showStats());
        content.addView(moreCard, moreParams);
    }

    private View companionCard() {
        View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, content, false);
        Pet pet = repository.selectedPet();
        String petName = pet == null ? "Mochi" : pet.name;
        String species = pet == null ? "Fox" : pet.species;
        int petLevel = pet == null ? store.level() : pet.level();
        int petXp = pet == null ? store.experience() : pet.experience;
        configurePetCard(card, petName + " the " + species,
                "Level " + petLevel + "  ·  " + petXp + " XP",
                "Your companion",
                petXp % 100, 1f, v -> showCompanionChooser());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(20), 0, dp(4));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showSetup() {
        reset("Focus setup");
        setSessionLock(false);
        setActiveTab(0);
        TextView back = text("‹  Back", 16, PURPLE_DARK);
        back.setTypeface(null, Typeface.BOLD);
        back.setPadding(0, dp(8), 0, dp(18));
        back.setOnClickListener(v -> showHome());
        content.addView(back);

        TextView nameLabel = text("What are you focusing on? (optional)", 14, Color.rgb(75, 67, 80));
        nameLabel.setPadding(0, 0, 0, dp(4));
        content.addView(nameLabel);

        EditText sessionNameInput = new EditText(this);
        sessionNameInput.setHint("e.g. math homework");
        sessionNameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        sessionNameInput.setTextSize(18);
        sessionNameInput.setSingleLine(true);
        sessionNameInput.setGravity(Gravity.CENTER_VERTICAL);
        sessionNameInput.setPadding(dp(16), 0, dp(16), 0);
        sessionNameInput.setBackground(outline(Color.rgb(110, 102, 115), 1, 8));
        sessionNameInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(60) });
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(72));
        nameParams.setMargins(0, 0, 0, dp(4));
        content.addView(sessionNameInput, nameParams);
        TextView nameHint = text("Leave blank and we'll call it a Focus session.", 14, Color.DKGRAY);
        nameHint.setPadding(dp(16), dp(2), dp(16), dp(12));
        content.addView(nameHint, new LinearLayout.LayoutParams(-1, -2));

        content.addView(text("How long would you like to focus?", 19, Color.rgb(35, 29, 38)));

        TextView customLabel = text("Custom focus time (minutes)", 14, Color.rgb(75, 67, 80));
        customLabel.setPadding(dp(16), dp(18), dp(16), dp(4));
        content.addView(customLabel);

        EditText customDuration = new EditText(this);
        customDuration.setText("25");
        customDuration.setInputType(InputType.TYPE_CLASS_NUMBER);
        customDuration.setTextSize(18);
        customDuration.setSingleLine(true);
        customDuration.setGravity(Gravity.CENTER_VERTICAL);
        customDuration.setPadding(dp(16), 0, dp(16), 0);
        customDuration.setBackground(outline(Color.rgb(110, 102, 115), 1, 8));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(72));
        inputParams.setMargins(0, 0, 0, dp(4));
        content.addView(customDuration, inputParams);
        TextView hint = text("Choose any whole number from 1 to 240 minutes.", 14, Color.DKGRAY);
        hint.setPadding(dp(16), dp(2), dp(16), dp(12));
        content.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout presets = new LinearLayout(this);
        presets.setGravity(Gravity.CENTER);
        int[] presetValues = {15, 25, 45, 60};
        for (int value : presetValues) {
            Button preset = outlineButton(value + " min", v -> customDuration.setText(String.valueOf(value)));
            preset.setTextSize(14);
            LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            presetParams.setMargins(dp(4), dp(4), dp(4), dp(4));
            presets.addView(preset, presetParams);
        }
        content.addView(presets);
        content.addView(button("Begin session", v -> {
            String custom = customDuration.getText().toString().trim();
            int selectedMinutes;
            try {
                selectedMinutes = Integer.parseInt(custom);
            } catch (NumberFormatException error) {
                android.widget.Toast.makeText(this, "Enter a valid whole number of minutes.",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedMinutes < 1 || selectedMinutes > 240) {
                android.widget.Toast.makeText(this, "Focus time must be between 1 and 240 minutes.",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            startFocus(selectedMinutes, sessionNameInput.getText().toString().trim());
        }));
    }

    private void startFocus(int minutes, String label) {
        Intent intent = new Intent(this, FocusService.class).setAction(FocusService.ACTION_START)
                .putExtra(FocusService.EXTRA_MINUTES, minutes).putExtra(FocusService.EXTRA_LABEL, label);
        ContextCompat.startForegroundService(this, intent);
        showActive(minutes, label);
    }

    private void showActive(int sessionMinutes, String label) {
        reset("Focus in progress");
        setSessionLock(true);
        setActiveTab(0);
        boolean hasName = label != null && !label.trim().isEmpty();
        String displayName = displayLabel(label);
        if (hasName) {
            TextView nameCaption = text("FOCUSING ON", 13, PURPLE);
            nameCaption.setTypeface(null, Typeface.BOLD);
            nameCaption.setGravity(Gravity.CENTER);
            nameCaption.setPadding(0, dp(24), 0, dp(4));
            content.addView(nameCaption);
            TextView nameValue = text(displayName, 22, PURPLE_DARK);
            nameValue.setTypeface(null, Typeface.BOLD);
            nameValue.setGravity(Gravity.CENTER);
            content.addView(nameValue);
        }
        TextView timer = text("Loading...", 64, Color.rgb(35, 29, 38));
        timer.setGravity(Gravity.CENTER);
        timer.setPadding(0, hasName ? dp(24) : dp(64), 0, dp(18));
        content.addView(timer);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        final long totalMillis = Math.max(1L, sessionMinutes * 60_000L);
        progress.setMax(100);
        long remainingAtOpen = Math.max(0L, store.activeEndAt() - SystemClock.elapsedRealtime());
        int initialPercent = (int) Math.max(0L, Math.min(100L,
                Math.round(((double) (totalMillis - remainingAtOpen) / totalMillis) * 100)));
        progress.setProgress(initialPercent);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LAVENDER));
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(12)));
        TextView encouragement = text("Mochi is cheering you on.", 19, Color.rgb(35, 29, 38));
        encouragement.setGravity(Gravity.CENTER);
        encouragement.setPadding(0, dp(24), 0, dp(28));
        content.addView(encouragement);
        Button end = outlineButton("End session", v -> showEndSessionConfirmation());
        content.addView(end);
        timerReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (FocusService.ACTION_TICK.equals(intent.getAction())) {
                    long remaining = intent.getLongExtra(FocusService.EXTRA_REMAINING, 0);
                    timer.setText(formatTime(remaining));
                    long elapsed = Math.max(0L, totalMillis - remaining);
                    int percent = (int) Math.max(0L, Math.min(100L,
                            Math.round(((double) elapsed / totalMillis) * 100)));
                    progress.setProgress(percent);
                } else if (FocusService.ACTION_COMPLETE.equals(intent.getAction())) {
                    showFinish(intent.getLongExtra(FocusService.EXTRA_SESSION_ID, 0),
                            intent.getIntExtra(FocusService.EXTRA_MINUTES, 0), false);
                } else if (FocusService.ACTION_CANCELLED.equals(intent.getAction())) {
                    if (timerReceiver != null) {
                        unregisterReceiver(timerReceiver);
                        timerReceiver = null;
                    }
                    int minutes = intent.getIntExtra(FocusService.EXTRA_MINUTES, 0);
                    if (minutes > 0) {
                        showFinish(intent.getLongExtra(FocusService.EXTRA_SESSION_ID, 0),
                                minutes, true);
                    } else {
                        android.widget.Toast.makeText(MainActivity.this,
                                "Focus session ended before one minute; no reward was created.",
                                android.widget.Toast.LENGTH_SHORT).show();
                        showHome();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(FocusService.ACTION_TICK);
        filter.addAction(FocusService.ACTION_COMPLETE);
        filter.addAction(FocusService.ACTION_CANCELLED);
        ContextCompat.registerReceiver(this, timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void showEndSessionConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("End focus session?")
                .setMessage("You can still claim rewards for the time you focused, but ending early may reduce XP, coins, Life Essence, and Pet Egg chances by 70%.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, end session", (dialog, which) ->
                        sendService(FocusService.ACTION_CANCEL))
                .show();
    }

    private void sendService(String action) {
        startService(new Intent(this, FocusService.class).setAction(action));
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private String displayLabel(String label) {
        return label == null || label.trim().isEmpty() ? "Focus session" : label;
    }

    private void showFinish(long sessionId, int minutes, boolean endedEarly) {
        if (timerReceiver != null) {
            unregisterReceiver(timerReceiver);
            timerReceiver = null;
        }
        FocusSession finished = repository.pendingSession(sessionId);
        reset("Session complete!");
        setSessionLock(true);
        setActiveTab(0);
        content.addView(imagePlaceholder("SESSION CELEBRATION"));
        TextView finishName = text(displayLabel(finished == null ? "" : finished.label),
                22, PURPLE_DARK);
        finishName.setTypeface(null, Typeface.BOLD);
        finishName.setGravity(Gravity.CENTER);
        finishName.setPadding(0, dp(8), 0, dp(8));
        content.addView(finishName);
        content.addView(text("🌟 Great work, your pet is proud.", 24, Color.rgb(74, 55, 95)));
        content.addView(text(minutes + " minutes focused\n"
                + (endedEarly ? "Partial rewards are ready to claim." : "Your rewards are ready to claim."),
                20, Color.DKGRAY));
        content.addView(button("Continue to claim", v -> showClaim(sessionId)));
    }

    private void showClaim(long sessionId) {
        FocusSession session = repository.pendingSession(sessionId);
        if (session == null || !FocusSession.STATUS_COMPLETED.equals(session.status)) {
            showHome();
            return;
        }
        reset("Claim rewards");
        setSessionLock(true);
        setActiveTab(0);
        content.addView(imagePlaceholder("REWARD ANIMATION"));
        TextView claimName = text(displayLabel(session.label), 22, PURPLE_DARK);
        claimName.setTypeface(null, Typeface.BOLD);
        claimName.setGravity(Gravity.CENTER);
        claimName.setPadding(0, dp(8), 0, dp(8));
        content.addView(claimName);
        content.addView(text("Your session rewards", 24, Color.rgb(74, 55, 95)));
        content.addView(text("⭐ +" + session.xpReward + " XP\n"
                + "🪙 +" + session.coinReward + " coins\n"
                + "✨ +" + session.essenceReward + " Life Essence\n"
                + (session.eggReward ? "🥚 Pet Egg found!" : "No Pet Egg this time"),
                20, Color.DKGRAY));
        content.addView(button("Claim rewards", v -> {
            if (repository.claimRewards(sessionId)) {
                showHome();
            }
        }));
    }

    private TextView imagePlaceholder(String label) {
        TextView placeholder = text("🐾\n" + label, 22, Color.rgb(112, 90, 145));
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(roundColor(Color.rgb(239, 233, 250), 16));
        placeholder.setContentDescription("Reserved space for " + label.toLowerCase(Locale.US));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(140));
        params.setMargins(dp(8), dp(8), dp(8), dp(8));
        placeholder.setLayoutParams(params);
        return placeholder;
    }

    private void showCollection() {
        reset("Pet collection");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_collection, content, false);
        content.addView(screen);

        LinearLayout resourceBar = screen.findViewById(R.id.resource_bar);
        addResourceChip(resourceBar, "🪙\nCoins\n" + store.coins(), "Coins",
                "Coins can be used for future shop items and pet accessories.");
        addResourceChip(resourceBar, "✨\nEssence\n" + store.lifeEssence(), "Life Essence",
                "Life Essence is used to hatch Pet Eggs. One egg requires 100 Essence.");
        addResourceChip(resourceBar, "🥚  Eggs  " + store.petEggs(), "Pet Eggs", null);

        LinearLayout ownedPets = screen.findViewById(R.id.owned_pets_container);
        Pet selected = repository.selectedPet();
        for (Pet pet : repository.pets()) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, ownedPets, false);
            configurePetCard(card, pet.name + " the " + pet.species,
                    pet.rarity + "  •  Level " + pet.level(),
                    pet.id == (selected == null ? -1 : selected.id)
                            ? "Selected companion  •  Tap for details" : "Tap for details",
                    pet.experience % 100, 1f, v -> showPetDetail(pet));
            ownedPets.addView(card);
        }
    }

    private void addResourceChip(LinearLayout parent, String label, String title, String description) {
        TextView chip = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.item_resource_chip, parent, false);
        chip.setText(label);
        chip.setText(label.replace("\n", "  "));
        chip.setOnClickListener(v -> {
            if ("Pet Eggs".equals(title)) {
                showHatching();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
        parent.addView(chip);
    }

    private void configurePetCard(View card, String name, String meta, String hint,
                                  int progressValue, float alpha, View.OnClickListener listener) {
        card.setAlpha(alpha);
        card.setOnClickListener(listener);
        ((TextView) card.findViewById(R.id.pet_name)).setText(name);
        ((TextView) card.findViewById(R.id.pet_meta)).setText(meta);
        ((TextView) card.findViewById(R.id.pet_hint)).setText(hint);
        ((ProgressBar) card.findViewById(R.id.pet_progress)).setProgress(progressValue);
        card.findViewById(R.id.pet_image_placeholder)
                .setContentDescription("Reserved artwork space for " + name);
    }

    private void showPetDetail(Pet pet) {
        reset(pet.name + "'s details");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_pet_detail, content, false);
        content.addView(screen);
        screen.findViewById(R.id.pet_detail_back).setOnClickListener(v -> showCollection());
        ((TextView) screen.findViewById(R.id.pet_detail_name))
                .setText(pet.name + " the " + pet.species);
        ((TextView) screen.findViewById(R.id.pet_detail_meta)).setText(
                pet.rarity + " companion" + (pet.selected ? "  •  Selected" : "")
                        + "\nLevel " + pet.level() + "  •  " + pet.experience + " XP"
                        + "\nMood / energy: " + pet.mood + " / " + pet.energy);
        ((ProgressBar) screen.findViewById(R.id.pet_detail_progress))
                .setProgress(pet.experience % 100);
        ((TextView) screen.findViewById(R.id.pet_detail_description)).setText(
                pet.description + "\n\nHatched: "
                        + new SimpleDateFormat("MMM d, yyyy", Locale.US)
                        .format(new Date(pet.hatchedAt))
                        + "\nAppearance: " + pet.appearanceData);
    }

    private void showHatching() {
        reset("Pet Egg hatching");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_hatching, content, false);
        content.addView(screen);
        screen.findViewById(R.id.hatching_back).setOnClickListener(v -> showCollection());

        int eggs = store.petEggs();
        int essence = store.lifeEssence();
        boolean canHatch = eggs > 0 && essence >= 100;
        ((TextView) screen.findViewById(R.id.hatching_inventory)).setText(
                "Pet Eggs: " + eggs + "\nLife Essence: " + essence + " / 100 required");
        Button hatchButton = screen.findViewById(R.id.hatch_button);
        hatchButton.setBackgroundResource(canHatch
                ? R.drawable.bg_focuspet_hatch_enabled
                : R.drawable.bg_focuspet_hatch_disabled);
        hatchButton.setOnClickListener(v -> {
            if (store.consumeHatchingResources()) {
                repository.hatchDefaultPet();
                new AlertDialog.Builder(this)
                        .setTitle("hatch success")
                        .setMessage("Your egg has been opened. The new pet result area is ready for future pet generation.")
                        .setPositiveButton("OK", (dialog, which) -> showHatching())
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Not enough resources")
                        .setMessage("You need 1 Pet Egg and 100 Life Essence to hatch.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void showCompanionChooser() {
                reset("Choose companion");
                setActiveTab(0);
                LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                        .inflate(R.layout.screen_companion_chooser, content, false);
                content.addView(screen);
                screen.findViewById(R.id.chooser_back).setOnClickListener(v -> showHome());
                LinearLayout container = screen.findViewById(R.id.chooser_pet_container);
                Pet selected = repository.selectedPet();
                for (Pet pet : repository.pets()) {
                    View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, container, false);
                    configurePetCard(card, pet.name + " the " + pet.species,
                            pet.rarity + "  •  Level " + pet.level(),
                            pet.id == (selected == null ? -1 : selected.id)
                                    ? "Selected companion  •  Tap to keep" : "Tap to select",
                            pet.experience % 100, 1f, v -> {
                                repository.selectPet(pet.id);
                                showHome();
                            });
                    container.addView(card);
                }
    }

    private void showStats() {
        reset("Focus statistics");
        setActiveTab(2);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_statistics, content, false);
        content.addView(screen);
        LinearLayout statsCards = screen.findViewById(R.id.stats_cards);
        View summary = LayoutInflater.from(this)
                .inflate(R.layout.item_statistics_summary, statsCards, false);
        ((TextView) summary.findViewById(R.id.summary_time_value))
                .setText(store.totalMinutes() + " min");
        ((TextView) summary.findViewById(R.id.summary_sessions_value))
                .setText(String.valueOf(store.sessions()));
        ((TextView) summary.findViewById(R.id.summary_current_value))
                .setText(store.streak() + " days");
        ((TextView) summary.findViewById(R.id.summary_longest_value))
                .setText(store.longestStreak() + " days");
        statsCards.addView(summary);

        LinearLayout historyContainer = screen.findViewById(R.id.history_container);
        List<FocusSession> history = repository.history();
        if (history.isEmpty()) {
            TextView empty = text("No focus sessions yet. Complete your first session to see it here.",
                    16, Color.DKGRAY);
            empty.setPadding(0, dp(8), 0, dp(12));
            historyContainer.addView(empty);
        } else {
            for (FocusSession session : history) {
                addHistoryRow(historyContainer, session);
            }
        }
    }

    private void addStatCard(LinearLayout parent, String label, String value) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_stat_card, parent, false);
        ((TextView) card.findViewById(R.id.stat_label)).setText(label);
        ((TextView) card.findViewById(R.id.stat_value)).setText(value);
        parent.addView(card);
    }

    private void addHistoryRow(LinearLayout parent, FocusSession session) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_session_history, parent, false);
        String label = displayLabel(session.label);
        if (label.length() > 20) {
            label = label.substring(0, 20) + "...";
        }
        String status = session.endedEarly ? "Ended early"
                : FocusSession.STATUS_CLAIMED.equals(session.status) ? "Completed"
                : FocusSession.STATUS_CANCELLED.equals(session.status) ? "Ended early"
                : "Reward pending";
        TextView labelView = row.findViewById(R.id.history_label);
        labelView.setMaxLines(1);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labelView.setText(label + "  •  " + status);
        String date = new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US)
                .format(new Date(session.startedAt));
        TextView dateView = row.findViewById(R.id.history_date);
        dateView.setMaxLines(1);
        dateView.setText(date);
        ((TextView) row.findViewById(R.id.history_duration))
                .setText(session.actualMinutes + " min");
        row.setOnClickListener(v -> showHistoryDetail(session));
        parent.addView(row);
    }

    private void showHistoryDetail(FocusSession session) {
        reset("Focus session details");
        setActiveTab(2);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_history_detail, content, false);
        content.addView(screen);
        screen.findViewById(R.id.history_detail_back).setOnClickListener(v -> showStats());

        String label = displayLabel(session.label);
        String status = session.endedEarly ? "Ended early"
                : FocusSession.STATUS_CLAIMED.equals(session.status) ? "Completed"
                : FocusSession.STATUS_CANCELLED.equals(session.status) ? "Ended early"
                : "Reward pending";
        ((TextView) screen.findViewById(R.id.history_detail_title)).setText(label);
        ((TextView) screen.findViewById(R.id.history_detail_status)).setText(
                status + "  •  " + session.actualMinutes + " minutes focused");

        LinearLayout info = screen.findViewById(R.id.history_detail_info);
        addDetailLine(info, "Started",
                new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US)
                        .format(new Date(session.startedAt)));
        addDetailLine(info, "Ended", session.endedAt > 0
                ? new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US)
                .format(new Date(session.endedAt)) : "Still in progress");
        addDetailLine(info, "Planned duration", session.plannedMinutes + " minutes");
        addDetailLine(info, "Actual duration", session.actualMinutes + " minutes");

        String rewards = FocusSession.STATUS_CLAIMED.equals(session.status)
                ? "Rewards claimed\n⭐ +" + session.xpReward + " XP\n🪙 +" + session.coinReward
                + " coins\n✨ +" + session.essenceReward + " Life Essence"
                + (session.eggReward ? "\n🥚 Pet Egg found" : "")
                : FocusSession.STATUS_CANCELLED.equals(session.status)
                ? "No completion rewards were granted for this session."
                : "Rewards are waiting to be claimed.";
        ((TextView) screen.findViewById(R.id.history_detail_rewards)).setText(rewards);
    }

    private void addDetailLine(LinearLayout parent, String label, String value) {
        TextView line = text(label + "\n" + value, 16, Color.rgb(75, 67, 80));
        line.setPadding(0, dp(4), 0, dp(10));
        parent.addView(line);
    }

    private void showSettings() {
        reset("Settings");
        setActiveTab(3);
        content.setPadding(dp(16), dp(24), dp(16), dp(24));

        int sectionColor = Color.rgb(120, 112, 128);
        int danger = Color.rgb(198, 40, 40);
        int dangerDark = Color.rgb(150, 24, 24);
        java.util.function.Supplier<View> divider = () -> {
            View line = new View(this);
            line.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
            params.setMargins(0, dp(8), 0, dp(16));
            line.setLayoutParams(params);
            return line;
        };

        TextView focusGoalsHeader = text("Focus Goals", 13, sectionColor);
        focusGoalsHeader.setTypeface(null, Typeface.BOLD);
        focusGoalsHeader.setPadding(0, 0, 0, dp(8));
        content.addView(focusGoalsHeader);
        content.addView(divider.get());

        TextView goalTitle = text("Daily goal", 18, Color.rgb(35, 29, 38));
        goalTitle.setTypeface(null, Typeface.BOLD);
        goalTitle.setPadding(0, 0, 0, dp(2));
        content.addView(goalTitle);
        EditText goalInput = new EditText(this);
        goalInput.setText(String.valueOf(store.dailyGoal()));
        goalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        goalInput.setTextSize(18);
        goalInput.setSingleLine(true);
        goalInput.setGravity(Gravity.CENTER_VERTICAL);
        goalInput.setPadding(dp(16), 0, dp(16), 0);
        goalInput.setBackground(outline(Color.rgb(110, 102, 115), 1, 8));
        LinearLayout.LayoutParams goalInputParams = new LinearLayout.LayoutParams(-1, dp(72));
        goalInputParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(goalInput, goalInputParams);

        TextView goalHint = text("Your current goal is " + store.dailyGoal() + " minutes",
                14, sectionColor);
        goalHint.setPadding(dp(2), 0, 0, dp(14));
        content.addView(goalHint);

        content.addView(button("Save goal", v -> {
            String value = goalInput.getText().toString().trim();
            int parsed;
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException error) {
                android.widget.Toast.makeText(this, "Enter a valid whole number of minutes.",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (parsed < 15 || parsed > 240) {
                android.widget.Toast.makeText(this, "Daily goal must be between 15 and 240 minutes.",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            store.setDailyGoal(parsed);
            android.widget.Toast.makeText(this, "Daily goal saved.",
                    android.widget.Toast.LENGTH_SHORT).show();
            showSettings();
        }));

        content.addView(divider.get());

        TextView preferencesHeader = text("App Preferences", 13, sectionColor);
        preferencesHeader.setTypeface(null, Typeface.BOLD);
        preferencesHeader.setPadding(0, 0, 0, dp(8));
        content.addView(preferencesHeader);
        content.addView(divider.get());

        Button resetButton = outlineButton("Reset local progress", v -> showResetConfirmation());
        resetButton.setTextColor(danger);
        resetButton.setBackground(statefulOutline(danger, dangerDark, 28));
        content.addView(resetButton, new LinearLayout.LayoutParams(-1, dp(52)));

        content.addView(divider.get());

        TextView aboutHeader = text("About", 13, sectionColor);
        aboutHeader.setTypeface(null, Typeface.BOLD);
        aboutHeader.setPadding(0, 0, 0, dp(8));
        content.addView(aboutHeader);
        content.addView(divider.get());

        TextView appName = text("FocusPet", 20, Color.rgb(35, 29, 38));
        appName.setTypeface(null, Typeface.BOLD);
        content.addView(appName);
        content.addView(text("Version " + BuildConfig.VERSION_NAME, 14, sectionColor));
        content.addView(text("A focus timer with a companion that grows alongside you. "
                + "All of your progress is stored privately on this device — "
                + "no account, no ads, and no network connection required.",
                14, Color.rgb(75, 67, 80)));
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Reset local progress?")
                .setMessage("This will permanently erase your focus history, rewards, pets, and streak progress from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes, reset everything", (dialog, which) -> {
                    store.reset();
                    showSettings();
                })
                .show();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    @Override protected void onDestroy() {
        if (timerReceiver != null) unregisterReceiver(timerReceiver);
        super.onDestroy();
    }
}
