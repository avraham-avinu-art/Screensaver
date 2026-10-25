package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.text.TextUtils;
import android.content.ActivityNotFoundException;
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

public class MainActivity extends Activity {
    public static final String EXTRA_OPENED_BY_CHARGER =
            "com.avrahamart.nightscreen.EXTRA_OPENED_BY_CHARGER";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoOffRunnable = this::exitApplicationAtSelectedTime;
    private boolean screenSleepPending = false;
    private boolean deviceEventsRegistered = false;
    private long activityDeadlineElapsedRealtime = 0L;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName screenLockAdminComponent;
    private String lastScheduledDurationValue = null;
    private long settingsOpenedElapsedRealtime = 0L;
    private final BroadcastReceiver deviceStateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)
                    || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                updateBatteryPercentView();
                if (getBool("noShutdownWhileCharging", false)) {
                    scheduleAutoOff(true);
                }
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                updateBatteryPercentView();
            } else if (Intent.ACTION_SCREEN_ON.equals(action) && screenSleepPending) {
                wakeSaverDisplay();
            }
        }
    };
    private final Calendar now = Calendar.getInstance();
    private final SimpleDateFormat numericDateFormat =
            new SimpleDateFormat("dd.MM.yyyy", Locale.US);

    private TextView timeText;
    private TextView gregorianText;
    private TextView hebrewText;
    private final int dateTextColor = Color.rgb(220, 220, 220);
    private SharedPreferences prefs;
    private ImageView settingsButton;
    private FrameLayout settingsButtonHitArea;
    private TextView batteryPercentText;
    private final Runnable hideSettingsButtonRunnable = () -> {
        if (settingsButtonHitArea != null) settingsButtonHitArea.setVisibility(View.GONE);
        else if (settingsButton != null) settingsButton.setVisibility(View.GONE);
    };
    private boolean settingsDialogOpen = false;
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

    private LinearLayout mediaSetup;
    private LinearLayout mediaMirror;
    private TextView mediaTitle;
    private ImageView mediaPlayPause;
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
        if (!getBool("showProgress", true) || getCurrentMediaDuration() <= 0L) {
            setProgressRowVisible(false);
        } else {
            setProgressRowVisible(true);
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateClock();
            refreshSystemMediaMirror();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            prefs = getSharedPreferences("settings", MODE_PRIVATE);
            initSettings();
            getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            applyChargingLaunchFlags(getIntent());
            hideSystemUi();
            buildUi();
            applySettings();
            updateClock();
            scheduleAutoOff();
        } catch (Throwable t) {
            showFatalError(t);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_OPENED_BY_CHARGER, false)) {
            applyChargingLaunchFlags(intent);
            hideSystemUi();
            scheduleAutoOff(true);
            showSettingsButtonForThreeSeconds();
            if (stageView != null) {
                stageView.post(() -> updateScreenPositions(
                        stageView, clockBlockView, mediaHolderView));
            }
        }
    }

    private void applyChargingLaunchFlags(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPENED_BY_CHARGER, false)) return;
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerDeviceStateReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

            if (screenSleepPending) {
                if (isScreenInteractive()) wakeSaverDisplay();
                else getWindow().clearFlags(
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                scheduleAutoOff();
            }
            hideSystemUi();
            handler.removeCallbacks(ticker);
            handler.post(ticker);
            registerActiveSessionsListener();
            handler.postDelayed(this::refreshSystemMediaMirror, 250L);
        } catch (Throwable t) {
            showFatalError(t);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        unregisterActiveSessionsListener();
        detachMediaController();
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterDeviceStateReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(autoOffRunnable);
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(10), 0, dp(10), dp(4));

        FrameLayout stage = new FrameLayout(this);
        stageView = stage;
        stage.setBackgroundColor(Color.BLACK);
        root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));

        settingsButtonHitArea = new FrameLayout(this);
        settingsButtonHitArea.setBackgroundColor(Color.TRANSPARENT);
        settingsButtonHitArea.setVisibility(View.GONE);
        settingsButtonHitArea.setClickable(true);
        settingsButtonHitArea.setContentDescription("הגדרות");
        settingsButtonHitArea.setOnClickListener(v -> showSettings());

        settingsButton = new ImageView(this);
        settingsButton.setImageResource(R.drawable.ic_settings);
        settingsButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        settingsButton.setColorFilter(Color.WHITE);
        settingsButton.setAlpha(0.48f);
        settingsButton.setPadding(dp(4), dp(4), dp(4), dp(4));
        settingsButton.setBackgroundColor(Color.TRANSPARENT);
        settingsButton.setContentDescription("הגדרות");
        settingsButton.setOnClickListener(v -> showSettings());
        // The hit target is twice as wide and tall; the visual settings icon stays 36x38dp.
        settingsButtonHitArea.addView(settingsButton, new FrameLayout.LayoutParams(
                dp(36), dp(38), Gravity.TOP | Gravity.LEFT));
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(
                dp(72), dp(57), Gravity.TOP | Gravity.LEFT);
        settingsParams.leftMargin = dp(4);
        settingsParams.topMargin = dp(14);
        stage.addView(settingsButtonHitArea, settingsParams);

        LinearLayout clockBlock = new LinearLayout(this);
        clockBlockView = clockBlock;
        clockBlock.setOrientation(LinearLayout.VERTICAL);
        clockBlock.setGravity(Gravity.CENTER_HORIZONTAL);

        timeText = clockDateTextView("--:--", getInt("clockDateSize", 96), Color.rgb(247, 247, 247));
        timeText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        timeText.setGravity(Gravity.CENTER);
        timeText.setIncludeFontPadding(false);
        int initialClockSize = getInt("clockDateSize", 96);
        int initialClockHeight = Math.max(dp(62), dp(initialClockSize));
        clockBlock.addView(timeText, new LinearLayout.LayoutParams(-1, initialClockHeight));

        gregorianText = clockDateTextView("", 22, dateTextColor);
        gregorianText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        gregorianText.setGravity(Gravity.CENTER);
        gregorianText.setIncludeFontPadding(false);
        int initialDateHeight = dateRowHeightForClockPreset(initialClockSize);
        gregorianText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        LinearLayout.LayoutParams gregorianParams =
                new LinearLayout.LayoutParams(-1, initialDateHeight);
        gregorianParams.topMargin = dp(dateClockGapDp(initialClockSize));
        clockBlock.addView(gregorianText, gregorianParams);

        hebrewText = clockDateTextView("", 22, dateTextColor);
        hebrewText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hebrewText.setGravity(Gravity.CENTER);
        hebrewText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        hebrewText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        hebrewText.setIncludeFontPadding(false);
        clockBlock.addView(hebrewText, new LinearLayout.LayoutParams(-1, initialDateHeight));

        int initialClockBlockHeight = initialClockHeight + initialDateHeight * 2
                + dp(dateClockGapDp(initialClockSize)) + dp(2);
        stage.addView(clockBlock, new FrameLayout.LayoutParams(-1, initialClockBlockHeight, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        LinearLayout mediaHolder = new LinearLayout(this);
        mediaHolderView = mediaHolder;
        mediaHolder.setOrientation(LinearLayout.VERTICAL);
        mediaHolder.setGravity(Gravity.CENTER_HORIZONTAL);
        stage.addView(mediaHolder, new FrameLayout.LayoutParams(-1, -2));

        // Notification permission guidance now lives only in Settings.
        buildSystemMediaMirror(mediaHolder);

        batteryPercentText = textView("", 15, Color.rgb(156, 178, 203));
        batteryPercentText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        batteryPercentText.setGravity(Gravity.CENTER);
        batteryPercentText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        batteryPercentText.setIncludeFontPadding(false);
        batteryPercentText.setVisibility(View.GONE);
        FrameLayout.LayoutParams batteryPercentParams = new FrameLayout.LayoutParams(
                dp(54), dp(30), Gravity.TOP | Gravity.RIGHT);
        batteryPercentParams.rightMargin = dp(14);
        batteryPercentParams.topMargin = dp(17);
        stage.addView(batteryPercentText, batteryPercentParams);

        stage.post(() -> {
            applySettings();
            updateScreenPositions(stageView, clockBlockView, mediaHolderView);
        });
        setContentView(root);
    }

    private void updateScreenPositions(FrameLayout stage, View clockBlock, View mediaHolder) {
        if (stage == null) return;
        try {
            int h = stage.getHeight();
            if (h <= 0) return;

            String clockPos = getString("clockDatePosition", "top");
            String playerPos = getString("playerPosition", "bottom");

            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) clockBlock.getLayoutParams();
            int blockHeight = Math.max(0, clockBlock.getMeasuredHeight());
            int clockLineHeight = blockHeight;
            if (timeText != null) {
                // Prefer the final row height calculated from the selected font and size;
                // this remains accurate even when the surrounding date rows are taller.
                ViewGroup.LayoutParams timeParams = timeText.getLayoutParams();
                if (timeParams != null && timeParams.height > 0) {
                    clockLineHeight = timeParams.height;
                } else if (timeText.getMeasuredHeight() > 0) {
                    clockLineHeight = timeText.getMeasuredHeight();
                }
            }
            cp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if ("upper".equals(clockPos)) {
                // Place the center of the clock line at exactly one quarter of the
                // screen height, independent of the clock/date block's total height.
                cp.topMargin = Math.max(0, Math.round(h / 4f - clockLineHeight / 2f));
            } else if ("middle".equals(clockPos)) {
                cp.topMargin = Math.max(0, Math.round(h * 0.50f - blockHeight / 2f));
            } else {
                // "למעלה" keeps the top edge of the block aligned with the screen top.
                cp.topMargin = 0;
            }
            clockBlock.setLayoutParams(cp);

            FrameLayout.LayoutParams mp = (FrameLayout.LayoutParams) mediaHolder.getLayoutParams();
            if ("bottom".equals(playerPos)) {
                // Let FrameLayout anchor the entire holder to the real bottom.
                // This avoids the first-layout race where measured height is still stale
                // and the player appears too low/cut off until the Activity is reopened.
                mp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                mp.topMargin = 0;
                mp.bottomMargin = 0;
                mp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            } else {
                mp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                int mediaH = Math.max(0, mediaHolder.getMeasuredHeight());
                // "Lower quarter" is positioned 20% above the bottom edge,
                // while "bottom" remains anchored to the very bottom edge.
                mp.topMargin = Math.max(0, Math.round((h * 0.65f) - mediaH / 2f));
                mp.bottomMargin = 0;
                mp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            }
            mediaHolder.setLayoutParams(mp);
        } catch (Throwable ignored) {}
    }

    private void buildMediaSetup(LinearLayout root) {
        mediaSetup = new LinearLayout(this);
        mediaSetup.setOrientation(LinearLayout.HORIZONTAL);
        mediaSetup.setGravity(Gravity.CENTER_VERTICAL);
        mediaSetup.setPadding(dp(8), dp(5), dp(8), dp(5));
        mediaSetup.setBackgroundColor(Color.BLACK);
        mediaSetup.setVisibility(View.GONE);

        TextView message = textView(
                "יש לאפשר גישה להתראות\nכדי להציג את נגן המערכת",
                14, Color.rgb(215, 220, 228));
        message.setGravity(Gravity.CENTER);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        message.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        message.setLineSpacing(dp(2), 1.0f);
        message.setSingleLine(false);
        message.setMaxLines(2);
        message.setEllipsize(null);
        message.setIncludeFontPadding(false);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(0, dp(58), 1f);
        messageParams.leftMargin = dp(6);
        mediaSetup.addView(message, messageParams);

        TextView openSettings = mediaButton("הפעל");
        openSettings.setTextSize(14);
        openSettings.setTextColor(Color.WHITE);
        GradientDrawable accessButtonBackground = new GradientDrawable();
        accessButtonBackground.setColor(Color.rgb(47, 83, 125));
        accessButtonBackground.setCornerRadius(dp(16));
        accessButtonBackground.setStroke(dp(1), Color.rgb(94, 137, 184));
        openSettings.setBackground(accessButtonBackground);
        openSettings.setGravity(Gravity.CENTER);
        openSettings.setPadding(dp(10), 0, dp(10), 0);
        openSettings.setElevation(dp(2));
        openSettings.setOnClickListener(v -> openNotificationAccessSettings());
        mediaSetup.addView(openSettings, new LinearLayout.LayoutParams(dp(76), dp(46)));

        int availableWidth = Math.max(
                dp(1),
                getResources().getDisplayMetrics().widthPixels - dp(20));
        LinearLayout.LayoutParams setupParams = new LinearLayout.LayoutParams(
                Math.min(dp(760), availableWidth), dp(68));
        setupParams.gravity = Gravity.CENTER_HORIZONTAL;
        setupParams.bottomMargin = dp(5);
        root.addView(mediaSetup, setupParams);
    }

    private void buildSystemMediaMirror(LinearLayout root) {
        mediaMirror = new LinearLayout(this);
        mediaMirror.setOrientation(LinearLayout.VERTICAL);
        mediaMirror.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mediaMirror.setPadding(0, 0, 0, 0);
        mediaMirror.setBackgroundColor(Color.BLACK);
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

        // Artist first and song title immediately below it, followed by progress.
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, dp(30));
        mediaMirror.addView(mediaDetails, detailsParams);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(30));
        titleParams.topMargin = dp(0);
        mediaMirror.addView(mediaTitle, titleParams);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(28));
        progressParams.topMargin = dp(0);
        mediaMirror.addView(mediaProgressRow, progressParams);

        FrameLayout controls = new FrameLayout(this);
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
        mediaNext = mediaIconButton(R.drawable.media_next, "הבא");
        mediaNext.setOnClickListener(v -> sendPrevious());
        FrameLayout.LayoutParams nextParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        controls.addView(mediaNext, nextParams);

        mediaPlayPause = mediaIconButton(R.drawable.media_play, "נגן");
        mediaPlayPause.setOnClickListener(v -> sendPlayPause());
        FrameLayout.LayoutParams playParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.CENTER);
        controls.addView(mediaPlayPause, playParams);

        mediaPrev = mediaIconButton(R.drawable.media_previous, "הקודם");
        mediaPrev.setOnClickListener(v -> sendNext());
        FrameLayout.LayoutParams prevParams =
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        controls.addView(mediaPrev, prevParams);

        mediaMirror.addView(controls, controlAreaParams);

        LinearLayout.LayoutParams mirrorParams =
                new LinearLayout.LayoutParams(
                        Math.min(dp(760), Math.max(dp(1),
                                getResources().getDisplayMetrics().widthPixels - dp(20))),
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

        SharedPreferences.Editor e = prefs.edit();

        e.putInt("fontColor", getInt("fontColor", dateTextColor));
        e.putInt("fontSize", getInt("fontSize", 17));
        e.putInt("clockDateSize", getInt("clockDateSize", 96));
        e.putInt("playerSize", getInt("playerSize", 17));
        e.putInt("fontWeight", getInt("fontWeight", 1));
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
        e.putBoolean("showPlayer", getBool("showPlayer", true));
        e.putBoolean("showDetails", getBool("showDetails", true));
        e.putBoolean("showProgress", getBool("showProgress", true));
        lastTitle = getString("lastTitle", lastTitle);
        lastArtist = getString("lastArtist", lastArtist);

        if (!getBool("referenceLayoutV3Applied", false)) {
            e.putBoolean("showDetails", getBool("showDetails", true));
            e.putBoolean("showProgress", true);
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
        e.putString("screenSaverDuration", getString("screenSaverDuration", "0"));
        e.putBoolean("autoStartOnCharge", getBool("autoStartOnCharge", false));
        e.putBoolean("noShutdownWhileCharging",
                getBool("noShutdownWhileCharging", false));
        e.putBoolean("showBatteryPercent", getBool("showBatteryPercent", false));
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
            int oldSize = getInt("clockDateSize", 96);
            int newSize;
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

    private void applySettings() {
        if (prefs == null || gregorianText == null) return;

        int color = getInt("fontColor", dateTextColor);
        int size = getInt("fontSize", 17);
        int clockDateSize = getInt("clockDateSize", 96);
        int playerSize = getInt("playerSize", 17);
        String fontFamily = getString("fontFamily", "sans-serif");
        int requestedWeight = getInt("fontWeight", 1);
        int weight = effectiveFontWeightForFamily(fontFamily, requestedWeight);
        Typeface tf;
        Typeface mediaTypeface;
        if (fontFamily.startsWith("custom:")) {
            Typeface baseTypeface = loadCustomTypeface(fontFamily.substring("custom:".length()));
            mediaTypeface = baseTypeface;
            if (weight == -1) {
                tf = Build.VERSION.SDK_INT >= 28
                        ? Typeface.create(baseTypeface, 300, false) : baseTypeface;
            } else if (weight == 0) {
                tf = baseTypeface;
            } else if (Build.VERSION.SDK_INT >= 28) {
                tf = Typeface.create(baseTypeface, weight == 1 ? 500 : 700, false);
            } else {
                tf = Typeface.create(baseTypeface,
                        weight == 1 ? Typeface.NORMAL : Typeface.BOLD);
            }
        } else {
            mediaTypeface = Typeface.create(fontFamily, Typeface.NORMAL);
            if (weight == -1 && "sans-serif".equals(fontFamily)) {
                tf = Typeface.create("sans-serif-thin", Typeface.NORMAL);
            } else if (weight == 2) {
                tf = Typeface.create(fontFamily, Typeface.BOLD);
            } else if (weight == 1 && "sans-serif".equals(fontFamily)) {
                tf = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            } else if (weight == 1) {
                tf = Typeface.create(fontFamily, Typeface.BOLD);
            } else {
                tf = Typeface.create(fontFamily, Typeface.NORMAL);
            }
        }

        gregorianText.setVisibility(
                getBool("showGregorian", true) ? View.VISIBLE : View.GONE);
        hebrewText.setVisibility(
                getBool("showHebrew", true) ? View.VISIBLE : View.GONE);

        int displayTextColor = color;
        gregorianText.setTextColor(displayTextColor);
        hebrewText.setTextColor(displayTextColor);
        timeText.setTextColor(displayTextColor);
        // Keep the chosen color independent of font weight. Proportional numerals
        // avoid tabular-width spacing in the built-in default font where supported.
        String fontFeatures = "sans-serif".equals(fontFamily) ? "'pnum'" : "";
        gregorianText.setFontFeatureSettings(fontFeatures);
        hebrewText.setFontFeatureSettings(fontFeatures);
        timeText.setFontFeatureSettings(fontFeatures);

        int effectiveClockSize = effectiveClockDateSize(fontFamily, clockDateSize);
        float dateSize = dateTextSizeForClockPreset(effectiveClockSize);
        float clockDisplaySize = effectiveClockSize;
        gregorianText.setTextSize(dateSize);
        hebrewText.setTextSize(dateSize);
        timeText.setTextSize(clockDisplaySize);
        gregorianText.setTypeface(tf);
        hebrewText.setTypeface(tf);
        timeText.setTypeface(tf);
        applyFontWeightToTextView(gregorianText, weight, fontFamily, requestedWeight);
        applyFontWeightToTextView(hebrewText, weight, fontFamily, requestedWeight);
        applyFontWeightToTextView(timeText, weight, fontFamily, requestedWeight);

        boolean showGregorian = getBool("showGregorian", true);
        boolean showHebrew = getBool("showHebrew", true);
        int visibleDates = (showGregorian ? 1 : 0) + (showHebrew ? 1 : 0);

        // Measure every row with its final Typeface and TextSize. Hebrew and Gregorian
        // dates are measured separately because the custom font's Hebrew glyphs can be
        // taller than its digits, even when both rows share the same nominal font size.
        // Use complete font metrics for the clock row, not only painted glyph bounds.
        // Centering and clipping can occur when this row is shorter than the font metrics.
        int clockH = Math.max(dp(62), measuredLineHeight(
                timeText, "0123456789:00", 2, effectiveClockSize));
        int gregorianH = Math.max(dateRowHeightForClockPreset(effectiveClockSize),
                measuredLineHeight(gregorianText, "יום ה׳ · 22.10.2026", 0, 24));
        int hebrewH = Math.max(dateRowHeightForClockPreset(effectiveClockSize),
                measuredLineHeight(hebrewText, "כ״ט בתשרי תשפ״ז אבגדהוזחטיכלמנסעפצקרשת", 0, 24));

        // Custom fonts can declare unusually tall font metrics even when the painted
        // characters are not tall. A fixed negative margin therefore leaves different
        // visible gaps for different font families. Calculate each margin from the actual
        // painted glyph bounds so the visible gap, rather than the font's empty line area,
        // is consistent between clock and date rows.
        int targetVisualGapPx = dp(visualGapDp(effectiveClockSize));
        int gregorianTopMargin = 0;
        int hebrewTopMargin = 0;
        if (showGregorian) {
            gregorianTopMargin = visualGapMarginPx(
                    timeText, clockH, "0123456789:00",
                    gregorianText, gregorianH, "יום ה׳ · 22.10.2026",
                    targetVisualGapPx);
        }
        if (showHebrew) {
            if (showGregorian) {
                hebrewTopMargin = visualGapMarginPx(
                        gregorianText, gregorianH, "יום ה׳ · 22.10.2026",
                        hebrewText, hebrewH, "כ״ט בתשרי תשפ״ז אבגדהוזחטיכלמנסעפצקרשת",
                        targetVisualGapPx);
            } else {
                hebrewTopMargin = visualGapMarginPx(
                        timeText, clockH, "0123456789:00",
                        hebrewText, hebrewH, "כ״ט בתשרי תשפ״ז אבגדהוזחטיכלמנסעפצקרשת",
                        targetVisualGapPx);
            }
        }

        LinearLayout.LayoutParams tp = (LinearLayout.LayoutParams) timeText.getLayoutParams();
        tp.height = clockH;
        timeText.setLayoutParams(tp);
        LinearLayout.LayoutParams gp = (LinearLayout.LayoutParams) gregorianText.getLayoutParams();
        gp.height = gregorianH;
        gp.topMargin = showGregorian ? gregorianTopMargin : 0;
        gregorianText.setLayoutParams(gp);
        LinearLayout.LayoutParams hp = (LinearLayout.LayoutParams) hebrewText.getLayoutParams();
        hp.height = hebrewH;
        hp.topMargin = showHebrew ? hebrewTopMargin : 0;
        hebrewText.setLayoutParams(hp);

        View clockParent = (View) timeText.getParent();
        if (clockParent instanceof LinearLayout) {
            LinearLayout cb = (LinearLayout) clockParent;
            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) cb.getLayoutParams();
            int dateRowsHeight = (showGregorian ? gregorianH : 0)
                    + (showHebrew ? hebrewH : 0);
            int visibleMargins = (showGregorian ? gregorianTopMargin : 0)
                    + (showHebrew ? hebrewTopMargin : 0);
            cp.height = clockH + dateRowsHeight + visibleMargins
                    + (visibleDates > 0 ? dp(2) : 0);
            cb.setLayoutParams(cp);
            cb.requestLayout();
            cb.post(() -> updateScreenPositions(stageView, clockBlockView, mediaHolderView));
        }

        if (settingsButton != null) {
            settingsButton.setColorFilter(color);
        }

        if (mediaTitle != null) {
            // Font family and color apply to the player, but the clock/date weight
            // selector is deliberately isolated from all player text.
            mediaTitle.setTypeface(mediaTypeface);
            mediaDetails.setTypeface(mediaTypeface);
            mediaProgressText.setTypeface(mediaTypeface);
            if (mediaCurrentText != null) mediaCurrentText.setTypeface(mediaTypeface);
            if (mediaTotalText != null) mediaTotalText.setTypeface(mediaTypeface);

            float scale = Math.max(0.72f, Math.min(1.45f, playerSize / 17f));
            int textRowHeight = Math.max(dp(24), Math.round(dp(30) * scale));
            mediaTitle.setTextSize(playerSize);
            mediaDetails.setTextSize(playerSize);
            if (mediaTitle.getLayoutParams() != null) {
                mediaTitle.getLayoutParams().height = textRowHeight;
                mediaTitle.requestLayout();
            }
            if (mediaDetails.getLayoutParams() != null) {
                mediaDetails.getLayoutParams().height = textRowHeight;
                mediaDetails.requestLayout();
            }
            mediaProgressText.setTextSize(Math.max(9, Math.round((playerSize - 4) * scale)));

            // Scale the position/time row, including its labels, track, and row height.
            int progressRowHeight = Math.max(dp(24), Math.round(dp(32) * scale));
            int timeLabelWidth = Math.max(dp(34), Math.round(dp(48) * scale));
            int timeLabelMargin = Math.round(dp(4) * scale);
            int seekBarPadding = Math.max(dp(3), Math.round(dp(7) * scale));
            int baseTrackWidth = mediaProgressBaseWidth > 0 ? mediaProgressBaseWidth
                    : Math.max(dp(120),
                            Math.round(getResources().getDisplayMetrics().widthPixels * 0.36f));
            int scaledTrackWidth = Math.max(dp(80), Math.round(baseTrackWidth * scale));

            if (mediaProgressRow != null) {
                mediaProgressRow.setPadding(
                        Math.round(dp(4) * scale), 0,
                        Math.round(dp(4) * scale), 0);
                if (mediaProgressRow.getLayoutParams() != null) {
                    mediaProgressRow.getLayoutParams().height = progressRowHeight;
                    mediaProgressRow.requestLayout();
                }
            }
            if (mediaCurrentText != null) {
                mediaCurrentText.setTextSize(Math.max(9, 12f * scale));
                LinearLayout.LayoutParams currentParams =
                        (LinearLayout.LayoutParams) mediaCurrentText.getLayoutParams();
                currentParams.width = timeLabelWidth;
                currentParams.height = progressRowHeight;
                currentParams.rightMargin = timeLabelMargin;
                mediaCurrentText.setLayoutParams(currentParams);
            }
            if (mediaTotalText != null) {
                mediaTotalText.setTextSize(Math.max(9, 12f * scale));
                LinearLayout.LayoutParams totalParams =
                        (LinearLayout.LayoutParams) mediaTotalText.getLayoutParams();
                totalParams.width = timeLabelWidth;
                totalParams.height = progressRowHeight;
                totalParams.leftMargin = timeLabelMargin;
                mediaTotalText.setLayoutParams(totalParams);
            }
            if (mediaSeekBar != null) {
                LinearLayout.LayoutParams seekParams =
                        (LinearLayout.LayoutParams) mediaSeekBar.getLayoutParams();
                seekParams.width = scaledTrackWidth;
                seekParams.height = progressRowHeight;
                mediaSeekBar.setLayoutParams(seekParams);
                mediaSeekBar.setPadding(seekBarPadding, 0, seekBarPadding, 0);
            }

            // Re-measure both rows after a preset change; only overflowing text scrolls.
            mediaTitle.post(() -> updateTitleMarqueeIfNeeded());
            mediaDetails.post(() -> updateArtistMarqueeIfNeeded());

            // Scale the control buttons together with the selected player size.
            int scaledButtonSize = Math.round(dp(44) * scale);
            if (mediaPrev != null && mediaPlayPause != null && mediaNext != null) {
                mediaPrev.getLayoutParams().width = scaledButtonSize;
                mediaPrev.getLayoutParams().height = scaledButtonSize;
                mediaPlayPause.getLayoutParams().width = scaledButtonSize;
                mediaPlayPause.getLayoutParams().height = scaledButtonSize;
                mediaNext.getLayoutParams().width = scaledButtonSize;
                mediaNext.getLayoutParams().height = scaledButtonSize;
                mediaPrev.requestLayout();
                mediaPlayPause.requestLayout();
                mediaNext.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mediaPlayPause.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mediaPrev.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int iconPadding = Math.max(dp(2), Math.round(dp(6) / scale));
                mediaNext.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                mediaPlayPause.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                mediaPrev.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
                mediaNext.requestLayout();
                View controls = (View) mediaPrev.getParent();
                if (controls != null) {
                    LinearLayout.LayoutParams cp =
                            (LinearLayout.LayoutParams) controls.getLayoutParams();
                    cp.height = scaledButtonSize;
                    controls.setLayoutParams(cp);
                }
            }

            mediaTitle.setTextColor(displayTextColor);
            mediaDetails.setTextColor(displayTextColor);
            mediaProgressText.setTextColor(displayTextColor);
            if (mediaCurrentText != null) mediaCurrentText.setTextColor(displayTextColor);
            if (mediaTotalText != null) mediaTotalText.setTextColor(displayTextColor);
            if (mediaSeekBar != null) {
                mediaSeekBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(color));
                mediaSeekBar.setProgressBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(blendWithBlack(color, 0.48f)));
                mediaSeekBar.setThumbTintList(
                        android.content.res.ColorStateList.valueOf(color));
            }

            applyMediaFrame(mediaController != null && hasUsableMediaSession(mediaController));

            mediaNext.setBackgroundColor(Color.TRANSPARENT);
            mediaPrev.setBackgroundColor(Color.TRANSPARENT);
            mediaPlayPause.setBackgroundColor(Color.TRANSPARENT);
            if (mediaNext.getDrawable() != null) mediaNext.getDrawable().setTint(color);
            if (mediaPrev.getDrawable() != null) mediaPrev.getDrawable().setTint(color);
            if (mediaPlayPause.getDrawable() != null) mediaPlayPause.getDrawable().setTint(color);
            View parent = mediaMirror.getParent() instanceof View ? (View) mediaMirror.getParent() : null;
            if (parent != null && parent.getParent() instanceof FrameLayout) {
                FrameLayout stage = (FrameLayout) parent.getParent();
                updateScreenPositions(stageView, clockBlockView, mediaHolderView);
            }
        }
        updateBatteryPercentView();
    }

    private int dateClockGapDp(int clockSize) {
        // Used only for the initial layout before the selected font has been measured.
        return -8;
    }

    private int visualGapDp(int clockSize) {
        // Keep the painted gap compact and consistent across fonts with different metrics.
        if (clockSize >= 108) return 6;
        if (clockSize >= 84) return 5;
        return 4;
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
        // The date rows are always 28% of the effective clock size, for every preset.
        return clockSize * 0.28f;
    }

    private int dateRowHeightForClockPreset(int clockSize) {
        // Let measured glyph metrics determine the line height; this is only a fallback minimum.
        return Math.max(dp(24), dp(Math.round(clockSize * 0.28f)));
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

    private void updateBatteryPercentView() {
        if (batteryPercentText == null) return;
        if (!getBool("showBatteryPercent", false) || !isDeviceCharging()) {
            batteryPercentText.setVisibility(View.GONE);
            return;
        }
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) {
                batteryPercentText.setVisibility(View.GONE);
                return;
            }
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) {
                batteryPercentText.setVisibility(View.GONE);
                return;
            }
            int percent = Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
            batteryPercentText.setText(percent + "%");
            batteryPercentText.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
            batteryPercentText.setVisibility(View.GONE);
        }
    }

    private void applyMediaFrame(boolean hasActiveSong) {
        if (mediaMirror == null) return;
        boolean enabled = getBool("frameEnabled", false) && hasActiveSong;
        if (enabled) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.BLACK);
            bg.setCornerRadius(dp(Math.max(0, getInt("frameRadius", 10))));
            bg.setStroke(dp(Math.max(1, getInt("frameWidth", 1))),
                    getInt("frameColor", Color.WHITE));
            mediaMirror.setBackground(bg);
            // Keep the text and controls inside the visible outline.
            int inset = dp(Math.max(3, getInt("frameWidth", 1) + 2));
            mediaMirror.setPadding(inset, inset, inset, inset);
        } else {
            mediaMirror.setBackgroundColor(Color.BLACK);
            mediaMirror.setPadding(0, 0, 0, 0);
        }
    }

    private void scheduleAutoOff() {
        scheduleAutoOff(false);
    }

    private void scheduleAutoOff(boolean restartCountdown) {
        handler.removeCallbacks(autoOffRunnable);
        if (settingsDialogOpen && !restartCountdown) {
            return;
        }
        String value = getString("screenSaverDuration", "0");
        boolean settingChanged = !value.equals(lastScheduledDurationValue);
        if (settingChanged) {
            lastScheduledDurationValue = value;
            restartCountdown = true;
        }

        // Layout changes, media refreshes and returning from system settings must not
        // restart an already running inactivity countdown.
        if (screenSleepPending && !restartCountdown) return;
        if (restartCountdown) {
            activityDeadlineElapsedRealtime = 0L;
            screenSleepPending = false;
        }

        // The dedicated charging option overrides every activity-time preset while plugged in.
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

        // Read Android's actual timeout; do not assume it is one or five minutes.
        // The inactivity timer does not restart when KEEP_SCREEN_ON is released.
        // If the selected duration exceeds the system timeout, keep the screen on
        // for the full remaining duration, then release it at the selected deadline.
        // If it is at most the system timeout, leave the display timeout to Android.
        boolean keepAwake = isScreenLockAdminActive();
        if (!keepAwake) {
            long systemTimeoutMs = getSystemScreenOffTimeoutMs();
            if (systemTimeoutMs < 0L) {
                // If the setting cannot be read, retain the existing safe behavior.
                keepAwake = true;
            } else {
                long selectedDurationMs = minutes > Long.MAX_VALUE / 60_000L
                        ? Long.MAX_VALUE : minutes * 60_000L;
                keepAwake = selectedDurationMs > systemTimeoutMs;
            }
        }

        if (keepAwake) {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    private void exitApplicationAtSelectedTime() {
        if (isFinishing() || screenSleepPending) return;
        handler.removeCallbacks(autoOffRunnable);
        activityDeadlineElapsedRealtime = 0L;
        getWindow().clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Without the optional device-admin permission, only end the screensaver.
        // Android's configured screen timeout and lock policy remain in control.
        if (!isScreenLockAdminActive()) {
            finishAffinity();
            return;
        }

        // When explicitly enabled by the user, lock the device at the chosen time.
        screenSleepPending = true;
        try {
            if (devicePolicyManager == null) {
                devicePolicyManager = (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
            }
            if (devicePolicyManager == null) throw new IllegalStateException("Device policy unavailable");
            devicePolicyManager.lockNow();
        } catch (Throwable ignored) {
            screenSleepPending = false;
            finishAffinity();
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
            return true;
        }
    }

    private void wakeSaverDisplay() {
        if (!screenSleepPending) return;
        screenSleepPending = false;
        scheduleAutoOff(true);
        showSettingsButtonForThreeSeconds();
        if (stageView != null) {
            stageView.post(() -> updateScreenPositions(
                    stageView, clockBlockView, mediaHolderView));
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

    private void showSettingsButtonForThreeSeconds() {
        if (settingsButton == null || settingsButtonHitArea == null || settingsDialogOpen) return;
        handler.removeCallbacks(hideSettingsButtonRunnable);
        settingsButtonHitArea.setVisibility(View.VISIBLE);
        settingsButton.setAlpha(0.58f);
        handler.postDelayed(hideSettingsButtonRunnable, 3_000L);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            if (screenSleepPending) wakeSaverDisplay();
            if (stageView != null && stageView.getHeight() > 0) {
                float localY = event.getY() - stageView.getTop();
                if (localY >= 0 && localY <= stageView.getHeight() / 2f) {
                    showSettingsButtonForThreeSeconds();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (screenSleepPending) wakeSaverDisplay();
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            showSettingsButtonForThreeSeconds();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSettings() {
        if (settingsDialogOpen) return;
        if (prefs == null) initSettings();

        settingsDialogOpen = true;
        settingsOpenedElapsedRealtime = SystemClock.elapsedRealtime();
        handler.removeCallbacks(autoOffRunnable);
        try {
            startActivity(new Intent(this, SettingsActivity.class));
        } catch (Throwable t) {
            settingsDialogOpen = false;
            settingsOpenedElapsedRealtime = 0L;
            scheduleAutoOff(false);
            android.widget.Toast.makeText(
                    this, "לא ניתן לפתוח את מסך ההגדרות.", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private MediaController selectPlayableSession(List<MediaController> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;

        for (MediaController controller : sessions) {
            try {
                PlaybackState state = controller.getPlaybackState();
                if (state != null &&
                        (state.getState() == PlaybackState.STATE_PAUSED ||
                         state.getState() == PlaybackState.STATE_PLAYING ||
                         state.getState() == PlaybackState.STATE_BUFFERING)) {
                    return controller;
                }
            } catch (Throwable ignored) {
            }
        }

        return sessions.get(0);
    }

    private void updateClock() {
        now.setTimeInMillis(System.currentTimeMillis());
        timeText.setText(numericTime());
        gregorianText.setText(hebrewWeekday() + " " +
                numericDateFormat.format(now.getTime()));

        try {
            JewishDate jewishDate = new JewishDate(now);
            HebrewDateFormatter formatter = new HebrewDateFormatter();
            formatter.setHebrewFormat(true);
            formatter.setUseGershGershayim(true);
            formatter.setUseLongHebrewYears(false);
            String formattedHebrewDate = formatter.format(jewishDate)
                    .replace("מרחשוון", "חשוון")
                    .replace("מרחשון", "חשוון")
                    .replace("חשון", "חשוון")
                    .replace("\u05F3", "'")
                    .replace("\u05F4", "\"")
                    .replace("’", "'")
                    .replace("”", "\"")
                    .replace("“", "\"");
            hebrewText.setText(formattedHebrewDate);
        } catch (Throwable t) {
            hebrewText.setText("");
        }
    }

    private String numericTime() {
        return String.format(Locale.US, "%d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE));
    }

    private String hebrewWeekday() {
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY: return "יום א'";
            case Calendar.MONDAY: return "יום ב'";
            case Calendar.TUESDAY: return "יום ג'";
            case Calendar.WEDNESDAY: return "יום ד'";
            case Calendar.THURSDAY: return "יום ה'";
            case Calendar.FRIDAY: return "יום ו'";
            default: return "שבת";
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
            activeSessionsListener = controllers -> {
                // A media session can be created after the screen saver starts.
                // Refresh immediately instead of waiting for the next lifecycle event.
                handler.post(this::refreshSystemMediaMirror);
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
                hideMediaMirror();
                return;
            }

            int playback = state.getState();
            if (playback != PlaybackState.STATE_PLAYING &&
                    playback != PlaybackState.STATE_BUFFERING &&
                    playback != PlaybackState.STATE_PAUSED) {
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
                title = "מדיה";
            }

            title = removeFileExtension(title);
            String previousTitle = mediaTitle.getText() == null
                    ? "" : mediaTitle.getText().toString();
            if (!title.equals(previousTitle)) {
                stopTitleMarquee();
                mediaTitle.setText(title);
                mediaTitle.post(() -> updateTitleMarqueeIfNeeded());
            }
            mediaTitle.setVisibility(View.VISIBLE);
            lastTitle = title;
            String artist = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            artist = artist == null ? "" : artist.trim();
            if (artist.equalsIgnoreCase("unknown artist") ||
                    artist.equalsIgnoreCase("unknown") ||
                    artist.equalsIgnoreCase("artist unknown") ||
                    artist.equals("אמן לא ידוע") ||
                    artist.equals("אמן לא ידוע/ה") ||
                    artist.equals("לא ידוע")) {
                artist = "";
            }
            String previousArtistText = mediaDetails.getText() == null
                    ? "" : mediaDetails.getText().toString();
            boolean artistChanged = !artist.equals(previousArtistText);
            lastArtist = artist;
            prefs.edit().putString("lastTitle", lastTitle).putString("lastArtist", lastArtist).apply();
            long duration = metadata == null ? 0L : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            long position = Math.max(0L, state.getPosition());
            if (duration > 0) position = Math.min(position, duration);
            boolean showDetails = getBool("showDetails", true);
            if (artistChanged) {
                stopArtistMarquee();
                mediaDetails.setText(lastArtist);
            }
            mediaDetails.setVisibility(showDetails && !lastArtist.isEmpty() ? View.VISIBLE : View.GONE);
            if (artistChanged && showDetails && !lastArtist.isEmpty()) {
                mediaDetails.post(() -> updateArtistMarqueeIfNeeded());
            }
            boolean showProgressSetting = getBool("showProgress", true);
            if (!showProgressSetting) {
                handler.removeCallbacks(hideProgressRowAfterMetadataChange);
                progressHidePending = false;
                setProgressRowVisible(false);
            } else if (duration > 0L) {
                handler.removeCallbacks(hideProgressRowAfterMetadataChange);
                progressHidePending = false;
                setProgressRowVisible(true);
                position = Math.max(0L, Math.min(position, duration));
                mediaCurrentText.setText(formatMs(position));
                mediaTotalText.setText(formatMs(duration));
                mediaSeekBar.setMax(1000);
                mediaSeekBar.setProgress((int)Math.min(1000L, (position * 1000L) / duration));
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
            mediaPlayPause.setVisibility(View.VISIBLE);
                boolean isPlaying = playback == PlaybackState.STATE_PLAYING;
            mediaPlayPause.setImageResource(
                    isPlaying ? R.drawable.media_pause : R.drawable.media_play);
            mediaPlayPause.setContentDescription(isPlaying ? "השהה" : "נגן");
            int mediaColor = getInt("fontColor", dateTextColor);
            if (mediaPlayPause.getDrawable() != null) mediaPlayPause.getDrawable().setTint(mediaColor);

            long actions = state.getActions();

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

            applyMediaFrame(true);
            mediaMirror.setVisibility(getBool("showPlayer", true) ? View.VISIBLE : View.GONE);
            mediaMirror.requestLayout();
            View parent = mediaMirror.getParent() instanceof View ? (View) mediaMirror.getParent() : null;
            if (parent != null && parent.getParent() instanceof FrameLayout) {
                FrameLayout stage = (FrameLayout) parent.getParent();
                stage.post(() -> updateScreenPositions(stageView, clockBlockView, mediaHolderView));
            }
        } catch (Throwable ignored) {
            hideMediaMirror();
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
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (mediaProgressRow != null) mediaProgressRow.setVisibility(visibility);
        if (mediaSeekBar != null) mediaSeekBar.setVisibility(visibility);
        if (mediaCurrentText != null) mediaCurrentText.setVisibility(visibility);
        if (mediaTotalText != null) mediaTotalText.setVisibility(visibility);
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
        if (mediaSetup != null) mediaSetup.setVisibility(View.GONE);
    }

    private void openNotificationAccessSettings() {
        // Use the top-level Notification Access page for maximum OEM compatibility.
        // Some devices crash when launched with the newer detail-page extra.
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        } catch (Throwable ignored) {
        }
    }

    private void hideMediaMirror() {
        if (mediaMirror != null) {
            mediaMirror.setVisibility(View.GONE);
        }
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
        if (mediaTitle == null) return;
        try {
            String value = mediaTitle.getText() == null ? "" : mediaTitle.getText().toString();
            float textWidth = mediaTitle.getPaint().measureText(value);
            float available = Math.max(0,
                    mediaTitle.getWidth() - mediaTitle.getPaddingLeft() - mediaTitle.getPaddingRight());

            stopTitleMarquee();

            if (!value.isEmpty() && textWidth > available + dp(2)) {
                // The title fits normally when short. Only overflowing titles
                // become horizontally scrollable.
                mediaTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                mediaTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
                mediaTitle.setHorizontallyScrolling(true);
                mediaTitle.setEllipsize(null);
                mediaTitle.setSelected(false);

                final int overflow = Math.max(1, (int)Math.ceil(textWidth - available));
                mediaTitle.setScrollX(overflow);
                titleMarqueeAnimator = ObjectAnimator.ofInt(
                        mediaTitle, "scrollX", overflow, 0);
                long duration = Math.max(8000L,
                        Math.min(20000L, 7000L + overflow * 20L));
                titleMarqueeAnimator.setDuration(duration);
                titleMarqueeAnimator.setStartDelay(1200L);
                titleMarqueeAnimator.setRepeatMode(ObjectAnimator.REVERSE);
                titleMarqueeAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                titleMarqueeAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
                titleMarqueeAnimator.start();
            } else {
                mediaTitle.setGravity(Gravity.CENTER);
                mediaTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
                mediaTitle.setHorizontallyScrolling(false);
                mediaTitle.setEllipsize(TextUtils.TruncateAt.END);
                mediaTitle.setSelected(false);
                mediaTitle.setScrollX(0);
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateArtistMarqueeIfNeeded() {
        if (mediaDetails == null) return;
        try {
            String value = mediaDetails.getText() == null ? "" : mediaDetails.getText().toString();
            if (value.isEmpty() || mediaDetails.getVisibility() != View.VISIBLE) {
                stopArtistMarquee();
                return;
            }
            float textWidth = mediaDetails.getPaint().measureText(value);
            float available = Math.max(0,
                    mediaDetails.getWidth() - mediaDetails.getPaddingLeft()
                            - mediaDetails.getPaddingRight());
            stopArtistMarquee();
            if (textWidth > available + dp(2)) {
                mediaDetails.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                mediaDetails.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
                mediaDetails.setHorizontallyScrolling(true);
                mediaDetails.setEllipsize(null);
                mediaDetails.setSelected(false);
                int overflow = Math.max(1, (int) Math.ceil(textWidth - available));
                mediaDetails.setScrollX(overflow);
                artistMarqueeAnimator = ObjectAnimator.ofInt(
                        mediaDetails, "scrollX", overflow, 0);
                long duration = Math.max(8000L,
                        Math.min(20000L, 7000L + overflow * 20L));
                artistMarqueeAnimator.setDuration(duration);
                artistMarqueeAnimator.setStartDelay(1200L);
                artistMarqueeAnimator.setRepeatMode(ObjectAnimator.REVERSE);
                artistMarqueeAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                artistMarqueeAnimator.setInterpolator(
                        new android.view.animation.LinearInterpolator());
                artistMarqueeAnimator.start();
            } else {
                mediaDetails.setGravity(Gravity.CENTER);
                mediaDetails.setHorizontallyScrolling(false);
                mediaDetails.setEllipsize(TextUtils.TruncateAt.END);
                mediaDetails.setSelected(false);
                mediaDetails.setScrollX(0);
            }
        } catch (Throwable ignored) {
        }
    }

    private void stopArtistMarquee() {
        try {
            if (artistMarqueeAnimator != null) {
                artistMarqueeAnimator.cancel();
                artistMarqueeAnimator = null;
            }
        } catch (Throwable ignored) {
        }
        if (mediaDetails != null) mediaDetails.setScrollX(0);
    }

    private void stopTitleMarquee() {
        try {
            if (titleMarqueeAnimator != null) {
                titleMarqueeAnimator.cancel();
                titleMarqueeAnimator = null;
            }
        } catch (Throwable ignored) {
        }
        if (mediaTitle != null) {
            mediaTitle.setScrollX(0);
        }
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

    private TextView mediaButton(String symbol) {
        TextView v = textView(symbol, 24, Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setClickable(true);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
        if (hasFocus && screenSleepPending && isScreenInteractive()) {
            wakeSaverDisplay();
        }
        if (hasFocus && !settingsDialogOpen) {
            hideSystemUi();
        }
    }

    private void showFatalError(Throwable t) {
        try {
            TextView error = textView(
                    "שומר מסך\n" + t.getClass().getSimpleName(),
                    22, Color.WHITE);
            error.setGravity(Gravity.CENTER);
            error.setBackgroundColor(Color.BLACK);
            setContentView(error);
        } catch (Throwable ignored) {
            finish();
        }
    }
}
