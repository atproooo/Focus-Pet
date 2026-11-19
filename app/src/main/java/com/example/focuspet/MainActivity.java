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
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import com.example.focuspet.model.FocusPetModels.CosmeticItem;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.Currency;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST = 7;
    private int purple;
    private int purpleDark;
    private int surface;
    private int lavender;
    private Typeface pixelFont;
    private FocusStore store;
    private FocusRepository repository;
    private LinearLayout content;
    private LinearLayout navigation;
    private TextView title;
    private TextView debugButton;
    private BroadcastReceiver timerReceiver;
    private String currentScreen;
    private LocalDate renderedDay;
    private long hatchResultPetId;
    private final Handler calendarHandler = new Handler(Looper.getMainLooper());
    private final Runnable calendarRefresh = new Runnable() {
        @Override public void run() {
            refreshCalendarScreen();
            scheduleCalendarRefresh();
        }
    };
    private final List<LinearLayout> navigationItems = new ArrayList<>();
    private final List<ImageView> navigationIcons = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        purple = ContextCompat.getColor(this, R.color.button_primary);
        purpleDark = ContextCompat.getColor(this, R.color.button_primary_pressed);
        surface = ContextCompat.getColor(this, R.color.background_app);
        lavender = ContextCompat.getColor(this, R.color.background_accent);
        pixelFont = ResourcesCompat.getFont(this, R.font.fusion_pixel_10px_monospaced_latin);
        store = new FocusStore(this);
        repository = new FocusRepository(this);
        repository.ensureStarterPet();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (store != null && store.hasActiveSession()) {
                    android.widget.Toast.makeText(MainActivity.this,
                            "Finish or end the current focus session first.",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (repository != null && repository.latestPending() != null) {
                    android.widget.Toast.makeText(MainActivity.this,
                            "Claim the pending reward first.",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
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
            } else if (store.hatchResultPetId() > 0 && repository.pet(store.hatchResultPetId()) != null) {
                showHatchResult(repository.pet(store.hatchResultPetId()));
            } else {
                showHome();
            }
        }
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.desktop_background));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        title = text("FOCUSGROVE.EXE", 20, ContextCompat.getColor(this, R.color.text_on_button));
        title.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_on_button));
        title.setPadding(dp(10), 0, dp(8), 0);
        header.setBackgroundResource(R.drawable.bg_focuspet_window_header);
        TextView iconPlaceholder = text("", 20,
                ContextCompat.getColor(this, R.color.text_on_button));
//        iconPlaceholder.setBackground(roundColor(
//                ContextCompat.getColor(this, R.color.window_title_highlight), 2));
        iconPlaceholder.setGravity(Gravity.CENTER);
        iconPlaceholder.setContentDescription("App icon placeholder");
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.setMargins(dp(6), dp(6), dp(4), dp(6));
        header.addView(iconPlaceholder, iconParams);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        header.setMinimumHeight(dp(48));
        shell.addView(header);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(ContextCompat.getColor(this, R.color.window_surface));
        content.setPadding(dp(14), dp(10), dp(14), dp(32));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        navigation.setBackgroundResource(R.drawable.bg_focuspet_taskbar);
        addNavigationButton(navigation, R.drawable.icon_home, "Focus", v -> showHome());
        addNavigationButton(navigation, R.drawable.icon_collection, "Collection", v -> showCollection());
        addNavigationButton(navigation, R.drawable.icon_statistics, "Statistics", v -> showStats());
        addNavigationButton(navigation, R.drawable.icon_settings, "Settings", v -> showSettings());
        navigation.setPadding(dp(4), dp(6), dp(4), dp(6));
        shell.addView(navigation, new LinearLayout.LayoutParams(-1,
                (int) getResources().getDimension(R.dimen.focuspet_navigation_height)));
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        if (BuildConfig.DEBUG) {
            debugButton = text("⚙", 22, ContextCompat.getColor(this, R.color.text_primary));
            debugButton.setGravity(Gravity.CENTER);
            debugButton.setBackgroundResource(R.drawable.bg_focuspet_debug_button);
            debugButton.setContentDescription("Development resource controls");
            debugButton.setOnClickListener(v -> showDebugResourceDialog());
            FrameLayout.LayoutParams debugParams =
                    new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.END);
            debugParams.setMargins(0, 0, dp(16), dp(80));
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
        panel.addView(text("Presets", 15, purpleDark));
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
        showDefaultFontDialog(new AlertDialog.Builder(this).setTitle("Debug resources")
                .setMessage("Development-only controls. These are removed from release builds.")
                .setView(panel)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add resources", (dialog, which) -> {
                    store.addDebugResources(parseAmount(coins), parseAmount(essence), parseAmount(eggs));
                    recreate();
                }));
    }

    private void addDebugPreset(LinearLayout parent, String label, int amount) {
        Button preset = outlineButton(label, v -> {
            store.addDebugResources(amount, amount, amount);
            recreate();
        });
        preset.setTextAppearance(R.style.TextAppearance_FocusPet_Caption);
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

    private void addNavigationButton(LinearLayout navigation, int iconResource, String label,
                                     View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconResource);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
        iconView.setAdjustViewBounds(true);
        TextView labelView = text(label, 14, ContextCompat.getColor(this, R.color.text_primary));
        labelView.setPadding(0, 0, 0, 0);
        labelView.setGravity(Gravity.CENTER);
        item.addView(iconView, new LinearLayout.LayoutParams(dp(26), dp(26)));
        item.addView(labelView, new LinearLayout.LayoutParams(-1, dp(26)));
        item.setBackground(statefulBackground(
                ContextCompat.getColor(this, android.R.color.transparent),
                lavender,
                ContextCompat.getColor(this, android.R.color.darker_gray), 2));
        item.setOnClickListener(listener);
        navigationItems.add(item);
        navigationIcons.add(iconView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        navigation.addView(item, params);
    }

    private void setActiveTab(int index) {
        for (int i = 0; i < navigationItems.size(); i++) {
            navigationItems.get(i).setBackground(roundColor(i == index ? lavender
                    : ContextCompat.getColor(this, android.R.color.transparent), 2));
            navigationIcons.get(i).setColorFilter(ContextCompat.getColor(
                    this, i == index ? R.color.text_primary : R.color.text_secondary));
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

    private GradientDrawable borderedColor(int fill, int border, int width, int radius) {
        GradientDrawable drawable = roundColor(fill, radius);
        drawable.setStroke(dp(width), border);
        return drawable;
    }

    private GradientDrawable outline(int color, int width, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ContextCompat.getColor(this, android.R.color.transparent));
        drawable.setStroke(dp(width), color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private StateListDrawable statefulBackground(int normal, int pressed, int disabled, int radius) {
        StateListDrawable states = new StateListDrawable();
        int border = ContextCompat.getColor(this, R.color.button_outline);
        states.addState(new int[]{-android.R.attr.state_enabled},
                borderedColor(disabled, border, 2, radius));
        states.addState(new int[]{android.R.attr.state_pressed},
                borderedColor(pressed, border, 2, radius));
        states.addState(new int[]{},
                borderedColor(normal, border, 2, radius));
        return states;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeResource(size));
        view.setTextColor(color);
        view.setTypeface(pixelFont);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        return view;
    }

    private TextView systemText(String value, int size, int color) {
        TextView view = text(value, size, color);
        view.setTypeface(Typeface.DEFAULT);
        return view;
    }

    private float textSizeResource(int size) {
        int resourceId;
        switch (size) {
            case 13: resourceId = R.dimen.text_size_13; break;
            case 14: resourceId = R.dimen.text_size_14; break;
            case 15: resourceId = R.dimen.text_size_15; break;
            case 16: resourceId = R.dimen.text_size_16; break;
            case 17: resourceId = R.dimen.text_size_17; break;
            case 18: resourceId = R.dimen.text_size_18; break;
            case 19: resourceId = R.dimen.text_size_19; break;
            case 20: resourceId = R.dimen.text_size_20; break;
            case 22: resourceId = R.dimen.text_size_22; break;
            case 24: resourceId = R.dimen.text_size_24; break;
            case 25: resourceId = R.dimen.focuspet_title_text; break;
            case 28: resourceId = R.dimen.text_size_28; break;
            case 64: resourceId = R.dimen.text_size_64; break;
            default: return getResources().getDimension(R.dimen.text_size_16);
        }
        return getResources().getDimension(resourceId);
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextAppearance(R.style.Widget_FocusPet_PrimaryButton);
        button.setAllCaps(false);
        button.setMinHeight((int) getResources().getDimension(R.dimen.focuspet_compact_button_height));
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(statefulBackground(
                ContextCompat.getColor(this, R.color.button_primary),
                ContextCompat.getColor(this, R.color.button_primary_pressed),
                ContextCompat.getColor(this, R.color.button_primary_disabled), 3));
        button.setTextColor(ContextCompat.getColor(this, R.color.text_on_button));
        button.setTypeface(pixelFont);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.text_size_18));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,
                (int) getResources().getDimension(R.dimen.focuspet_button_height));
        params.setMargins(0, dp(18), 0, dp(5));
        button.setLayoutParams(params);
        return button;
    }

    private Button outlineButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextAppearance(R.style.Widget_FocusPet_OutlineButton);
        button.setAllCaps(false);
        button.setMinHeight(dp(46));
        button.setBackground(statefulOutline(
                ContextCompat.getColor(this, R.color.button_outline), purple, 3));
        button.setOnClickListener(listener);
        return button;
    }

    private StateListDrawable statefulOutline(int border, int pressedColor, int radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, outline(pressedColor, 2, radius));
        states.addState(new int[]{}, outline(border, 2, radius));
        return states;
    }

    private void reset(String screenTitle) {
        if (hatchResultPetId > 0 && !"New pet".equals(screenTitle)) {
            store.acknowledgeHatchResult();
            hatchResultPetId = 0;
        }
        currentScreen = screenTitle;
        renderedDay = LocalDate.now();
        title.setText(screenTitle);
        content.removeAllViews();
        content.setPadding(dp(20), dp(8), dp(20), dp(32));
    }

    private void showHome() {
        reset("Focus Grove");
        setSessionLock(false);
        setActiveTab(0);
        content.addView(text("Start a session with your pets today! ", 18, ContextCompat.getColor(this, R.color.text_primary)));
        View companion = companionCard();
        companion.setOnClickListener(v -> showCompanionChooser());
        content.addView(companion);
        TextView today = text("Today", 20, purpleDark);
        today.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        today.setPadding(dp(0), dp(24), dp(0), dp(0));
        content.addView(today);
        content.addView(text(store.todayMinutes() + " min focused", 28, ContextCompat.getColor(this, R.color.text_primary)));

        int todayMinutes = store.todayMinutes();
        int goal = store.dailyGoal();
        int remaining = Math.max(0, goal - todayMinutes);
        ProgressBar goalProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        goalProgress.setMax(goal);
        goalProgress.setProgress(Math.min(todayMinutes, goal));
        goalProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(purple));
        goalProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender));
        LinearLayout.LayoutParams goalProgressParams = new LinearLayout.LayoutParams(-1, dp(12));
        goalProgressParams.setMargins(0, dp(8), 0, dp(4));
        content.addView(goalProgress, goalProgressParams);
        content.addView(text(remaining == 0
                ? "Daily goal reached? nice work today!"
                : remaining + " min to reach your " + goal + "-min daily goal",
                14, ContextCompat.getColor(this, R.color.text_secondary)));

        content.addView(text(store.sessions() + " sessions completed.  Keep showing up!",
                16, ContextCompat.getColor(this, R.color.text_primary)));
        content.addView(button("Start focus", v -> showSetup()));

        TextView recentTitle = text("Recent Sessions", 20, purpleDark);
        recentTitle.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        recentTitle.setPadding(0, dp(24), 0, dp(8));
        content.addView(recentTitle);

        List<FocusSession> recent = repository.recentHistory(3);
        if (recent.isEmpty()) {
            content.addView(systemText("No sessions yet. Your recent focus sessions will appear here.",
                    16, ContextCompat.getColor(this, R.color.text_secondary)));
        } else {
            for (FocusSession session : recent) {
                addHistoryRow(content, session);
            }
        }

        TextView moreCard = text("More...", 15, purpleDark);
        moreCard.setBackgroundResource(R.drawable.bg_focuspet_card);
        moreCard.setGravity(Gravity.CENTER);
        moreCard.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
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
        TextView back = text("Back", 16, purpleDark);
        back.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        back.setPadding(0, dp(8), 0, dp(18));
        back.setOnClickListener(v -> showHome());
        content.addView(back);

        TextView nameLabel = text("What are you focusing on? (optional)", 14, ContextCompat.getColor(this, R.color.text_secondary));
        nameLabel.setPadding(0, 0, 0, dp(4));
        content.addView(nameLabel);

        EditText sessionNameInput = new EditText(this);
        sessionNameInput.setHint("e.g. math homework");
        sessionNameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        sessionNameInput.setTextAppearance(R.style.TextAppearance_FocusPet_Input);
        sessionNameInput.setSingleLine(true);
        sessionNameInput.setGravity(Gravity.CENTER_VERTICAL);
        sessionNameInput.setPadding(dp(16), 0, dp(16), 0);
        sessionNameInput.setBackground(outline(ContextCompat.getColor(this, R.color.text_secondary), 1, 8));
        sessionNameInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(60) });
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(72));
        nameParams.setMargins(0, 0, 0, dp(4));
        content.addView(sessionNameInput, nameParams);
        TextView nameHint = text("Leave blank and we'll call it a Focus session.", 14, ContextCompat.getColor(this, R.color.text_secondary));
        nameHint.setPadding(dp(16), dp(2), dp(16), dp(12));
        content.addView(nameHint, new LinearLayout.LayoutParams(-1, -2));

        content.addView(text("How long would you like to focus?", 19, ContextCompat.getColor(this, R.color.text_primary)));

        TextView customLabel = text("Custom focus time (minutes)", 14, ContextCompat.getColor(this, R.color.text_secondary));
        customLabel.setPadding(dp(16), dp(18), dp(16), dp(4));
        content.addView(customLabel);

        EditText customDuration = new EditText(this);
        customDuration.setText("25");
        customDuration.setInputType(InputType.TYPE_CLASS_NUMBER);
        customDuration.setTextAppearance(R.style.TextAppearance_FocusPet_Input);
        customDuration.setSingleLine(true);
        customDuration.setGravity(Gravity.CENTER_VERTICAL);
        customDuration.setPadding(dp(16), 0, dp(16), 0);
        customDuration.setBackground(outline(ContextCompat.getColor(this, R.color.text_secondary), 1, 8));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(72));
        inputParams.setMargins(0, 0, 0, dp(4));
        content.addView(customDuration, inputParams);
        TextView hint = text("Choose any whole number from 1 to 240 minutes.", 14, ContextCompat.getColor(this, R.color.text_secondary));
        hint.setPadding(dp(16), dp(2), dp(16), dp(12));
        content.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout presets = new LinearLayout(this);
        presets.setGravity(Gravity.CENTER);
        int[] presetValues = {15, 25, 45, 60};
        for (int value : presetValues) {
            Button preset = outlineButton(value + " min", v -> customDuration.setText(String.valueOf(value)));
            preset.setTextAppearance(R.style.TextAppearance_FocusPet_Caption);
            LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(0, dp(44), 1);
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
            TextView nameCaption = text("FOCUSING ON", 13, purple);
            nameCaption.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
            nameCaption.setGravity(Gravity.CENTER);
            nameCaption.setPadding(0, dp(24), 0, dp(4));
            content.addView(nameCaption);
            TextView nameValue = text(displayName, 22, purpleDark);
            nameValue.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
            nameValue.setGravity(Gravity.CENTER);
            content.addView(nameValue);
        }
        TextView timer = text("Loading...", 64, ContextCompat.getColor(this, R.color.text_primary));
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
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(purple));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender));
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(12)));
        Pet companion = repository.selectedPet();
        TextView encouragement = text((companion == null ? "Your pet" : companion.name)
                + " is cheering you on.", 19, ContextCompat.getColor(this, R.color.text_primary));
        encouragement.setGravity(Gravity.CENTER);
        encouragement.setPadding(0, dp(24), 0, dp(28));
        content.addView(encouragement);
        Button end = outlineButton("End session", v -> showEndSessionConfirmation());
        content.addView(end);
        content.addView(petAnimationSlot(companion));
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
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle("End focus session?")
                .setMessage("You can still claim rewards for the time you focused, but ending early may reduce XP, coins, Life Essence, Pet Egg chances, and the minutes credited toward your daily goal, by 70%.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, end session", (dialog, which) -> {
                    long totalMillis = (long) store.activeMinutes() * 60_000L;
                    long remaining = store.activePaused()
                            ? store.activeRemaining()
                            : Math.max(0L, store.activeEndAt() - SystemClock.elapsedRealtime());
                    int focusedMinutes = (int) (Math.max(0L, totalMillis - remaining) / 60_000L);
                    if (focusedMinutes < 4) showFinalEndWarning();
                    else sendService(FocusService.ACTION_CANCEL);
                })
                );
    }

    private void showFinalEndWarning() {
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle("Really end your session early?")
                .setMessage("Sessions under 4 focused minutes may count as 0 minutes toward your goal. This cannot be undone. Do you still want to end now?")
                .setNegativeButton("No, keep focusing", null)
                .setPositiveButton("Yes, end anyway", (dialog, which) ->
                        sendService(FocusService.ACTION_CANCEL))
                );
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
                22, purpleDark);
        finishName.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        finishName.setGravity(Gravity.CENTER);
        finishName.setPadding(0, dp(8), 0, dp(8));
        content.addView(finishName);
        content.addView(text("★ Great work, your pet is proud.", 24, ContextCompat.getColor(this, R.color.text_primary)));
        content.addView(text(minutes + " minutes focused\n"
                + (endedEarly ? "Partial rewards are ready to claim." : "Your rewards are ready to claim."),
                20, ContextCompat.getColor(this, R.color.text_secondary)));
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
        TextView claimName = text(displayLabel(session.label), 22, purpleDark);
        claimName.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        claimName.setGravity(Gravity.CENTER);
        claimName.setPadding(0, dp(8), 0, dp(8));
        content.addView(claimName);
        content.addView(text("Your session rewards", 24, ContextCompat.getColor(this, R.color.text_primary)));
        content.addView(systemText("★ +" + session.xpReward + " XP\n"
                + "● +" + session.coinReward + " coins\n"
                + "✦ +" + session.essenceReward + " Life Essence\n"
                + (session.eggReward ? "Pet Egg found!" : "No Pet Egg this time"),
                20, ContextCompat.getColor(this, R.color.text_secondary)));
        content.addView(button("Claim rewards", v -> {
            if (repository.claimRewards(sessionId)) {
                showHome();
            }
        }));
    }

    private TextView imagePlaceholder(String label) {
        TextView placeholder = text("🐾\n" + label, 22, ContextCompat.getColor(this, R.color.text_primary));
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(roundColor(ContextCompat.getColor(this, R.color.background_card), 16));
        placeholder.setContentDescription("Reserved space for " + label.toLowerCase(Locale.US));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(140));
        params.setMargins(dp(8), dp(8), dp(8), dp(8));
        placeholder.setLayoutParams(params);
        return placeholder;
    }

    private void showCollection() {
        reset("Pet Collection");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_collection, content, false);
        content.addView(screen);

        LinearLayout resourceBar = screen.findViewById(R.id.resource_bar);
        addResourceChip(resourceBar, "●\nCoins\n" + store.coins(), "Coins",
                "You have " + store.coins() + " Coins.\n"
                        + "Earned by completing focus sessions. Spend them on clothing and decorations in the Shop.");
        addResourceChip(resourceBar, "✦\nEssence\n" + store.lifeEssence(), "Life Essence",
                "You have " + store.lifeEssence() + " Life Essence.\n"
                        + "Earned through focus. Hatching one Pet Egg costs 100 Essence. "
                        + "Pet Eggs drop from focus sessions over time.");
        addResourceChip(resourceBar, "Eggs  " + store.petEggs(), "Pet Eggs", null);
        Button shop = button("Shop", v -> showShop());
        shop.setId(R.id.open_shop);
        screen.addView(shop, 1);

        LinearLayout ownedPets = screen.findViewById(R.id.owned_pets_container);
        Pet selected = repository.selectedPet();
        for (Pet pet : repository.pets()) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, ownedPets, false);
            configurePetCard(card, pet.name + " the " + pet.species,
                    pet.rarity + " - Level " + pet.level(),
                    pet.id == (selected == null ? -1 : selected.id)
                            ? "Selected companion - Tap for details" : "Tap for details",
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
                showDefaultFontDialog(new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton("OK", null)
                        );
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
        Pet latest = repository.pet(pet.id);
        if (latest == null) { showCollection(); return; }
        pet = latest;
        reset(pet.name + "'s details");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_pet_detail, content, false);
        content.addView(screen);
        screen.findViewById(R.id.pet_detail_back).setOnClickListener(v -> showCollection());
        ((TextView) screen.findViewById(R.id.pet_detail_name))
                .setText(pet.name + " the " + pet.species);
        ((TextView) screen.findViewById(R.id.pet_detail_meta)).setText(
                petDetailMeta(pet));
        ((ProgressBar) screen.findViewById(R.id.pet_detail_progress))
                .setProgress(pet.experience % 100);
        ((TextView) screen.findViewById(R.id.pet_detail_description)).setText(
                pet.description + "\n\nHatched: "
                        + new SimpleDateFormat("MMM d, yyyy", Locale.US)
                        .format(new Date(pet.hatchedAt))
                        + "\nWearing: " + equippedSummary(pet.id));
        long petId = pet.id;
        LinearLayout actions = screen.findViewById(R.id.pet_detail_actions);
        actions.addView(text("Interact with your pet", 17, purpleDark));
        TextView feedback = text("Give your pet a short break between interactions.", 14, ContextCompat.getColor(this, R.color.text_secondary));
        feedback.setId(R.id.pet_interaction_feedback);
        actions.addView(feedback);
        addInteraction(actions, screen, petId, "Pet", R.id.pet_interaction_pet, PetInteraction.Action.PET, feedback);
        addInteraction(actions, screen, petId, "Play", R.id.pet_interaction_play, PetInteraction.Action.PLAY, feedback);
        addInteraction(actions, screen, petId, "Rest", R.id.pet_interaction_rest, PetInteraction.Action.REST, feedback);
        Button wardrobe = button("Customize", v -> showWardrobe(petId));
        wardrobe.setId(R.id.open_wardrobe);
        actions.addView(wardrobe);
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
        boolean canHatch = store.canHatch() || store.pendingHatch() != null;
        ((TextView) screen.findViewById(R.id.hatching_inventory)).setText(
                "Pet Eggs: " + eggs + "\nLife Essence: " + essence + " / 100 required");
        Button hatchButton = screen.findViewById(R.id.hatch_button);
        setAvailable(hatchButton, canHatch);
        if (store.pendingHatch() != null) hatchButton.setText("Resume hatching");
        final String requestId = UUID.randomUUID().toString();
        hatchButton.setOnClickListener(v -> {
            setAvailable(hatchButton, false);
            try {
                Pet hatched = repository.hatchPet(requestId);
                if (hatched != null) showHatchResult(hatched);
                else { toast("You need 1 Pet Egg and 100 Essence."); showHatching(); }
            } catch (IllegalStateException error) {
                toast("Hatching was interrupted. Please try again.");
                showHatching();
            }
        });
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showDefaultFontDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            setDefaultFont(dialog.getWindow().getDecorView());
        });
        dialog.show();
    }

    private void setDefaultFont(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.DEFAULT);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setDefaultFont(group.getChildAt(i));
            }
        }
    }

    private void setAvailable(Button button, boolean available) {
        button.setEnabled(available);
        button.setBackground(statefulBackground(
                ContextCompat.getColor(this, R.color.button_primary),
                ContextCompat.getColor(this, R.color.button_primary_pressed),
                ContextCompat.getColor(this, R.color.button_primary_disabled), 3));
        button.setTextColor(available
                ? ContextCompat.getColor(this, R.color.text_on_button)
                : ContextCompat.getColor(this, R.color.text_secondary));
        button.setTypeface(pixelFont);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.text_size_18));
    }

    private String equippedSummary(long petId) {
        List<String> names = new ArrayList<>();
        for (CosmeticItem item : ShopCatalog.COSMETICS) {
            if (item.id.equals(store.equippedItemId(petId, item.slotKey))) names.add(item.name);
        }
        return names.isEmpty() ? "None" : android.text.TextUtils.join(", ", names);
    }

    private View petAnimationSlot(Pet pet) {
        View slot = LayoutInflater.from(this).inflate(R.layout.view_pet_animation, content, false);
        bindPetAnimation(slot, pet);
        return slot;
    }

    private void bindPetAnimation(View slot, Pet pet) {
        slot.setTag(pet == null ? null : pet.id);
        slot.findViewById(R.id.pet_animation_image).setTag(pet == null ? null : pet.imageKey);
        ((TextView) slot.findViewById(R.id.pet_animation_placeholder))
                .setText((pet == null ? "Your pet" : pet.name) + "\nPet animation");
        slot.setContentDescription(pet == null ? "Reserved pet animation area"
                : "Animation area for " + pet.name + ". Wearing: " + equippedSummary(pet.id));
    }

    private void showHatchResult(Pet pet) {
        reset("New pet");
        hatchResultPetId = pet.id;
        setSessionLock(false);
        setActiveTab(1);
        View screen = LayoutInflater.from(this).inflate(R.layout.screen_hatch_result, content, false);
        content.addView(screen);
        ((TextView) screen.findViewById(R.id.hatch_result_name)).setText(pet.name + " the " + pet.species);
        ((TextView) screen.findViewById(R.id.hatch_result_meta)).setText(
                "Hatched successfully\n" + pet.rarity + " - Level " + pet.level());
        bindPetAnimation(screen.findViewById(R.id.pet_animation_container), pet);
        LinearLayout actions = screen.findViewById(R.id.hatch_result_actions);
        Button select = button("Choose as companion", v -> {
            repository.selectPet(pet.id);
            showHome();
        });
        select.setId(R.id.hatch_result_select);
        actions.addView(select);
        actions.addView(button("View pet", v -> showPetDetail(pet)));
        actions.addView(outlineButton("Back to collection", v -> showCollection()));
    }

    private String petDetailMeta(Pet pet) {
        return pet.rarity + " companion" + (pet.selected ? " - Selected" : "")
                + "\nLevel " + pet.level() + " - " + pet.experience + " XP"
                + "\nMood / energy: " + pet.mood + " / " + pet.energy;
    }

    private void addInteraction(LinearLayout parent, View screen, long petId, String label, int id,
                                PetInteraction.Action action, TextView feedback) {
        Button control = outlineButton(label, v -> {
            FocusRepository.InteractionResult result = repository.interact(petId, action);
            Pet updated = repository.pet(petId);
            if (updated == null) { showCollection(); return; }
            ((TextView) screen.findViewById(R.id.pet_detail_meta)).setText(petDetailMeta(updated));
            switch (result) {
                case UPDATED:
                    feedback.setText(action == PetInteraction.Action.PET ? updated.name + " enjoyed being petted."
                            : action == PetInteraction.Action.PLAY ? updated.name + " enjoyed playing with you."
                            : updated.name + " rested and recovered energy.");
                    break;
                case COOLDOWN: feedback.setText("Please wait a few seconds before another interaction."); break;
                case LOW_ENERGY: feedback.setText("Your pet needs to rest before playing again."); break;
                default: feedback.setText("This pet is no longer available.");
            }
        });
        control.setId(id);
        parent.addView(control);
    }

    private void showShop() {
        reset("Shop");
        setSessionLock(false);
        setActiveTab(1);
        content.addView(outlineButton("Back to collection", v -> showCollection()));
        TextView balance = text("Coins: " + store.coins() + "\nEssence: " + store.lifeEssence()
                + "\nPet Eggs: " + store.petEggs(), 18, ContextCompat.getColor(this, R.color.text_secondary));
        balance.setId(R.id.shop_balance);
        content.addView(balance);
        content.addView(systemText("Coins buy clothing and decorations.\n"
                + "Pet Eggs drop from focus sessions. Hatching costs one egg and 100 Essence.", 15, ContextCompat.getColor(this, R.color.text_secondary)));
        for (ShopProduct product : ShopCatalog.PRODUCTS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.addView(text(product.name, 19, purpleDark));
            boolean cosmetic = product.productType == ProductType.COSMETIC;
            boolean owned = cosmetic && store.ownedCosmeticIds().contains(product.itemKey);
            String currency = product.currency == Currency.COINS ? "Coins" : "Essence";
            boolean enough = (product.currency == Currency.COINS ? store.coins() : store.lifeEssence()) >= product.price;
            String label = owned ? "Owned" : enough ? "Buy " + product.price + " " + currency
                    : "Need " + product.price + " " + currency;
            Button buy = button(label, v -> confirmPurchase(product));
            buy.setTag("buy_" + product.id);
            setAvailable(buy, !owned && enough);
            row.addView(buy);
            if (cosmetic) row.addView(text("Permanently unlocked for your pets.", 14, ContextCompat.getColor(this, R.color.text_secondary)));
            content.addView(row);
        }
        Pet selected = repository.selectedPet();
        if (selected != null) content.addView(button("Customize companion", v -> showWardrobe(selected.id)));
    }

    private void confirmPurchase(ShopProduct product) {
        String requestId = UUID.randomUUID().toString();
        showDefaultFontDialog(new AlertDialog.Builder(this).setTitle("Buy " + product.name + "?")
                .setMessage(product.price + (product.currency == Currency.COINS ? " Coins" : " Essence"))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Buy", (dialog, which) -> {
                    FocusStore.PurchaseResult result = store.purchase(product.id, requestId);
                    if (result == FocusStore.PurchaseResult.PURCHASED) toast("Purchased " + product.name + ".");
                    else if (result == FocusStore.PurchaseResult.ALREADY_OWNED) toast("You already own this item.");
                    else if (result == FocusStore.PurchaseResult.INSUFFICIENT_RESOURCES) toast("Not enough resources.");
                    else toast("Purchase could not be saved. Please try again.");
                    showShop();
                }));
    }

    private void showWardrobe(long petId) {
        Pet pet = repository.pet(petId);
        if (pet == null) { showCollection(); return; }
        reset("Customize " + pet.name);
        setActiveTab(1);
        content.addView(outlineButton("Back to pet", v -> showPetDetail(pet)));
        content.addView(text("Wearing: " + equippedSummary(petId), 17, ContextCompat.getColor(this, R.color.text_secondary)));
        boolean hasItems = false;
        for (CosmeticItem item : ShopCatalog.COSMETICS) {
            if (!store.ownedCosmeticIds().contains(item.id)) continue;
            hasItems = true;
            content.addView(text(item.name, 18, purpleDark));
            boolean equipped = item.id.equals(store.equippedItemId(petId, item.slotKey));
            Button toggle = button(equipped ? "Remove " + item.name : "Equip " + item.name, v -> {
                if (equipped) store.unequip(petId, item.slotKey);
                else if (!repository.equipCosmetic(petId, item.id)) toast("This item is not available.");
                showWardrobe(petId);
            });
            toggle.setTag("equip_" + item.id);
            content.addView(toggle);
        }
        if (!hasItems) content.addView(systemText("Buy clothing and decorations in the shop to customize your pet.", 16, ContextCompat.getColor(this, R.color.text_secondary)));
        content.addView(button("Shop", v -> showShop()));
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
                            pet.rarity + " - Level " + pet.level(),
                            pet.id == (selected == null ? -1 : selected.id)
                                    ? "Selected companion - Tap to keep" : "Tap to select",
                            pet.experience % 100, 1f, v -> {
                                repository.selectPet(pet.id);
                                showHome();
                            });
                    container.addView(card);
                }
    }

    private void showStats() {
        reset("Focus Statistics");
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
            TextView empty = systemText("No focus sessions yet. Complete your first session to see it here.",
                    16, ContextCompat.getColor(this, R.color.text_secondary));
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
        label = SessionLabels.abbreviate(label, 20);
        String status = session.endedEarly ? "Ended early"
                : FocusSession.STATUS_CLAIMED.equals(session.status) ? "Completed"
                : FocusSession.STATUS_CANCELLED.equals(session.status) ? "Ended early"
                : "Reward pending";
        TextView labelView = row.findViewById(R.id.history_label);
        labelView.setMaxLines(1);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labelView.setText(label + " - " + status);
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
                status + " - " + session.actualMinutes + " minutes focused");

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
                ? "Rewards claimed\n★ +" + session.xpReward + " XP\n● +" + session.coinReward
                + " coins\n✦ +" + session.essenceReward + " Life Essence"
                + (session.eggReward ? "\nPet Egg found" : "")
                : FocusSession.STATUS_CANCELLED.equals(session.status)
                ? "No completion rewards were granted for this session."
                : "Rewards are waiting to be claimed.";
        ((TextView) screen.findViewById(R.id.history_detail_rewards)).setText(rewards);
    }

    private void addDetailLine(LinearLayout parent, String label, String value) {
        TextView line = (TextView) LayoutInflater.from(this)
                .inflate(R.layout.item_detail_line, parent, false);
        line.setText(label + "\n" + value);
        parent.addView(line);
    }

    private void showSettings() {
        reset("Settings");
        setActiveTab(3);
        content.setPadding(dp(20), dp(8), dp(20), dp(100));

        int sectionColor = ContextCompat.getColor(this, R.color.text_secondary);
        int danger = ContextCompat.getColor(this, R.color.button_outline);
        int dangerDark = ContextCompat.getColor(this, R.color.button_outline);

        TextView focusGoalsHeader = text("Focus Goals", 20, purpleDark);
        focusGoalsHeader.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        focusGoalsHeader.setPadding(0, 0, 0, dp(8));
        content.addView(focusGoalsHeader);

        TextView goalTitle = text("Daily Goal", 14, ContextCompat.getColor(this, R.color.text_primary));
        goalTitle.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        goalTitle.setPadding(0, 0, 0, dp(2));
        content.addView(goalTitle);
        EditText goalInput = new EditText(this);
        goalInput.setText(String.valueOf(store.dailyGoal()));
        goalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        goalInput.setTextAppearance(R.style.TextAppearance_FocusPet_Input);
        goalInput.setSingleLine(true);
        goalInput.setGravity(Gravity.CENTER_VERTICAL);
        goalInput.setPadding(dp(16), 0, dp(16), 0);
        goalInput.setBackground(roundColor(ContextCompat.getColor(this, R.color.background_card), 12));
        LinearLayout.LayoutParams goalInputParams = new LinearLayout.LayoutParams(-1, dp(56));
        goalInputParams.setMargins(0, dp(4), 0, dp(12));
        content.addView(goalInput, goalInputParams);

        TextView goalHint = text("Your current goal is " + store.dailyGoal() + " minutes",
                13, sectionColor);
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
        }), new LinearLayout.LayoutParams(-1, dp(46)));

        TextView preferencesHeader = text("App Preferences", 20, purpleDark);
        preferencesHeader.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        preferencesHeader.setPadding(0, 0, 0, dp(8));
        LinearLayout.LayoutParams prefsHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        prefsHeaderParams.setMargins(0, dp(32), 0, 0);
        content.addView(preferencesHeader, prefsHeaderParams);

        Button resetButton = outlineButton("Reset local progress", v -> showResetConfirmation());
        resetButton.setTextColor(danger);
        resetButton.setBackground(statefulOutline(danger, dangerDark, 3));
        content.addView(resetButton, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView aboutHeader = text("About", 20, purpleDark);
        aboutHeader.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        aboutHeader.setPadding(0, 0, 0, dp(8));
        LinearLayout.LayoutParams aboutHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        aboutHeaderParams.setMargins(0, dp(32), 0, 0);
        content.addView(aboutHeader, aboutHeaderParams);

        TextView appName = text("FocusPet", 20, ContextCompat.getColor(this, R.color.text_primary));
        appName.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        content.addView(appName);
        content.addView(text("Version " + BuildConfig.VERSION_NAME, 13, sectionColor));
        content.addView(systemText("A focus timer with a companion that grows alongside you. "
                + "All of your progress is stored privately on this device. "
                + "no account, no ads, and no network connection required.",
                13, ContextCompat.getColor(this, R.color.text_secondary)));
    }

    private void showResetConfirmation() {
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle("Reset local progress?")
                .setMessage("This will permanently erase your focus history, rewards, pets, and streak progress from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes, reset everything", (dialog, which) -> {
                    repository.clearAllData();
                    repository.ensureStarterPet();
                    showSettings();
                })
                );
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private void refreshCalendarScreen() {
        if (!LocalDate.now().equals(renderedDay)) {
            if ("Focus Grove".equals(currentScreen)) showHome();
            else if ("Focus Statistics".equals(currentScreen)) showStats();
        }
    }

    private void scheduleCalendarRefresh() {
        calendarHandler.removeCallbacks(calendarRefresh);
        long midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        calendarHandler.postDelayed(calendarRefresh, Math.max(1L, midnight - System.currentTimeMillis()));
    }

    @Override protected void onResume() {
        super.onResume();
        refreshCalendarScreen();
        scheduleCalendarRefresh();
    }

    @Override protected void onPause() {
        calendarHandler.removeCallbacks(calendarRefresh);
        super.onPause();
    }

    @Override protected void onDestroy() {
        calendarHandler.removeCallbacks(calendarRefresh);
        if (timerReceiver != null) unregisterReceiver(timerReceiver);
        super.onDestroy();
    }
}
