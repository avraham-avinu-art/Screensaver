package com.avrahamart.nightscreen;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class MainActivity extends Activity {
    private static final int SCREEN_TIMEOUT_PROFILE_RELEASE_RESTARTS = 1;
    private static final int SCREEN_TIMEOUT_PROFILE_ELAPSED_DURING_HOLD = 2;
    private static final double SCREEN_TIMEOUT_EARLY_OFF_THRESHOLD = 0.40;
    private static final double SCREEN_TIMEOUT_RESTART_THRESHOLD = 0.60;

    public static final String EXTRA_OPENED_BY_CHARGER =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_CHARGER";
    public static final String EXTRA_OPENED_BY_DEVICE_LOCK =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_DEVICE_LOCK";
    public static final String EXTRA_OPENED_BY_WAKE =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_WAKE";
    public static final String EXTRA_OPENED_BY_BOOT =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_BOOT";
    public static final String EXTRA_OPENED_BY_TRACK_CHANGE =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_TRACK_CHANGE";
    public static final String EXTRA_OPENED_BY_IDLE =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_IDLE";
    public static final String ACTION_FINISH_FROM_TILE =
            "com.screensaver.action.FINISH_FROM_TILE";
    private static volatile boolean mainActivityResumed = false;
    private static volatile boolean mainActivitySessionAlive = false;
    private static volatile boolean mainActivityPausedByDeviceLock = false;

    
    public static boolean isMainActivitySessionAlive() {
        return mainActivitySessionAlive;
    }

    
        private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoOffRunnable = this::exitApplicationAtSelectedTime;
    private final Runnable screenOnReleaseRunnable = this::releaseScreenOnHoldForSystemTimeout;
    private boolean waitingForScreenWakeAfterActivityEnd = false;
    private boolean lockScreenContinuationArmed = false;
    private long lastScreenOffElapsedRealtime = 0L;
    private long lastScreenOnElapsedRealtime = 0L;
    private boolean deviceEventsRegistered = false;
    private boolean screenTimeoutObservationPending = false;
    private long screenHoldReleasedAtElapsedRealtime = 0L;
    private long screenTimeoutAtHoldReleaseMs = -1L;
    private ContentObserver screenOffTimeoutObserver;
    private boolean screenOffTimeoutObserverRegistered = false;
    private boolean openedByCharger = false;
    private boolean bottomSwipeCandidate = false;
    private boolean bottomSwipeDragging = false;
    private boolean bottomSwipeDismissAnimating = false;
    private float bottomSwipeStartX = 0f;
    private float bottomSwipeStartY = 0f;
    private long activityDeadlineElapsedRealtime = 0L;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName screenLockAdminComponent;
    private String lastScheduledDurationValue = null;
    private long settingsOpenedElapsedRealtime = 0L;
    private long lastBackPressElapsedRealtime = 0L;
    private final BroadcastReceiver deviceStateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mainActivityPausedByDeviceLock = false;
                clearInitialDeviceLockLaunchOverride();
                return;
            }
            if (Intent.ACTION_POWER_CONNECTED.equals(action)
                    || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                updateChargingBatteryDisplay();
                if (getBool("noShutdownWhileCharging", false)) {
                    scheduleAutoOff(true);
                }
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                updateChargingBatteryDisplay();
            } else if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_DATE_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                if (Intent.ACTION_TIME_CHANGED.equals(action)
                        || Intent.ACTION_DATE_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    lastFormattedHebrewDateKey = null;
                }
                if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    java.util.TimeZone deviceTimeZone = java.util.TimeZone.getDefault();
                    now.setTimeZone(deviceTimeZone);
                    numericDateFormat.setTimeZone(deviceTimeZone);
                }
                updateClock();
                scheduleNextClockUpdate();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                lastScreenOffElapsedRealtime = SystemClock.elapsedRealtime();
                learnScreenTimeoutBehaviorFromScreenOff();
                handleScreenOffForLockScreenContinuation();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                lastScreenOnElapsedRealtime = SystemClock.elapsedRealtime();
                if (waitingForScreenWakeAfterActivityEnd) {
                    resumeScreensaverAfterScreenWake();
                } else {
                    if (lockScreenContinuationArmed) {
                        applyPreventLockWhileActivePreference();
                    }
                    if (activityDeadlineElapsedRealtime > SystemClock.elapsedRealtime()
                            && getScreenTimeoutBehaviorProfile()
                            == SCREEN_TIMEOUT_PROFILE_ELAPSED_DURING_HOLD) {
                        // If this device's timeout kept counting while held, restore the hold
                        // after the user wakes the display and keep the original deadline.
                        getWindow().addFlags(
                                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        scheduleAutoOff(false);
                    }
                }
            }
        }
    };
    private final Calendar now = Calendar.getInstance();
    private final SimpleDateFormat numericDateFormat =
            new SimpleDateFormat("dd.MM.yyyy", Locale.US);

    private TextView timeText;
    private AnalogClockView analogClockView;
    private LinearLayout clockTextColumn;
    private View screenRootView;
    private BatteryChargeIndicatorView chargingBatteryView;

    private TextView gregorianText;
    private TextView hebrewText;
    private final int dateTextColor = Color.WHITE;
    private SharedPreferences prefs;
    private ImageView settingsButton;
    private FrameLayout settingsButtonHitArea;

    private final Runnable hideSettingsButtonRunnable = () -> {
        if (isSettingsButtonAlwaysVisible()) {
            if (settingsButtonHitArea != null) settingsButtonHitArea.setVisibility(View.VISIBLE);
            if (settingsButton != null) settingsButton.setAlpha(1f);
            return;
        }
        // Keep the hit target in the layout while hidden so the first tap can hit it
        // immediately after it is made visible.
        if (settingsButtonHitArea != null) settingsButtonHitArea.setVisibility(View.INVISIBLE);
        else if (settingsButton != null) settingsButton.setVisibility(View.INVISIBLE);
    };
    private boolean settingsDialogOpen = false;
    private ScreensaverDisplayView displayView;
    private FrameLayout stageView;
    private LinearLayout clockBlockView;
    private LinearLayout mediaHolderView;
    private TextView mediaDetails;
    private TextView mediaProgressText;
    private TextView mediaCurrentText;
    private TextView mediaTotalText;
    private LinearLayout mediaProgressRow;
    private SeekBar mediaSeekBar;
    private MediaController lastKnownMediaController;
    private ObjectAnimator titleMarqueeAnimator;
    private ObjectAnimator artistMarqueeAnimator;
    private int mediaProgressBaseWidth;
    private String lastTitle = "";
    private String lastArtist = "";
    private String lastObservedTrackIdentity = null;
    private long lastObservedTrackPositionMs = -1L;
    private long currentMediaDurationMs = 0L;
    private boolean lastPlaybackWasActive = false;
    private boolean lastPlayerIntroOnlySetting = false;
    private String lastPlayerControlsDisplayMode = "on";
    private boolean playerControlsIntroWindowVisible = false;
    private long playerControlsIntroWindowDeadlineElapsedRealtime = 0L;
    private final Runnable hidePlayerControlsIntroWindowRunnable = () -> {
        playerControlsIntroWindowVisible = false;
        playerControlsIntroWindowDeadlineElapsedRealtime = 0L;
        refreshPlayerControlsAfterIntro();
        refreshMediaHolderPosition();
    };

    private boolean playerIntroWindowVisible = false;
    private long playerIntroWindowDeadlineElapsedRealtime = 0L;
    private final Runnable hidePlayerIntroWindowRunnable = () -> {
        playerIntroWindowVisible = false;
        playerIntroWindowDeadlineElapsedRealtime = 0L;
        updatePlayerMirrorVisibility();
        refreshMediaHolderPosition();
    };

    private LinearLayout mediaMirror;
    private LinearLayout playerTextColumn;
    private TextView mediaTitle;
    private Bitmap currentAlbumArt;
    private ImageView mediaPlayPause;
    private FrameLayout mediaControlsHolder;
    private ImageView mediaPrev;
    private ImageView mediaNext;

    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;
    private MediaController.Callback mediaCallback;
    private MediaSessionManager.OnActiveSessionsChangedListener activeSessionsListener;
    private boolean activeSessionsListenerRegistered = false;
    private boolean progressHidePending = false;
    private boolean previousButtonHidePending = false;
    private boolean nextButtonHidePending = false;
    private final Runnable hidePreviousButtonIfStillUnsupported = () -> {
        previousButtonHidePending = false;
        if (mediaPrev == null || mediaController == null) return;
        PlaybackState currentState = mediaController.getPlaybackState();
        if (currentState == null
                || (currentState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0L) {
            mediaPrev.setVisibility(View.GONE);
        }
    };
    private final Runnable hideNextButtonIfStillUnsupported = () -> {
        nextButtonHidePending = false;
        if (mediaNext == null || mediaController == null) return;
        PlaybackState currentState = mediaController.getPlaybackState();
        if (currentState == null
                || (currentState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) == 0L) {
            mediaNext.setVisibility(View.GONE);
        }
    };
    private final Runnable hideProgressRowAfterMetadataChange = () -> {
        progressHidePending = false;
        if (mediaProgressRow == null) return;
        if (!arePlayerControlsVisible() || getCurrentMediaDuration() <= 0L) {
            setProgressRowVisible(false);
        } else {
            setProgressRowVisible(true);
        }
    };

    private final Runnable clockUpdateRunnable = new Runnable() {
        @Override public void run() {
            if (!mainActivityResumed || isFinishing()) return;
            updateClock();
            applyScreenAppearance();
            scheduleNextClockUpdate();
        }
    };
    private final Runnable mediaSessionRefreshRunnable = this::refreshSystemMediaMirror;
    private final Runnable mediaProgressTicker = new Runnable() {
        @Override public void run() {
            if (!mainActivityResumed || !updateCurrentMediaProgress()) return;
            handler.postDelayed(this, 1000L);
        }
    };
    private String lastFormattedHebrewDateKey = null;
    private String lastFormattedHebrewDateFormat;

    private void startClockUpdates() {
        handler.removeCallbacks(clockUpdateRunnable);
        updateClock();
        scheduleNextClockUpdate();
    }

    private void stopClockUpdates() {
        handler.removeCallbacks(clockUpdateRunnable);
    }

    private void scheduleNextClockUpdate() {
        long nowMillis = System.currentTimeMillis();
        long delay = 60_000L - (nowMillis % 60_000L) + 40L;
        handler.removeCallbacks(clockUpdateRunnable);
        handler.postDelayed(clockUpdateRunnable, Math.max(250L, delay));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        mainActivitySessionAlive = true;
        try {
            prefs = getSharedPreferences("settings", MODE_PRIVATE);
            initSettings();
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            applyChargingLaunchFlags(getIntent());
            applyDeviceLockLaunchFlags(getIntent());
            applyWakeLaunchFlags(getIntent());
            applyTrackChangeLaunchFlags(getIntent());
            applyPreventLockWhileActivePreference();
            hideSystemUi();
            buildUi();
            applySettings();
            refreshSettingsButtonVisibility();
            updateClock();
            scheduleAutoOff();
        } catch (Throwable t) {
            showFatalError(t);
        }
    }

    @Override
    public void onBackPressed() {
        long now = SystemClock.elapsedRealtime();
        if (lastBackPressElapsedRealtime > 0L
                && now - lastBackPressElapsedRealtime <= 2_000L) {
            lastBackPressElapsedRealtime = 0L;
            super.onBackPressed();
            return;
        }

        lastBackPressElapsedRealtime = now;
        Toast notice = AppToast.makeText(
                this, getString(R.string.main_activity_text_001), Toast.LENGTH_SHORT);
        notice.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dp(64));
        notice.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && ACTION_FINISH_FROM_TILE.equals(intent.getAction())) {
            finish();
            return;
        }
        if (intent != null && (intent.getBooleanExtra(EXTRA_OPENED_BY_CHARGER, false)
                || intent.getBooleanExtra(EXTRA_OPENED_BY_DEVICE_LOCK, false)
                || intent.getBooleanExtra(EXTRA_OPENED_BY_WAKE, false)
                || intent.getBooleanExtra(EXTRA_OPENED_BY_BOOT, false)
                || intent.getBooleanExtra(EXTRA_OPENED_BY_TRACK_CHANGE, false)
                || intent.getBooleanExtra(EXTRA_OPENED_BY_IDLE, false))) {
            waitingForScreenWakeAfterActivityEnd = false;
            lockScreenContinuationArmed = false;
            applyChargingLaunchFlags(intent);
            applyDeviceLockLaunchFlags(intent);
            applyWakeLaunchFlags(intent);
            applyTrackChangeLaunchFlags(intent);
            applyPreventLockWhileActivePreference();
            hideSystemUi();
            scheduleAutoOff(true);
            if (stageView != null) {
                stageView.post(() -> updateScreenPositions(
                        stageView, clockBlockView, mediaHolderView));
            }
        }
    }

    private void applyChargingLaunchFlags(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPENED_BY_CHARGER, false)) return;
        openedByCharger = true;
        if (Build.VERSION.SDK_INT >= 27) {
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void applyWakeLaunchFlags(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPENED_BY_WAKE, false)) return;
        if (Build.VERSION.SDK_INT >= 27) setTurnScreenOn(true);
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void applyTrackChangeLaunchFlags(Intent intent) {
        if (intent == null
                || !intent.getBooleanExtra(EXTRA_OPENED_BY_TRACK_CHANGE, false)) return;
        if (Build.VERSION.SDK_INT >= 27) setTurnScreenOn(true);
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void applyDeviceLockLaunchFlags(Intent intent) {
        if (intent == null
                || !intent.getBooleanExtra(EXTRA_OPENED_BY_DEVICE_LOCK, false)) return;
        if (Build.VERSION.SDK_INT >= 27) {
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

        /**
     * The lock-start marker belongs to one locked cycle only. Remove it after
     * the user unlocks so later wake/charge/boot launches cannot inherit it.
     */
    private void clearInitialDeviceLockLaunchOverride() {
        Intent current = getIntent();
        if (current == null
                || !current.getBooleanExtra(EXTRA_OPENED_BY_DEVICE_LOCK, false)) return;

        Intent updated = new Intent(current);
        updated.removeExtra(EXTRA_OPENED_BY_DEVICE_LOCK);
        setIntent(updated);

        if (Build.VERSION.SDK_INT >= 27) setTurnScreenOn(false);
        getWindow().clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        applyPreventLockWhileActivePreference();
    }

    /**
     * Lock-screen visibility while the selected activity session is active is
     * normal screensaver behavior and does not depend on the continuation switch.
     * That switch is consulted only after the selected duration ended and the
     * Activity is waiting for the next screen wake.
     */
    private void applyPreventLockWhileActivePreference() {
        boolean preventLock = LockBehaviorPreferences.isPreventionEnabled(this);
        boolean openedByDeviceLock = getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_OPENED_BY_DEVICE_LOCK, false);
        boolean openedByDeepWake = getIntent() != null
                && (getIntent().getBooleanExtra(EXTRA_OPENED_BY_WAKE, false)
                || getIntent().getBooleanExtra(EXTRA_OPENED_BY_TRACK_CHANGE, false)
                || getIntent().getBooleanExtra(EXTRA_OPENED_BY_IDLE, false));
        if (!getBool("continueOnLockScreen", false)) {
            lockScreenContinuationArmed = false;
        }
        boolean showAboveExistingKeyguard = !preventLock || openedByDeviceLock
                || openedByDeepWake || lockScreenContinuationArmed;
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(showAboveExistingKeyguard);
        } else if (showAboveExistingKeyguard) {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        if (preventLock) {
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        } else {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reconcile the independent foreground monitor whenever the screensaver
        // Activity becomes visible again. The service preserves an unchanged
        // receiver registration, so this repeated request does not interrupt monitoring.
        LockScreenMonitorService.sync(this);
        registerDeviceStateReceiver();
        registerScreenOffTimeoutObserver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainActivityResumed = true;
        mainActivityPausedByDeviceLock = false;
        String disabledFeatures =
                PermissionDependencySync.disableFeaturesWithoutPermissions(this);
        if (!disabledFeatures.isEmpty()) {
            AppToast.makeText(this, disabledFeatures, Toast.LENGTH_LONG).show();
        }
        NightScreenApplication.syncChargerReceiver(this);
        applyPreventLockWhileActivePreference();
        try {
            if (settingsDialogOpen) {
                String activeDuration = getString("screenSaverDuration", "0");
                boolean durationWasChanged = lastScheduledDurationValue != null
                        && !activeDuration.equals(lastScheduledDurationValue);
                long nowElapsed = SystemClock.elapsedRealtime();
                if (!durationWasChanged && settingsOpenedElapsedRealtime > 0L
                        && activityDeadlineElapsedRealtime > 0L) {
                    activityDeadlineElapsedRealtime += Math.max(
                            0L, nowElapsed - settingsOpenedElapsedRealtime);
                }
                settingsDialogOpen = false;
                settingsOpenedElapsedRealtime = 0L;
                applySettings();
                if (stageView != null) {
                    stageView.post(() -> updateScreenPositions(
                            stageView, clockBlockView, mediaHolderView));
                }
            }

            refreshSettingsButtonVisibility();
            if (waitingForScreenWakeAfterActivityEnd) {
                if (isScreenInteractive()) resumeScreensaverAfterScreenWake();
            } else {
                scheduleAutoOff();
            }
            hideSystemUi();
            startClockUpdates();
            if (getBool("showPlayer", false)) {
                registerActiveSessionsListener();
                handler.removeCallbacks(mediaSessionRefreshRunnable);
                handler.postDelayed(mediaSessionRefreshRunnable, 250L);
            } else {
                unregisterActiveSessionsListener();
                detachMediaController();
                stopMediaProgressTicker();
                hideMediaMirror();
            }
        } catch (Throwable t) {
            showFatalError(t);
        }
    }

    @Override
    protected void onPause() {
        android.app.KeyguardManager keyguardManager =
                (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        mainActivityPausedByDeviceLock =
                keyguardManager != null && keyguardManager.isKeyguardLocked();
        mainActivityResumed = false;
        stopClockUpdates();
        handler.removeCallbacks(mediaSessionRefreshRunnable);
        stopMediaProgressTicker();
        unregisterActiveSessionsListener();
        detachMediaController();
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterDeviceStateReceiver();
        unregisterScreenOffTimeoutObserver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mainActivitySessionAlive = false;
        handler.removeCallbacks(clockUpdateRunnable);
        handler.removeCallbacks(mediaSessionRefreshRunnable);
        handler.removeCallbacks(mediaProgressTicker);
        handler.removeCallbacks(autoOffRunnable);
        handler.removeCallbacks(screenOnReleaseRunnable);
        handler.removeCallbacks(hideSettingsButtonRunnable);
        handler.removeCallbacks(hideProgressRowAfterMetadataChange);
        handler.removeCallbacks(hidePreviousButtonIfStillUnsupported);
        handler.removeCallbacks(hideNextButtonIfStillUnsupported);
        unregisterActiveSessionsListener();
        stopTitleMarquee();
        stopArtistMarquee();
        detachMediaController();
        super.onDestroy();
    }

    private void buildUi() {
        displayView = new ScreensaverDisplayView(this, prefs,
                new ScreensaverDisplayView.Actions() {
                    @Override public void onOpenSettings() { showSettings(); }
                    @Override public void onPlayPause() { sendPlayPause(); }
                    @Override public void onPrevious() { sendPrevious(); }
                    @Override public void onNext() { sendNext(); }
                    @Override public void onSeekProgress(int progress) {
                        try {
                            if (mediaController == null || currentMediaDurationMs <= 0L) return;
                            mediaController.getTransportControls().seekTo(
                                    currentMediaDurationMs * progress / 1000L);
                        } catch (Throwable ignored) { }
                    }
                });

        // Both activation paths now use this single shared visual component.
        screenRootView = displayView;
        stageView = displayView.stageView;
        clockBlockView = displayView.clockBlockView;
        mediaHolderView = displayView.mediaHolderView;
        timeText = displayView.timeText;
        analogClockView = displayView.analogClockView;
        clockTextColumn = displayView.clockTextColumn;
        chargingBatteryView = displayView.chargingBatteryView;
        gregorianText = displayView.gregorianText;
        hebrewText = displayView.hebrewText;
        settingsButton = displayView.settingsButton;
        settingsButtonHitArea = displayView.settingsButtonHitArea;
        mediaMirror = displayView.mediaMirror;
        playerTextColumn = displayView.playerTextColumn;
        mediaTitle = displayView.mediaTitle;
        mediaDetails = displayView.mediaDetails;
        mediaProgressText = displayView.mediaProgressText;
        mediaCurrentText = displayView.mediaCurrentText;
        mediaTotalText = displayView.mediaTotalText;
        mediaProgressRow = displayView.mediaProgressRow;
        mediaSeekBar = displayView.mediaSeekBar;
        mediaControlsHolder = displayView.mediaControlsHolder;
        mediaPlayPause = displayView.mediaPlayPause;
        mediaPrev = displayView.mediaPrev;
        mediaNext = displayView.mediaNext;
        mediaProgressBaseWidth = displayView.mediaProgressBaseWidth;

        setContentView(displayView);
        displayView.post(() -> {
            displayView.applySettings(getWindow(),
                    mediaController != null && hasUsableMediaSession(mediaController),
                    currentAlbumArt);
            displayView.updateScreenPositions();
        });
    }

    private void updateScreenPositions(FrameLayout stage, View clockBlock, View mediaHolder) {
        if (displayView != null) displayView.updateScreenPositions();
    }

    private void translateCenteredChildrenToOffset(
            ViewGroup container, int availableWidth, float offsetFraction) {
        if (container == null || availableWidth <= 0) return;

        int widestChild = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            int childWidth = Math.max(child.getWidth(), child.getMeasuredWidth());
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params != null && params.width > 0) {
                childWidth = Math.max(childWidth, params.width);
            }
            widestChild = Math.max(widestChild, childWidth);
        }

        // At +/-100%, the widest visible row reaches the exact screen edge.
        float maxOffset = Math.max(0f, (availableWidth - widestChild) / 2f);
        float translation = Math.max(-1f, Math.min(1f, offsetFraction)) * maxOffset;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.setTranslationX(translation);
            } else {
                child.setTranslationX(0f);
            }
        }
    }

    
    private void buildSystemMediaMirror(LinearLayout root) {
        mediaMirror = new LinearLayout(this);
        mediaMirror.setOrientation(LinearLayout.VERTICAL);
        mediaMirror.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mediaMirror.setPadding(0, 0, 0, 0);
        mediaMirror.setBackgroundColor(Color.TRANSPARENT);
        mediaMirror.setClipChildren(false);
        mediaMirror.setClipToPadding(false);
        mediaMirror.setVisibility(View.GONE);

        // The reference player is intentionally clean: the position row is
        // the first visible element and the three controls are widely spaced.
        mediaTitle = textView("", 17, Color.WHITE);
        mediaTitle.setVisibility(View.VISIBLE);
        mediaTitle.setGravity(Gravity.CENTER);
        mediaTitle.setSingleLine(true);
        mediaTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        mediaTitle.setIncludeFontPadding(true);
        mediaTitle.addOnLayoutChangeListener((view, left, top, right, bottom,
                                              oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft)) {
                mediaTitle.post(() -> updateTitleMarqueeIfNeeded());
            }
        });
        mediaDetails = textView("", 17, Color.rgb(175, 175, 175));
        mediaDetails.setVisibility(View.INVISIBLE);
        mediaDetails.setGravity(Gravity.CENTER);
        mediaDetails.setSingleLine(true);
        mediaDetails.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        mediaDetails.setIncludeFontPadding(true);
        mediaDetails.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft)) {
                mediaDetails.post(() -> updateArtistMarqueeIfNeeded());
            }
        });
        mediaProgressText = textView("", 12, Color.WHITE);
        mediaProgressText.setVisibility(View.GONE);

        mediaProgressRow = new LinearLayout(this);
        mediaProgressRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaProgressRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mediaProgressRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        mediaProgressRow.setPadding(dp(4), 0, dp(4), 0);
        mediaProgressRow.setVisibility(View.GONE);

        mediaCurrentText = textView("0:00", 12, Color.WHITE);
        mediaCurrentText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        mediaCurrentText.setSingleLine(true);
        mediaCurrentText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams currentParams =
                new LinearLayout.LayoutParams(dp(48), dp(32));
        currentParams.rightMargin = dp(4);
        mediaProgressRow.addView(mediaCurrentText, currentParams);

        mediaSeekBar = new SeekBar(this);
        mediaSeekBar.setVisibility(View.GONE);
        mediaSeekBar.setPadding(dp(7), 0, dp(7), 0);
        mediaSeekBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        mediaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    PlaybackState st = mediaController.getPlaybackState();
                    if (st != null && mediaController.getMetadata() != null) {
                        long duration = mediaController.getMetadata()
                                .getLong(MediaMetadata.METADATA_KEY_DURATION);
                        if (duration > 0) {
                            mediaController.getTransportControls().seekTo(
                                    (duration * progress) / 1000L);
                        }
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // The user requested the progress line to be 50% shorter than the previous version.
        int lineWidth = Math.max(
                dp(120),
                Math.round(getResources().getDisplayMetrics().widthPixels * 0.36f));
        mediaProgressBaseWidth = lineWidth;
        mediaProgressRow.addView(mediaSeekBar,
                new LinearLayout.LayoutParams(lineWidth, dp(32)));

        mediaTotalText = textView("0:00", 12, Color.WHITE);
        mediaTotalText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        mediaTotalText.setSingleLine(true);
        mediaTotalText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams totalParams =
                new LinearLayout.LayoutParams(dp(48), dp(32));
        totalParams.leftMargin = dp(4);
        mediaProgressRow.addView(mediaTotalText, totalParams);

        // Artist first and song title immediately below it. Both rows share one
        // content-width column so their selected alignment is relative to each other.
        playerTextColumn = new LinearLayout(this);
        playerTextColumn.setOrientation(LinearLayout.VERTICAL);
        playerTextColumn.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        playerTextColumn.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        playerTextColumn.setClipChildren(false);
        playerTextColumn.setClipToPadding(false);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, dp(30));
        playerTextColumn.addView(mediaDetails, detailsParams);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(30));
        titleParams.topMargin = dp(0);
        playerTextColumn.addView(mediaTitle, titleParams);

        LinearLayout.LayoutParams playerTextColumnParams =
                new LinearLayout.LayoutParams(-2, -2);
        playerTextColumnParams.gravity = Gravity.CENTER_HORIZONTAL;
        mediaMirror.addView(playerTextColumn, playerTextColumnParams);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-2, dp(28));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(0);
        mediaMirror.addView(mediaProgressRow, progressParams);

        FrameLayout controls = new FrameLayout(this);
        mediaControlsHolder = controls;
        controls.setPadding(0, 0, 0, 0);
        controls.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        int buttonSize = dp(44);
        int controlWidth = Math.max(
                dp(180),
                Math.round(getResources().getDisplayMetrics().widthPixels * 0.44f));
        LinearLayout.LayoutParams controlAreaParams =
                new LinearLayout.LayoutParams(controlWidth, dp(44));
        controlAreaParams.gravity = Gravity.CENTER_HORIZONTAL;
        controlAreaParams.topMargin = dp(-5);

        // Physical order: Next on the left, Play/Pause in the center, Previous on the right.
        mediaNext = mediaIconButton(R.drawable.media_next, getString(R.string.main_activity_text_005));
        mediaNext.setOnClickListener(v -> sendPrevious());
        FrameLayout.LayoutParams nextParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        controls.addView(mediaNext, nextParams);

        mediaPlayPause = mediaIconButton(R.drawable.media_play, getString(R.string.main_activity_text_006));
        mediaPlayPause.setOnClickListener(v -> sendPlayPause());
        FrameLayout.LayoutParams playParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.CENTER);
        controls.addView(mediaPlayPause, playParams);

        mediaPrev = mediaIconButton(R.drawable.media_previous, getString(R.string.main_activity_text_007));
        mediaPrev.setOnClickListener(v -> sendNext());
        FrameLayout.LayoutParams prevParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        controls.addView(mediaPrev, prevParams);

        mediaMirror.addView(controls, controlAreaParams);

        LinearLayout.LayoutParams mirrorParams =
                new LinearLayout.LayoutParams(
                        Math.min(dp(760), Math.max(dp(1),
                                getResources().getDisplayMetrics().widthPixels)),
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        mirrorParams.gravity = Gravity.CENTER_HORIZONTAL;
        mirrorParams.topMargin = dp(28);
        mirrorParams.bottomMargin = 0;
        root.addView(mediaMirror, mirrorParams);
    }

    private void initSettings() {
        if (prefs == null) {
            prefs = getSharedPreferences("settings", MODE_PRIVATE);
        }

        // Detect a clean installation before writing any defaults to preferences.
        boolean freshInstall = prefs.getAll().isEmpty();
        boolean legacyDreamDefaultWasApplied =
                getBool("chargeActivationMethodDefaultV1Applied", false);
        SharedPreferences.Editor e = prefs.edit();

        // Migrate the previous implicit light-gray default to the requested white default.
        // Explicitly selected colors other than the old default remain unchanged.
        boolean applyFontColorDefaultMigration =
                !getBool("fontColorWhiteDefaultMigrationV1Applied", false);
        int legacyFontColorDefault = Color.rgb(220, 220, 220);
        int initialFontColor = getInt("fontColor", legacyFontColorDefault);
        if (applyFontColorDefaultMigration) {
            if (!prefs.contains("fontColor") || initialFontColor == legacyFontColorDefault) {
                initialFontColor = Color.WHITE;
            }
            e.putBoolean("fontColorWhiteDefaultMigrationV1Applied", true);
        }
        e.putInt("fontColor", initialFontColor);
        e.putInt("screenBackgroundColor", getInt("screenBackgroundColor", Color.BLACK));
        e.putInt("screenBrightness", getInt("screenBrightness", -1));
        e.remove("deviceLockStartConsumed")
                .remove("mainActivitySessionAlive")
                .remove("mainActivityResumed")
                .remove("mainActivityPausedByDeviceLock")
                .remove("mainActivityWaitingForScreenWake")
                .remove("mainActivitySessionPid")
                .remove("screenLockObservationKnown")
                .remove("screenLockObserved")
                .remove("monitorNotificationDetached")
                .remove("chargeActivationMethodDefaultV1Applied");
        e.putInt("fontSize", getInt("fontSize", 17));
        e.putInt("clockDateSize", getInt("clockDateSize", 84));
        e.putInt("clockHorizontalOffset", getInt("clockHorizontalOffset", 0));
        e.putInt("playerHorizontalOffset", getInt("playerHorizontalOffset", 0));
        e.putInt("nightModeStartHour", getInt("nightModeStartHour", 22));
        e.putInt("nightModeEndHour", getInt("nightModeEndHour", 7));
        e.putInt("batteryProtectionThreshold", getInt("batteryProtectionThreshold", 20));
        String selectedTimeFormat = getString("timeFormat", "24");
        if (!"12".equals(selectedTimeFormat) && !"24".equals(selectedTimeFormat)) {
            selectedTimeFormat = "24";
        }
        e.putString("timeFormat", selectedTimeFormat);
        e.putString("backgroundStyle", getString("backgroundStyle", "solid"));
        e.putString("clockDatePosition", getString("clockDatePosition", "upper"));
        e.putString("clockDisplayMode", getString("clockDisplayMode", "digital"));
        String savedDialStyle = getString("clockDialStyle", "lines_only");
        if ("classic".equals(savedDialStyle) || "minimal".equals(savedDialStyle)) {
            savedDialStyle = "lines_only";
        } else if ("numerals".equals(savedDialStyle)) {
            savedDialStyle = "lines_numbers";
        } else if (!"numbers_only".equals(savedDialStyle)
                && !"lines_only".equals(savedDialStyle)
                && !"lines_numbers".equals(savedDialStyle)) {
            savedDialStyle = "lines_only";
        }
        e.putString("clockDialStyle", savedDialStyle);
        e.putString("playerPosition", getString("playerPosition", "bottom"));
        e.putString("chargingBatteryDisplayMode",
                getString("chargingBatteryDisplayMode", "off"));
        e.putBoolean("nightModeEnabled", getBool("nightModeEnabled", false));
        e.putBoolean("batteryProtectionEnabled", getBool("batteryProtectionEnabled", false));
        boolean useAndroidDreamForCharging = getBool("useAndroidDreamForCharging", false);
        // The post-575 migration forced Android Dream on for existing installs.
        // Restore the Build 575 direct charger route once; a user may opt into Dream later.
        if (legacyDreamDefaultWasApplied) useAndroidDreamForCharging = false;
        e.putBoolean("useAndroidDreamForCharging", useAndroidDreamForCharging);
        e.putBoolean("frameUseAlbumArt",
                getBool("frameUseAlbumArt", getBool("showAlbumArt", true)));
        e.remove("showAlbumArt");
        e.putInt("playerSize", getInt("playerSize", 17));
        e.putInt("fontWeight", getInt("fontWeight", 0));
        String savedFontFamily = getString("fontFamily", "sans-serif");
        // Earlier builds stored DL Adva as the initial default. Restore Sans Serif
        // once on upgrade, but preserve any other font the user explicitly chose.
        if (!getBool("fontSansSerifDefaultRestoredV1", false)) {
            if (!prefs.contains("fontFamily")
                    || "custom:DL Adva.ttf".equals(savedFontFamily)
                    || "custom:Merienda-Black.ttf".equals(savedFontFamily)) {
                savedFontFamily = "sans-serif";
            }
            e.putBoolean("fontSansSerifDefaultRestoredV1", true);
        } else if ("custom:Merienda-Black.ttf".equals(savedFontFamily)) {
            // Merienda was removed because it clipped on screen.
            savedFontFamily = "sans-serif";
        }
        e.putString("fontFamily", savedFontFamily);

        e.putBoolean("showGregorian", getBool("showGregorian", true));
        e.putBoolean("showHebrew", getBool("showHebrew", true));
        boolean showPlayerSetting = getBool("showPlayer", false);
        if (!getBool("showPlayerNotificationAccessFlowV1Applied", false)) {
            // Earlier versions enabled the player by default without requiring notification access.
            // Move old installs without that permission to an explicit opt-in; preserve the choice
            // for installs that already have access.
            if (!prefs.contains("showPlayer") || !hasNotificationAccess()) {
                showPlayerSetting = false;
            }
            e.putBoolean("showPlayerNotificationAccessFlowV1Applied", true);
        }
        e.putBoolean("showPlayer", showPlayerSetting);
        e.putBoolean("showDetails", getBool("showDetails", true));
        if (!prefs.contains("playerControlsDisplayMode")) {
            e.putString("playerControlsDisplayMode",
                    getBool("showProgress", true) ? "on" : "off");
        }
        e.remove("showProgress");
        lastTitle = getString("lastTitle", lastTitle);
        lastArtist = getString("lastArtist", lastArtist);

        if (!getBool("referenceLayoutV3Applied", false)) {
            e.putBoolean("showDetails", getBool("showDetails", true));
            e.putBoolean("frameEnabled", false);
            e.putBoolean("referenceLayoutV3Applied", true);
        } else if (!getBool("referenceLayoutV2Applied", false)) {
            e.putBoolean("frameEnabled", false);
            e.putBoolean("referenceLayoutV2Applied", true);
        } else {
            e.putBoolean("frameEnabled", getBool("frameEnabled", false));
        }
        e.putInt("frameColor", getInt("frameColor", Color.WHITE));
        e.putInt("frameWidth", getInt("frameWidth", 1));
        e.putInt("frameRadius", getInt("frameRadius", 10));
        e.putInt("buttonColor", getInt("buttonColor", Color.rgb(45,45,45)));
        e.putInt("buttonBorderColor", getInt("buttonBorderColor", Color.rgb(110,110,110)));
        e.putString("screenSaverDuration", getString("screenSaverDuration", "device"));
        // Keep the existing charge auto-start setting for both activation routes.
        e.putBoolean("autoStartOnCharge", getBool("autoStartOnCharge", false));
        e.putBoolean("noShutdownWhileCharging",
                getBool("noShutdownWhileCharging", false));
        e.remove("showBatteryPercent");
        if (!getBool("chargingOptionsSplitV1Applied", false)) {
            // Migrate the old activity-time choice once to the dedicated charging switch.
            if ("charging".equals(getString("screenSaverDuration", "device"))) {
                e.putString("screenSaverDuration", "device");
                if (!prefs.contains("noShutdownWhileCharging")) {
                    e.putBoolean("noShutdownWhileCharging", true);
                }
            }
            e.putBoolean("chargingOptionsSplitV1Applied", true);
        }
        if (!getBool("defaultLayoutV4Applied", false)) {
            // Apply the requested layout positions once to existing installations.
            // The clock-size preference is migrated separately below.
            e.putString("clockDatePosition", "upper");
            e.putString("playerPosition", "bottom");
            e.putBoolean("defaultLayoutV4Applied", true);
        } else {
            e.putString("clockDatePosition", getString("clockDatePosition", "upper"));
            e.putString("playerPosition", getString("playerPosition", "bottom"));
        }
        if (!getBool("clockSizePresetsV5Applied", false)) {
            // The active options are exactly 48, 60, 72, 84 and 96sp.
            // Migrate old saved values once, then never reinterpret this preference again.
            boolean defaultLayoutPending = !getBool("defaultLayoutV4Applied", false);
            boolean hadClockSize = prefs.contains("clockDateSize");
            int savedClockSize = getInt("clockDateSize", 96);
            boolean previousPresetMigrationApplied =
                    getBool("clockSizePresetsV4Applied", false);
            boolean legacyFourPresetScale =
                    getBool("clockSizesV2Applied", false)
                    && !getBool("clockSizePresetsV3Applied", false)
                    && !getBool("clockSizesV24Restored", false);
            int migratedClockSize;

            if (defaultLayoutPending || !hadClockSize) {
                migratedClockSize = 96;
            } else if (previousPresetMigrationApplied
                    && (savedClockSize == 48 || savedClockSize == 60
                    || savedClockSize == 72 || savedClockSize == 84
                    || savedClockSize == 96)) {
                // These values already use the current five-size scale.
                migratedClockSize = savedClockSize;
            } else if (legacyFourPresetScale) {
                // Preserve the label from older four-size versions.
                switch (savedClockSize) {
                    case 38: migratedClockSize = 48; break;
                    case 48: migratedClockSize = 48; break;
                    case 60: migratedClockSize = 60; break;
                    case 72: migratedClockSize = 84; break;
                    case 84: migratedClockSize = 96; break;
                    case 96: migratedClockSize = 96; break;
                    default: migratedClockSize = 96; break;
                }
            } else {
                // Old five-size options: 38 / 48 / 60 / 72 / 84.
                switch (savedClockSize) {
                    case 38: migratedClockSize = 48; break;
                    case 48: migratedClockSize = 60; break;
                    case 60: migratedClockSize = 72; break;
                    case 72: migratedClockSize = 84; break;
                    case 84: migratedClockSize = 96; break;
                    case 96: migratedClockSize = 96; break;
                    default: migratedClockSize = 96; break;
                }
            }
            e.putInt("clockDateSize", migratedClockSize);
            e.putBoolean("clockSizePresetsV5Applied", true);
        }
        e.commit();

        // Upgrade the existing five presets to the requested 60/72/84/96/108sp scale.
        // Run once to preserve the selected preset label across the update.
        if (!getBool("clockSizePresetsV6Applied", false)) {
            int oldSize = getInt("clockDateSize", 84);
            int newSize;
            if (freshInstall) {
                // A clean install starts at the "רגיל" preset (84sp).
                // Existing installations keep the established one-time size migration.
                newSize = 84;
            } else {
                switch (oldSize) {
                    case 38:
                    case 48: newSize = 60; break;
                    case 60: newSize = 72; break;
                    case 72: newSize = 84; break;
                    case 84: newSize = 96; break;
                    case 96: newSize = 108; break;
                    case 108: newSize = 108; break;
                    default: newSize = 108; break;
                }
            }
            prefs.edit()
                    .putInt("clockDateSize", newSize)
                    .putBoolean("clockSizePresetsV6Applied", true)
                    .commit();
        }
    }

    private boolean getBool(String key, boolean def) {
        if (prefs == null) return def;
        try {
            Object value = prefs.getAll().get(key);
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof String) return Boolean.parseBoolean((String) value);
            if (value instanceof Number) return ((Number) value).intValue() != 0;
        } catch (Throwable ignored) {
        }
        return def;
    }

    private int getInt(String key, int def) {
        if (prefs == null) return def;
        try {
            Object value = prefs.getAll().get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
            if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        } catch (Throwable ignored) {
        }
        return def;
    }

    private String getString(String key, String def) {
        if (prefs == null) return def;
        try {
            Object value = prefs.getAll().get(key);
            return value == null ? def : String.valueOf(value);
        } catch (Throwable ignored) {
            return def;
        }
    }

    private int measuredLineHeight(TextView view, String sampleText,
                                   int extraSpaceDp, int minimumHeightDp) {
        if (view == null) return dp(minimumHeightDp);

        android.graphics.Paint paint = view.getPaint();
        android.graphics.Paint.FontMetrics metrics = paint.getFontMetrics();

        // Exclude excess font padding; visible glyph bounds below still protect custom fonts.
        int lineMetricsHeight = Math.round(metrics.descent - metrics.ascent);

        // Also inspect a representative sample and the current text, including Hebrew
        // characters and digits, in case visible outlines extend beyond font metrics.
        android.graphics.Rect bounds = new android.graphics.Rect();
        int visibleGlyphHeight = 0;
        if (sampleText != null && !sampleText.isEmpty()) {
            paint.getTextBounds(sampleText, 0, sampleText.length(), bounds);
            visibleGlyphHeight = Math.max(visibleGlyphHeight, bounds.height());
        }
        CharSequence currentText = view.getText();
        if (currentText != null && currentText.length() > 0) {
            paint.getTextBounds(currentText.toString(), 0, currentText.length(), bounds);
            visibleGlyphHeight = Math.max(visibleGlyphHeight, bounds.height());
        }

        int measured = Math.max(lineMetricsHeight, visibleGlyphHeight) + dp(extraSpaceDp);
        return Math.max(dp(minimumHeightDp), measured);
    }

    private int blendWithBlack(int color, float factor) {
        float f = Math.max(0f, Math.min(1f, factor));
        return Color.rgb(
                Math.round(Color.red(color) * f),
                Math.round(Color.green(color) * f),
                Math.round(Color.blue(color) * f));
    }

    private String formatMs(long ms) {
        long t = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", t / 60, t % 60);
    }

    private Typeface loadCustomTypeface(String fileName) {
        Typeface fallback = Typeface.create("sans-serif", Typeface.NORMAL);
        if (fileName == null || fileName.isEmpty()
                || fileName.contains("/") || fileName.contains("\\")
                || !(fileName.toLowerCase(Locale.US).endsWith(".ttf")
                     || fileName.toLowerCase(Locale.US).endsWith(".otf"))) {
            return fallback;
        }

        try {
            return Typeface.createFromAsset(getAssets(), "fonts/" + fileName);
        } catch (Exception ignored) {
            // Keep the screensaver readable if an asset is unexpectedly missing.
            return fallback;
        }
    }

    private int horizontalGravityForTextMode(String mode) {
        if ("right".equals(mode)) return Gravity.RIGHT;
        if ("left".equals(mode)) return Gravity.LEFT;
        return Gravity.CENTER_HORIZONTAL;
    }

    private void applyHorizontalTextGravity(TextView view, int horizontalGravity) {
        if (view == null) return;
        int verticalGravity = view.getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
        if (verticalGravity == 0) verticalGravity = Gravity.CENTER_VERTICAL;
        // Explicit textAlignment values override gravity. Force gravity-based
        // alignment so the selected left/center/right option is actually applied.
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setGravity(horizontalGravity | verticalGravity);
    }

    private int playerTextHorizontalGravity() {
        return horizontalGravityForTextMode(getString("playerTextAlignment", "center"));
    }

    private int measuredTextColumnWidth(TextView[] rows, int maxWidth) {
        float widest = 0f;
        for (TextView row : rows) {
            if (row == null || row.getVisibility() != View.VISIBLE) continue;
            CharSequence value = row.getText();
            if (value == null || value.length() == 0) continue;
            float measured = row.getPaint().measureText(value, 0, value.length());
            // Leave a symmetric 32dp alignment gutter around the widest row.
            // This makes all rows, including the widest one, visibly respond to
            // left/center/right without moving the centered text column itself.
            measured += row.getPaddingLeft() + row.getPaddingRight() + dp(32);
            widest = Math.max(widest, measured);
        }
        return Math.max(dp(1), Math.min(Math.max(dp(1), maxWidth),
                (int) Math.ceil(widest)));
    }

    private void updateClockTextColumnWidth() {
        if (displayView != null) displayView.updateClockTextColumnWidth();
    }

    private void updatePlayerTextColumnWidth() {
        if (displayView != null) displayView.updatePlayerTextColumnWidth();
    }

    private void applySectionTextAlignmentPreferences() {
        if (displayView != null) displayView.applySectionTextAlignmentPreferences();
    }

    private void applySettings() {
        if (displayView != null) {
            displayView.applySettings(getWindow(),
                    mediaController != null && hasUsableMediaSession(mediaController),
                    currentAlbumArt);
        }
    }

    private int dateClockGapDp(int clockSize) {
        // Used only for the initial layout before the selected font has been measured.
        return -8;
    }

    private int visualGapDp(int clockSize) {
        return 20;
    }

    private int visualGapMarginPx(TextView upper, int upperHeight, String upperSample,
                                  TextView lower, int lowerHeight, String lowerSample,
                                  int desiredGapPx) {
        int upperBottomInset = glyphBottomInsetPx(upper, upperHeight, upperSample);
        int lowerTopInset = glyphTopInsetPx(lower, lowerHeight, lowerSample);
        return desiredGapPx - upperBottomInset - lowerTopInset;
    }

    private int glyphTopInsetPx(TextView view, int rowHeight, String fallbackText) {
        if (view == null) return 0;
        android.graphics.Paint paint = view.getPaint();
        android.graphics.Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int lineHeight = metrics.descent - metrics.ascent;
        float baseline = (rowHeight - lineHeight) / 2f - metrics.ascent;

        android.graphics.Rect bounds = new android.graphics.Rect();
        CharSequence current = view.getText();
        String text = current == null || current.length() == 0
                ? fallbackText : current.toString();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return Math.round(baseline + bounds.top);
    }

    private int glyphBottomInsetPx(TextView view, int rowHeight, String fallbackText) {
        if (view == null) return 0;
        android.graphics.Paint paint = view.getPaint();
        android.graphics.Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        int lineHeight = metrics.descent - metrics.ascent;
        float baseline = (rowHeight - lineHeight) / 2f - metrics.ascent;

        android.graphics.Rect bounds = new android.graphics.Rect();
        CharSequence current = view.getText();
        String text = current == null || current.length() == 0
                ? fallbackText : current.toString();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return Math.round(rowHeight - (baseline + bounds.bottom));
    }

    private float dateTextSizeForClockPreset(int clockSize) {
        // Date rows are always 26% of the effective clock size.
        return clockSize * 0.26f;
    }

    private int dateRowHeightForClockPreset(int clockSize) {
        // Let measured glyph metrics determine the line height; this is only a fallback minimum.
        return Math.max(dp(24), dp(Math.round(clockSize * 0.30f)));
    }

    private int effectiveFontWeightForFamily(String fontFamily, int requestedWeight) {
        // Haim FB Black is intrinsically heavier than the other faces; lower it by two
        // weight grades, clamping at the lightest available setting.
        if ("custom:fmhaimblack.ttf".equals(fontFamily)) {
            return Math.max(-1, requestedWeight - 2);
        }
        return requestedWeight;
    }

    private int effectiveClockDateSize(String fontFamily, int requestedSize) {
        // Normador and Stam Ashkenaz render materially smaller; promote them one preset
        // for every weight without modifying the user's stored size preference.
        if (!"custom:DL Normador.TTF".equals(fontFamily)
                && !"custom:Ashkenazi-Stam.ttf".equals(fontFamily)) {
            return requestedSize;
        }
        switch (requestedSize) {
            case 60: return 72;
            case 72: return 84;
            case 84: return 96;
            case 96: return 108;
            case 108: return 120;
            default: return 120;
        }
    }

    private void applyFontWeightToTextView(TextView view, int weight,
                                           String fontFamily, int requestedWeight) {
        if (view == null) return;
        Paint paint = view.getPaint();

        if ("custom:fmhaimblack.ttf".equals(fontFamily)) {
            // Haim FB Black is intrinsically very heavy. Use a lighter requested typeface
            // and erode the outer edge of the white glyphs against the black saver surface.
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0f);
            if (view instanceof ClockDateTextView) {
                float[] erosionByWeight = {2.0f, 1.6f, 1.15f, 0.7f};
                int index = Math.max(0, Math.min(3, requestedWeight + 1));
                ((ClockDateTextView) view).setEdgeErosionDp(erosionByWeight[index]);
            }
            view.invalidate();
            return;
        }

        if (view instanceof ClockDateTextView) {
            ((ClockDateTextView) view).setEdgeErosionDp(0f);
        }
        if (weight == 1 || weight == 2) {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            float density = getResources().getDisplayMetrics().density;
            boolean apolloBlack = "custom:DL ApolloBlack.ttf".equals(fontFamily);
            float strokeDp;
            if (apolloBlack) {
                // This face needs a wider visible difference between medium and bold.
                strokeDp = weight == 1 ? 0.75f : 1.30f;
            } else {
                strokeDp = weight == 1 ? 0.42f : 0.90f;
            }
            paint.setStrokeWidth(density * strokeDp);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0f);
        }
        view.invalidate();
    }

    private boolean isBatteryProtectionNeeded() {
        int threshold = Math.max(1, Math.min(50,
                getInt("batteryProtectionThreshold", 20)));
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return false;
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if (level < 0 || scale <= 0 || plugged != 0) return false;
            int percent = Math.max(0, Math.min(100,
                    Math.round(level * 100f / scale)));
            return percent <= threshold;
        } catch (Throwable ignored) {
            return false;
        }
    }

    
    private int getScreenBackgroundColor() {
        return getInt("screenBackgroundColor", Color.BLACK);
    }

    
    
    
    private int mixColor(int from, int to, float amount) {
        float f = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(from) * (1f - f) + Color.red(to) * f),
                Math.round(Color.green(from) * (1f - f) + Color.green(to) * f),
                Math.round(Color.blue(from) * (1f - f) + Color.blue(to) * f));
    }

    private Drawable createScreenBackgroundDrawable() {
        int base = getScreenBackgroundColor();
        String style = getString("backgroundStyle", "solid");
        if ("solid".equals(style)) {
            return new android.graphics.drawable.ColorDrawable(base);
        }
        if ("gradient".equals(style)) {
            return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{
                            mixColor(base, Color.rgb(47, 89, 132), 0.52f),
                            mixColor(base, Color.rgb(20, 36, 56), 0.30f),
                            mixColor(base, Color.rgb(5, 12, 22), 0.68f)
                    });
        }
        if ("glow".equals(style)) {
            GradientDrawable glow = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            mixColor(base, Color.rgb(52, 112, 169), 0.78f),
                            mixColor(base, Color.rgb(25, 56, 88), 0.44f),
                            base
                    });
            glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            glow.setGradientRadius(dp(Math.max(250, Math.round(Math.max(
                    getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels) * 0.78f))));
            glow.setGradientCenter(0.5f, 0.42f);
            return glow;
        }
        if ("vignette".equals(style)) {
            GradientDrawable vignette = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{
                            base,
                            mixColor(base, Color.BLACK, 0.62f),
                            Color.BLACK
                    });
            vignette.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            vignette.setGradientRadius(dp(Math.max(250, Math.round(Math.max(
                    getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels) * 0.66f))));
            vignette.setGradientCenter(0.5f, 0.45f);
            return vignette;
        }
        return new android.graphics.drawable.ColorDrawable(base);
    }

    private boolean isAutomaticNightModeActive() {
        if (!getBool("nightModeEnabled", false)) return false;
        int start = Math.max(0, Math.min(23, getInt("nightModeStartHour", 22)));
        int end = Math.max(0, Math.min(23, getInt("nightModeEndHour", 7)));
        if (start == end) return false;
        Calendar current = Calendar.getInstance();
        int hour = current.get(Calendar.HOUR_OF_DAY);
        if (start < end) return hour >= start && hour < end;
        return hour >= start || hour < end;
    }

    private void applyScreenAppearance() {
        if (displayView != null) displayView.applyScreenAppearance(getWindow());
    }

    
    private static final class CenterCropBitmapDrawable
            extends android.graphics.drawable.Drawable {
        private final android.graphics.Bitmap bitmap;
        private final android.graphics.BitmapShader shader;
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG
                        | android.graphics.Paint.FILTER_BITMAP_FLAG);
        private final android.graphics.Matrix matrix = new android.graphics.Matrix();

        CenterCropBitmapDrawable(android.graphics.Bitmap bitmap) {
            this.bitmap = bitmap;
            shader = new android.graphics.BitmapShader(bitmap,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(shader);
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect bounds) {
            if (bitmap == null || bounds.width() <= 0 || bounds.height() <= 0) return;
            float scale = Math.max(
                    bounds.width() / (float) bitmap.getWidth(),
                    bounds.height() / (float) bitmap.getHeight());
            float scaledWidth = bitmap.getWidth() * scale;
            float scaledHeight = bitmap.getHeight() * scale;
            float dx = bounds.left + (bounds.width() - scaledWidth) * 0.5f;
            float dy = bounds.top + (bounds.height() - scaledHeight) * 0.5f;
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            shader.setLocalMatrix(matrix);
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            if (bounds.width() > 0 && bounds.height() > 0) {
                canvas.drawRect(bounds, paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(Math.max(0, Math.min(255, alpha)));
            invalidateSelf();
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    private void applyMediaFrame(boolean hasActiveSong) {
        if (displayView != null) displayView.applyMediaFrame(hasActiveSong, currentAlbumArt);
    }

    private void scheduleAutoOff() {
        scheduleAutoOff(false);
    }

    private void scheduleAutoOff(boolean restartCountdown) {
        handler.removeCallbacks(autoOffRunnable);
        handler.removeCallbacks(screenOnReleaseRunnable);
        if (screenTimeoutObservationPending) {
            long currentTimeoutMs = getSystemScreenOffTimeoutMs();
            if (currentTimeoutMs > 0L
                    && currentTimeoutMs != screenTimeoutAtHoldReleaseMs) {
                screenTimeoutObservationPending = false;
                screenHoldReleasedAtElapsedRealtime = 0L;
                screenTimeoutAtHoldReleaseMs = -1L;
            }
        }
        if (settingsDialogOpen && !restartCountdown) {
            return;
        }
        Intent launchIntent = getIntent();
        if (launchIntent != null
                && launchIntent.getBooleanExtra(EXTRA_OPENED_BY_TRACK_CHANGE, false)) {
            int seconds = PlayerStartDuration.normalize(
                    getInt("playerStartDurationSeconds", 6));
            long durationMs = seconds * 1000L;
            if (restartCountdown || activityDeadlineElapsedRealtime <= 0L) {
                activityDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + durationMs;
            }
            long remaining = activityDeadlineElapsedRealtime - SystemClock.elapsedRealtime();
            if (remaining <= 0L) {
                exitApplicationAtSelectedTime();
            } else {
                handler.postDelayed(autoOffRunnable, remaining);
            }
            return;
        }
        String value = getString("screenSaverDuration", "device");
        boolean settingChanged = !value.equals(lastScheduledDurationValue);
        if (settingChanged) {
            lastScheduledDurationValue = value;
            restartCountdown = true;
        }

        // Layout changes, media refreshes and returning from system settings must not
        // restart an already running inactivity countdown.
        if (restartCountdown) {
            activityDeadlineElapsedRealtime = 0L;
            // Screen-off/on samples belong to one activity cycle only. A previous
            // cycle's power event must never qualify lock-screen continuation now.
            lastScreenOffElapsedRealtime = 0L;
            lastScreenOnElapsedRealtime = 0L;
        }

        // "No shutdown while charging" keeps the display on regardless of the
        // lock-screen preference. FLAG_ALLOW_LOCK_WHILE_SCREEN_ON separately
        // controls whether Android may activate the keyguard while the display
        // remains on and the screensaver stays visible above it.
        if (getBool("noShutdownWhileCharging", false) && isDeviceCharging()) {
            activityDeadlineElapsedRealtime = 0L;
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return;
        }

        if ("device".equals(value)) {
            activityDeadlineElapsedRealtime = 0L;
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return;
        }

        long minutes = 0L;
        try { minutes = Long.parseLong(value); } catch (Throwable ignored) {}

        if (minutes <= 0L) {
            activityDeadlineElapsedRealtime = 0L;
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return;
        }

        if (activityDeadlineElapsedRealtime <= 0L) {
            activityDeadlineElapsedRealtime = SystemClock.elapsedRealtime()
                    + minutes * 60_000L;
        }
        long remaining = activityDeadlineElapsedRealtime - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            exitApplicationAtSelectedTime();
            return;
        }

        // Select the hold policy from the behavior observed on this device model/OS.
        // The initial profile matches the already observed behavior: the system timeout
        // runs after KEEP_SCREEN_ON is released. Measurements update this profile if the
        // device instead continues counting inactivity during the hold.
        long systemTimeoutMs = getSystemScreenOffTimeoutMs();
        boolean adminActive = isScreenLockAdminActive();
        int behaviorProfile = getScreenTimeoutBehaviorProfile();
        boolean timeoutKnown = systemTimeoutMs > 0L;
        boolean timeoutAlreadyElapsed =
                behaviorProfile == SCREEN_TIMEOUT_PROFILE_ELAPSED_DURING_HOLD;
        boolean keepAwake = adminActive || !timeoutKnown
                || timeoutAlreadyElapsed || remaining > systemTimeoutMs;

        if (keepAwake) {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            releaseScreenOnHoldForObservation(systemTimeoutMs);
        }

        // For devices whose timeout starts after release, release exactly one system
        // timeout before the selected activity deadline. If the timeout has already
        // counted down while the hold was active, keep the display on until the deadline
        // and release it there instead.
        if (keepAwake && !adminActive && timeoutKnown && !timeoutAlreadyElapsed) {
            long releaseDelay = remaining - systemTimeoutMs;
            if (releaseDelay > 0L) {
                handler.postDelayed(screenOnReleaseRunnable, releaseDelay);
            } else if (remaining <= systemTimeoutMs) {
                releaseScreenOnHoldForObservation(systemTimeoutMs);
            }
        }
        handler.postDelayed(autoOffRunnable, remaining);
    }

    private long getSystemScreenOffTimeoutMs() {
        try {
            return Settings.System.getLong(
                    getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private void releaseScreenOnHoldForSystemTimeout() {
        if (isFinishing() || waitingForScreenWakeAfterActivityEnd
                || activityDeadlineElapsedRealtime <= 0L) return;

        String value = getString("screenSaverDuration", "device");
        if ("device".equals(value)) return;
        long minutes = 0L;
        try { minutes = Long.parseLong(value); } catch (Throwable ignored) {}
        if (minutes <= 0L || (getBool("noShutdownWhileCharging", false)
                && isDeviceCharging()) || isScreenLockAdminActive()) return;

        long remaining = activityDeadlineElapsedRealtime - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            exitApplicationAtSelectedTime();
            return;
        }

        long systemTimeoutMs = getSystemScreenOffTimeoutMs();
        if (systemTimeoutMs < 0L) {
            // If the current system timeout cannot be read, keep the screen on safely.
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return;
        }

        if (getScreenTimeoutBehaviorProfile()
                == SCREEN_TIMEOUT_PROFILE_ELAPSED_DURING_HOLD) {
            // This device keeps counting the timeout while the hold is active. Keep the
            // display awake until the selected activity deadline; release is handled there.
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return;
        }

        if (remaining > systemTimeoutMs) {
            // The timeout changed after this callback was scheduled. Keep the display
            // awake and recompute the release time from the unchanged activity deadline.
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            handler.postDelayed(screenOnReleaseRunnable, remaining - systemTimeoutMs);
        } else {
            releaseScreenOnHoldForObservation(systemTimeoutMs);
        }
    }

    private void exitApplicationAtSelectedTime() {
        if (isFinishing()) return;
        handler.removeCallbacks(autoOffRunnable);
        handler.removeCallbacks(screenOnReleaseRunnable);
        if (getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_OPENED_BY_TRACK_CHANGE, false)) {
            activityDeadlineElapsedRealtime = 0L;
            waitingForScreenWakeAfterActivityEnd = false;
            lockScreenContinuationArmed = false;
            finish();
            return;
        }

        long selectedDeadline = activityDeadlineElapsedRealtime;
        String durationValue = getString("screenSaverDuration", "device");
        long selectedDurationMs = 0L;
        try {
            selectedDurationMs = Math.max(0L, Long.parseLong(durationValue)) * 60_000L;
        } catch (Throwable ignored) {
        }
        long systemTimeoutMs = getSystemScreenOffTimeoutMs();
        boolean systemCanExpireBySelectedEnd = selectedDurationMs > 0L
                && systemTimeoutMs > 0L && systemTimeoutMs <= selectedDurationMs;
        boolean screenWasOffAtSelectedEnd = systemCanExpireBySelectedEnd
                && selectedDeadline > 0L
                && lastScreenOffElapsedRealtime > 0L
                && lastScreenOffElapsedRealtime <= selectedDeadline
                && (lastScreenOnElapsedRealtime == 0L
                    || lastScreenOnElapsedRealtime > selectedDeadline);
        boolean adminActive = isScreenLockAdminActive();
        boolean continueOnLockScreen = getBool("continueOnLockScreen", false)
                && selectedDurationMs > 0L
                && (screenWasOffAtSelectedEnd || adminActive);

        activityDeadlineElapsedRealtime = 0L;
        releaseScreenOnHoldForObservation(systemTimeoutMs);

        if (continueOnLockScreen) {
            // The activity continues only when Android had turned the display off
            // by the selected end, or device-admin can request the lock at that point.
            // A longer system timeout alone never extends a one/five-minute selection.
            waitingForScreenWakeAfterActivityEnd = true;
            lockScreenContinuationArmed = true;
            applyPreventLockWhileActivePreference();
            if (adminActive) {
                try {
                    if (devicePolicyManager == null) {
                        devicePolicyManager = (DevicePolicyManager) getSystemService(
                                Context.DEVICE_POLICY_SERVICE);
                    }
                    if (devicePolicyManager != null) devicePolicyManager.lockNow();
                } catch (Throwable ignored) {
                }
            }
            // Do not restart a fixed one/five-minute timer merely because the
            // device-admin request left the display interactive. The next real
            // screen wake is handled by resumeScreensaverAfterScreenWake().
            return;
        }

        waitingForScreenWakeAfterActivityEnd = false;
        lockScreenContinuationArmed = false;
        if (adminActive) {
            try {
                if (devicePolicyManager == null) {
                    devicePolicyManager = (DevicePolicyManager) getSystemService(
                            Context.DEVICE_POLICY_SERVICE);
                }
                if (devicePolicyManager != null) devicePolicyManager.lockNow();
            } catch (Throwable ignored) {
            }
        }
        finish();
    }

    private String screenTimeoutBehaviorPreferenceKey() {
        // Include the device software fingerprint, because OEM changes can alter
        // power-timeout behavior even when the model and Android API level are equal.
        String identity = (Build.MANUFACTURER + "|" + Build.BRAND + "|"
                + Build.MODEL + "|sdk=" + Build.VERSION.SDK_INT + "|"
                + Build.FINGERPRINT).toLowerCase(Locale.US);
        return "screenOffBehaviorProfileV1_"
                + Integer.toHexString(identity.hashCode());
    }

    private int getScreenTimeoutBehaviorProfile() {
        return getSharedPreferences("settings", MODE_PRIVATE).getInt(
                screenTimeoutBehaviorPreferenceKey(),
                SCREEN_TIMEOUT_PROFILE_RELEASE_RESTARTS);
    }

    private void releaseScreenOnHoldForObservation(long systemTimeoutMs) {
        int flags = getWindow().getAttributes().flags;
        boolean wasKeepingScreenOn =
                (flags & android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        if (wasKeepingScreenOn && !isScreenLockAdminActive()
                && systemTimeoutMs > 0L) {
            screenTimeoutObservationPending = true;
            screenHoldReleasedAtElapsedRealtime = SystemClock.elapsedRealtime();
            screenTimeoutAtHoldReleaseMs = systemTimeoutMs;
        }
        getWindow().clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void learnScreenTimeoutBehaviorFromScreenOff() {
        if (!screenTimeoutObservationPending || screenTimeoutAtHoldReleaseMs <= 0L) return;

        long elapsedSinceRelease = Math.max(
                0L, SystemClock.elapsedRealtime() - screenHoldReleasedAtElapsedRealtime);
        long measuredTimeout = screenTimeoutAtHoldReleaseMs;
        screenTimeoutObservationPending = false;
        screenHoldReleasedAtElapsedRealtime = 0L;
        screenTimeoutAtHoldReleaseMs = -1L;

        int observedProfile = 0;
        if (elapsedSinceRelease <= measuredTimeout * SCREEN_TIMEOUT_EARLY_OFF_THRESHOLD) {
            observedProfile = SCREEN_TIMEOUT_PROFILE_ELAPSED_DURING_HOLD;
        } else if (elapsedSinceRelease >= measuredTimeout * SCREEN_TIMEOUT_RESTART_THRESHOLD
                && elapsedSinceRelease <= measuredTimeout * 3L) {
            observedProfile = SCREEN_TIMEOUT_PROFILE_RELEASE_RESTARTS;
        }

        // Ignore ambiguous measurements (for example, a manual power-button press
        // shortly after release); retain the last confirmed behavior instead.
        if (observedProfile != 0) {
            getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putInt(screenTimeoutBehaviorPreferenceKey(), observedProfile)
                    .apply();
        }
    }

    private boolean isScreenLockAdminActive() {
        try {
            if (devicePolicyManager == null) {
                devicePolicyManager = (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
            }
            if (screenLockAdminComponent == null) {
                screenLockAdminComponent = new ComponentName(
                        this, ScreenLockAdminReceiver.class);
            }
            return devicePolicyManager != null
                    && devicePolicyManager.isAdminActive(screenLockAdminComponent);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isScreenInteractive() {
        try {
            android.os.PowerManager power = (android.os.PowerManager)
                    getSystemService(POWER_SERVICE);
            return power != null && power.isInteractive();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void resumeScreensaverAfterScreenWake() {
        if (!waitingForScreenWakeAfterActivityEnd || !isScreenInteractive()) return;
        waitingForScreenWakeAfterActivityEnd = false;
        lockScreenContinuationArmed = getBool("continueOnLockScreen", false);
        applyPreventLockWhileActivePreference();

        // If this device/OS did not deliver ACTION_SCREEN_OFF to this Activity,
        // discard the stale sample rather than measuring from the wrong release cycle.
        screenTimeoutObservationPending = false;
        screenHoldReleasedAtElapsedRealtime = 0L;
        screenTimeoutAtHoldReleaseMs = -1L;

        scheduleAutoOff(true);
    }

    private void handleScreenOffForLockScreenContinuation() {
        if ((!mainActivityResumed && !mainActivityPausedByDeviceLock)
                || settingsDialogOpen || isFinishing()) return;

        String duration = getString("screenSaverDuration", "device");
        boolean systemManagedDuration = "device".equals(duration) || "0".equals(duration);
        boolean continueOnLockScreen = getBool("continueOnLockScreen", false);

        if (continueOnLockScreen) {
            lockScreenContinuationArmed = true;
            applyPreventLockWhileActivePreference();
            if (systemManagedDuration) {
                // In device mode Android's own timeout ends the visible cycle.
                // The next wake starts a new cycle only when continuation is enabled.
                waitingForScreenWakeAfterActivityEnd = true;
            }
        } else if (systemManagedDuration) {
            handler.post(this::finish);
        }
    }

    

    private boolean isDeviceCharging() {
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return false;
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return plugged != 0
                    || status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void registerDeviceStateReceiver() {
        if (deviceEventsRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(deviceStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(deviceStateReceiver, filter);
            }
            deviceEventsRegistered = true;
        } catch (Throwable ignored) {
            deviceEventsRegistered = false;
        }
    }

    private void unregisterDeviceStateReceiver() {
        if (!deviceEventsRegistered) return;
        try { unregisterReceiver(deviceStateReceiver); } catch (Throwable ignored) {}
        deviceEventsRegistered = false;
    }

    private void registerScreenOffTimeoutObserver() {
        if (screenOffTimeoutObserverRegistered) return;
        if (screenOffTimeoutObserver == null) {
            screenOffTimeoutObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    // Re-evaluate the policy without restarting the activity deadline.
                    // Once the activity timer has ended, its next cycle starts on screen wake.
                    if (mainActivityResumed && !settingsDialogOpen
                            && !waitingForScreenWakeAfterActivityEnd) {
                        scheduleAutoOff(false);
                    }
                }
            };
        }
        try {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
                    false, screenOffTimeoutObserver);
            screenOffTimeoutObserverRegistered = true;
        } catch (Throwable ignored) {
            screenOffTimeoutObserverRegistered = false;
        }
    }

    private void unregisterScreenOffTimeoutObserver() {
        if (!screenOffTimeoutObserverRegistered || screenOffTimeoutObserver == null) return;
        try {
            getContentResolver().unregisterContentObserver(screenOffTimeoutObserver);
        } catch (Throwable ignored) {
        }
        screenOffTimeoutObserverRegistered = false;
    }

    private boolean isSettingsButtonAlwaysVisible() {
        return getBool("settingsButtonAlwaysVisible", false);
    }

    private void refreshSettingsButtonVisibility() {
        if (settingsButton == null || settingsButtonHitArea == null) return;
        handler.removeCallbacks(hideSettingsButtonRunnable);
        if (settingsDialogOpen) return;

        if (isSettingsButtonAlwaysVisible()) {
            settingsButtonHitArea.setVisibility(View.VISIBLE);
            settingsButton.setAlpha(1f);
        } else {
            settingsButtonHitArea.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isPlayerInteractionTouch(android.view.MotionEvent event) {
        return isTouchInsideVisibleView(event, mediaProgressRow)
                || isTouchInsideVisibleView(event, mediaControlsHolder);
    }

    private boolean isTouchInsideVisibleView(
            android.view.MotionEvent event, View target) {
        if (target == null || target.getVisibility() != View.VISIBLE) return false;
        android.graphics.Rect visibleBounds = new android.graphics.Rect();
        return target.getGlobalVisibleRect(visibleBounds)
                && visibleBounds.contains(
                        Math.round(event.getRawX()), Math.round(event.getRawY()));
    }

    private void showSettingsButtonForThreeSeconds() {
        if (settingsButton == null || settingsButtonHitArea == null || settingsDialogOpen) return;
        handler.removeCallbacks(hideSettingsButtonRunnable);
        settingsButtonHitArea.setVisibility(View.VISIBLE);
        settingsButton.setAlpha(1f);
        if (isSettingsButtonAlwaysVisible()) return;
        handler.postDelayed(hideSettingsButtonRunnable, 3_000L);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (bottomSwipeDismissAnimating) return true;

        int action = event.getActionMasked();
        boolean exitBySwipe = false;
        boolean consumeForSwipe = bottomSwipeDragging;

        if (action == android.view.MotionEvent.ACTION_DOWN) {
            bottomSwipeCandidate = false;
            bottomSwipeDragging = false;
            if (stageView != null) {
                stageView.animate().cancel();
                stageView.setTranslationY(0f);
            }

            // Accept an upward-dismiss gesture from anywhere in the lower third.
            if (!settingsDialogOpen) {
                int decorHeight = getWindow().getDecorView().getHeight();
                if (decorHeight > 0 && event.getY() >= decorHeight * (2f / 3f)) {
                    bottomSwipeCandidate = true;
                    bottomSwipeStartX = event.getX();
                    bottomSwipeStartY = event.getY();
                }
            }

            if (!settingsDialogOpen && !isPlayerInteractionTouch(event)) {
                showSettingsButtonForThreeSeconds();
            }
        } else if (action == android.view.MotionEvent.ACTION_MOVE
                && bottomSwipeCandidate) {
            float deltaX = Math.abs(event.getX() - bottomSwipeStartX);
            float downDistance = event.getY() - bottomSwipeStartY;

            if (!bottomSwipeDragging) {
                if (downDistance > dp(24) || deltaX > dp(96)) {
                    bottomSwipeCandidate = false;
                } else if (downDistance <= -dp(12)
                        && -downDistance > deltaX * 1.2f) {
                    bottomSwipeDragging = true;
                    consumeForSwipe = true;

                    // Cancel the child control's touch gesture once an upward drag is
                    // recognized, so the drag moves the screensaver rather than a control.
                    android.view.MotionEvent cancel =
                            android.view.MotionEvent.obtain(event);
                    cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
            }

            if (bottomSwipeDragging && stageView != null) {
                stageView.setTranslationY(Math.min(0f, downDistance));
                consumeForSwipe = true;
            }
        } else if (action == android.view.MotionEvent.ACTION_UP) {
            if (bottomSwipeDragging) {
                float upwardDistance = bottomSwipeStartY - event.getY();
                float horizontalDistance = Math.abs(event.getX() - bottomSwipeStartX);
                int decorHeight = getWindow().getDecorView().getHeight();
                float gestureHeight = decorHeight > 0
                        ? decorHeight : (stageView == null ? dp(600) : stageView.getHeight());
                boolean startedInLowerThird =
                        bottomSwipeStartY >= gestureHeight * (2f / 3f);
                boolean reachedUpperThird = event.getY() <= gestureHeight / 3f;
                float dismissThreshold = gestureHeight / 3f;
                exitBySwipe = startedInLowerThird && reachedUpperThird
                        && upwardDistance >= dismissThreshold
                        && upwardDistance > horizontalDistance * 1.2f;
                consumeForSwipe = true;
                if (!exitBySwipe && stageView != null) {
                    stageView.animate().cancel();
                    stageView.animate().translationY(0f)
                            .setDuration(160L).start();
                }
            } else if (bottomSwipeCandidate) {
                // Fallback for a short stream in which Android delivers no MOVE events.
                float upwardDistance = bottomSwipeStartY - event.getY();
                float horizontalDistance = Math.abs(event.getX() - bottomSwipeStartX);
                int decorHeight = getWindow().getDecorView().getHeight();
                float gestureHeight = decorHeight > 0
                        ? decorHeight : (stageView == null ? dp(600) : stageView.getHeight());
                exitBySwipe = bottomSwipeStartY >= gestureHeight * (2f / 3f)
                        && event.getY() <= gestureHeight / 3f
                        && upwardDistance >= gestureHeight / 3f
                        && upwardDistance > horizontalDistance * 1.2f;
                consumeForSwipe = exitBySwipe;
            }
            bottomSwipeCandidate = false;
            bottomSwipeDragging = false;
        } else if (action == android.view.MotionEvent.ACTION_CANCEL) {
            if (bottomSwipeDragging && stageView != null) {
                stageView.animate().cancel();
                stageView.animate().translationY(0f).setDuration(160L).start();
            }
            consumeForSwipe = bottomSwipeDragging;
            bottomSwipeCandidate = false;
            bottomSwipeDragging = false;
        }

        boolean handled = consumeForSwipe ? true : super.dispatchTouchEvent(event);
        if (exitBySwipe && !settingsDialogOpen) {
            dismissScreensaverWithUpwardSlide();
            return true;
        }
        return handled;
    }

    private void dismissScreensaverWithUpwardSlide() {
        if (bottomSwipeDismissAnimating || settingsDialogOpen) return;
        bottomSwipeDismissAnimating = true;
        handler.removeCallbacks(autoOffRunnable);
        handler.removeCallbacks(screenOnReleaseRunnable);

        if (stageView == null || stageView.getHeight() <= 0) {
            finish();
            return;
        }

        float exitY = -Math.max(stageView.getHeight(), getWindow().getDecorView().getHeight());
        stageView.animate().cancel();
        stageView.animate()
                .translationY(exitY)
                .setDuration(190L)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(this::finish)
                .start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (event == null || event.getRepeatCount() == 0) {
                showSettings();
            }
            return true;
        }
        // Other physical keys do not reveal the hidden settings icon.
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) return true;
        return super.onKeyUp(keyCode, event);
    }

    private void showSettings() {
        if (settingsDialogOpen) return;
        if (prefs == null) initSettings();

        settingsDialogOpen = true;
        settingsOpenedElapsedRealtime = SystemClock.elapsedRealtime();
        handler.removeCallbacks(autoOffRunnable);
        try {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(0, 0);
        } catch (Throwable t) {
            settingsDialogOpen = false;
            settingsOpenedElapsedRealtime = 0L;
            scheduleAutoOff(false);
            AppToast.makeText(
                    this, getString(R.string.main_activity_text_010), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    
    private void updateChargingBatteryDisplay() {
        if (displayView != null) displayView.updateChargingBatteryDisplay();
    }

    private void updateClockBlockHeightForChargingIndicator() {
        if (displayView != null) displayView.updateClockBlockHeightForChargingIndicator();
    }

    private void updateClock() {
        if (displayView != null) displayView.updateClock();
    }

    private String formattedGregorianDateValue() {
        String pattern = getString("gregorianDateFormat", "dd.MM.yyyy");
        if ("dd.MM.yyyy".equals(pattern)) {
            numericDateFormat.setTimeZone(now.getTimeZone());
            return numericDateFormat.format(now.getTime());
        }
        Locale formatLocale = getString(R.string.main_activity_text_011).equals(pattern)
                ? new Locale("he", "IL") : Locale.US;
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, formatLocale);
        formatter.setTimeZone(now.getTimeZone());
        return formatter.format(now.getTime());
    }

    private String formatHebrewDate(String rawDate, String format) {
        String normalized = rawDate
                .replace(getString(R.string.main_activity_text_012), getString(R.string.main_activity_text_013))
                .replace(getString(R.string.main_activity_text_014), getString(R.string.main_activity_text_013))
                .replace(getString(R.string.main_activity_text_015), getString(R.string.main_activity_text_013))
                .replace("\u05F3", "'")
                .replace("\u05F4", "\"")
                .replace("’", "'")
                .replace("”", "\"")
                .replace("“", "\"");
        int firstSpace = normalized.indexOf(' ');
        int lastSpace = normalized.lastIndexOf(' ');
        if (firstSpace <= 0 || lastSpace <= firstSpace) return normalized;
        String day = normalized.substring(0, firstSpace);
        String month = normalized.substring(firstSpace + 1, lastSpace);
        String year = normalized.substring(lastSpace + 1);
        if ("monthBet".equals(format) || "monthBetYearArticle".equals(format)) {
            month = getString(R.string.main_activity_text_016) + month;
        }
        if ("yearArticle".equals(format) || "monthBetYearArticle".equals(format)) {
            year = getString(R.string.main_activity_text_017) + year;
        }
        return day + " " + month + " " + year;
    }

    private String numericTime() {
        boolean use24Hour = "24".equals(getString("timeFormat", "24"));
        int hourOfDay = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        if (use24Hour) {
            return String.format(Locale.US, "%d:%02d", hourOfDay, minute);
        }
        int hour = hourOfDay % 12;
        if (hour == 0) hour = 12;
        String period = hourOfDay < 12 ? getString(R.string.main_activity_text_018) : getString(R.string.main_activity_text_019);
        return String.format(Locale.US, "%d:%02d", hour, minute) + period;
    }

    private String hebrewWeekday() {
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY: return getString(R.string.main_activity_text_020);
            case Calendar.MONDAY: return getString(R.string.main_activity_text_021);
            case Calendar.TUESDAY: return getString(R.string.main_activity_text_022);
            case Calendar.WEDNESDAY: return getString(R.string.main_activity_text_023);
            case Calendar.THURSDAY: return getString(R.string.main_activity_text_024);
            case Calendar.FRIDAY: return getString(R.string.main_activity_text_025);
            default: return getString(R.string.main_activity_text_026);
        }
    }

    private void registerActiveSessionsListener() {
        if (Build.VERSION.SDK_INT < 21) return;
        try {
            if (mediaSessionManager == null) {
                mediaSessionManager = (MediaSessionManager)
                        getSystemService(MEDIA_SESSION_SERVICE);
            }
            if (mediaSessionManager == null || activeSessionsListenerRegistered) return;

            final ComponentName listenerComponent = new ComponentName(
                    this, SystemMediaNotificationListener.class);
            if (!getBool("showPlayer", false)) return;
            activeSessionsListener = controllers -> {
                // A media session can be created after the screen saver starts.
                // Only inspect sessions while the player setting is enabled.
                if (mainActivityResumed && getBool("showPlayer", false)) {
                    handler.removeCallbacks(mediaSessionRefreshRunnable);
                    handler.post(mediaSessionRefreshRunnable);
                }
            };
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    activeSessionsListener,
                    listenerComponent,
                    handler);
            activeSessionsListenerRegistered = true;
        } catch (SecurityException ignored) {
            activeSessionsListener = null;
            activeSessionsListenerRegistered = false;
        } catch (Throwable ignored) {
            activeSessionsListener = null;
            activeSessionsListenerRegistered = false;
        }
    }

    private void unregisterActiveSessionsListener() {
        if (mediaSessionManager == null || activeSessionsListener == null || !activeSessionsListenerRegistered) {
            activeSessionsListener = null;
            activeSessionsListenerRegistered = false;
            return;
        }
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener);
        } catch (Throwable ignored) {
        }
        activeSessionsListener = null;
        activeSessionsListenerRegistered = false;
    }

    private void refreshSystemMediaMirror() {
        if (mediaMirror == null) return;
        if (!mainActivityResumed || !getBool("showPlayer", false)) {
            stopMediaProgressTicker();
            detachMediaController();
            hideMediaMirror();
            return;
        }

        try {
            if (!hasNotificationAccess()) {
                detachMediaController();
                hideMediaMirror();
                showMediaSetup();
                return;
            }
            hideMediaSetup();

            if (mediaSessionManager == null) {
                mediaSessionManager = (MediaSessionManager)
                        getSystemService(MEDIA_SESSION_SERVICE);
            }

            if (mediaSessionManager == null) {
                hideMediaMirror();
                return;
            }

            ComponentName listener = new ComponentName(
                    this, SystemMediaNotificationListener.class);

            // Prefer the media-key session only when it is a real media session.
            // Some Android versions keep a stale system session here with no title;
            // in that case we must fall back to the complete active-session list.
            MediaController selected = null;
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    MediaSession.Token token = mediaSessionManager.getMediaKeyEventSession();
                    if (token != null) {
                        MediaController candidate = new MediaController(this, token);
                        if (hasUsableMediaSession(candidate)) {
                            selected = candidate;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            // Always inspect active sessions when the preferred session is stale,
            // so playback started after app launch is detected on the next refresh.
            if (selected == null) {
                List<MediaController> sessions =
                        mediaSessionManager.getActiveSessions(listener);
                selected = selectBestSession(sessions);
            }

            if (selected == null) {
                hideMediaMirror();
                return;
            }

            lastKnownMediaController = selected;
            if (mediaController == null ||
                    !mediaController.getSessionToken().equals(selected.getSessionToken())) {
                attachMediaController(selected);
            }

            renderMediaMirror();
        } catch (SecurityException ignored) {
            detachMediaController();
            hideMediaMirror();
            showMediaSetup();
        } catch (Throwable ignored) {
            hideMediaMirror();
        }
    }

    private boolean hasUsableMediaSession(MediaController controller) {
        if (controller == null) return false;
        try {
            PlaybackState state = controller.getPlaybackState();
            MediaMetadata metadata = controller.getMetadata();
            if (state != null) {
                int s = state.getState();
                if (s == PlaybackState.STATE_PLAYING ||
                        s == PlaybackState.STATE_BUFFERING ||
                        s == PlaybackState.STATE_PAUSED) {
                    return true;
                }
            }
            if (metadata != null) {
                String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                if (title == null || title.trim().isEmpty()) {
                    title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                }
                return title != null && !title.trim().isEmpty();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private MediaController selectBestSession(List<MediaController> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;

        // Prefer playing/buffering, then paused, preserving Android's priority order.
        for (MediaController controller : sessions) {
            PlaybackState state = controller.getPlaybackState();
            if (state == null) continue;
            int s = state.getState();
            if (s == PlaybackState.STATE_PLAYING ||
                    s == PlaybackState.STATE_BUFFERING) {
                return controller;
            }
        }
        for (MediaController controller : sessions) {
            PlaybackState state = controller.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PAUSED) {
                return controller;
            }
        }

        // Some players create their MediaSession before playback starts.
        // Keep the session if it already exposes a real title, so the mirror
        // can appear when playback begins after the app was opened.
        for (MediaController controller : sessions) {
            try {
                MediaMetadata metadata = controller.getMetadata();
                String title = metadata == null ? null :
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                if (title == null || title.trim().isEmpty()) {
                    title = metadata == null ? null :
                            metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
                }
                if (title != null && !title.trim().isEmpty()) return controller;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private void attachMediaController(MediaController controller) {
        detachMediaController();
        mediaController = controller;
        mediaCallback = new MediaController.Callback() {
            @Override public void onPlaybackStateChanged(PlaybackState state) {
                runOnUiThread(() -> renderMediaMirror());
            }

            @Override public void onMetadataChanged(MediaMetadata metadata) {
                runOnUiThread(() -> renderMediaMirror());
            }

            @Override public void onSessionDestroyed() {
                runOnUiThread(() -> {
                    lastKnownMediaController = mediaController;
                    detachMediaController();
                    hideMediaMirror();
                });
            }
        };

        try {
            mediaController.registerCallback(mediaCallback);
        } catch (Throwable ignored) {
        }
    }

    private void detachMediaController() {
        stopMediaProgressTicker();
        currentMediaDurationMs = 0L;
        if (mediaController != null && mediaCallback != null) {
            try {
                mediaController.unregisterCallback(mediaCallback);
            } catch (Throwable ignored) {
            }
        }
        mediaController = null;
        mediaCallback = null;
    }

    private void renderMediaMirror() {
        if (mediaMirror == null || mediaController == null) return;

        try {
            PlaybackState state = mediaController.getPlaybackState();
            MediaMetadata metadata = mediaController.getMetadata();
            if (state == null) {
                lastPlaybackWasActive = false;
                hideMediaMirror();
                return;
            }

            int playback = state.getState();
            if (playback != PlaybackState.STATE_PLAYING &&
                    playback != PlaybackState.STATE_BUFFERING &&
                    playback != PlaybackState.STATE_PAUSED) {
                lastPlaybackWasActive = false;
                hideMediaMirror();
                return;
            }

            String title = metadata == null ? null :
                    metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            if (title == null || title.trim().isEmpty()) {
                title = metadata == null ? null :
                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            }
            if (title == null || title.trim().isEmpty()) {
                title = getString(R.string.main_activity_text_027);
            }

            title = removeFileExtension(title);
            String previousTitle = mediaTitle.getText() == null
                    ? "" : mediaTitle.getText().toString();
            boolean titleChanged = !title.equals(previousTitle);
            if (titleChanged) {
                stopTitleMarquee();
                mediaTitle.setText(title);
            }
            mediaTitle.setVisibility(View.VISIBLE);
            lastTitle = title;
            updateAlbumArtwork(metadata);
            String artist = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            artist = artist == null ? "" : artist.trim();
            if (artist.equalsIgnoreCase("unknown artist") ||
                    artist.equalsIgnoreCase("unknown") ||
                    artist.equalsIgnoreCase("artist unknown") ||
                    artist.equals(getString(R.string.main_activity_text_028)) ||
                    artist.equals(getString(R.string.main_activity_text_029)) ||
                    artist.equals(getString(R.string.main_activity_text_030))) {
                artist = "";
            }
            String previousArtistText = mediaDetails.getText() == null
                    ? "" : mediaDetails.getText().toString();
            boolean artistChanged = !artist.equals(previousArtistText);
            lastArtist = artist;
            boolean savedTrackMetadataChanged = false;
            SharedPreferences.Editor metadataEditor = prefs.edit();
            if (!title.equals(getString("lastTitle", ""))) {
                metadataEditor.putString("lastTitle", title);
                savedTrackMetadataChanged = true;
            }
            if (!artist.equals(getString("lastArtist", ""))) {
                metadataEditor.putString("lastArtist", artist);
                savedTrackMetadataChanged = true;
            }
            if (savedTrackMetadataChanged) metadataEditor.apply();

            long duration = metadata == null ? 0L : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            currentMediaDurationMs = Math.max(0L, duration);
            long position = Math.max(0L, state.getPosition());
            if (duration > 0) position = Math.min(position, duration);

            String mediaId = metadata == null ? null
                    : metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            String trackIdentity = mediaId != null && !mediaId.trim().isEmpty()
                    ? "id:" + mediaId.trim()
                    : (!getString(R.string.main_activity_text_027).equals(title) && !title.trim().isEmpty()
                            ? "title:" + title.trim() : "");
            boolean playbackActive = playback == PlaybackState.STATE_PLAYING
                    || playback == PlaybackState.STATE_BUFFERING;
            if (!trackIdentity.isEmpty()) {
                observeTrackForPlayerIntro(trackIdentity, playbackActive, position);
            } else {
                lastPlaybackWasActive = playbackActive;
            }

            boolean showDetails = getBool("showDetails", true);
            if (artistChanged) {
                stopArtistMarquee();
                mediaDetails.setText(lastArtist);
            }
            mediaDetails.setVisibility(showDetails && !lastArtist.isEmpty() ? View.VISIBLE : View.GONE);
            updatePlayerTextColumnWidth();
            if (titleChanged || artistChanged) {
                playerTextColumn.post(() -> {
                    updateTitleMarqueeIfNeeded();
                    updateArtistMarqueeIfNeeded();
                });
            }
            boolean controlsVisible = arePlayerControlsVisible();
            if (!controlsVisible) {
                // With the progress row disabled, no periodic progress work is needed.
                stopMediaProgressTicker();
                handler.removeCallbacks(hideProgressRowAfterMetadataChange);
                progressHidePending = false;
                setProgressRowVisible(false);
            } else if (duration > 0L) {
                handler.removeCallbacks(hideProgressRowAfterMetadataChange);
                progressHidePending = false;
                updateMediaProgressViews(state, duration);
            } else if (mediaProgressRow.getVisibility() == View.VISIBLE) {
                // Track changes can briefly publish metadata without a duration. Keep the
                // last progress row visible instead of blinking it off during the hand-off.
                if (!progressHidePending) {
                    progressHidePending = true;
                    handler.postDelayed(hideProgressRowAfterMetadataChange, 1800L);
                }
            } else {
                setProgressRowVisible(false);
            }
            controlsVisible = arePlayerControlsVisible();
            if (mediaControlsHolder != null) {
                mediaControlsHolder.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
            }
            mediaPlayPause.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
                boolean isPlaying = playback == PlaybackState.STATE_PLAYING;
            mediaPlayPause.setImageResource(
                    isPlaying ? R.drawable.media_pause : R.drawable.media_play);
            mediaPlayPause.setContentDescription(isPlaying ? getString(R.string.main_activity_text_031) : getString(R.string.main_activity_text_006));
            int mediaColor = getInt("fontColor", dateTextColor);
            if (mediaPlayPause.getDrawable() != null) mediaPlayPause.getDrawable().setTint(mediaColor);

            long actions = state.getActions();

            if (!controlsVisible) {
                handler.removeCallbacks(hidePreviousButtonIfStillUnsupported);
                handler.removeCallbacks(hideNextButtonIfStillUnsupported);
                previousButtonHidePending = false;
                nextButtonHidePending = false;
                mediaPrev.setVisibility(View.GONE);
                mediaNext.setVisibility(View.GONE);
            } else {
                if ((actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L) {
                    handler.removeCallbacks(hidePreviousButtonIfStillUnsupported);
                    previousButtonHidePending = false;
                    mediaPrev.setVisibility(View.VISIBLE);
                } else if (mediaPrev.getVisibility() == View.VISIBLE && !previousButtonHidePending) {
                    // Some players briefly clear action flags while changing tracks.
                    previousButtonHidePending = true;
                    handler.postDelayed(hidePreviousButtonIfStillUnsupported, 1500L);
                }

                if ((actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0L) {
                    handler.removeCallbacks(hideNextButtonIfStillUnsupported);
                    nextButtonHidePending = false;
                    mediaNext.setVisibility(View.VISIBLE);
                } else if (mediaNext.getVisibility() == View.VISIBLE && !nextButtonHidePending) {
                    // Avoid a one-frame disappearance during a track hand-off.
                    nextButtonHidePending = true;
                    handler.postDelayed(hideNextButtonIfStillUnsupported, 1500L);
                }
            }

            applyMediaFrame(true);
            updatePlayerMirrorVisibility();
            mediaMirror.requestLayout();
            refreshMediaHolderPosition();
        } catch (Throwable ignored) {
            hideMediaMirror();
        }
    }

    
    private void refreshPlayerControlsAfterIntro() {
        if (mediaController != null) renderMediaMirror();
    }

    private boolean arePlayerControlsVisible() {
        String mode = getString("playerControlsDisplayMode", "on");
        if ("off".equals(mode)) return false;
        if (!"start".equals(mode)) return true;
        return playerControlsIntroWindowVisible
                && SystemClock.elapsedRealtime()
                < playerControlsIntroWindowDeadlineElapsedRealtime;
    }

    private void startPlayerControlsIntroWindow(long durationMs) {
        if (!"start".equals(getString("playerControlsDisplayMode", "on"))) return;
        long delay = Math.max(1L, durationMs);
        playerControlsIntroWindowVisible = true;
        playerControlsIntroWindowDeadlineElapsedRealtime =
                SystemClock.elapsedRealtime() + delay;
        handler.removeCallbacks(hidePlayerControlsIntroWindowRunnable);
        handler.postDelayed(hidePlayerControlsIntroWindowRunnable, delay);
    }

    

    private long getPlayerStartWindowMs() {
        return PlayerStartDuration.millis(getInt("playerStartDurationSeconds", 6));
    }

    private void observeTrackForPlayerIntro(String trackIdentity,
                                            boolean playbackActive,
                                            long positionMs) {
        long playerStartWindowMs = getPlayerStartWindowMs();
        boolean firstObservedTrack = lastObservedTrackIdentity == null;
        boolean trackChanged = !firstObservedTrack
                && !trackIdentity.equals(lastObservedTrackIdentity);
        boolean repeatedOrRestartedTrack = !firstObservedTrack && !trackChanged
                && lastObservedTrackPositionMs > playerStartWindowMs
                && positionMs < playerStartWindowMs
                && lastObservedTrackPositionMs - positionMs > 5_000L;
        boolean playbackJustStarted = playbackActive && !lastPlaybackWasActive;
        boolean introOnly = getBool("showPlayerOnlyAtStart", false);
        boolean introOnlyJustEnabled = introOnly && !lastPlayerIntroOnlySetting;
        String controlsMode = getString("playerControlsDisplayMode", "on");
        boolean controlsModeJustEnabled = "start".equals(controlsMode)
                && !"start".equals(lastPlayerControlsDisplayMode);

        if (!introOnly) {
            handler.removeCallbacks(hidePlayerIntroWindowRunnable);
            playerIntroWindowVisible = false;
            playerIntroWindowDeadlineElapsedRealtime = 0L;
        } else if (playbackActive && (trackChanged || repeatedOrRestartedTrack)) {
            // This includes manual/automatic track changes and repeat/restart of the same track.
            startPlayerIntroWindow(playerStartWindowMs);
        } else if (playbackActive
                && (firstObservedTrack || playbackJustStarted || introOnlyJustEnabled)
                && positionMs < playerStartWindowMs) {
            // On first observation, playback start, or enabling this option during a song's
            // first six seconds, show only the portion of that six-second window remaining.
            startPlayerIntroWindow(playerStartWindowMs - Math.max(0L, positionMs));
        }

        if (!"start".equals(controlsMode)) {
            handler.removeCallbacks(hidePlayerControlsIntroWindowRunnable);
            playerControlsIntroWindowVisible = !"off".equals(controlsMode);
            playerControlsIntroWindowDeadlineElapsedRealtime = 0L;
        } else if (playbackActive
                && (trackChanged || repeatedOrRestartedTrack
                || firstObservedTrack || playbackJustStarted || controlsModeJustEnabled)) {
            if (positionMs < playerStartWindowMs) {
                startPlayerControlsIntroWindow(
                        playerStartWindowMs - Math.max(0L, positionMs));
            } else {
                handler.removeCallbacks(hidePlayerControlsIntroWindowRunnable);
                playerControlsIntroWindowVisible = false;
                playerControlsIntroWindowDeadlineElapsedRealtime = 0L;
            }
        }

        lastObservedTrackIdentity = trackIdentity;
        lastObservedTrackPositionMs = positionMs;
        lastPlaybackWasActive = playbackActive;
        lastPlayerIntroOnlySetting = introOnly;
        lastPlayerControlsDisplayMode = controlsMode;
    }

    private void startPlayerIntroWindow(long durationMs) {
        if (!getBool("showPlayerOnlyAtStart", false)) return;
        long delay = Math.max(1L, durationMs);
        playerIntroWindowVisible = true;
        playerIntroWindowDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + delay;
        handler.removeCallbacks(hidePlayerIntroWindowRunnable);
        handler.postDelayed(hidePlayerIntroWindowRunnable, delay);
    }

    private void updatePlayerMirrorVisibility() {
        if (mediaMirror == null) return;
        boolean enabled = getBool("showPlayer", false);
        boolean introOnly = getBool("showPlayerOnlyAtStart", false);
        boolean introWindowOpen = playerIntroWindowVisible
                && SystemClock.elapsedRealtime() < playerIntroWindowDeadlineElapsedRealtime;
        if (introOnly && !introWindowOpen) enabled = false;

        // Apply the final frame and its inner padding before showing the player.
        // This prevents the unframed player from flashing while media callbacks settle.
        boolean hasActiveSong = mediaController != null
                && hasUsableMediaSession(mediaController);
        applyMediaFrame(enabled && hasActiveSong);
        mediaMirror.requestLayout();
        mediaMirror.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            stopTitleMarquee();
            stopArtistMarquee();
            stopMediaProgressTicker();
        } else if (arePlayerControlsVisible()) {
            scheduleMediaProgressTicker();
        } else {
            stopMediaProgressTicker();
        }
    }

    private boolean isMediaProgressPollingEnabled() {
        if (!mainActivityResumed || !getBool("showPlayer", false)
                || !arePlayerControlsVisible()
                || mediaMirror == null || mediaMirror.getVisibility() != View.VISIBLE
                || mediaController == null) return false;
        try {
            PlaybackState state = mediaController.getPlaybackState();
            return state != null && state.getState() == PlaybackState.STATE_PLAYING
                    && currentMediaDurationMs > 0L;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void scheduleMediaProgressTicker() {
        handler.removeCallbacks(mediaProgressTicker);
        if (isMediaProgressPollingEnabled()) {
            handler.postDelayed(mediaProgressTicker, 1000L);
        }
    }

    private void stopMediaProgressTicker() {
        handler.removeCallbacks(mediaProgressTicker);
    }

    private boolean updateCurrentMediaProgress() {
        if (!isMediaProgressPollingEnabled()) return false;
        try {
            PlaybackState state = mediaController.getPlaybackState();
            if (state == null || state.getState() != PlaybackState.STATE_PLAYING
                    || currentMediaDurationMs <= 0L) return false;
            updateMediaProgressViews(state, currentMediaDurationMs);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateMediaProgressViews(PlaybackState state, long duration) {
        if (state == null || mediaCurrentText == null
                || mediaTotalText == null || mediaSeekBar == null) return;
        duration = Math.max(0L, duration);
        if (duration <= 0L || !arePlayerControlsVisible()) {
            setProgressRowVisible(false);
            return;
        }

        long position = Math.max(0L, state.getPosition());
        if (state.getState() == PlaybackState.STATE_PLAYING) {
            long lastUpdate = state.getLastPositionUpdateTime();
            if (lastUpdate > 0L) {
                long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - lastUpdate);
                position += Math.round(elapsed * Math.max(0f, state.getPlaybackSpeed()));
            }
        }
        position = Math.min(position, duration);
        setProgressRowVisible(true);
        String currentText = formatMs(position);
        String totalText = formatMs(duration);
        if (!currentText.contentEquals(mediaCurrentText.getText())) {
            mediaCurrentText.setText(currentText);
        }
        if (!totalText.contentEquals(mediaTotalText.getText())) {
            mediaTotalText.setText(totalText);
        }
        if (mediaSeekBar.getMax() != 1000) mediaSeekBar.setMax(1000);
        int progress = (int) Math.min(1000L, (position * 1000L) / duration);
        if (mediaSeekBar.getProgress() != progress) {
            mediaSeekBar.setProgress(progress);
        }
    }

    private void refreshMediaHolderPosition() {
        if (stageView != null) {
            stageView.post(() -> updateScreenPositions(
                    stageView, clockBlockView, mediaHolderView));
        }
    }

    private long getCurrentMediaDuration() {
        try {
            MediaMetadata metadata = mediaController == null ? null : mediaController.getMetadata();
            return metadata == null ? 0L
                    : Math.max(0L, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private void setProgressRowVisible(boolean visible) {
        if (displayView != null) displayView.setProgressRowVisible(visible);
    }

    private boolean hasNotificationAccess() {
        ComponentName component = new ComponentName(
                this, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                return manager != null &&
                        manager.isNotificationListenerAccessGranted(component);
            }
        } catch (Throwable ignored) {
        }

        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(), "enabled_notification_listeners");
            return enabled != null && enabled.contains(component.flattenToString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showMediaSetup() {
        // Do not show permission prompts on the saver screen.
        hideMediaSetup();
    }

    private void hideMediaSetup() {
    }

    
    private void updateAlbumArtwork(MediaMetadata metadata) {
        Bitmap artwork = null;
        if (metadata != null) {
            try { artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART); }
            catch (Throwable ignored) {}
            if (artwork == null) {
                try { artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART); }
                catch (Throwable ignored) {}
            }
            if (artwork == null) {
                try { artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON); }
                catch (Throwable ignored) {}
            }
        }
        currentAlbumArt = artwork != null && !artwork.isRecycled() ? artwork : null;
        if (mediaMirror != null) {
            boolean active = mediaController != null && hasUsableMediaSession(mediaController);
            applyMediaFrame(active);
        }
    }

    private void hideMediaMirror() {
        stopMediaProgressTicker();
        handler.removeCallbacks(hidePlayerIntroWindowRunnable);
        handler.removeCallbacks(hidePlayerControlsIntroWindowRunnable);
        playerIntroWindowVisible = false;
        playerIntroWindowDeadlineElapsedRealtime = 0L;
        playerControlsIntroWindowVisible = false;
        playerControlsIntroWindowDeadlineElapsedRealtime = 0L;
        lastObservedTrackIdentity = null;
        lastObservedTrackPositionMs = -1L;
        lastPlaybackWasActive = false;
        currentAlbumArt = null;
        if (mediaMirror != null) mediaMirror.setVisibility(View.GONE);
        stopTitleMarquee();
        stopArtistMarquee();
    }

    private String removeFileExtension(String title) {
        if (title == null) return "";
        String value = title.trim();
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        if (dot > slash + 0 && dot < value.length() - 1) {
            return value.substring(0, dot);
        }
        return value;
    }

    private void updateTitleMarqueeIfNeeded() {
        if (displayView != null) displayView.updateTitleMarqueeIfNeeded();
    }

    private void updateArtistMarqueeIfNeeded() {
        if (displayView != null) displayView.updateArtistMarqueeIfNeeded();
    }

    private void stopArtistMarquee() {
        if (displayView != null) displayView.stopArtistMarquee();
    }

    private void stopTitleMarquee() {
        if (displayView != null) displayView.stopTitleMarquee();
    }

    private void sendPlayPause() {
        try {
            if (mediaController != null) {
                PlaybackState state = mediaController.getPlaybackState();
                if (state != null) {
                    if (state.getState() == PlaybackState.STATE_PLAYING) {
                        if ((state.getActions() & PlaybackState.ACTION_PAUSE) != 0) {
                            mediaController.getTransportControls().pause();
                            return;
                        }
                    } else {
                        if ((state.getActions() & PlaybackState.ACTION_PLAY) != 0) {
                            mediaController.getTransportControls().play();
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Some players expose media-key handling but not TransportControls.
        try {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audio != null) {
                long nowMs = System.currentTimeMillis();
                audio.dispatchMediaKeyEvent(new KeyEvent(nowMs, nowMs,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
                audio.dispatchMediaKeyEvent(new KeyEvent(nowMs, nowMs,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0));
            }
        } catch (Throwable ignored) {
        }

        if (mediaPlayPause != null) {
            mediaPlayPause.postDelayed(this::renderMediaMirror, 180L);
        }
    }

    private void sendPrevious() {
        try {
            if (mediaController != null) {
                MediaController.TransportControls controls =
                        mediaController.getTransportControls();
                if (controls != null) {
                    controls.skipToPrevious();
                    handler.postDelayed(this::refreshSystemMediaMirror, 220L);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    private void sendNext() {
        try {
            if (mediaController != null) {
                MediaController.TransportControls controls =
                        mediaController.getTransportControls();
                if (controls != null) {
                    controls.skipToNext();
                    handler.postDelayed(this::refreshSystemMediaMirror, 220L);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    private void dispatchMediaKey(int keyCode) {
        try {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audio == null) return;
            long nowMs = System.currentTimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(nowMs, nowMs,
                    KeyEvent.ACTION_DOWN, keyCode, 0));
            audio.dispatchMediaKeyEvent(new KeyEvent(nowMs, nowMs,
                    KeyEvent.ACTION_UP, keyCode, 0));
            handler.postDelayed(this::refreshSystemMediaMirror, 250L);
        } catch (Throwable ignored) {}
    }

    
    private ImageView mediaIconButton(int drawableRes, String description) {
        ImageView v = new ImageView(this);
        v.setImageResource(drawableRes);
        v.setScaleType(ImageView.ScaleType.CENTER);
        v.setContentDescription(description);
        v.setClickable(true);
        v.setFocusable(true);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setPadding(0, 0, 0, 0);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        return v;
    }

    private ClockDateTextView clockDateTextView(String text, float size, int color) {
        ClockDateTextView view = new ClockDateTextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return view;
    }

    

    private final class ClockDateTextView extends TextView {
        private float edgeErosionPx;

        ClockDateTextView(Context context) {
            super(context);
            setIncludeFontPadding(false);
        }

        void setEdgeErosionDp(float erosionDp) {
            float density = getResources().getDisplayMetrics().density;
            edgeErosionPx = Math.max(0f, erosionDp) * density;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (edgeErosionPx <= 0f) return;
            Layout layout = getLayout();
            if (layout == null) return;

            Paint paint = getPaint();
            int oldColor = paint.getColor();
            Paint.Style oldStyle = paint.getStyle();
            float oldStrokeWidth = paint.getStrokeWidth();
            Paint.Join oldJoin = paint.getStrokeJoin();
            Paint.Cap oldCap = paint.getStrokeCap();
            android.graphics.Shader oldShader = paint.getShader();
            android.graphics.PathEffect oldPathEffect = paint.getPathEffect();

            int extendedTop = getExtendedPaddingTop();
            int extendedBottom = getExtendedPaddingBottom();
            int availableHeight = Math.max(0, getHeight() - extendedTop - extendedBottom);
            int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            int verticalOffset = 0;
            if (verticalGravity == Gravity.CENTER_VERTICAL) {
                verticalOffset = (availableHeight - layout.getHeight()) / 2;
            } else if (verticalGravity == Gravity.BOTTOM) {
                verticalOffset = availableHeight - layout.getHeight();
            }

            canvas.save();
            canvas.clipRect(getCompoundPaddingLeft(), extendedTop,
                    getWidth() - getCompoundPaddingRight(), getHeight() - extendedBottom);
            canvas.translate(getCompoundPaddingLeft() - getScrollX(),
                    extendedTop + verticalOffset - getScrollY());
            paint.setColor(Color.BLACK);
            paint.setShader(null);
            paint.setPathEffect(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(edgeErosionPx);
            layout.draw(canvas);
            canvas.restore();

            paint.setColor(oldColor);
            paint.setStyle(oldStyle);
            paint.setStrokeWidth(oldStrokeWidth);
            paint.setStrokeJoin(oldJoin);
            paint.setStrokeCap(oldCap);
            paint.setShader(oldShader);
            paint.setPathEffect(oldPathEffect);
        }
    }

    private TextView textView(String text, float size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    
    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }

    
    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars()
                        | android.view.WindowInsets.Type.captionBar());
                controller.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !settingsDialogOpen) {
            hideSystemUi();
        }
    }

    private void showFatalError(Throwable t) {
        try {
            TextView error = textView(
                    getString(R.string.main_activity_text_032) + t.getClass().getSimpleName(),
                    22, Color.WHITE);
            error.setGravity(Gravity.CENTER);
            error.setBackgroundColor(Color.BLACK);
            setContentView(error);
        } catch (Throwable ignored) {
            finish();
        }
    }
}