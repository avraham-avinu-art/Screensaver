package com.avrahamart.nightscreen;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class ScreensaverDreamService extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Calendar now = Calendar.getInstance();
    private TextView timeView;
    private TextView gregorianView;
    private TextView hebrewView;
    private boolean dreaming = false;
    private FrameLayout root;
    private LinearLayout clockBlock;
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            if (!dreaming) return;
            if (!isChargeDreamEnabled()) {
                finish();
                return;
            }
            updateClock();
            updatePosition();
            handler.postDelayed(this, 60_000L);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!prefs().getBoolean("useAndroidDreamForCharging", false)) {
            finish();
            return;
        }
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        buildDreamUi();
        updateClock();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        dreaming = true;
        updateClock();
        updatePosition();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override
    public void onDreamingStopped() {
        dreaming = false;
        handler.removeCallbacks(refreshRunnable);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        dreaming = false;
        handler.removeCallbacks(refreshRunnable);
        super.onDetachedFromWindow();
    }

    private void buildDreamUi() {
        root = new FrameLayout(this);
        int background = prefs().getInt("screenBackgroundColor", Color.BLACK);
        root.setBackgroundColor(background);

        clockBlock = new LinearLayout(this);
        clockBlock.setOrientation(LinearLayout.VERTICAL);
        clockBlock.setGravity(Gravity.CENTER_HORIZONTAL);

        timeView = makeText("--:--", prefs().getInt("clockDateSize", 84), Color.WHITE);
        timeView.setGravity(Gravity.CENTER);
        clockBlock.addView(timeView, new LinearLayout.LayoutParams(-1, -2));

        gregorianView = makeText("", 22, Color.WHITE);
        gregorianView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gregorianParams = new LinearLayout.LayoutParams(-1, -2);
        gregorianParams.topMargin = dp(10);
        clockBlock.addView(gregorianView, gregorianParams);

        hebrewView = makeText("", 22, Color.WHITE);
        hebrewView.setGravity(Gravity.CENTER);
        clockBlock.addView(hebrewView, new LinearLayout.LayoutParams(-1, -2));

        root.addView(clockBlock, new FrameLayout.LayoutParams(-1, -2,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        setContentView(root);
        applyTextSettings();
    }

    
    private void applyTextSettings() {
        android.content.SharedPreferences p = prefs();
        int color = p.getInt("fontColor", Color.rgb(220, 220, 220));
        int clockSize = p.getInt("clockDateSize", 84);
        String family = p.getString("fontFamily", "sans-serif");
        Typeface face = Typeface.create("sans-serif", Typeface.NORMAL);
        if (family != null && family.startsWith("custom:")) {
            try {
                face = Typeface.createFromAsset(getAssets(),
                        "fonts/" + family.substring("custom:".length()));
            } catch (Throwable ignored) {}
        } else if (family != null) {
            face = Typeface.create(family, Typeface.NORMAL);
        }
        int weight = p.getInt("fontWeight", 0);
        if (weight >= 1) face = Typeface.create(face, Typeface.BOLD);
        timeView.setTextColor(color);
        gregorianView.setTextColor(color);
        hebrewView.setTextColor(color);
        timeView.setTypeface(face);
        gregorianView.setTypeface(face);
        hebrewView.setTypeface(face);
        timeView.setTextSize(clockSize);
        gregorianView.setTextSize(clockSize * 0.26f);
        hebrewView.setTextSize(clockSize * 0.26f);
        gregorianView.setVisibility(p.getBoolean("showGregorian", true) ? View.VISIBLE : View.GONE);
        hebrewView.setVisibility(p.getBoolean("showHebrew", true) ? View.VISIBLE : View.GONE);
    }

    private boolean isChargeDreamEnabled() {
        android.content.SharedPreferences settings = prefs();
        return settings.getBoolean("useAndroidDreamForCharging", true)
                && settings.getBoolean("autoStartOnCharge", false);
    }

    private void updateClock() {
        if (timeView == null) return;
        now.setTimeInMillis(System.currentTimeMillis());
        boolean use24 = "24".equals(prefs().getString("timeFormat", "24"));
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (use24) {
            timeView.setText(String.format(Locale.US, "%d:%02d",
                    hour, now.get(Calendar.MINUTE)));
        } else {
            int twelveHour = hour % 12;
            if (twelveHour == 0) twelveHour = 12;
            timeView.setText(String.format(Locale.US, "%d:%02d",
                    twelveHour, now.get(Calendar.MINUTE))
                    + (hour < 12 ? " לפנה״צ" : " אחה״צ"));
        }

        try {
            String pattern = prefs().getString("gregorianDateFormat", "dd.MM.yyyy");
            if (pattern == null || pattern.trim().isEmpty()) pattern = "dd.MM.yyyy";
            Locale locale = "d 'ב'MMMM yyyy".equals(pattern)
                    ? new Locale("he", "IL") : Locale.US;
            gregorianView.setText(hebrewWeekday() + " · "
                    + new SimpleDateFormat(pattern, locale).format(now.getTime()));
        } catch (Throwable ignored) {
            gregorianView.setText(hebrewWeekday() + " · "
                    + new SimpleDateFormat("dd.MM.yyyy", Locale.US).format(now.getTime()));
        }

        try {
            JewishDate date = new JewishDate(now);
            HebrewDateFormatter formatter = new HebrewDateFormatter();
            formatter.setHebrewFormat(true);
            formatter.setUseGershGershayim(true);
            formatter.setUseLongHebrewYears(false);
            String value = formatter.format(date)
                    .replace("מרחשוון", "חשוון")
                    .replace("מרחשון", "חשוון")
                    .replace("חשון", "חשוון");
            hebrewView.setText(value);
        } catch (Throwable ignored) {
            hebrewView.setText("");
        }
    }

    private void updatePosition() {
        if (root == null || clockBlock == null) return;
        int height = root.getHeight();
        if (height <= 0) return;
        String position = prefs().getString("clockDatePosition", "upper");
        int blockHeight = Math.max(clockBlock.getMeasuredHeight(), dp(80));
        int top;
        if ("top".equals(position)) {
            top = 0;
        } else if ("middle".equals(position)) {
            top = Math.max(0, Math.round(height * 0.5f - blockHeight * 0.5f));
        } else {
            top = Math.max(0, Math.round(height * 0.25f - blockHeight * 0.5f));
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockBlock.getLayoutParams();
        params.topMargin = top;
        clockBlock.setTranslationX(height == 0 ? 0f : root.getWidth()
                * Math.max(-0.20f, Math.min(0.20f,
                prefs().getInt("clockHorizontalOffset", 0) / 100f)));
        clockBlock.setLayoutParams(params);
    }

    private TextView makeText(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        view.setIncludeFontPadding(false);
        return view;
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("settings", MODE_PRIVATE);
    }

    private String hebrewWeekday() {
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY: return "יום א׳";
            case Calendar.MONDAY: return "יום ב׳";
            case Calendar.TUESDAY: return "יום ג׳";
            case Calendar.WEDNESDAY: return "יום ד׳";
            case Calendar.THURSDAY: return "יום ה׳";
            case Calendar.FRIDAY: return "יום ו׳";
            default: return "שבת";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
