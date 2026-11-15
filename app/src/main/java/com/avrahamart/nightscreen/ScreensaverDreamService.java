package com.avrahamart.nightscreen;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

/**
 * Native Android Dream implementation. Android owns the lifecycle and this
 * service renders directly into the Dream window instead of launching an
 * Activity that would end the Dream session.
 */
public final class ScreensaverDreamService extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Calendar now = Calendar.getInstance();
    private SharedPreferences prefs;
    private TextView digitalClock;
    private TextView gregorianDate;
    private TextView hebrewDate;
    private AnalogClockView analogClock;
    private View clockBlock;
    private int clockLineHeightPx;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!isDreaming()) return;
            updateClock();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        buildDreamContent();
        updateClock();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(clockTick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(clockTick);
        super.onDetachedFromWindow();
    }

    private void buildDreamContent() {
        int background = prefs.getInt("screenBackgroundColor", Color.BLACK);
        int foreground = prefs.getInt("fontColor", Color.WHITE);
        int requestedSize = prefs.getInt("clockDateSize", 84);
        String fontFamily = prefs.getString("fontFamily", "sans-serif");
        int effectiveSize = effectiveClockDateSize(fontFamily, requestedSize);
        int requestedWeight = prefs.getInt("fontWeight", 1);
        Typeface face = loadTypeface(fontFamily, requestedWeight);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(background);

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER_HORIZONTAL);
        block.setClipChildren(false);
        block.setClipToPadding(false);
        clockBlock = block;

        boolean analog = "analog".equals(prefs.getString("clockDisplayMode", "digital"));
        int analogDiameter = dp(Math.max(170,
                Math.round(effectiveSize * ("numbers_only".equals(
                        prefs.getString("clockDialStyle", "lines_only")) ? 2.50f : 2.25f))));
        if (analog) {
            analogClock = new AnalogClockView(this);
            analogClock.setDialStyle(prefs.getString("clockDialStyle", "lines_only"));
            analogClock.setColors(foreground, background);
            LinearLayout.LayoutParams analogParams =
                    new LinearLayout.LayoutParams(analogDiameter, analogDiameter);
            block.addView(analogClock, analogParams);
            clockLineHeightPx = analogDiameter;
        } else {
            digitalClock = makeText("", effectiveSize, foreground, face);
            LinearLayout.LayoutParams clockParams =
                    new LinearLayout.LayoutParams(-1, dp(Math.max(48, effectiveSize)));
            block.addView(digitalClock, clockParams);
            clockLineHeightPx = clockParams.height;
        }

        float dateSize = effectiveSize * 0.26f;
        int dateRowHeight = dp(Math.max(24, Math.round(effectiveSize * 0.30f)));
        boolean showGregorian = prefs.getBoolean("showGregorian", true);
        boolean showHebrew = prefs.getBoolean("showHebrew", true);
        if (showGregorian) {
            gregorianDate = makeText("", dateSize, foreground, face);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dateRowHeight);
            params.topMargin = analog ? 0 : dp(20);
            block.addView(gregorianDate, params);
        }
        if (showHebrew) {
            hebrewDate = makeText("", dateSize, foreground, face);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dateRowHeight);
            params.topMargin = gregorianDate == null ? (analog ? 0 : dp(20)) : dp(10);
            block.addView(hebrewDate, params);
        }

        FrameLayout.LayoutParams blockParams =
                new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        String position = prefs.getString("clockDatePosition", "upper");
        if ("middle".equals(position)) {
            blockParams.gravity = Gravity.CENTER;
        } else if ("upper".equals(position)) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
            blockParams.topMargin = Math.max(0,
                    Math.round(screenHeight / 4f - clockLineHeightPx / 2f));
            // The outer block is vertically positioned so the clock line itself is
            // at 25% of the display, exactly as in the regular screensaver screen.
        } else {
            blockParams.topMargin = 0;
        }
        root.addView(block, blockParams);
        setContentView(root);
    }

    private TextView makeText(String value, float size, int color, Typeface face) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(face);
        String alignment = prefs == null
                ? "center" : prefs.getString("clockTextAlignment", "center");
        int horizontalGravity = "right".equals(alignment) ? Gravity.RIGHT
                : "left".equals(alignment) ? Gravity.LEFT : Gravity.CENTER_HORIZONTAL;
        view.setGravity(horizontalGravity | Gravity.CENTER_VERTICAL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setIncludeFontPadding(false);
        return view;
    }

    private Typeface loadTypeface(String family, int weight) {
        Typeface fallback = Typeface.create("sans-serif", Typeface.NORMAL);
        Typeface base = fallback;
        if (family != null && family.startsWith("custom:")) {
            String file = family.substring("custom:".length());
            if (!file.isEmpty() && !file.contains("/") && !file.contains("\\")) {
                try {
                    base = Typeface.createFromAsset(getAssets(), "fonts/" + file);
                } catch (Throwable ignored) {
                    base = fallback;
                }
            }
        } else if (family != null && !family.isEmpty()) {
            base = Typeface.create(family, Typeface.NORMAL);
        }

        int effectiveWeight = Math.max(-1, Math.min(2, weight));
        if ("custom:fmhaimblack.ttf".equals(family)) effectiveWeight -= 2;
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            int androidWeight = effectiveWeight < 0 ? 300
                    : effectiveWeight == 0 ? 400
                    : effectiveWeight == 1 ? 500 : 700;
            return Typeface.create(base, androidWeight, false);
        }
        return Typeface.create(base, effectiveWeight >= 2 ? Typeface.BOLD
                : effectiveWeight == 1 ? Typeface.NORMAL : Typeface.NORMAL);
    }

    private int effectiveClockDateSize(String family, int requestedSize) {
        if (!"custom:DL Normador.TTF".equals(family)
                && !"custom:Ashkenazi-Stam.ttf".equals(family)) return requestedSize;
        switch (requestedSize) {
            case 60: return 72;
            case 72: return 84;
            case 84: return 96;
            case 96: return 108;
            case 108: return 120;
            default: return requestedSize;
        }
    }

    private void updateClock() {
        if (prefs == null) return;
        now.setTimeInMillis(System.currentTimeMillis());

        if (digitalClock != null) {
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);
            if ("12".equals(prefs.getString("timeFormat", "24"))) {
                int displayHour = hour % 12;
                if (displayHour == 0) displayHour = 12;
                String period = hour < 12 ? " לפנה״צ" : " אחה״צ";
                digitalClock.setText(String.format(Locale.US, "%d:%02d%s",
                        displayHour, minute, period));
            } else {
                digitalClock.setText(String.format(Locale.US, "%d:%02d", hour, minute));
            }
        }

        if (analogClock != null) {
            analogClock.setTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }

        if (gregorianDate != null) {
            String pattern = prefs.getString("gregorianDateFormat", "dd.MM.yyyy");
            Locale dateLocale = getString(R.string.main_activity_text_011).equals(pattern)
                    ? new Locale("he", "IL") : Locale.US;
            SimpleDateFormat formatter;
            try {
                formatter = new SimpleDateFormat(pattern, dateLocale);
            } catch (IllegalArgumentException ignored) {
                formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            }
            formatter.setTimeZone(now.getTimeZone());
            String weekday = hebrewWeekday(now.get(Calendar.DAY_OF_WEEK));
            gregorianDate.setText(weekday + " " + formatter.format(now.getTime()));
        }

        if (hebrewDate != null) {
            try {
                JewishDate jewishDate = new JewishDate(now);
                HebrewDateFormatter formatter = new HebrewDateFormatter();
                formatter.setHebrewFormat(true);
                formatter.setUseGershGershayim(true);
                formatter.setUseLongHebrewYears(false);
                hebrewDate.setText(formatHebrewDate(formatter.format(jewishDate),
                        prefs.getString("hebrewDateFormat", "plain")));
            } catch (Throwable ignored) {
                hebrewDate.setText("");
            }
        }
    }

    private String hebrewWeekday(int day) {
        switch (day) {
            case Calendar.SUNDAY: return getString(R.string.main_activity_text_020);
            case Calendar.MONDAY: return getString(R.string.main_activity_text_021);
            case Calendar.TUESDAY: return getString(R.string.main_activity_text_022);
            case Calendar.WEDNESDAY: return getString(R.string.main_activity_text_023);
            case Calendar.THURSDAY: return getString(R.string.main_activity_text_024);
            case Calendar.FRIDAY: return getString(R.string.main_activity_text_025);
            default: return getString(R.string.main_activity_text_026);
        }
    }

    private String formatHebrewDate(String rawDate, String format) {
        String normalized = rawDate
                .replace("מרחשוון", "חשוון")
                .replace("מרחשון", "חשון")
                .replace("׳", "'")
                .replace("״", "\\\"")
                .replace("’", "'")
                .replace("”", "\\\"")
                .replace("“", "\\\"");
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
