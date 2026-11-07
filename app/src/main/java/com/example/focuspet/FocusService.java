package com.example.focuspet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FocusService extends Service {
    public static final String ACTION_START = "com.example.focuspet.START";
    public static final String ACTION_RESTORE = "com.example.focuspet.RESTORE";
    public static final String ACTION_PAUSE = "com.example.focuspet.PAUSE";
    public static final String ACTION_RESUME = "com.example.focuspet.RESUME";
    public static final String ACTION_CANCEL = "com.example.focuspet.CANCEL";
    public static final String ACTION_TICK = "com.example.focuspet.TICK";
    public static final String ACTION_COMPLETE = "com.example.focuspet.COMPLETE";
    public static final String ACTION_CANCELLED = "com.example.focuspet.CANCELLED";
    public static final String EXTRA_MINUTES = "minutes";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_REMAINING = "remaining";
    private static final String CHANNEL = "focus_timer";
    private long endAt;
    private long remaining;
    private long duration;
    private volatile boolean paused;
    private volatile boolean stopped;
    private Thread worker;
    private volatile int workerGeneration;
    private FocusStore store;

    @Override public void onCreate() {
        super.onCreate();
        store = new FocusStore(this);
        restoreFromStore();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Focus timer",
                NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_START.equals(action) && !store.hasActiveSession()) {
            duration = intent.getIntExtra(EXTRA_MINUTES, 25) * 60_000L;
            remaining = duration;
            endAt = SystemClock.elapsedRealtime() + duration;
            paused = false;
            stopped = false;
            store.saveActiveSession(endAt, (int) (duration / 60_000L));
            startForeground(21, notification());
            startWorker();
        } else if (ACTION_START.equals(action) || ACTION_RESTORE.equals(action)
                || action.isEmpty()) {
            restoreFromStore();
            if (store.hasActiveSession()) {
                startForeground(21, notification());
                if (!paused) startWorker();
            }
        } else if (ACTION_PAUSE.equals(action) && !paused) {
            remaining = Math.max(0, endAt - SystemClock.elapsedRealtime());
            paused = true;
            workerGeneration++;
            store.saveActiveSessionState(endAt, (int) (duration / 60_000L), true, remaining);
            updateNotification();
        } else if (ACTION_RESUME.equals(action) && paused) {
            endAt = SystemClock.elapsedRealtime() + remaining;
            paused = false;
            store.saveActiveSessionState(endAt, (int) (duration / 60_000L), false, remaining);
            startWorker();
        } else if (ACTION_CANCEL.equals(action)) {
            store.clearActiveSession();
            sendBroadcast(new Intent(ACTION_CANCELLED).setPackage(getPackageName()));
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    private void startWorker() {
        if (worker != null && worker.isAlive()) return;
        final int generation = ++workerGeneration;
        worker = new Thread(() -> {
            while (!paused && !stopped && generation == workerGeneration) {
                remaining = Math.max(0, endAt - SystemClock.elapsedRealtime());
                sendBroadcast(new Intent(ACTION_TICK).setPackage(getPackageName())
                        .putExtra(EXTRA_REMAINING, remaining));
                if (remaining == 0) {
                    store.clearActiveSession();
                    sendBroadcast(new Intent(ACTION_COMPLETE).setPackage(getPackageName())
                            .putExtra(EXTRA_MINUTES, (int) (duration / 60_000L)));
                    completionNotification();
                    stopSelf();
                    return;
                }
                updateNotification();
                SystemClock.sleep(1000);
            }
        });
        worker.start();
    }

    private void restoreFromStore() {
        if (!store.hasActiveSession()) return;
        duration = Math.max(1L, store.activeMinutes() * 60_000L);
        paused = store.activePaused();
        endAt = store.activeEndAt();
        remaining = paused ? store.activeRemaining()
                : Math.max(0L, endAt - SystemClock.elapsedRealtime());
        stopped = false;
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(paused ? "Focus paused" : "Focus in progress")
                .setContentText(format(remaining)).setOngoing(true).setContentIntent(open).build();
    }

    private void updateNotification() {
        getSystemService(NotificationManager.class).notify(21, notification());
    }

    private void completionNotification() {
        getSystemService(NotificationManager.class).notify(22, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Focus complete!")
                .setContentText("Your pet earned rewards.").setAutoCancel(true).build());
    }

    private String format(long millis) {
        return String.format(java.util.Locale.US, "%02d:%02d", millis / 60000, (millis / 1000) % 60);
    }

    @Override public void onDestroy() {
        stopped = true;
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
