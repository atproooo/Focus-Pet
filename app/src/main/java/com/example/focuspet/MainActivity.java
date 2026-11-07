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
import android.view.View;
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
import android.graphics.drawable.GradientDrawable;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST = 7;
    private static final int PURPLE = Color.rgb(108, 80, 174);
    private static final int PURPLE_DARK = Color.rgb(82, 57, 145);
    private static final int SURFACE = Color.rgb(255, 248, 255);
    private static final int CARD = Color.rgb(232, 226, 234);
    private static final int LAVENDER = Color.rgb(238, 224, 255);
    private FocusStore store;
    private LinearLayout content;
    private LinearLayout navigation;
    private TextView title;
    private BroadcastReceiver timerReceiver;
    private final List<LinearLayout> navigationItems = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FocusStore(this);
        buildShell();
        requestNotifications();
        if (store.hasActiveSession()) {
            ContextCompat.startForegroundService(this,
                    new Intent(this, FocusService.class).setAction(FocusService.ACTION_RESTORE));
            showActive(store.activeMinutes());
        } else {
            showHome();
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SURFACE);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });
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
        root.addView(header);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), 0, dp(20), dp(32));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        navigation.setBackgroundColor(Color.rgb(248, 241, 250));
        addNavigationButton(navigation, "🎯", "Focus", v -> showHome());
        addNavigationButton(navigation, "🐾", "Collection", v -> showCollection());
        addNavigationButton(navigation, "📊", "Statistics", v -> showStats());
        addNavigationButton(navigation, "⚙️", "Settings", v -> showSettings());
        navigation.setPadding(dp(4), dp(6), dp(4), dp(6));
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(92)));
        setContentView(root);
    }

    private void setSessionLock(boolean locked) {
        navigation.setVisibility(locked ? View.GONE : View.VISIBLE);
    }

    @Override public void onBackPressed() {
        if (store != null && store.hasActiveSession()) {
            android.widget.Toast.makeText(this, "Finish or end the current focus session first.",
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
        content.addView(text("Grow your focus, one calm session at a time.", 18, Color.rgb(35, 29, 38)));
        content.addView(companionCard());
        TextView today = text("Today", 16, PURPLE);
        today.setTypeface(null, Typeface.BOLD);
        today.setPadding(dp(0), dp(24), dp(0), dp(0));
        content.addView(today);
        content.addView(text(store.todayMinutes() + " min focused", 28, Color.rgb(35, 29, 38)));
        content.addView(text(store.sessions() + " sessions completed  •  Keep showing up!",
                16, Color.rgb(45, 39, 50)));
        content.addView(button("Start focus", v -> showSetup()));
    }

    private View companionCard() {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(roundColor(CARD, 16));
        TextView image = text("PET", 20, PURPLE);
        image.setGravity(Gravity.CENTER);
        image.setTypeface(null, Typeface.BOLD);
        image.setBackground(roundColor(LAVENDER, 16));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(132), dp(132));
        card.addView(image, imageParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(18), 0, 0, 0);
        details.addView(text("Your companion", 16, Color.rgb(35, 29, 38)));
        TextView name = text("Mochi the Fox", 24, Color.rgb(35, 29, 38));
        name.setPadding(24, 0, 24, 0);
        details.addView(name);
        details.addView(text("Level " + store.level() + "  ·  " + store.experience() + " XP", 18, PURPLE));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(store.experience() % 100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        details.addView(progress, new LinearLayout.LayoutParams(-1, dp(18)));
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(180));
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
        TextView hint = text("Choose any whole number from 1 to 180 minutes.", 14, Color.DKGRAY);
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
        content.addView(text("Put distractions aside. Your pet will be waiting\nwhen you finish.",
                18, Color.rgb(35, 29, 38)));
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
            startFocus(selectedMinutes, "");
        }));
    }

    private void startFocus(int minutes, String label) {
        Intent intent = new Intent(this, FocusService.class).setAction(FocusService.ACTION_START)
                .putExtra(FocusService.EXTRA_MINUTES, minutes).putExtra(FocusService.EXTRA_LABEL, label);
        ContextCompat.startForegroundService(this, intent);
        showActive(minutes);
    }

    private void showActive(int sessionMinutes) {
        reset("Focus in progress");
        setSessionLock(true);
        setActiveTab(0);
        TextView timer = text("Loading...", 64, Color.rgb(35, 29, 38));
        timer.setGravity(Gravity.CENTER);
        timer.setPadding(0, dp(64), 0, dp(18));
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
        Button end = outlineButton("End session", v -> sendService(FocusService.ACTION_CANCEL));
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
                    showCompletion(intent.getIntExtra(FocusService.EXTRA_MINUTES, 0));
                } else if (FocusService.ACTION_CANCELLED.equals(intent.getAction())) {
                    if (timerReceiver != null) {
                        unregisterReceiver(timerReceiver);
                        timerReceiver = null;
                    }
                    android.widget.Toast.makeText(MainActivity.this, "Focus session ended early.",
                            android.widget.Toast.LENGTH_SHORT).show();
                    showHome();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(FocusService.ACTION_TICK);
        filter.addAction(FocusService.ACTION_COMPLETE);
        filter.addAction(FocusService.ACTION_CANCELLED);
        ContextCompat.registerReceiver(this, timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void sendService(String action) {
        startService(new Intent(this, FocusService.class).setAction(action));
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void showCompletion(int minutes) {
        if (timerReceiver != null) {
            unregisterReceiver(timerReceiver);
            timerReceiver = null;
        }
        store.recordCompleted(minutes);
        reset("Session complete!");
        setSessionLock(false);
        setActiveTab(0);
        content.addView(text("🌟 Great work, your pet is proud.", 24, Color.rgb(74, 55, 95)));
        content.addView(text("+" + minutes + " minutes focused\n+" + minutes + " XP\n+" + minutes + " coins",
                20, Color.DKGRAY));
        content.addView(button("Back to home", v -> showHome()));
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
        content.addView(imagePlaceholder("PET IMAGE"));
        content.addView(text("🐱 Mochi\nCommon • Level " + store.level()
                + "\nYour starter pet grows with every completed session.", 20, Color.rgb(74, 55, 95)));
        content.addView(text("🔒 Fox — unlock at 100 coins\n🔒 Owl — unlock after a 7-day streak",
                17, Color.DKGRAY));
    }

    private void showStats() {
        reset("Focus statistics");
        setActiveTab(2);
        content.addView(text("Total focus time: " + store.totalMinutes() + " minutes\n"
                + "Completed sessions: " + store.sessions() + "\n"
                + "Current streak: " + store.streak() + " days\n"
                + "Longest streak: " + store.longestStreak() + " days", 20, Color.rgb(74, 55, 95)));
    }

    private void showSettings() {
        reset("Settings");
        setActiveTab(3);
        content.setPadding(dp(16), dp(24), dp(16), dp(24));
        TextView goalTitle = text("Daily goal", 18, Color.rgb(35, 29, 38));
        goalTitle.setTypeface(null, Typeface.BOLD);
        goalTitle.setPadding(0, 0, 0, dp(2));
        content.addView(goalTitle);
        TextView goalValue = text(store.dailyGoal() + " minutes", 18, Color.rgb(45, 39, 50));
        goalValue.setPadding(0, 0, 0, dp(28));
        content.addView(goalValue);
        Button resetButton = outlineButton("Reset local progress", v -> {
            store.reset();
            showSettings();
        });
        content.addView(resetButton, new LinearLayout.LayoutParams(-1, dp(52)));
        content.addView(text("Focus Grove works offline. Your progress stays\non this device.",
                18, Color.rgb(45, 39, 50)));
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
