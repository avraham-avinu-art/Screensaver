package com.avrahamart.nightscreen;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * One shared screensaver view tree and renderer for Activity and Android Dream.
 * Host classes supply window/lifecycle and actions; they do not build a second UI.
 */
public final class ScreensaverDisplayView extends LinearLayout {
    public interface Actions {
        void onOpenSettings();
        void onPlayPause();
        void onPrevious();
        void onNext();
        void onSeekProgress(int progress);
    }

    private static final int dateTextColor = Color.WHITE;

    final SharedPreferences prefs;
    final Actions actions;

    final FrameLayout stageView;
    final LinearLayout clockBlockView;
    final LinearLayout mediaHolderView;
    final LinearLayout clockTextColumn;
    final TextView timeText;
    final AnalogClockView analogClockView;
    final TextView gregorianText;
    final TextView hebrewText;
    final BatteryChargeIndicatorView chargingBatteryView;
    final FrameLayout settingsButtonHitArea;
    final ImageView settingsButton;

    final LinearLayout mediaMirror;
    final LinearLayout playerTextColumn;
    final TextView mediaTitle;
    final TextView mediaDetails;
    final TextView mediaProgressText;
    final TextView mediaCurrentText;
    final TextView mediaTotalText;
    final LinearLayout mediaProgressRow;
    final SeekBar mediaSeekBar;
    final FrameLayout mediaControlsHolder;
    final ImageView mediaPlayPause;
    final ImageView mediaPrev;
    final ImageView mediaNext;

    int mediaProgressBaseWidth;
    private final Calendar now = Calendar.getInstance();
    private final SimpleDateFormat numericDateFormat =
            new SimpleDateFormat("dd.MM.yyyy", Locale.US);
    private String lastFormattedHebrewDateKey;
    private String lastFormattedHebrewDateFormat;
    private String cachedHebrewDateValue = "";
    private ObjectAnimator titleMarqueeAnimator;
    private ObjectAnimator artistMarqueeAnimator;
    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    public ScreensaverDisplayView(Context context, SharedPreferences preferences, Actions actions) {
        super(context);
        this.prefs = preferences;
        this.actions = actions;
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setBackgroundColor(getInt("screenBackgroundColor", Color.BLACK));
        setPadding(0, 0, 0, dp(4));
        setClipChildren(false);
        setClipToPadding(false);

        int initialBackground = getInt("screenBackgroundColor", Color.BLACK);
        stageView = new FrameLayout(context);
        stageView.setBackgroundColor(initialBackground);
        addView(stageView, new LinearLayout.LayoutParams(-1, 0, 1f));

        settingsButtonHitArea = new FrameLayout(context);
        settingsButtonHitArea.setBackgroundColor(Color.TRANSPARENT);
        settingsButtonHitArea.setVisibility(View.INVISIBLE);
        settingsButtonHitArea.setClickable(true);
        settingsButtonHitArea.setContentDescription(context.getString(R.string.main_activity_text_002));
        settingsButtonHitArea.setOnClickListener(v -> openSettings());
        settingsButton = new ImageView(context);
        settingsButton.setImageResource(R.drawable.ic_settings);
        settingsButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        settingsButton.setColorFilter(Color.rgb(247, 247, 247));
        settingsButton.setAlpha(1f);
        settingsButton.setPadding(dp(4), dp(4), dp(4), dp(4));
        settingsButton.setBackgroundColor(Color.TRANSPARENT);
        settingsButton.setContentDescription(context.getString(R.string.main_activity_text_002));
        settingsButton.setOnClickListener(v -> openSettings());
        settingsButtonHitArea.addView(settingsButton, new FrameLayout.LayoutParams(
                dp(36), dp(38), Gravity.TOP | Gravity.LEFT));
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(
                dp(72), dp(57), Gravity.TOP | Gravity.LEFT);
        settingsParams.leftMargin = dp(4);
        settingsParams.topMargin = dp(14);
        stageView.addView(settingsButtonHitArea, settingsParams);

        clockBlockView = new LinearLayout(context);
        clockBlockView.setOrientation(LinearLayout.VERTICAL);
        clockBlockView.setGravity(Gravity.CENTER_HORIZONTAL);
        clockBlockView.setClipChildren(false);
        clockBlockView.setClipToPadding(false);

        int initialClockSize = getInt("clockDateSize", 96);
        int initialClockHeight = Math.max(dp(62), dp(initialClockSize));
        timeText = clockDateTextView("--:--", initialClockSize, Color.rgb(247, 247, 247));
        timeText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        timeText.setGravity(Gravity.CENTER);
        timeText.setIncludeFontPadding(false);

        analogClockView = new AnalogClockView(context);
        analogClockView.setVisibility(View.GONE);
        LinearLayout.LayoutParams analogParams = new LinearLayout.LayoutParams(dp(120), dp(120));
        analogParams.gravity = Gravity.CENTER_HORIZONTAL;
        clockBlockView.addView(analogClockView, analogParams);

        clockTextColumn = new LinearLayout(context);
        clockTextColumn.setOrientation(LinearLayout.VERTICAL);
        clockTextColumn.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockTextColumn.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        clockTextColumn.setClipChildren(false);
        clockTextColumn.setClipToPadding(false);
        clockTextColumn.addView(timeText, new LinearLayout.LayoutParams(-1, initialClockHeight));

        gregorianText = clockDateTextView("", 22, Color.WHITE);
        gregorianText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        gregorianText.setGravity(Gravity.CENTER);
        gregorianText.setIncludeFontPadding(false);
        gregorianText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        int initialDateHeight = dateRowHeightForClockPreset(initialClockSize);
        LinearLayout.LayoutParams gregorianParams =
                new LinearLayout.LayoutParams(-1, initialDateHeight);
        gregorianParams.topMargin = dp(dateClockGapDp(initialClockSize));
        clockTextColumn.addView(gregorianText, gregorianParams);

        hebrewText = clockDateTextView("", 22, Color.WHITE);
        hebrewText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hebrewText.setGravity(Gravity.CENTER);
        hebrewText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        hebrewText.setIncludeFontPadding(false);
        clockTextColumn.addView(hebrewText,
                new LinearLayout.LayoutParams(-1, initialDateHeight));

        LinearLayout.LayoutParams clockColumnParams = new LinearLayout.LayoutParams(-2, -2);
        clockColumnParams.gravity = Gravity.CENTER_HORIZONTAL;
        clockBlockView.addView(clockTextColumn, clockColumnParams);

        chargingBatteryView = new BatteryChargeIndicatorView(context);
        chargingBatteryView.setVisibility(View.GONE);
        FrameLayout.LayoutParams batteryParams = new FrameLayout.LayoutParams(
                -2, dp(28), Gravity.TOP | Gravity.RIGHT);
        batteryParams.rightMargin = dp(12);
        batteryParams.topMargin = dp(12);
        stageView.addView(chargingBatteryView, batteryParams);

        int initialClockBlockHeight = initialClockHeight + initialDateHeight * 2
                + dp(dateClockGapDp(initialClockSize)) + dp(2);
        stageView.addView(clockBlockView, new FrameLayout.LayoutParams(
                -1, initialClockBlockHeight, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        mediaHolderView = new LinearLayout(context);
        mediaHolderView.setOrientation(LinearLayout.VERTICAL);
        mediaHolderView.setGravity(Gravity.CENTER_HORIZONTAL);
        stageView.addView(mediaHolderView, new FrameLayout.LayoutParams(-1, -2));

        mediaMirror = new LinearLayout(context);
        mediaMirror.setOrientation(LinearLayout.VERTICAL);
        mediaMirror.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mediaMirror.setPadding(0, 0, 0, 0);
        mediaMirror.setBackgroundColor(Color.TRANSPARENT);
        mediaMirror.setClipChildren(false);
        mediaMirror.setClipToPadding(false);
        mediaMirror.setVisibility(View.GONE);

        mediaTitle = textView("", 17, Color.WHITE);
        mediaTitle.setGravity(Gravity.CENTER);
        mediaTitle.setSingleLine(true);
        mediaTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        mediaDetails = textView("", 17, Color.rgb(175, 175, 175));
        mediaDetails.setVisibility(View.INVISIBLE);
        mediaDetails.setGravity(Gravity.CENTER);
        mediaDetails.setSingleLine(true);
        mediaDetails.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        mediaProgressText = textView("", 12, Color.WHITE);
        mediaProgressText.setVisibility(View.GONE);

        mediaProgressRow = new LinearLayout(context);
        mediaProgressRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaProgressRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mediaProgressRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        mediaProgressRow.setPadding(dp(4), 0, dp(4), 0);
        mediaProgressRow.setVisibility(View.GONE);

        mediaCurrentText = textView("0:00", 12, Color.WHITE);
        mediaCurrentText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        mediaCurrentText.setSingleLine(true);
        mediaCurrentText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(dp(48), dp(32));
        currentParams.rightMargin = dp(4);
        mediaProgressRow.addView(mediaCurrentText, currentParams);

        mediaSeekBar = new SeekBar(context);
        mediaSeekBar.setVisibility(View.GONE);
        mediaSeekBar.setPadding(dp(7), 0, dp(7), 0);
        mediaSeekBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        mediaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && ScreensaverDisplayView.this.actions != null) {
                    ScreensaverDisplayView.this.actions.onSeekProgress(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        mediaProgressBaseWidth = Math.max(dp(120),
                Math.round(getResources().getDisplayMetrics().widthPixels * 0.36f));
        mediaProgressRow.addView(mediaSeekBar,
                new LinearLayout.LayoutParams(mediaProgressBaseWidth, dp(32)));

        mediaTotalText = textView("0:00", 12, Color.WHITE);
        mediaTotalText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        mediaTotalText.setSingleLine(true);
        mediaTotalText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(dp(48), dp(32));
        totalParams.leftMargin = dp(4);
        mediaProgressRow.addView(mediaTotalText, totalParams);

        playerTextColumn = new LinearLayout(context);
        playerTextColumn.setOrientation(LinearLayout.VERTICAL);
        playerTextColumn.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        playerTextColumn.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        playerTextColumn.setClipChildren(false);
        playerTextColumn.setClipToPadding(false);
        playerTextColumn.addView(mediaDetails, new LinearLayout.LayoutParams(-1, dp(30)));
        playerTextColumn.addView(mediaTitle, new LinearLayout.LayoutParams(-1, dp(30)));
        LinearLayout.LayoutParams playerTextParams = new LinearLayout.LayoutParams(-2, -2);
        playerTextParams.gravity = Gravity.CENTER_HORIZONTAL;
        mediaMirror.addView(playerTextColumn, playerTextParams);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-2, dp(28));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        mediaMirror.addView(mediaProgressRow, progressParams);

        mediaControlsHolder = new FrameLayout(context);
        mediaControlsHolder.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int buttonSize = dp(44);
        int controlWidth = Math.max(dp(180),
                Math.round(getResources().getDisplayMetrics().widthPixels * 0.44f));
        LinearLayout.LayoutParams controlAreaParams = new LinearLayout.LayoutParams(controlWidth, dp(44));
        controlAreaParams.gravity = Gravity.CENTER_HORIZONTAL;
        controlAreaParams.topMargin = dp(-5);
        mediaNext = mediaIconButton(R.drawable.media_next, context.getString(R.string.main_activity_text_005));
        mediaNext.setOnClickListener(v -> { if (this.actions != null) this.actions.onPrevious(); });
        mediaControlsHolder.addView(mediaNext,
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.LEFT | Gravity.CENTER_VERTICAL));
        mediaPlayPause = mediaIconButton(R.drawable.media_play, context.getString(R.string.main_activity_text_006));
        mediaPlayPause.setOnClickListener(v -> { if (this.actions != null) this.actions.onPlayPause(); });
        mediaControlsHolder.addView(mediaPlayPause,
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.CENTER));
        mediaPrev = mediaIconButton(R.drawable.media_previous, context.getString(R.string.main_activity_text_007));
        mediaPrev.setOnClickListener(v -> { if (this.actions != null) this.actions.onNext(); });
        mediaControlsHolder.addView(mediaPrev,
                new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        mediaMirror.addView(mediaControlsHolder, controlAreaParams);

        LinearLayout.LayoutParams mirrorParams = new LinearLayout.LayoutParams(
                Math.min(dp(760), Math.max(dp(1), getResources().getDisplayMetrics().widthPixels)),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mirrorParams.gravity = Gravity.CENTER_HORIZONTAL;
        mirrorParams.topMargin = dp(28);
        mediaHolderView.addView(mediaMirror, mirrorParams);

        stageView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) post(this::updateScreenPositions);
        });
        post(this::updateScreenPositions);
    }

    private void openSettings() {
        if (actions != null) actions.onOpenSettings();
    }

    public void setPlayerVisible(boolean visible) {
        mediaMirror.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            stopTitleMarquee();
            stopArtistMarquee();
        }
        mediaMirror.requestLayout();
        updateScreenPositions();
    }

    public void setMediaText(String title, String artist, boolean showArtist) {
        mediaTitle.setText(title == null ? "" : title);
        mediaDetails.setText(artist == null ? "" : artist);
        mediaDetails.setVisibility(showArtist && artist != null && !artist.isEmpty()
                ? View.VISIBLE : View.GONE);
        updatePlayerTextColumnWidth();
        mediaTitle.post(this::updateTitleMarqueeIfNeeded);
        mediaDetails.post(this::updateArtistMarqueeIfNeeded);
    }

    public void updateMediaProgress(long positionMs, long durationMs) {
        if (durationMs <= 0L || mediaProgressRow.getVisibility() != View.VISIBLE) {
            setProgressRowVisible(false);
            return;
        }
        long clamped = Math.max(0L, Math.min(positionMs, durationMs));
        mediaCurrentText.setText(formatMs(clamped));
        mediaTotalText.setText(formatMs(durationMs));
        mediaSeekBar.setMax(1000);
        mediaSeekBar.setProgress((int) Math.min(1000L, (clamped * 1000L) / durationMs));
        setProgressRowVisible(true);
    }

    public void setMediaControls(boolean enabled, boolean playing,
                                 boolean canPrevious, boolean canNext) {
        mediaControlsHolder.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mediaPlayPause.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mediaPrev.setVisibility(enabled && canNext ? View.VISIBLE : View.GONE);
        mediaNext.setVisibility(enabled && canPrevious ? View.VISIBLE : View.GONE);
        mediaPlayPause.setImageResource(playing ? R.drawable.media_pause : R.drawable.media_play);
        mediaPlayPause.setContentDescription(getString(playing
                ? R.string.main_activity_text_031 : R.string.main_activity_text_006));
        int color = getInt("fontColor", Color.WHITE);
        for (ImageView button : new ImageView[]{mediaPlayPause, mediaPrev, mediaNext}) {
            if (button.getDrawable() != null) button.getDrawable().setTint(color);
        }
    }

    public void setMediaArtwork(Bitmap artwork, boolean active) {
        applyMediaFrame(active, artwork);
    }

    public void setProgressRowVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        mediaProgressRow.setVisibility(visibility);
        mediaSeekBar.setVisibility(visibility);
        mediaCurrentText.setVisibility(visibility);
        mediaTotalText.setVisibility(visibility);
    }

    public void showSettingsButtonForThreeSeconds() {
        settingsButtonHitArea.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideSettingsButtonRunnable);
        if (!getBool("settingsButtonAlwaysVisible", false)) {
            handler.postDelayed(hideSettingsButtonRunnable, 3000L);
        }
    }

    private void hideSettingsButton() {
        if (settingsButtonHitArea != null) {
            settingsButtonHitArea.setVisibility(View.INVISIBLE);
        }
    }

    private final Runnable hideSettingsButtonRunnable = this::hideSettingsButton;

    private boolean getBool(String key, boolean def) {
        return prefs == null ? def : prefs.getBoolean(key, def);
    }

    private int getInt(String key, int def) {
        return prefs == null ? def : prefs.getInt(key, def);
    }

    private String getString(String key, String def) {
        return prefs == null ? def : prefs.getString(key, def);
    }

    private String getString(int resId) {
        return getContext().getString(resId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatMs(long ms) {
        long seconds = Math.max(0L, ms / 1000L);
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    public void updateScreenPositions() {
        updateScreenPositions(stageView, clockBlockView, mediaHolderView);
    }

    private void updateScreenPositions(FrameLayout stage, View clockBlock, View mediaHolder) {
        if (stage == null) return;
        try {
            int h = stage.getHeight();
            int w = stage.getWidth();
            if (h <= 0 || w <= 0) return;

            FrameLayout.LayoutParams restoreClockParams =
                    (FrameLayout.LayoutParams) clockBlock.getLayoutParams();
            restoreClockParams.width = -1;
            restoreClockParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            clockBlock.setLayoutParams(restoreClockParams);
            mediaProgressBaseWidth = Math.max(dp(120),
                    Math.round(getResources().getDisplayMetrics().widthPixels * 0.36f));
            FrameLayout.LayoutParams restoreMediaParams =
                    (FrameLayout.LayoutParams) mediaHolder.getLayoutParams();
            restoreMediaParams.width = -1;
            restoreMediaParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            mediaHolder.setLayoutParams(restoreMediaParams);
            if (mediaMirror != null && mediaMirror.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams mirrorParams =
                        (LinearLayout.LayoutParams) mediaMirror.getLayoutParams();
                mirrorParams.width = Math.min(dp(760), Math.max(dp(1), w));
                mediaMirror.setLayoutParams(mirrorParams);
                updatePlayerTextColumnWidth();
            }
            if (mediaSeekBar != null && mediaSeekBar.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams seekParams =
                        (LinearLayout.LayoutParams) mediaSeekBar.getLayoutParams();
                float playerScale = Math.max(0.72f, Math.min(1.45f,
                        getInt("playerSize", 17) / 17f));
                seekParams.width = Math.max(dp(80), Math.round(mediaProgressBaseWidth * playerScale));
                mediaSeekBar.setLayoutParams(seekParams);
            }

            String clockPos = getString("clockDatePosition", "top");
            String playerPos = getString("playerPosition", "bottom");

            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) clockBlock.getLayoutParams();
            int blockHeight = Math.max(0, clockBlock.getMeasuredHeight());
            int clockLineHeight = blockHeight;
            if (analogClockView != null && analogClockView.getVisibility() == View.VISIBLE) {
                ViewGroup.LayoutParams analogParams = analogClockView.getLayoutParams();
                if (analogParams != null && analogParams.height > 0) {
                    clockLineHeight = analogParams.height;
                } else if (analogClockView.getMeasuredHeight() > 0) {
                    clockLineHeight = analogClockView.getMeasuredHeight();
                }
            } else if (timeText != null) {
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
            float clockOffset = Math.max(-1f, Math.min(1f,
                    getInt("clockHorizontalOffset", 0) / 20f));
            // The outer clockBlock intentionally spans the full screen to keep
            // its content column centered. Move its visible children together,
            // using their actual content width rather than the full-width parent.
            clockBlock.setTranslationX(0f);
            translateCenteredChildrenToOffset((ViewGroup) clockBlock, stage.getWidth(), clockOffset);
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
            float playerOffset = Math.max(-1f, Math.min(1f,
                    getInt("playerHorizontalOffset", 0) / 20f));
            // mediaHolder also spans the full screen. Its visible player rows
            // are centered inside mediaMirror, so translate those rows as one group.
            mediaHolder.setTranslationX(0f);
            if (mediaMirror != null) {
                translateCenteredChildrenToOffset(mediaMirror, stage.getWidth(), playerOffset);
            }
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
                // "Lower quarter" is positioned 20% above the bottom edge.
                mp.topMargin = Math.max(0, Math.round((h * 0.65f) - mediaH / 2f));
                mp.bottomMargin = 0;
                mp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            }
            mediaHolder.setLayoutParams(mp);
        } catch (Throwable ignored) {}
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

    public void applySettings(Window window, boolean hasActiveMedia, Bitmap artwork) {
        if (prefs == null || gregorianText == null) return;

        applyScreenAppearance(window);
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
        boolean analogClock = "analog".equals(getString("clockDisplayMode", "digital"));
        String analogDialStyle = getString("clockDialStyle", "classic");
        float analogSizeMultiplier = "numbers_only".equals(analogDialStyle)
                ? 2.85f : 2.25f;
        int analogClockDiameter = Math.max(dp(170),
                dp(Math.round(effectiveClockSize * analogSizeMultiplier)));
        if (analogClockView != null) {
            analogClockView.setDialStyle(getString("clockDialStyle", "classic"));
            analogClockView.setColors(color, getScreenBackgroundColor());
            analogClockView.setVisibility(analogClock ? View.VISIBLE : View.GONE);
            ViewGroup.LayoutParams analogParams = analogClockView.getLayoutParams();
            if (analogParams != null) {
                analogParams.width = analogClock ? analogClockDiameter : 0;
                analogParams.height = analogClock ? analogClockDiameter : 0;
                analogClockView.setLayoutParams(analogParams);
            }
        }
        if (timeText != null) timeText.setVisibility(analogClock ? View.GONE : View.VISIBLE);
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
        int clockH = analogClock ? analogClockDiameter
                : Math.max(dp(62), measuredLineHeight(
                        timeText, "0123456789:00", 2, effectiveClockSize));
        int gregorianH = Math.max(dateRowHeightForClockPreset(effectiveClockSize),
                measuredLineHeight(gregorianText, getString(R.string.main_activity_text_008), 0, 24));
        int hebrewH = Math.max(dateRowHeightForClockPreset(effectiveClockSize),
                measuredLineHeight(hebrewText, getString(R.string.main_activity_text_009), 0, 24));

        // Custom fonts can declare unusually tall font metrics even when the painted
        // characters are not tall. A fixed negative margin therefore leaves different
        // visible gaps for different font families. Calculate each margin from the actual
        // painted glyph bounds so the visible gap, rather than the font's empty line area,
        // is consistent between clock and date rows.
        int clockToDateGapPx = dp(visualGapDp(effectiveClockSize));
        int dateToDateGapPx = dp(visualGapDp(effectiveClockSize) / 2);
        int gregorianTopMargin = 0;
        int hebrewTopMargin = 0;
        // The numbers-only hands end at 72% of the dial radius, while the
        // other styles' outer tick marks reach 93%. First compensate for that
        // shorter visual extent, then reduce the remaining visible gap by one-third.
        int numbersOnlyDateOverlap = analogClock && "numbers_only".equals(analogDialStyle)
                ? Math.round(analogClockDiameter * 0.455f * (0.93f - 0.72f)) : 0;
        if (numbersOnlyDateOverlap > 0 && (showGregorian || showHebrew)) {
            TextView firstDateView = showGregorian ? gregorianText : hebrewText;
            int firstDateHeight = showGregorian ? gregorianH : hebrewH;
            String firstDateSample = showGregorian
                    ? getString(R.string.main_activity_text_008)
                    : getString(R.string.main_activity_text_009);
            int clockVisibleBottom = Math.round(analogClockDiameter * 0.5f
                    + analogClockDiameter * 0.455f * 0.72f);
            int dateVisibleTop = analogClockDiameter - numbersOnlyDateOverlap
                    + glyphTopInsetPx(firstDateView, firstDateHeight, firstDateSample);
            int currentVisibleGap = Math.max(0, dateVisibleTop - clockVisibleBottom);
            numbersOnlyDateOverlap += Math.round(currentVisibleGap / 3f);
        }
        if (showGregorian) {
            gregorianTopMargin = analogClock ? -numbersOnlyDateOverlap
                    : visualGapMarginPx(
                            timeText, clockH, "0123456789:00",
                            gregorianText, gregorianH, getString(R.string.main_activity_text_008),
                            clockToDateGapPx);
        }
        if (showHebrew) {
            if (showGregorian) {
                hebrewTopMargin = visualGapMarginPx(
                        gregorianText, gregorianH, getString(R.string.main_activity_text_008),
                        hebrewText, hebrewH, getString(R.string.main_activity_text_009),
                        dateToDateGapPx);
            } else if (analogClock) {
                hebrewTopMargin = -numbersOnlyDateOverlap;
            } else {
                hebrewTopMargin = visualGapMarginPx(
                        timeText, clockH, "0123456789:00",
                        hebrewText, hebrewH, getString(R.string.main_activity_text_009),
                        clockToDateGapPx);
            }
        }

        LinearLayout.LayoutParams tp = (LinearLayout.LayoutParams) timeText.getLayoutParams();
        tp.height = analogClock ? 0 : clockH;
        timeText.setLayoutParams(tp);
        LinearLayout.LayoutParams gp = (LinearLayout.LayoutParams) gregorianText.getLayoutParams();
        gp.height = gregorianH;
        gp.topMargin = showGregorian ? gregorianTopMargin : 0;
        gregorianText.setLayoutParams(gp);
        LinearLayout.LayoutParams hp = (LinearLayout.LayoutParams) hebrewText.getLayoutParams();
        hp.height = hebrewH;
        hp.topMargin = showHebrew ? hebrewTopMargin : 0;
        hebrewText.setLayoutParams(hp);

        View clockParent = clockBlockView;
        if (clockParent instanceof LinearLayout) {
            LinearLayout cb = (LinearLayout) clockParent;
            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) cb.getLayoutParams();
            updateChargingBatteryDisplay();
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

            applyMediaFrame(hasActiveMedia, artwork);

            mediaNext.setBackgroundColor(Color.TRANSPARENT);
            mediaPrev.setBackgroundColor(Color.TRANSPARENT);
            mediaPlayPause.setBackgroundColor(Color.TRANSPARENT);
            if (mediaNext.getDrawable() != null) mediaNext.getDrawable().setTint(color);
            if (mediaPrev.getDrawable() != null) mediaPrev.getDrawable().setTint(color);
            if (mediaPlayPause.getDrawable() != null) mediaPlayPause.getDrawable().setTint(color);
            View parent = mediaMirror.getParent() instanceof View ? (View) mediaMirror.getParent() : null;
            if (parent != null && parent.getParent() instanceof FrameLayout) {
                FrameLayout stage = (FrameLayout) parent.getParent();
                updateScreenPositions();
            }
        }
        updateClock();
        applySectionTextAlignmentPreferences();
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
            Intent battery = getContext().registerReceiver(null,
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

    public void applyScreenAppearance(Window window) {
        int backgroundColor = getScreenBackgroundColor();
        Drawable screenBackground = createScreenBackgroundDrawable();
        if (stageView != null) {
            stageView.setBackground(screenBackground);
            if (stageView.getParent() instanceof View) {
                ((View) stageView.getParent()).setBackground(createScreenBackgroundDrawable());
            }
        }
        window.getDecorView().setBackground(createScreenBackgroundDrawable());

        // -1 means follow the device's brightness setting. A percentage applies
        // only to this Activity and does not modify the user's system setting.
        int brightnessPercent = getInt("screenBrightness", -1);
        float requestedBrightness = brightnessPercent < 0 ? -1f
                : Math.max(0.01f, Math.min(1.0f, brightnessPercent / 100.0f));
        float brightnessCap = 1f;
        if (getBool("batteryProtectionEnabled", false) && isBatteryProtectionNeeded()) {
            brightnessCap = Math.min(brightnessCap, 0.25f);
        }
        if (isAutomaticNightModeActive()) {
            brightnessCap = Math.min(brightnessCap, 0.20f);
        }
        if (brightnessCap < 1f) {
            // These caps affect only this Activity window, never system-wide brightness.
            requestedBrightness = requestedBrightness < 0f
                    ? brightnessCap : Math.min(requestedBrightness, brightnessCap);
        }
        android.view.WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.screenBrightness = requestedBrightness < 0f
                ? android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                : requestedBrightness;
        window.setAttributes(attributes);
    }

    public void updateClockBlockHeightForChargingIndicator() {
        if (clockBlockView == null || timeText == null) return;
        int totalHeight = 0;
        for (int i = 0; i < clockBlockView.getChildCount(); i++) {
            View child = clockBlockView.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            ViewGroup.LayoutParams rawParams = child.getLayoutParams();
            int childHeight = child.getMeasuredHeight();
            int topMargin = 0;
            int bottomMargin = 0;
            if (rawParams instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) rawParams;
                if (params.height >= 0) childHeight = params.height;
                topMargin = params.topMargin;
                bottomMargin = params.bottomMargin;
            } else if (rawParams != null && rawParams.height >= 0) {
                childHeight = rawParams.height;
            }
            totalHeight += Math.max(0, childHeight) + topMargin + bottomMargin;
        }
        if (totalHeight <= 0) return;
        FrameLayout.LayoutParams blockParams =
                (FrameLayout.LayoutParams) clockBlockView.getLayoutParams();
        if (blockParams != null && blockParams.height != totalHeight) {
            blockParams.height = totalHeight;
            clockBlockView.setLayoutParams(blockParams);
            clockBlockView.post(() -> updateScreenPositions(
                    stageView, clockBlockView, mediaHolderView));
        }
    }

    public void updateChargingBatteryDisplay() {
        if (chargingBatteryView == null || prefs == null) return;
        int previousVisibility = chargingBatteryView.getVisibility();
        String mode = getString("chargingBatteryDisplayMode", "off");
        if ("off".equals(mode)) {
            chargingBatteryView.setBatteryState("off", 0, false,
                    getInt("fontColor", dateTextColor));
            return;
        }
        int percent = 0;
        boolean pluggedIn = false;
        try {
            Intent battery = getContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    percent = Math.max(0, Math.min(100,
                            Math.round(level * 100f / scale)));
                }
                int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                pluggedIn = plugged != 0
                        || status == BatteryManager.BATTERY_STATUS_CHARGING;
            }
        } catch (Throwable ignored) {
        }
        chargingBatteryView.setBatteryState(mode, percent, pluggedIn,
                getInt("fontColor", dateTextColor));
        if (previousVisibility != chargingBatteryView.getVisibility()) {
            updateClockBlockHeightForChargingIndicator();
        }
    }

    public void updateClock() {
        if (timeText == null || gregorianText == null || hebrewText == null) return;
        now.setTimeZone(java.util.TimeZone.getDefault());
        numericDateFormat.setTimeZone(now.getTimeZone());
        now.setTimeInMillis(System.currentTimeMillis());
        String formattedTime = numericTime();
        if (!formattedTime.contentEquals(timeText.getText())) {
            timeText.setText(formattedTime);
        }
        if (analogClockView != null) {
            analogClockView.setTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }
        updateChargingBatteryDisplay();

        boolean showGregorian = getBool("showGregorian", true);
        boolean showHebrew = getBool("showHebrew", true);

        String formattedGregorianDate = showGregorian
                ? hebrewWeekday() + " " + formattedGregorianDateValue()
                : "";
        if (!formattedGregorianDate.contentEquals(gregorianText.getText())) {
            gregorianText.setText(formattedGregorianDate);
        }

        String hebrewDateKey = now.get(Calendar.ERA) + "-"
                + now.get(Calendar.YEAR) + "-"
                + now.get(Calendar.DAY_OF_YEAR);
        String selectedHebrewDateFormat = getString("hebrewDateFormat", "plain");
        if (!hebrewDateKey.equals(lastFormattedHebrewDateKey)
                || !selectedHebrewDateFormat.equals(lastFormattedHebrewDateFormat)) {
            try {
                JewishDate jewishDate = new JewishDate(now);
                HebrewDateFormatter formatter = new HebrewDateFormatter();
                formatter.setHebrewFormat(true);
                formatter.setUseGershGershayim(true);
                formatter.setUseLongHebrewYears(false);
                cachedHebrewDateValue = formatHebrewDate(
                        formatter.format(jewishDate), selectedHebrewDateFormat);
            } catch (Throwable t) {
                cachedHebrewDateValue = "";
            }
            lastFormattedHebrewDateKey = hebrewDateKey;
            lastFormattedHebrewDateFormat = selectedHebrewDateFormat;
        }

        String formattedHebrewDate = cachedHebrewDateValue;
        if (showHebrew && !showGregorian && !formattedHebrewDate.isEmpty()) {
            formattedHebrewDate = hebrewWeekday() + " " + formattedHebrewDate;
        }
        if (!showHebrew) formattedHebrewDate = "";
        if (!formattedHebrewDate.contentEquals(hebrewText.getText())) {
            hebrewText.setText(formattedHebrewDate);
        }
        updateClockTextColumnWidth();
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

    public void updateClockTextColumnWidth() {
        if (clockTextColumn == null) return;
        int screenWidth = stageView != null && stageView.getWidth() > 0
                ? stageView.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        int available = Math.max(dp(1), screenWidth - dp(24));
        int width = measuredTextColumnWidth(
                new TextView[]{timeText, gregorianText, hebrewText}, available);
        ViewGroup.LayoutParams raw = clockTextColumn.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) raw;
            if (params.width != width) {
                params.width = width;
                clockTextColumn.setLayoutParams(params);
            }
        }
    }

    public void updatePlayerTextColumnWidth() {
        if (playerTextColumn == null || mediaMirror == null) return;
        int available = mediaMirror.getWidth();
        if (available <= 0) {
            available = Math.min(dp(760),
                    Math.max(dp(1), getResources().getDisplayMetrics().widthPixels - dp(20)));
        }
        int width = measuredTextColumnWidth(
                new TextView[]{mediaDetails, mediaTitle}, available);
        ViewGroup.LayoutParams raw = playerTextColumn.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) raw;
            if (params.width != width) {
                params.width = width;
                playerTextColumn.setLayoutParams(params);
                playerTextColumn.post(() -> {
                    updateTitleMarqueeIfNeeded();
                    updateArtistMarqueeIfNeeded();
                });
            }
        }
    }

    public void applySectionTextAlignmentPreferences() {
        int clockGravity = horizontalGravityForTextMode(
                getString("clockTextAlignment", "center"));
        applyHorizontalTextGravity(timeText, clockGravity);
        applyHorizontalTextGravity(gregorianText, clockGravity);
        applyHorizontalTextGravity(hebrewText, clockGravity);
        updateClockTextColumnWidth();

        int playerGravity = playerTextHorizontalGravity();
        applyHorizontalTextGravity(mediaTitle, playerGravity);
        applyHorizontalTextGravity(mediaDetails, playerGravity);
        updatePlayerTextColumnWidth();
    }

    private int blendWithBlack(int color, float factor) {
        float f = Math.max(0f, Math.min(1f, factor));
        return Color.rgb(
                Math.round(Color.red(color) * f),
                Math.round(Color.green(color) * f),
                Math.round(Color.blue(color) * f));
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
            return Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fileName);
        } catch (Exception ignored) {
            // Keep the screensaver readable if an asset is unexpectedly missing.
            return fallback;
        }
    }

    private TextView textView(String text, float size, int color) {
        TextView v = new TextView(getContext());
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private ImageView mediaIconButton(int drawableRes, String description) {
        ImageView v = new ImageView(getContext());
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
        ClockDateTextView view = new ClockDateTextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return view;
    }

    public void updateTitleMarqueeIfNeeded() {
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
                mediaTitle.setGravity(playerTextHorizontalGravity() | Gravity.CENTER_VERTICAL);
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
                mediaTitle.setGravity(playerTextHorizontalGravity() | Gravity.CENTER_VERTICAL);
                mediaTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
                mediaTitle.setHorizontallyScrolling(false);
                mediaTitle.setEllipsize(TextUtils.TruncateAt.END);
                mediaTitle.setSelected(false);
                mediaTitle.setScrollX(0);
            }
        } catch (Throwable ignored) {
        }
    }

    public void updateArtistMarqueeIfNeeded() {
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
                mediaDetails.setGravity(playerTextHorizontalGravity() | Gravity.CENTER_VERTICAL);
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
                mediaDetails.setGravity(playerTextHorizontalGravity() | Gravity.CENTER_VERTICAL);
                mediaDetails.setHorizontallyScrolling(false);
                mediaDetails.setEllipsize(TextUtils.TruncateAt.END);
                mediaDetails.setSelected(false);
                mediaDetails.setScrollX(0);
            }
        } catch (Throwable ignored) {
        }
    }

    public void stopArtistMarquee() {
        try {
            if (artistMarqueeAnimator != null) {
                artistMarqueeAnimator.cancel();
                artistMarqueeAnimator = null;
            }
        } catch (Throwable ignored) {
        }
        if (mediaDetails != null) mediaDetails.setScrollX(0);
    }

    public void stopTitleMarquee() {
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

    public void applyMediaFrame(boolean hasActiveSong, Bitmap artwork) {
        if (mediaMirror == null) return;
        boolean enabled = getBool("frameEnabled", false) && hasActiveSong;
        if (!enabled) {
            mediaMirror.setBackgroundColor(Color.TRANSPARENT);
            mediaMirror.setPadding(0, 0, 0, 0);
            if (Build.VERSION.SDK_INT >= 21) mediaMirror.setClipToOutline(false);
            return;
        }
        try {
            int radius = Math.max(0, getInt("frameRadius", 10));
            int selectedWidth = Math.max(1, Math.min(3, getInt("frameWidth", 1)));
            int frameColor = getInt("frameColor", Color.WHITE);
            float strokeWidthDp = selectedWidth == 1 ? 1.0f
                    : selectedWidth == 2 ? 1.25f : 1.5f;
            int strokeWidthPx = Math.max(1, Math.round(
                    strokeWidthDp * getResources().getDisplayMetrics().density));
            GradientDrawable base = new GradientDrawable();
            base.setColor(Color.rgb(20, 36, 56));
            base.setCornerRadius(dp(radius));
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(radius));
            border.setStroke(strokeWidthPx, frameColor);
            boolean useAlbumArt = getBool("frameUseAlbumArt", true)
                    && artwork != null && !artwork.isRecycled();
            if (useAlbumArt) {
                // Draw album art as a center-cropped shader inside the already measured
                // frame bounds. The source bitmap never affects the frame dimensions.
                CenterCropBitmapDrawable artworkLayer =
                        new CenterCropBitmapDrawable(artwork);
                artworkLayer.setAlpha(185);
                GradientDrawable textScrim = new GradientDrawable();
                textScrim.setColor(Color.argb(126, 9, 19, 33));
                textScrim.setCornerRadius(dp(radius));
                mediaMirror.setBackground(new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{
                                base, artworkLayer, textScrim, border
                        }));
            } else {
                mediaMirror.setBackground(new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{base, border}));
            }
            if (Build.VERSION.SDK_INT >= 21) mediaMirror.setClipToOutline(true);
            int inset = Math.max(dp(5), strokeWidthPx + dp(3));
            mediaMirror.setPadding(inset, inset, inset, inset);
        } catch (Throwable ignored) {
            mediaMirror.setBackgroundColor(Color.TRANSPARENT);
            mediaMirror.setPadding(0, 0, 0, 0);
            if (Build.VERSION.SDK_INT >= 21) mediaMirror.setClipToOutline(false);
        }
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
}
