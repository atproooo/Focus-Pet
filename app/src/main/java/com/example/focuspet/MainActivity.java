package com.example.focuspet;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;

import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import java.util.Set;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import com.example.focuspet.model.FocusPetModels.ShopProduct;
import com.example.focuspet.model.FocusPetModels.ProductType;
import com.example.focuspet.model.FocusPetModels.Currency;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST = 7;
    private enum CustomizeSection { ACCESSORIES, EXPRESSIONS, BACKGROUND }

    private int purple;
    private int purpleDark;
    private int surface;
    private int lavender;
    private int navigationSelected;
    private Typeface pixelFont;
    private FocusStore store;
    private FocusRepository repository;
    private ShopRepository shopRepository;
    private ShopCategory selectedShopCategory = ShopCategory.ALL;
    private long expressionInventoryReturnPetId;
    private long accessoryShopReturnPetId;
    private long backgroundShopReturnPetId;
    private LinearLayout content;
    private LinearLayout navigation;
    private TextView title;
    private TextView debugButton;
    private BroadcastReceiver timerReceiver;
    private String currentScreen;
    private LocalDate renderedDay;
    private HatchDraft activeHatchDraft;
    private EditText hatchNameInput;
    private boolean hatchRecoveryRequired;
    private boolean hatchCommitPending;
    private AnimatedImageDrawable hatchAnimation;
    private final Handler calendarHandler = new Handler(Looper.getMainLooper());
    private final Runnable calendarRefresh = new Runnable() {
        @Override public void run() {
            refreshCalendarScreen();
            scheduleCalendarRefresh();
        }
    };
    private final List<LinearLayout> navigationItems = new ArrayList<>();
    private final List<ImageView> navigationIcons = new ArrayList<>();
    private final PetArtworkRenderer petArtworkRenderer = new PetArtworkRenderer();
    private final List<PetAnimationController> petAnimationControllers = new ArrayList<>();
    private SessionCelebrationAnimator sessionCelebrationAnimator;
    private RewardBurstAnimator rewardBurstAnimator;
    private boolean petAnimationsResumed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        purple = ContextCompat.getColor(this, R.color.button_primary);
        purpleDark = ContextCompat.getColor(this, R.color.button_primary_pressed);
        surface = ContextCompat.getColor(this, R.color.background_app);
        lavender = ContextCompat.getColor(this, R.color.background_accent);
        navigationSelected = ContextCompat.getColor(this, R.color.navigation_selected);
        pixelFont = ResourcesCompat.getFont(this, R.font.fusion_pixel_10px_monospaced_latin);
        store = new FocusStore(this);
        repository = new FocusRepository(this);
        shopRepository = ShopRepository.getInstance(this);
        repository.ensureStarterPet();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (activeHatchDraft != null) {
                    if (hatchCommitPending) {
                        toast("Your confirmed hatch is being completed.");
                        return;
                    }
                    moveBackInHatchFlow();
                    return;
                }
                if (hatchRecoveryRequired) {
                    toast("Hatching is being recovered. Please retry.");
                    return;
                }
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
            } else {
                HatchDraft draft = null;
                hatchCommitPending = store.pendingHatch() != null;
                try {
                    repository.recoverPendingHatch();
                    draft = repository.activeHatchDraft();
                } catch (IllegalStateException error) {
                    hatchRecoveryRequired = true;
                }
                if (draft != null) {
                    activeHatchDraft = draft;
                    hatchCommitPending = hatchCommitPending
                            && draft.step == HatchDraft.STEP_PREVIEW;
                    showHatchDraft(draft);
                } else if (hatchRecoveryRequired) {
                    showHatchRecoveryFailure();
                } else if (store.hatchResultPetId() > 0
                        && repository.pet(store.hatchResultPetId()) != null) {
                    showHatchSuccessDialog(repository.pet(store.hatchResultPetId()));
                } else {
                    showHome();
                }
            }
        }
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.desktop_background));
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header = new LinearLayout(this);
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });
        header.setGravity(Gravity.CENTER_VERTICAL);
        title = text("FOCUSGROVE.EXE", 20,
                ContextCompat.getColor(this, R.color.window_title_text));
        title.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        title.setTextColor(ContextCompat.getColor(this, R.color.window_title_text));
        title.setPadding(dp(32), dp(16), dp(8), dp(12));
        header.setBackgroundResource(R.drawable.bg_focuspet_window_header);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        header.setMinimumHeight(dp(64));
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
                navigationSelected,
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
            navigationItems.get(i).setBackground(roundColor(i == index ? navigationSelected
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

    private TextView backNavigationButton(String label, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText("< " + label);
        button.setBackgroundResource(R.drawable.bg_focuspet_back);
        button.setClickable(true);
        button.setFocusable(true);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setTypeface(pixelFont);
        button.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setOnClickListener(listener);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private StateListDrawable statefulOutline(int border, int pressedColor, int radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, outline(pressedColor, 2, radius));
        states.addState(new int[]{}, outline(border, 2, radius));
        return states;
    }

    private void reset(String screenTitle) {
        stopHatchAnimation();
        cancelRewardAnimations();
        currentScreen = screenTitle;
        renderedDay = LocalDate.now();
        title.setText(screenTitle);
        clearPetAnimationControllers();
        content.removeAllViews();
        content.setPadding(dp(20), dp(8), dp(20), dp(32));
    }

    private void showHome() {
        reset("Focus");
        setSessionLock(false);
        setActiveTab(0);
        content.addView(text("Start a session with your pets today! ", 12, ContextCompat.getColor(this, R.color.text_primary)));
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

        TextView moreCard = text("More...", 15,
                ContextCompat.getColor(this, R.color.text_on_button));
        moreCard.setGravity(Gravity.CENTER);
        moreCard.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        moreCard.setTextColor(ContextCompat.getColor(this, R.color.text_on_button));
        moreCard.setBackground(statefulBackground(
                ContextCompat.getColor(this, R.color.button_secondary),
                ContextCompat.getColor(this, R.color.button_secondary_pressed),
                ContextCompat.getColor(this, R.color.button_primary_disabled), 3));
        moreCard.setPadding(dp(12), dp(14), dp(12), dp(14));
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(-1, -2);
        moreParams.setMargins(0, dp(12), 0, dp(8));
        moreCard.setOnClickListener(v -> showStats());
        content.addView(moreCard, moreParams);
    }

    private View companionCard() {
        View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, content, false);
        Pet pet = repository.selectedPet();
        String petName = pet == null ? "Toast" : pet.name;
        int petLevel = pet == null ? store.level() : pet.level();
        int petXp = pet == null ? store.experience() : pet.experience;
        configurePetCard(card, pet, petName,
                "Level " + petLevel + "  ·  " + petXp + " XP",
                "Your companion",
                petXp % 100, 1f, v -> showCompanionChooser());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(10), 0, dp(4));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showSetup() {
        reset("Focus setup");
        setSessionLock(false);
        setActiveTab(0);
        content.addView(backNavigationButton("Back", v -> showHome()));

        TextView nameLabel = text("What are you focusing on? (optional)", 14, ContextCompat.getColor(this, R.color.text_secondary));
        nameLabel.setPadding(0, dp(18), 0, dp(4));
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
        TextView hint = text("Choose any whole number from 5 to 240 minutes.", 14, ContextCompat.getColor(this, R.color.text_secondary));
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
            if (selectedMinutes < 5 || selectedMinutes > 240) {
                android.widget.Toast.makeText(this, "Focus time must be between 5 and 240 minutes.",
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
        FocusSession active = store.activeSessionId() > 0
                ? repository.pendingSession(store.activeSessionId()) : null;
        Pet companion = petFromSession(active);
        if (companion == null) companion = repository.selectedPet();
        TextView encouragement = text((companion == null ? "Your pet" : companion.name)
                + " is cheering you on.", 19, ContextCompat.getColor(this, R.color.text_primary));
        encouragement.setGravity(Gravity.CENTER);
        encouragement.setPadding(0, dp(24), 0, dp(24));
        content.addView(encouragement);
        String accessoryKey = active != null && active.petId > 0
                ? active.accessoryKey : companion == null
                ? null : store.equippedAccessoryId(companion.id);
        String backgroundKey = active == null ? backgroundForPet(companion)
                : active.backgroundKey;
        View petAnimation = petAnimationSlot(companion, null,
                accessoryKey, backgroundKey);
        LinearLayout.LayoutParams petAnimationParams =
                new LinearLayout.LayoutParams(-1, dp(220));
        petAnimationParams.setMargins(0, 0, 0, 0);
        petAnimation.setLayoutParams(petAnimationParams);
        View artworkScaleLayer = petAnimation.findViewById(R.id.pet_artwork_scale_layer);
        artworkScaleLayer.setScaleX(0.95f);
        artworkScaleLayer.setScaleY(0.95f);
        PetAnimationController petAnimationController = registerPetAnimation(
                petAnimation, companion, PetAnimationController.Mode.INTERACTIVE);
        Pet currentCompanion = companion == null ? null : repository.pet(companion.id);
        if (currentCompanion != null) {
            petAnimationController.setRestingExpression(
                    PetExpression.forState(currentCompanion.mood, currentCompanion.energy));
        }
        petAnimation.setClickable(true);
        petAnimation.setFocusable(true);
        petAnimation.setOnClickListener(v -> petAnimationController.playTap());
        content.addView(petAnimation);
        Button end = outlineButton("End session", v -> showEndSessionConfirmation());
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(-1, -2);
        endParams.setMargins(0, dp(28), 0, 0);
        content.addView(end, endParams);
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
                .setMessage("Ending early under 5 minutes earns no rewards. After 5 minutes, you get 30% of normal XP, Coins, and Life Essence (minimum 1 each). Pet Eggs are not awarded, but all focused minutes count toward your daily goal.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, end session", (dialog, which) -> {
                    long totalMillis = (long) store.activeMinutes() * 60_000L;
                    long remaining = store.activePaused()
                            ? store.activeRemaining()
                            : Math.max(0L, store.activeEndAt() - SystemClock.elapsedRealtime());
                    int focusedMinutes = (int) (Math.max(0L, totalMillis - remaining) / 60_000L);
                    if (focusedMinutes < 5) showFinalEndWarning();
                    else sendService(FocusService.ACTION_CANCEL);
                })
                );
    }

    private void showFinalEndWarning() {
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle("Really end your session early?")
                .setMessage("Sessions under 5 minutes earn no rewards. Focused minutes still count toward your daily goal. This cannot be undone. Do you still want to end now?")
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
        View celebration = LayoutInflater.from(this)
                .inflate(R.layout.view_session_celebration, content, false);
        Pet companion = petFromSession(finished);
        if (companion == null) companion = repository.selectedPet();
        String accessoryKey = finished != null && finished.petId > 0
                ? finished.accessoryKey : companion == null
                ? null : store.equippedAccessoryId(companion.id);
        bindPetAnimation(celebration.findViewById(R.id.session_celebration_pet_host),
                companion, accessoryKey, null);
        content.addView(celebration);
        sessionCelebrationAnimator = new SessionCelebrationAnimator(
                celebration.findViewById(R.id.session_celebration_pet_layer),
                celebration.findViewById(R.id.session_celebration_popup));
        sessionCelebrationAnimator.start();
        TextView finishName = text(displayLabel(finished == null ? "" : finished.label),
                22, purpleDark);
        finishName.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        finishName.setGravity(Gravity.CENTER);
        finishName.setPadding(0, dp(8), 0, dp(8));
        content.addView(finishName);
        content.addView(text("Great work! Your pet is proud.", 24,
                ContextCompat.getColor(this, R.color.text_primary)));
        boolean canClaim = finished != null
                && FocusSession.STATUS_COMPLETED.equals(finished.status)
                && repository.hasClaimableRewards(finished);
        String outcome = canClaim
                ? endedEarly ? "Partial rewards are ready to claim."
                : "Your rewards are ready to claim."
                : "No rewards were earned for this session.";
        content.addView(text(minutes + " minutes focused\n" + outcome,
                20, ContextCompat.getColor(this, R.color.text_secondary)));
        content.addView(button(canClaim ? "Continue to claim" : "Continue",
                v -> {
                    if (canClaim) showClaim(sessionId);
                    else showHome();
                }));
    }

    private void showClaim(long sessionId) {
        FocusSession session = repository.pendingSession(sessionId);
        if (session == null || !FocusSession.STATUS_COMPLETED.equals(session.status)
                || !repository.hasClaimableRewards(session)) {
            showHome();
            return;
        }
        reset("Claim rewards");
        setSessionLock(true);
        setActiveTab(0);
        View rewardBanner = LayoutInflater.from(this)
                .inflate(R.layout.view_claim_reward_banner, content, false);
        content.addView(rewardBanner);
        rewardBurstAnimator = new RewardBurstAnimator(
                rewardBanner.findViewById(R.id.claim_reward_particle_layer),
                session.xpReward, session.coinReward, session.essenceReward,
                session.eggReward, pixelFont);
        rewardBurstAnimator.start();
        TextView claimName = text(displayLabel(session.label), 22, purpleDark);
        claimName.setTextAppearance(R.style.TextAppearance_FocusPet_Title);
        claimName.setGravity(Gravity.CENTER);
        claimName.setPadding(0, dp(8), 0, dp(8));
        content.addView(claimName);
        content.addView(text(getString(R.string.session_reward_title), 24,
                ContextCompat.getColor(this, R.color.text_primary)));
        addRewardSummaryRow(content, 0,
                "★ " + getString(R.string.session_reward_xp, session.xpReward), false);
        addRewardSummaryRow(content, R.drawable.icon_coin,
                getString(R.string.session_reward_coins, session.coinReward));
        addRewardSummaryRow(content, R.drawable.icon_essence,
                getString(R.string.session_reward_essence, session.essenceReward));
        if (session.eggReward) {
            addRewardSummaryRow(content, R.drawable.icon_eggs,
                    getString(R.string.session_reward_egg));
        } else {
            addRewardSummaryRow(content, 0,
                    getString(R.string.session_reward_no_egg));
        }
        content.addView(button("Claim rewards", v -> {
            if (repository.claimRewards(sessionId)) {
                showHome();
            }
        }));
    }

    private void addRewardSummaryRow(LinearLayout parent, int iconResource, String label) {
        addRewardSummaryRow(parent, iconResource, label, iconResource == 0);
    }

    private void addRewardSummaryRow(LinearLayout parent, int iconResource, String label,
                                     boolean reserveIconSpace) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (iconResource != 0) {
            ImageView icon = new ImageView(this);
            icon.setImageResource(iconResource);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        }
        TextView value = text(label, 18,
                ContextCompat.getColor(this, R.color.text_secondary));
        value.setPadding(iconResource == 0
                ? reserveIconSpace ? dp(32) : 0 : dp(8), dp(2), 0, dp(2));
        row.addView(value, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, dp(1), 0, dp(1));
        parent.addView(row, rowParams);
    }

    private void cancelRewardAnimations() {
        if (sessionCelebrationAnimator != null) {
            sessionCelebrationAnimator.cancel();
            sessionCelebrationAnimator = null;
        }
        if (rewardBurstAnimator != null) {
            rewardBurstAnimator.cancel();
            rewardBurstAnimator = null;
        }
    }

    private void showCollection() {
        reset("Pet Collection");
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_collection, content, false);
        content.addView(screen);

        LinearLayout resourceBar = screen.findViewById(R.id.resource_bar);
        addResourceChip(resourceBar, R.drawable.icon_coin, "Coins " + store.coins(),
                R.id.coins_chip, v -> showShop());
        addResourceChip(resourceBar, R.drawable.icon_essence, "Essence " + store.lifeEssence(),
                R.id.essence_chip, v -> showHatching());
        addResourceChip(resourceBar, R.drawable.icon_eggs, "Eggs " + store.petEggs(),
                R.id.pet_eggs_chip, v -> showHatching());
        Button shop = button("Shop", v -> showShop());
        shop.setId(R.id.open_shop);
        screen.addView(shop, 1);

        LinearLayout ownedPets = screen.findViewById(R.id.owned_pets_container);
        Pet selected = repository.selectedPet();
        for (Pet pet : repository.pets()) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_pet_card, ownedPets, false);
            configurePetCard(card, pet, pet.name,
                    pet.rarity + " - Level " + pet.level(),
                    pet.id == (selected == null ? -1 : selected.id)
                            ? "Selected companion - Tap for details" : "Tap for details",
                    pet.experience % 100, 1f, v -> showPetDetail(pet));
            ownedPets.addView(card);
        }
    }

    private void addResourceChip(LinearLayout parent, int iconResource, String label, int id,
                                 View.OnClickListener listener) {
        LinearLayout chip = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.item_resource_chip, parent, false);
        chip.setId(id);
        ((ImageView) chip.findViewById(R.id.resource_chip_icon)).setImageResource(iconResource);
        ((TextView) chip.findViewById(R.id.resource_chip_label)).setText(label);
        chip.setContentDescription(label);
        chip.setOnClickListener(listener);
        parent.addView(chip);
    }

    private void configurePetCard(View card, Pet pet, String name, String meta, String hint,
                                  int progressValue, float alpha, View.OnClickListener listener) {
        card.setAlpha(alpha);
        card.setOnClickListener(listener);
        ((TextView) card.findViewById(R.id.pet_name)).setText(name);
        ((TextView) card.findViewById(R.id.pet_meta)).setText(meta);
        ((TextView) card.findViewById(R.id.pet_hint)).setText(hint);
        ((ProgressBar) card.findViewById(R.id.pet_progress)).setProgress(progressValue);
        View art = card.findViewById(R.id.pet_animation_container);
        art.setBackgroundResource(R.drawable.bg_focuspet_lavender);
        bindPetAnimation(art, pet,
                pet == null ? null : store.equippedAccessoryId(pet.id), null);
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
        screen.addView(backNavigationButton("Back to collection",
                v -> showCollection()), 0);
        View art = screen.findViewById(R.id.pet_animation_container);
        art.setBackgroundResource(R.drawable.bg_focuspet_image_placeholder);
        bindPetAnimation(art, pet);
        PetAnimationController animationController = registerPetAnimation(
                art, pet, PetAnimationController.Mode.INTERACTIVE);
        art.setClickable(true);
        art.setFocusable(true);
        art.setOnClickListener(v -> animationController.playTap());
        ((TextView) screen.findViewById(R.id.pet_detail_name))
                .setText(pet.name);
        ((TextView) screen.findViewById(R.id.pet_detail_meta)).setText(
                petDetailMeta(pet));
        ((ProgressBar) screen.findViewById(R.id.pet_detail_progress))
                .setProgress(pet.experience % 100);
        ((TextView) screen.findViewById(R.id.pet_detail_hatched_info)).setText(
                "Hatched: "
                        + new SimpleDateFormat("MMM d, yyyy", Locale.US)
                        .format(new Date(pet.hatchedAt))
                        + "\nWearing: " + equippedSummary(pet.id));
        long petId = pet.id;
        LinearLayout actions = screen.findViewById(R.id.pet_detail_actions);
        actions.addView(text("Interactions", 17, purpleDark));
        TextView feedback = text("Give your pet a short break between interactions.", 14, ContextCompat.getColor(this, R.color.text_secondary));
        feedback.setId(R.id.pet_interaction_feedback);
        actions.addView(feedback);
        addInteraction(actions, screen, petId, "Pet", R.id.pet_interaction_pet,
                PetInteraction.Action.PET, feedback, animationController);
        addInteraction(actions, screen, petId, "Play", R.id.pet_interaction_play,
                PetInteraction.Action.PLAY, feedback, animationController);
        addInteraction(actions, screen, petId, "Rest", R.id.pet_interaction_rest,
                PetInteraction.Action.REST, feedback, animationController);
        Button customize = button("Customize", v -> showCustomize(petId));
        customize.setId(R.id.open_customize);
        LinearLayout.LayoutParams customizeParams = new LinearLayout.LayoutParams(-1, -2);
        customizeParams.setMargins(0, dp(10), 0, dp(8));
        screen.addView(customize, customizeParams);
    }

    private void showHatching() {
        reset("Pet Egg hatching");
        setSessionLock(false);
        setActiveTab(1);
        LinearLayout screen = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.screen_hatching, content, false);
        addHatchBackButton(v -> showCollection(), true);
        content.addView(screen);
        startHatchAnimation(screen.findViewById(R.id.hatching_egg_art));

        int eggs = store.petEggs();
        int essence = store.lifeEssence();
        boolean canHatch = store.canHatch() || store.pendingHatch() != null;
        ((TextView) screen.findViewById(R.id.hatching_inventory)).setText(
                "Pet Eggs: " + eggs + "\nLife Essence: " + essence + " / 100 required");
        Button hatchButton = screen.findViewById(R.id.hatch_button);
        setAvailable(hatchButton, canHatch);
        boolean resumingHatch = store.pendingHatch() != null;
        if (resumingHatch) hatchButton.setText("Resume customization");
        hatchButton.setOnClickListener(v -> {
            if (resumingHatch) {
                beginHatchCustomization();
                return;
            }
            showDefaultFontDialog(new AlertDialog.Builder(this)
                    .setTitle(R.string.hatch_confirmation_title)
                    .setMessage(R.string.hatch_confirmation_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.hatch_confirmation_confirm,
                            (dialog, which) -> beginHatchCustomization()));
        });
    }

    private void beginHatchCustomization() {
        try {
            HatchDraft draft = repository.startHatchDraft();
            if (draft == null) {
                toast("You need 1 Pet Egg and 100 Essence.");
                return;
            }
            activeHatchDraft = draft;
            hatchRecoveryRequired = false;
            hatchCommitPending = store.pendingHatch() != null
                    && draft.step == HatchDraft.STEP_PREVIEW;
            showHatchDraft(draft);
        } catch (IllegalStateException error) {
            toast("Unable to start pet customization. Please try again.");
        }
    }

    private void startHatchAnimation(ImageView imageView) {
        try {
            AnimatedImageDrawable animation = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(getResources(), R.drawable.background_hatch));
            animation.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            hatchAnimation = animation;
            imageView.setImageDrawable(animation);
            if (petAnimationsResumed) animation.start();
        } catch (IOException error) {
            android.util.Log.e("FocusPet", "Unable to load the Pet Egg animation", error);
            toast(getString(R.string.hatch_animation_load_error));
        }
    }

    private void stopHatchAnimation() {
        if (hatchAnimation != null) {
            hatchAnimation.stop();
            hatchAnimation = null;
        }
    }

    private void addHatchBackButton(View.OnClickListener listener, boolean enabled) {
        TextView back = text("<", 22, ContextCompat.getColor(this, R.color.text_primary));
        back.setId(R.id.hatching_back);
        back.setContentDescription("Back");
        back.setGravity(Gravity.CENTER);
        back.setBackgroundResource(R.drawable.bg_focuspet_back);
        back.setMinHeight(dp(48));
        back.setPadding(dp(12), 0, dp(12), 0);
        back.setEnabled(enabled);
        back.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.bottomMargin = dp(8);
        content.addView(back, params);
    }

    private void showHatchDraft(HatchDraft draft) {
        activeHatchDraft = draft;
        hatchRecoveryRequired = false;
        setSessionLock(true);
        switch (draft.step) {
            case HatchDraft.STEP_NAME:
                showHatchName(draft);
                break;
            case HatchDraft.STEP_APPEARANCE:
                showHatchAppearance(draft);
                break;
            case HatchDraft.STEP_PREVIEW:
                showHatchPreview(draft);
                break;
            default:
                throw new IllegalStateException("Unknown hatch customization step");
        }
    }

    private void showHatchName(HatchDraft draft) {
        reset("Name your pet");
        setSessionLock(true);
        setActiveTab(1);
        View screen = LayoutInflater.from(this).inflate(R.layout.screen_hatch_name, content, false);
        content.addView(screen);
        hatchNameInput = screen.findViewById(R.id.hatch_name_input);
        hatchNameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        hatchNameInput.setText(draft.name == null ? "" : draft.name);
        hatchNameInput.setTypeface(Typeface.DEFAULT);
        Button next = button("Continue", v -> {
            try {
                activeHatchDraft = repository.advanceHatchName(draft.id,
                        hatchNameInput.getText().toString());
                hatchNameInput = null;
                showHatchAppearance(activeHatchDraft);
            } catch (IllegalArgumentException error) {
                hatchNameInput.setError(error.getMessage());
            } catch (IllegalStateException error) {
                toast("Unable to save the pet name. Please try again.");
            }
        });
        content.addView(next);
    }

    private void showHatchAppearance(HatchDraft draft) {
        hatchNameInput = null;
        reset("Choose appearance");
        setSessionLock(true);
        setActiveTab(1);
        View screen = LayoutInflater.from(this)
                .inflate(R.layout.screen_hatch_appearance, content, false);
        addHatchBackButton(v -> moveBackInHatchFlow(), true);
        content.addView(screen);
        View preview = screen.findViewById(R.id.hatch_appearance_preview);
        Pet previewPet = hatchPreviewPet(draft);
        PetLook selectedLook = PetLook.fromKey(draft.appearanceKey);
        if (selectedLook == null || selectedLook == PetLook.ORIGINAL
                || !store.isPetLookUnlocked(selectedLook.key)) {
            selectedLook = PetLook.CLASSIC;
        }
        bindPetLook(preview, previewPet, selectedLook);

        RadioGroup options = screen.findViewById(R.id.hatch_appearance_options);
        for (PetLook look : PetLook.values()) {
            if (look == PetLook.ORIGINAL || !store.isPetLookUnlocked(look.key)
                    || !look.canChoose(previewPet)) continue;
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setTag(look);
            option.setText(getString(look.label));
            option.setMinHeight(dp(48));
            options.addView(option);
            if (look == selectedLook) options.check(option.getId());
        }
        options.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked == null) return;
            PetLook look = (PetLook) checked.getTag();
            try {
                activeHatchDraft = repository.updateHatchAppearance(draft.id, look.key);
                bindPetLook(preview, previewPet, look);
            } catch (IllegalStateException error) {
                toast("Unable to save this appearance. Please try again.");
            }
        });

        content.addView(button("Preview pet", v -> {
            try {
                activeHatchDraft = repository.advanceHatchAppearance(draft.id);
                showHatchPreview(activeHatchDraft);
            } catch (IllegalStateException error) {
                toast("Choose an appearance before continuing.");
            }
        }));
    }

    private void showHatchPreview(HatchDraft draft) {
        hatchNameInput = null;
        reset("Preview your pet");
        setSessionLock(true);
        setActiveTab(1);
        View screen = LayoutInflater.from(this).inflate(R.layout.screen_hatch_preview, content, false);
        addHatchBackButton(v -> moveBackInHatchFlow(), !hatchCommitPending);
        content.addView(screen);
        ((TextView) screen.findViewById(R.id.hatch_preview_name)).setText(draft.name);
        PetLook look = PetLook.fromKey(draft.appearanceKey);
        bindPetLook(screen.findViewById(R.id.hatch_final_preview),
                hatchPreviewPet(draft), look == null ? PetLook.CLASSIC : look);
        Button confirm = button(hatchCommitPending
                ? "Retry confirmed hatch" : "Confirm and add to collection", v -> {
            Button confirmButton = (Button) v;
            hatchCommitPending = true;
            setAvailable(confirmButton, false);
            try {
                Pet created = repository.confirmHatch(draft.id);
                if (created == null) {
                    hatchCommitPending = false;
                    setAvailable(confirmButton, true);
                    toast("You need 1 Pet Egg and 100 Essence.");
                    return;
                }
                activeHatchDraft = null;
                hatchNameInput = null;
                hatchCommitPending = false;
                showHatchSuccessDialog(created);
            } catch (IllegalArgumentException | IllegalStateException error) {
                hatchCommitPending = store.pendingHatch() != null;
                if (hatchCommitPending) {
                    showHatchPreview(activeHatchDraft);
                    toast("Hatch confirmation is saved. Retry to finish safely.");
                } else {
                    setAvailable(confirmButton, true);
                    toast("Unable to confirm the hatch. Please try again.");
                }
            }
        });
        content.addView(confirm);
    }

    private Pet hatchPreviewPet(HatchDraft draft) {
        Pet pet = new Pet();
        pet.name = draft.name;
        pet.species = "Pet";
        pet.appearanceData = draft.appearanceKey;
        pet.imageKey = draft.appearanceKey;
        return pet;
    }

    private void moveBackInHatchFlow() {
        if (activeHatchDraft == null || hatchCommitPending) return;
        if (activeHatchDraft.step == HatchDraft.STEP_NAME) {
            toast("Enter a name to continue customizing your pet.");
            return;
        }
        try {
            activeHatchDraft = repository.moveHatchDraftBack(activeHatchDraft.id);
            showHatchDraft(activeHatchDraft);
        } catch (IllegalStateException error) {
            toast("Unable to return to the previous customization step.");
        }
    }

    private void showHatchRecoveryFailure() {
        reset("Finishing hatch");
        setSessionLock(true);
        setActiveTab(1);
        content.addView(systemText("Your confirmed hatch is safe, but could not be completed yet.",
                17, ContextCompat.getColor(this, R.color.text_secondary)));
        content.addView(button("Retry", v -> retryHatchRecovery()));
    }

    private void retryHatchRecovery() {
        try {
            hatchRecoveryRequired = false;
            Pet pet = repository.recoverPendingHatch();
            HatchDraft draft = repository.activeHatchDraft();
            if (draft != null) {
                activeHatchDraft = draft;
                hatchCommitPending = store.pendingHatch() != null
                        && draft.step == HatchDraft.STEP_PREVIEW;
                showHatchDraft(draft);
            } else if (pet != null || store.hatchResultPetId() > 0) {
                Pet result = pet != null ? pet : repository.pet(store.hatchResultPetId());
                if (result != null) showHatchSuccessDialog(result);
                else showHatchRecoveryFailure();
            } else {
                showHome();
            }
        } catch (IllegalStateException error) {
            hatchRecoveryRequired = true;
            showHatchRecoveryFailure();
        }
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
        button.setBackgroundTintList(available ? null
                : android.content.res.ColorStateList.valueOf(Color.rgb(218, 218, 218)));
        button.setTextColor(available
                ? ContextCompat.getColor(this, R.color.text_on_button)
                : Color.rgb(105, 105, 105));
        button.setTypeface(pixelFont);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.text_size_18));
    }

    private String equippedSummary(long petId) {
        AccessoryCatalog.Accessory accessory =
                AccessoryCatalog.fromKey(store.equippedAccessoryId(petId));
        return accessory == null ? "None" : accessory.name;
    }

    private View petAnimationSlot(Pet pet) {
        return petAnimationSlot(pet, null);
    }

    private View petAnimationSlot(Pet pet, PetAnimationController.Mode mode) {
        return petAnimationSlot(pet, mode,
                pet == null ? null : store.equippedAccessoryId(pet.id),
                backgroundForPet(pet));
    }

    private View petAnimationSlot(Pet pet, PetAnimationController.Mode mode,
                                  String accessoryKey, String backgroundKey) {
        View slot = LayoutInflater.from(this).inflate(R.layout.view_pet_animation, content, false);
        bindPetAnimation(slot, pet, accessoryKey, backgroundKey);
        if (mode != null) registerPetAnimation(slot, pet, mode);
        return slot;
    }

    private PetAnimationController registerPetAnimation(
            View host, Pet pet, PetAnimationController.Mode mode) {
        PetAnimationController controller = new PetAnimationController(
                host,
                host.findViewById(R.id.pet_artwork_layer),
                host.findViewById(R.id.pet_expression_bubble),
                host.findViewById(R.id.pet_expression_bubble_image),
                host.findViewById(R.id.pet_expression_overlay),
                mode, pet == null ? java.util.Collections.emptySet()
                        : store.enabledPetExpressions(pet.id));
        if (pet != null) {
            controller.setRestingExpression(PetExpression.forState(pet.mood, pet.energy));
        }
        petAnimationControllers.add(controller);
        if (petAnimationsResumed) controller.resume();
        return controller;
    }

    private void clearPetAnimationControllers() {
        for (PetAnimationController controller : petAnimationControllers) {
            controller.dispose();
        }
        petAnimationControllers.clear();
    }

    private void bindPetAnimation(View slot, Pet pet) {
        bindPetAnimation(slot, pet, pet == null ? null : store.equippedAccessoryId(pet.id),
                backgroundForPet(pet));
    }

    private void bindPetAnimation(View slot, Pet pet, String accessoryKey,
                                  String backgroundKey) {
        petArtworkRenderer.bind(slot, pet, PetLook.forPet(pet), accessoryKey, backgroundKey);
    }

    private void bindPetLook(View slot, Pet pet, PetLook look) {
        petArtworkRenderer.bind(slot, pet, look, null, BackgroundCatalog.DEFAULT_KEY);
    }

    private Pet petFromSession(FocusSession session) {
        if (session == null || session.petId <= 0) return null;
        Pet pet = new Pet();
        pet.id = session.petId;
        pet.name = session.petName;
        pet.species = session.petSpecies;
        pet.imageKey = session.petImageKey;
        pet.appearanceData = session.petAppearanceData;
        return pet;
    }

    private String backgroundForPet(Pet pet) {
        return pet == null ? BackgroundCatalog.DEFAULT_KEY : store.petBackgroundKey(pet.id);
    }

    private void showHatchSuccessDialog(Pet pet) {
        activeHatchDraft = null;
        hatchNameInput = null;
        hatchRecoveryRequired = false;
        hatchCommitPending = false;
        setSessionLock(false);
        showCollection();
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle("Hatched successfully!")
                .setMessage(pet.name + " has been added to your Collection.")
                .setPositiveButton("OK", (dialog, which) -> store.acknowledgeHatchResult())
                .setCancelable(false));
    }

    private String petDetailMeta(Pet pet) {
        return pet.rarity + " companion" + (pet.selected ? " - Selected" : "")
                + "\nLevel " + pet.level() + " - " + pet.experience + " XP"
                + "\nMood / energy: " + pet.mood + " / " + pet.energy;
    }

    private void addInteraction(LinearLayout parent, View screen, long petId, String label, int id,
                                PetInteraction.Action action, TextView feedback,
                                PetAnimationController animationController) {
        Button control = outlineButton(label, v -> {
            FocusRepository.InteractionResult result = repository.interact(petId, action);
            Pet updated = repository.pet(petId);
            if (updated == null) { showCollection(); return; }
            ((TextView) screen.findViewById(R.id.pet_detail_meta)).setText(petDetailMeta(updated));
            animationController.setRestingExpression(
                    PetExpression.forState(updated.mood, updated.energy));
            switch (result) {
                case UPDATED:
                    animationController.playInteraction(action);
                    feedback.setText(action == PetInteraction.Action.PET ? updated.name + " enjoyed being petted."
                            : action == PetInteraction.Action.PLAY ? updated.name + " enjoyed playing with you."
                            : updated.name + " rested and recovered energy.");
                    break;
                case COOLDOWN: feedback.setText("Please wait a few seconds before another interaction."); break;
                case LOW_ENERGY:
                    animationController.showTransientExpression(PetExpression.TIRED, 1600L);
                    feedback.setText("Your pet needs to rest before playing again.");
                    break;
                default: feedback.setText("This pet is no longer available.");
            }
        });
        control.setId(id);
        parent.addView(control);
    }

    private void showShop() {
        showShop(ShopCategory.ALL, 0);
    }

    private void showShop(ShopCategory category) {
        showShop(category, 0);
    }

    private void showShop(ShopCategory category, long inventoryReturnPetId) {
        selectedShopCategory = category;
        expressionInventoryReturnPetId = category == ShopCategory.EXPRESSIONS
                ? inventoryReturnPetId : 0;
        accessoryShopReturnPetId = category == ShopCategory.ACCESSORIES
                ? inventoryReturnPetId : 0;
        backgroundShopReturnPetId = category == ShopCategory.BACKGROUNDS
                ? inventoryReturnPetId : 0;
        reset("Shop");
        setSessionLock(false);
        setActiveTab(1);
        content.addView(backNavigationButton("Back to collection", v -> showCollection()));
        if (expressionInventoryReturnPetId > 0) {
            long returnPetId = expressionInventoryReturnPetId;
            content.addView(backNavigationButton("Back to expression inventory", v -> {
                expressionInventoryReturnPetId = 0;
                showExpressionInventory(returnPetId);
            }));
        }
        if (accessoryShopReturnPetId > 0) {
            long returnPetId = accessoryShopReturnPetId;
            content.addView(backNavigationButton("Back to accessories", v -> {
                accessoryShopReturnPetId = 0;
                showCustomize(returnPetId, CustomizeSection.ACCESSORIES);
            }));
        }
        if (backgroundShopReturnPetId > 0) {
            long returnPetId = backgroundShopReturnPetId;
            content.addView(backNavigationButton("Back to backgrounds", v -> {
                backgroundShopReturnPetId = 0;
                showCustomize(returnPetId, CustomizeSection.BACKGROUND);
            }));
        }
        LinearLayout balanceRow = new LinearLayout(this);
        balanceRow.setOrientation(LinearLayout.HORIZONTAL);
        balanceRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView coinIcon = new ImageView(this);
        coinIcon.setId(R.id.shop_balance_icon);
        coinIcon.setImageResource(R.drawable.icon_coin);
        coinIcon.setContentDescription("Coins");
        balanceRow.addView(coinIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView balance = text("Coins: " + store.coins(), 16,
                ContextCompat.getColor(this, R.color.text_secondary));
        balance.setId(R.id.shop_balance);
        LinearLayout.LayoutParams balanceTextParams = new LinearLayout.LayoutParams(-2, -2);
        balanceTextParams.setMargins(dp(6), 0, 0, 0);
        balanceRow.addView(balance, balanceTextParams);
        LinearLayout.LayoutParams balanceRowParams = new LinearLayout.LayoutParams(-1, -2);
        balanceRowParams.setMargins(dp(8), dp(10), 0, dp(8));
        content.addView(balanceRow, balanceRowParams);
        content.addView(systemText("Browse pet looks, expressions, accessories and backgrounds with Coins.",
                14, ContextCompat.getColor(this, R.color.text_secondary)));
        addShopCategories();
        List<ShopProductEntity> products = new ArrayList<>();
        for (ShopProductEntity product : shopRepository.availableProducts()) {
            if (category.includes(product)) products.add(product);
        }
        products.sort((first, second) -> Boolean.compare(
                isShopProductOwned(first), isShopProductOwned(second)));
        if (products.isEmpty()) {
            content.addView(systemText("No items in this category yet.", 16,
                    ContextCompat.getColor(this, R.color.text_secondary)));
        } else {
            addShopProductsMasonry(products);
        }
        Pet selected = repository.selectedPet();
        if (selected != null) content.addView(button("Customize companion",
                v -> showCustomize(selected.id)));
    }

    private void addShopProductsMasonry(List<ShopProductEntity> products) {
        int contentWidth = content.getWidth();
        if (contentWidth <= 0) {
            contentWidth = getResources().getDisplayMetrics().widthPixels;
        }
        int availableWidth = contentWidth - content.getPaddingLeft() - content.getPaddingRight();
        int columnWidth = Math.max(1, availableWidth / 2);

        LinearLayout masonry = new LinearLayout(this);
        masonry.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams masonryParams = new LinearLayout.LayoutParams(-1, -2);
        masonryParams.setMargins(0, dp(4), 0, dp(4));
        content.addView(masonry, masonryParams);

        LinearLayout[] columns = {new LinearLayout(this), new LinearLayout(this)};
        int[] columnHeights = new int[columns.length];
        for (LinearLayout column : columns) {
            column.setOrientation(LinearLayout.VERTICAL);
            masonry.addView(column, new LinearLayout.LayoutParams(0, -2, 1));
        }

        int cardWidth = Math.max(1, columnWidth - dp(8));
        for (ShopProductEntity product : products) {
            View card = shopProductCard(product);
            card.measure(
                    View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int columnIndex = columnHeights[0] <= columnHeights[1] ? 0 : 1;
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(dp(4), 0, dp(4), dp(8));
            columns[columnIndex].addView(card, cardParams);
            columnHeights[columnIndex] += card.getMeasuredHeight() + dp(8);
        }
    }

    private void addShopCategories() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        for (ShopCategory category : ShopCategory.values()) {
            TextView tab = text(category.label, 14, ContextCompat.getColor(this,
                    category == selectedShopCategory ? R.color.text_on_button : R.color.text_primary));
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(12), dp(8), dp(12), dp(8));
            tab.setBackground(roundColor(ContextCompat.getColor(this,
                    category == selectedShopCategory ? R.color.button_primary : R.color.background_card), 12));
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, -2);
            tabParams.setMargins(0, 0, dp(8), 0);
            categories.addView(tab, tabParams);
            tab.setOnClickListener(v -> showShop(category));
        }
        scroll.addView(categories);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(8));
        content.addView(scroll, params);
    }

    private View shopProductCard(ShopProductEntity product) {
        ProductType type = ProductType.valueOf(product.productType);
        boolean owned = isShopProductOwned(product);
        boolean repeatable = type == ProductType.PET_EXPRESSION;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(borderedColor(ContextCompat.getColor(this, R.color.background_card),
                ContextCompat.getColor(this, R.color.button_outline), 1, 8));

        FrameLayout artworkFrame = new FrameLayout(this);
        artworkFrame.setBackground(roundColor(ContextCompat.getColor(this, R.color.background_accent), 6));
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(-1, dp(128));
        artworkParams.setMargins(0, 0, 0, dp(8));
        card.addView(artworkFrame, artworkParams);
        if (type == ProductType.PET_LOOK) {
            PetLook look = PetLook.fromKey(product.itemKey);
            if (look == null || look.artworkFor(null) == 0) {
                throw new IllegalStateException("Shop product references an unknown pet look: " + product.itemKey);
            }
            addPetLookArtwork(artworkFrame, look, product, owned, "pet_look_art_");
        } else if (type == ProductType.PET_EXPRESSION) {
            PetExpression expression = PetExpression.fromKey(product.itemKey);
            if (expression == null || !expression.learnable) {
                throw new IllegalStateException("Shop product references an unknown expression: " + product.itemKey);
            }
            artworkFrame.addView(expressionPreview(expression, -1, -1),
                    new FrameLayout.LayoutParams(-1, -1));
        } else if (type == ProductType.PET_BACKGROUND) {
            addBackgroundArtwork(artworkFrame, product.itemKey);
        } else if (type == ProductType.COSMETIC
                && AccessoryCatalog.fromKey(product.itemKey) != null) {
            addAccessoryArtwork(artworkFrame, product.itemKey);
        } else {
            TextView placeholder = text(product.categoryKey, 14,
                    ContextCompat.getColor(this, R.color.text_secondary));
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setContentDescription(product.name + " preview");
            artworkFrame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        }

        TextView name = text(shopProductName(product), 16, purpleDark);
        card.addView(name);

        String currency = Currency.valueOf(product.currency) == Currency.COINS ? "Coins" : "Essence";
        long balance = Currency.valueOf(product.currency) == Currency.COINS
                ? store.coins() : store.lifeEssence();
        boolean enough = balance >= product.price;
        TextView price = text(product.price + " " + currency, 14,
                ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(-1, -2);
        priceParams.setMargins(0, dp(6), 0, dp(4));
        card.addView(price, priceParams);

        if (repeatable) {
            TextView stock = systemText(getString(R.string.expression_inventory_stock,
                    store.expressionStock(product.itemKey)), 13,
                    ContextCompat.getColor(this, R.color.text_secondary));
            card.addView(stock);
        }
        boolean canBuy = enough && (!repeatable || maxPurchaseCount(product) > 0);
        String label = repeatable
                ? canBuy ? "Buy" : enough ? "Inventory full" : "Not enough " + currency
                : owned ? "Owned" : enough ? "Buy" : "Not enough " + currency;
        Button buy = button(label, v -> confirmPurchase(product));
        buy.setTag("buy_" + product.id);
        setAvailable(buy, repeatable ? canBuy : !owned && enough);
        LinearLayout.LayoutParams buyParams = new LinearLayout.LayoutParams(-1, dp(44));
        buyParams.setMargins(0, dp(8), 0, 0);
        card.addView(buy, buyParams);
        return card;
    }

    private void confirmPurchase(ShopProductEntity product) {
        String requestId = UUID.randomUUID().toString();
        ProductType type = ProductType.valueOf(product.productType);
        boolean repeatable = type == ProductType.PET_EXPRESSION;
        int maxPurchaseCount = maxPurchaseCount(product);
        if (repeatable && maxPurchaseCount < 1) {
            toast("No more expression items can be added to inventory.");
            return;
        }
        LinearLayout confirmation = new LinearLayout(this);
        confirmation.setOrientation(LinearLayout.VERTICAL);
        confirmation.setGravity(Gravity.CENTER_HORIZONTAL);
        confirmation.setPadding(dp(16), 0, dp(16), 0);
        confirmation.addView(shopProductArtwork(product, dp(112)));
        String currency = Currency.valueOf(product.currency) == Currency.COINS
                ? "Coins" : "Essence";
        confirmation.addView(text(repeatable
                        ? getString(R.string.shop_unit_price, product.price, currency)
                        : product.price + " " + currency,
                16, purpleDark));
        NumberPicker quantityPicker = null;
        if (repeatable) {
            LinearLayout quantityRow = new LinearLayout(this);
            quantityRow.setGravity(Gravity.CENTER_VERTICAL);
            quantityRow.addView(systemText(getString(R.string.shop_purchase_quantity), 15,
                    ContextCompat.getColor(this, R.color.text_secondary)));
            quantityPicker = new NumberPicker(this);
            quantityPicker.setId(R.id.shop_purchase_quantity);
            quantityPicker.setMinValue(1);
            quantityPicker.setMaxValue(maxPurchaseCount);
            quantityPicker.setValue(1);
            quantityPicker.setWrapSelectorWheel(false);
            LinearLayout.LayoutParams pickerParams =
                    new LinearLayout.LayoutParams(dp(96), dp(76));
            pickerParams.setMargins(dp(10), 0, 0, 0);
            quantityRow.addView(quantityPicker, pickerParams);
            confirmation.addView(quantityRow);

            TextView total = text(getString(R.string.shop_purchase_total,
                    product.price, currency), 16, purpleDark);
            total.setId(R.id.shop_purchase_total);
            confirmation.addView(total);
            NumberPicker pickerForListener = quantityPicker;
            pickerForListener.setOnValueChangedListener((picker, oldValue, newValue) ->
                    total.setText(getString(R.string.shop_purchase_total,
                            product.price * newValue, currency)));
        }
        String productName = shopProductName(product);
        String title = type == ProductType.PET_LOOK
                ? "Unlock " + productName + "?" : "Buy " + productName + "?";
        String benefit = type == ProductType.PET_LOOK
                ? "This appearance will be available when hatching a pet."
                : type == ProductType.PET_EXPRESSION
                ? "This adds one expression item to your inventory. Use it to teach one pet."
                : "This item will be permanently unlocked for your pets.";
        confirmation.addView(systemText(benefit, 14,
                ContextCompat.getColor(this, R.color.text_secondary)));
        NumberPicker pickerForPurchase = quantityPicker;
        AlertDialog purchaseDialog = new AlertDialog.Builder(this).setTitle(title)
                .setView(confirmation)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Buy", null)
                .create();
        purchaseDialog.setOnShowListener(ignored -> {
            setDefaultFont(purchaseDialog.getWindow().getDecorView());
            Button confirmButton = purchaseDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmButton.setId(R.id.shop_purchase_confirm);
            confirmButton.setOnClickListener(view -> {
                    int purchaseCount = pickerForPurchase == null
                            ? 1 : pickerForPurchase.getValue();
                    FocusStore.PurchaseResult result =
                            shopRepository.purchase(product.id, purchaseCount, requestId);
                    if (result == FocusStore.PurchaseResult.PURCHASED) {
                        toast(repeatable
                                ? getString(R.string.shop_purchase_success_quantity,
                                        purchaseCount, productName)
                                : "Purchased " + productName + ".");
                    } else if (result == FocusStore.PurchaseResult.ALREADY_OWNED) {
                        toast("You already own this item.");
                    } else if (result == FocusStore.PurchaseResult.INSUFFICIENT_RESOURCES) {
                        toast("Not enough resources.");
                    } else {
                        toast("Purchase could not be saved. Please try again.");
                    }
                    long returnPetId = expressionInventoryReturnPetId;
                    long accessoryReturnPetId = accessoryShopReturnPetId;
                    long backgroundReturnPetId = backgroundShopReturnPetId;
                    purchaseDialog.dismiss();
                    if (result == FocusStore.PurchaseResult.PURCHASED
                            && type == ProductType.PET_EXPRESSION && returnPetId > 0) {
                        expressionInventoryReturnPetId = 0;
                        showExpressionInventory(returnPetId);
                    } else if (result == FocusStore.PurchaseResult.PURCHASED
                            && type == ProductType.COSMETIC
                            && AccessoryCatalog.fromKey(product.itemKey) != null
                            && accessoryReturnPetId > 0) {
                        accessoryShopReturnPetId = 0;
                        showCustomize(accessoryReturnPetId, CustomizeSection.ACCESSORIES);
                    } else if (result == FocusStore.PurchaseResult.PURCHASED
                            && type == ProductType.PET_BACKGROUND
                            && backgroundReturnPetId > 0) {
                        backgroundShopReturnPetId = 0;
                        showCustomize(backgroundReturnPetId, CustomizeSection.BACKGROUND);
                    } else {
                        long returnPetIdForShop = expressionInventoryReturnPetId > 0
                                ? returnPetId : accessoryReturnPetId > 0
                                ? accessoryReturnPetId : backgroundReturnPetId;
                        showShop(selectedShopCategory, returnPetIdForShop);
                    }
                });
        });
        purchaseDialog.show();
    }

    private int maxPurchaseCount(ShopProductEntity product) {
        if (ProductType.valueOf(product.productType) != ProductType.PET_EXPRESSION
                || product.quantityGranted <= 0) {
            return 1;
        }
        long balance = Currency.valueOf(product.currency) == Currency.COINS
                ? store.coins() : store.lifeEssence();
        long affordableCount = product.price == 0
                ? Integer.MAX_VALUE : balance / product.price;
        long stockCapacity = ((long) Integer.MAX_VALUE
                - store.expressionStock(product.itemKey)) / product.quantityGranted;
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE,
                Math.min(affordableCount, stockCapacity)));
    }

    private View shopProductArtwork(ShopProductEntity product, int size) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(roundColor(ContextCompat.getColor(this, R.color.background_accent), 6));
        frame.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        ProductType type = ProductType.valueOf(product.productType);
        if (type == ProductType.PET_LOOK) {
            PetLook look = PetLook.fromKey(product.itemKey);
            if (look == null || look.artworkFor(null) == 0) {
                throw new IllegalStateException("Shop product references an unknown pet look: " + product.itemKey);
            }
            addPetLookArtwork(frame, look, product, isShopProductOwned(product),
                    "pet_look_dialog_art_");
        } else if (type == ProductType.PET_EXPRESSION) {
            PetExpression expression = PetExpression.fromKey(product.itemKey);
            if (expression == null || !expression.learnable) {
                throw new IllegalStateException("Shop product references an unknown expression: " + product.itemKey);
            }
            frame.addView(expressionPreview(expression, -1, -1),
                    new FrameLayout.LayoutParams(-1, -1));
        } else if (type == ProductType.PET_BACKGROUND) {
            addBackgroundArtwork(frame, product.itemKey);
        } else if (type == ProductType.COSMETIC
                && AccessoryCatalog.fromKey(product.itemKey) != null) {
            addAccessoryArtwork(frame, product.itemKey);
        } else {
            TextView placeholder = text(product.categoryKey, 14,
                    ContextCompat.getColor(this, R.color.text_secondary));
            placeholder.setGravity(Gravity.CENTER);
            frame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        }
        return frame;
    }

    private void addAccessoryArtwork(FrameLayout frame, String accessoryKey) {
        AccessoryCatalog.Accessory accessory = AccessoryCatalog.fromKey(accessoryKey);
        if (accessory == null) {
            throw new IllegalStateException("Shop product references an unknown accessory: "
                    + accessoryKey);
        }
        ImageView image = new ImageView(this);
        image.setImageResource(accessory.drawable);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(accessory.name);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    }

    private void addBackgroundArtwork(FrameLayout frame, String backgroundKey) {
        BackgroundCatalog.Background background = BackgroundCatalog.fromKey(backgroundKey);
        if (background == null) {
            throw new IllegalStateException("Shop product references an unknown background: "
                    + backgroundKey);
        }
        ImageView image = new ImageView(this);
        image.setImageResource(background.drawable);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription(background.name);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addPetLookArtwork(FrameLayout frame, PetLook look,
                                   ShopProductEntity product, boolean owned, String tagPrefix) {
        ImageView image = new ImageView(this);
        image.setImageResource(look.artworkFor(null));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setTag(tagPrefix + product.itemKey);
        image.setContentDescription(owned ? getString(look.label)
                : getString(R.string.shop_unrevealed_pet_look));
        if (!owned) {
            image.setRenderEffect(RenderEffect.createBlurEffect(
                    dp(14), dp(14), Shader.TileMode.CLAMP));
        }
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (!owned) {
            View veil = new View(this);
            veil.setBackgroundColor(Color.argb(24, 247, 243, 228));
            veil.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            frame.addView(veil, new FrameLayout.LayoutParams(-1, -1));
        }
    }

    private boolean isShopProductOwned(ShopProductEntity product) {
        ProductType type = ProductType.valueOf(product.productType);
        if (type == ProductType.PET_LOOK) {
            return store.ownedPetLookKeys().contains(product.itemKey);
        }
        if (type == ProductType.PET_EXPRESSION) {
            return false;
        }
        if (type == ProductType.PET_BACKGROUND) {
            return store.ownedBackgroundKeys().contains(product.itemKey);
        }
        return type == ProductType.COSMETIC
                && store.ownedCosmeticIds().contains(product.itemKey);
    }

    private String shopProductName(ShopProductEntity product) {
        if (ProductType.valueOf(product.productType) == ProductType.PET_EXPRESSION) {
            PetExpression expression = PetExpression.fromKey(product.itemKey);
            if (expression == null || !expression.learnable) {
                throw new IllegalStateException("Shop product references an unknown expression: " + product.itemKey);
            }
            return getString(expression.nameRes);
        }
        return product.name;
    }

    private void showCustomize(long petId) {
        showCustomize(petId, CustomizeSection.ACCESSORIES);
    }

    private void showCustomize(long petId, CustomizeSection section) {
        Pet pet = repository.pet(petId);
        if (pet == null) { showCollection(); return; }
        expressionInventoryReturnPetId = 0;
        accessoryShopReturnPetId = 0;
        backgroundShopReturnPetId = 0;
        reset(pet.name + " Customize");
        setActiveTab(1);
        content.addView(backNavigationButton("Back to pet", v -> showPetDetail(pet)));
        View art = petAnimationSlot(pet);
        art.setBackgroundResource(R.drawable.bg_focuspet_image_placeholder);
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(-1, dp(164));
        artParams.setMargins(0, dp(8), 0, 0);
        content.addView(art, artParams);
        addCustomizeSectionTabs(petId, section);

        switch (section) {
            case ACCESSORIES:
                addAccessoryCustomizeSection(petId);
                break;
            case EXPRESSIONS:
                addExpressionCustomizeSection(petId);
                break;
            case BACKGROUND:
                addBackgroundCustomizeSection(petId);
                break;
            default:
                throw new IllegalStateException("Unknown customize section: " + section);
        }
    }

    private void addCustomizeSectionTabs(long petId, CustomizeSection selected) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        CustomizeSection[] sections = CustomizeSection.values();
        int[] labels = {
                R.string.customize_tab_accessories,
                R.string.customize_tab_expressions,
                R.string.customize_tab_background
        };
        int[] ids = {
                R.id.customize_tab_accessories,
                R.id.customize_tab_expressions,
                R.id.customize_tab_background
        };
        for (int i = 0; i < sections.length; i++) {
            CustomizeSection section = sections[i];
            boolean isSelected = section == selected;
            TextView tab = text(getString(labels[i]), 14, ContextCompat.getColor(this,
                    isSelected ? R.color.text_on_button : R.color.text_primary));
            tab.setId(ids[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(dp(44));
            tab.setPadding(dp(14), dp(8), dp(14), dp(8));
            tab.setBackground(roundColor(ContextCompat.getColor(this,
                    isSelected ? R.color.button_primary : R.color.background_card), 10));
            tab.setSelected(isSelected);
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, -2);
            tabParams.setMargins(0, 0, dp(8), 0);
            tabs.addView(tab, tabParams);
            tab.setOnClickListener(v -> showCustomize(petId, section));
        }
        scroll.addView(tabs);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(12), 0, dp(10));
        content.addView(scroll, params);
    }

    private void addAccessoryCustomizeSection(long petId) {
        content.addView(text(getString(R.string.accessories_wearing,
                equippedSummary(petId)), 17, purpleDark));
        content.addView(systemText(getString(R.string.accessories_customize_intro), 14,
                ContextCompat.getColor(this, R.color.text_secondary)));
        Set<String> owned = store.ownedCosmeticIds();
        boolean hasItems = false;
        String equippedId = store.equippedAccessoryId(petId);
        for (AccessoryCatalog.Accessory item : AccessoryCatalog.ACCESSORIES) {
            if (!owned.contains(item.key)) continue;
            hasItems = true;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            row.setBackgroundResource(R.drawable.bg_focuspet_card);
            ImageView image = new ImageView(this);
            image.setImageResource(item.drawable);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription(item.name);
            row.addView(image, new LinearLayout.LayoutParams(dp(56), dp(56)));

            TextView name = text(item.name, 16, purpleDark);
            name.setMaxLines(2);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
            nameParams.setMargins(dp(8), 0, dp(4), 0);
            row.addView(name, nameParams);

            boolean equipped = item.key.equals(equippedId);
            Button toggle = button(equipped
                    ? getString(R.string.accessory_remove)
                    : getString(R.string.accessory_equip), v -> {
                if (equipped) store.unequip(petId, "accessory");
                else if (!repository.equipCosmetic(petId, item.key)) {
                    toast("This item is not available.");
                }
                showCustomize(petId, CustomizeSection.ACCESSORIES);
            });
            toggle.setTag("equip_" + item.key);
            LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(dp(104), dp(52));
            toggleParams.setMargins(dp(4), 0, 0, 0);
            row.addView(toggle, toggleParams);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, dp(6), 0, dp(4));
            content.addView(row, rowParams);
        }
        if (!hasItems) content.addView(systemText(getString(R.string.accessories_empty), 15,
                ContextCompat.getColor(this, R.color.text_secondary)));
        Button shop = button(getString(R.string.accessories_shop),
                v -> showShop(ShopCategory.ACCESSORIES, petId));
        content.addView(shop);
    }

    private void addExpressionCustomizeSection(long petId) {
        content.addView(systemText(getString(R.string.customize_expressions_intro), 14,
                ContextCompat.getColor(this, R.color.text_secondary)));

        Button inventory = outlineButton(getString(R.string.expression_inventory_button),
                v -> showExpressionInventory(petId));
        inventory.setId(R.id.open_expression_inventory);
        LinearLayout.LayoutParams inventoryParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        inventoryParams.setMargins(0, dp(10), 0, dp(10));
        content.addView(inventory, inventoryParams);

        Set<String> learned = store.learnedPetExpressions(petId);
        if (learned.isEmpty()) {
            LinearLayout emptyState = new LinearLayout(this);
            emptyState.setGravity(Gravity.CENTER);
            emptyState.setBackgroundResource(R.drawable.bg_focuspet_card);
            emptyState.setPadding(dp(16), dp(14), dp(16), dp(14));
            emptyState.addView(systemText(getString(R.string.expression_learned_empty), 15,
                    ContextCompat.getColor(this, R.color.text_secondary)));
            content.addView(emptyState);
        } else {
            for (PetExpression expression : PetExpression.LEARNABLE) {
                if (learned.contains(expression.key)) {
                    addLearnedExpressionRow(petId, expression);
                }
            }
        }
    }

    private void addBackgroundCustomizeSection(long petId) {
        content.addView(systemText(getString(R.string.customize_background_intro), 14,
                ContextCompat.getColor(this, R.color.text_secondary)));
        Set<String> owned = store.ownedBackgroundKeys();
        String equippedKey = store.petBackgroundKey(petId);
        for (BackgroundCatalog.Background background : BackgroundCatalog.BACKGROUNDS) {
            if (!owned.contains(background.key)) continue;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            row.setBackgroundResource(R.drawable.bg_focuspet_card);

            FrameLayout preview = new FrameLayout(this);
            addBackgroundArtwork(preview, background.key);
            row.addView(preview, new LinearLayout.LayoutParams(dp(60), dp(60)));

            TextView name = text(background.name, 16, purpleDark);
            name.setMaxLines(2);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
            nameParams.setMargins(dp(8), 0, dp(4), 0);
            row.addView(name, nameParams);

            boolean equipped = background.key.equals(equippedKey);
            Button toggle = button(getString(equipped
                    ? R.string.background_equipped : R.string.background_use), v -> {
                if (!store.setPetBackground(petId, background.key)) {
                    toast("This background is not available.");
                }
                showCustomize(petId, CustomizeSection.BACKGROUND);
            });
            toggle.setTag("use_background_" + background.key);
            toggle.setEnabled(!equipped);
            row.addView(toggle, new LinearLayout.LayoutParams(dp(100), dp(52)));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, dp(6), 0, dp(4));
            content.addView(row, rowParams);
        }
        Button shop = button(getString(R.string.backgrounds_shop),
                v -> showShop(ShopCategory.BACKGROUNDS, petId));
        content.addView(shop);
    }

    private void addLearnedExpressionRow(long petId, PetExpression expression) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_focuspet_card);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        row.setMinimumHeight(dp(72));

        row.addView(expressionPreview(expression, dp(68), dp(58)),
                new LinearLayout.LayoutParams(dp(68), dp(58)));
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(8), 0, dp(4), 0);
        TextView name = text(getString(expression.nameRes), 16, purpleDark);
        name.setPadding(0, 0, 0, 0);
        details.addView(name);
        TextView description = systemText(getString(expression.descriptionRes), 13,
                ContextCompat.getColor(this, R.color.text_secondary));
        description.setPadding(0, dp(2), 0, 0);
        description.setMaxLines(2);
        details.addView(description);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        CheckBox use = new CheckBox(this);
        use.setText(R.string.expression_use_toggle);
        use.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        use.setTypeface(pixelFont);
        use.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeResource(14));
        use.setContentDescription(getString(R.string.expression_use_accessibility,
                getString(expression.nameRes)));
        use.setMinHeight(dp(48));
        use.setPadding(0, 0, 0, 0);
        use.setChecked(store.enabledPetExpressions(petId).contains(expression.key));
        use.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (store.setPetExpressionEnabled(petId, expression.key, checked)) return;
                buttonView.setOnCheckedChangeListener(null);
                buttonView.setChecked(!checked);
                buttonView.setOnCheckedChangeListener(this);
                toast("Could not save this expression setting.");
            }
        });
        row.addView(use, new LinearLayout.LayoutParams(-2, dp(52)));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, dp(4), 0, dp(6));
        content.addView(row, rowParams);
    }

    private FrameLayout expressionPreview(PetExpression expression, int width, int height) {
        FrameLayout preview = new FrameLayout(this);
        if (width > 0 && height > 0) {
            preview.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        }
        preview.setContentDescription(getString(expression.descriptionRes));
        ImageView bubble = new ImageView(this);
        bubble.setImageResource(R.drawable.bubble_thinking);
        bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bubble.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        preview.addView(bubble, new FrameLayout.LayoutParams(-1, -1));

        TextView art = new TextView(this);
        art.setText(expression.asciiArt);
        art.setGravity(Gravity.CENTER);
        art.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        art.setTypeface(Typeface.MONOSPACE);
        art.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        art.setTextScaleX(0.85f);
        art.setMaxLines(2);
        art.setIncludeFontPadding(false);
        art.setAutoSizeTextTypeUniformWithConfiguration(
                6, 10, 1, TypedValue.COMPLEX_UNIT_SP);
        art.setPadding(dp(4), 0, dp(4), dp(8));
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams artParams = new FrameLayout.LayoutParams(-1, -1);
        artParams.setMargins(dp(10), dp(8), dp(10), dp(10));
        preview.addView(art, artParams);
        return preview;
    }

    private void showExpressionInventory(long petId) {
        Pet pet = repository.pet(petId);
        if (pet == null) { showCollection(); return; }
        reset("Expression inventory");
        setActiveTab(1);
        content.addView(backNavigationButton("Back to expressions",
                v -> showCustomize(petId, CustomizeSection.EXPRESSIONS)));
        content.addView(systemText(
                getString(R.string.expression_inventory_intro, pet.name), 14,
                ContextCompat.getColor(this, R.color.text_secondary)));

        Set<String> learned = store.learnedPetExpressions(petId);
        boolean hasItems = false;
        for (PetExpression expression : PetExpression.LEARNABLE) {
            int stock = store.expressionStock(expression.key);
            if (stock <= 0) continue;
            hasItems = true;
            addExpressionInventoryRow(pet, expression, stock,
                    learned.contains(expression.key));
        }
        if (!hasItems) {
            LinearLayout emptyState = new LinearLayout(this);
            emptyState.setOrientation(LinearLayout.VERTICAL);
            emptyState.setBackgroundResource(R.drawable.bg_focuspet_card);
            emptyState.setPadding(dp(16), dp(12), dp(16), dp(12));
            emptyState.addView(systemText(getString(R.string.expression_inventory_empty), 15,
                    ContextCompat.getColor(this, R.color.text_secondary)));
            content.addView(emptyState);
        }
        content.addView(button(getString(R.string.expression_shop_button),
                v -> showShop(ShopCategory.EXPRESSIONS, petId)));
    }

    private void addExpressionInventoryRow(
            Pet pet, PetExpression expression, int stock, boolean alreadyLearned) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_focuspet_card);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        row.setMinimumHeight(dp(72));
        row.addView(expressionPreview(expression, dp(68), dp(58)),
                new LinearLayout.LayoutParams(dp(68), dp(58)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(8), 0, dp(4), 0);
        TextView name = text(getString(expression.nameRes), 16, purpleDark);
        name.setPadding(0, 0, 0, 0);
        details.addView(name);
        TextView quantity = systemText(
                getString(R.string.expression_inventory_stock, stock), 13,
                ContextCompat.getColor(this, R.color.text_secondary));
        quantity.setPadding(0, dp(2), 0, 0);
        details.addView(quantity);
        if (alreadyLearned) {
            TextView learned = systemText("Already learned by " + pet.name, 13,
                    ContextCompat.getColor(this, R.color.text_secondary));
            learned.setPadding(0, dp(2), 0, 0);
            details.addView(learned);
        }
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        Button teach = outlineButton(
                getString(alreadyLearned ? R.string.expression_learned : R.string.expression_teach),
                v -> confirmTeachExpression(pet.id, expression));
        teach.setTag("teach_expression_" + expression.key);
        teach.setEnabled(!alreadyLearned);
        row.addView(teach, new LinearLayout.LayoutParams(dp(92), dp(48)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, dp(4), 0, dp(6));
        content.addView(row, rowParams);
    }

    private void confirmTeachExpression(long petId, PetExpression expression) {
        Pet pet = repository.pet(petId);
        if (pet == null) { showCollection(); return; }
        String expressionName = getString(expression.nameRes);
        showDefaultFontDialog(new AlertDialog.Builder(this)
                .setTitle(getString(R.string.expression_teach_confirmation_title,
                        expressionName, pet.name))
                .setMessage(getString(R.string.expression_teach_confirmation_message,
                        expressionName, pet.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.expression_teach, (dialog, which) -> {
                    Pet latest = repository.pet(petId);
                    if (latest == null) { showCollection(); return; }
                    FocusStore.ExpressionUseResult result =
                            store.teachExpression(petId, expression.key);
                    if (result == FocusStore.ExpressionUseResult.LEARNED) {
                        toast(getString(R.string.expression_taught,
                                latest.name, expressionName));
                        showCustomize(petId, CustomizeSection.EXPRESSIONS);
                    } else {
                        if (result == FocusStore.ExpressionUseResult.ALREADY_LEARNED) {
                            toast("This pet has already learned this expression.");
                        } else if (result == FocusStore.ExpressionUseResult.NO_STOCK) {
                            toast("This expression item is no longer available.");
                        } else {
                            toast("Could not save this expression. Please try again.");
                        }
                        showExpressionInventory(petId);
                    }
                }));
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
                    configurePetCard(card, pet, pet.name,
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
                : FocusSession.STATUS_COMPLETED_NO_REWARD.equals(session.status) ? "Completed"
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
        FrameLayout illustration = screen.findViewById(R.id.history_detail_art);
        addBackgroundArtwork(illustration, session.backgroundKey == null
                ? BackgroundCatalog.DEFAULT_KEY : session.backgroundKey);

        String label = displayLabel(session.label);
        String status = session.endedEarly ? "Ended early"
                : FocusSession.STATUS_CLAIMED.equals(session.status) ? "Completed"
                : FocusSession.STATUS_CANCELLED.equals(session.status) ? "Ended early"
                : FocusSession.STATUS_COMPLETED_NO_REWARD.equals(session.status) ? "Completed"
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

        LinearLayout rewards = screen.findViewById(R.id.history_detail_rewards);
        if (FocusSession.STATUS_CLAIMED.equals(session.status)
                && repository.hasClaimableRewards(session)) {
            rewards.addView(text("Rewards claimed", 18,
                    ContextCompat.getColor(this, R.color.text_secondary)));
            addRewardSummaryRow(rewards, 0,
                    "★ " + getString(R.string.session_reward_xp, session.xpReward), false);
            addRewardSummaryRow(rewards, R.drawable.icon_coin,
                    getString(R.string.session_reward_coins, session.coinReward));
            addRewardSummaryRow(rewards, R.drawable.icon_essence,
                    getString(R.string.session_reward_essence, session.essenceReward));
            if (session.eggReward) {
                addRewardSummaryRow(rewards, R.drawable.icon_eggs,
                        getString(R.string.session_reward_egg));
            }
        } else if (!repository.hasClaimableRewards(session)
                || FocusSession.STATUS_CANCELLED.equals(session.status)) {
            rewards.addView(text("No completion rewards were granted for this session.", 18,
                    ContextCompat.getColor(this, R.color.text_secondary)));
        } else {
            rewards.addView(text("Rewards are waiting to be claimed.", 18,
                    ContextCompat.getColor(this, R.color.text_secondary)));
        }
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
        petAnimationsResumed = true;
        if (hatchAnimation != null && !hatchAnimation.isRunning()) {
            hatchAnimation.start();
        }
        refreshCalendarScreen();
        for (PetAnimationController controller : petAnimationControllers) {
            controller.resume();
        }
        scheduleCalendarRefresh();
    }

    @Override protected void onPause() {
        petAnimationsResumed = false;
        if (hatchAnimation != null) hatchAnimation.stop();
        for (PetAnimationController controller : petAnimationControllers) {
            controller.pause();
        }
        cancelRewardAnimations();
        calendarHandler.removeCallbacks(calendarRefresh);
        if (hatchNameInput != null && activeHatchDraft != null
                && activeHatchDraft.step == HatchDraft.STEP_NAME) {
            try {
                repository.updateHatchDraftName(activeHatchDraft.id,
                        hatchNameInput.getText().toString());
            } catch (IllegalStateException error) {
                android.util.Log.e("FocusPet", "Unable to save hatch name draft", error);
            }
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        calendarHandler.removeCallbacks(calendarRefresh);
        stopHatchAnimation();
        clearPetAnimationControllers();
        cancelRewardAnimations();
        if (timerReceiver != null) unregisterReceiver(timerReceiver);
        super.onDestroy();
    }
}
