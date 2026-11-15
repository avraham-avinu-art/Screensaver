package com.avrahamart.nightscreen;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
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
    private LinearLayout clockTextColumn;
    private BatteryChargeIndicatorView chargingBatteryView;
    private int clockLineHeightPx;
    private boolean dreamActive;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!dreamActive) return;
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
        dreamActive = true;
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override
    public void onDreamingStopped() {
        dreamActive = false;
        handler.removeCallbacks(clockTick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        dreamActive = false;
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
        root.setBackground(createDreamBackground(background));

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER_HORIZONTAL);
        block.setClipChildren(false);
        block.setClipToPadding(false);
        clockBlock = block;

        boolean analog = "analog".equals(
                prefs.getString("clockDisplayMode", "digital"));
        boolean numbersOnly = "numbers_only".equals(
                prefs.getString("clockDialStyle", "lines_only"));
        int analogDiameter = dp(Math.max(170,
                Math.round(effectiveSize * (numbersOnly ? 2.85f : 2.25f))));

        if (analog) {
            analogClock = new AnalogClockView(this);
            analogClock.setDialStyle(prefs.getString("clockDialStyle", "lines_only"));
            analogClock.setColors(foreground, background);
            LinearLayout.LayoutParams analogParams =
                    new LinearLayout.LayoutParams(analogDiameter, analogDiameter);
            analogParams.gravity = Gravity.CENTER_HORIZONTAL;
            block.addView(analogClock, analogParams);
            clockLineHeightPx = analogDiameter;
        }

        clockTextColumn = new LinearLayout(this);
        clockTextColumn.setOrientation(LinearLayout.VERTICAL);
        clockTextColumn.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockTextColumn.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        clockTextColumn.setClipChildren(false);
        clockTextColumn.setClipToPadding(false);

        if (!analog) {
            digitalClock = makeText("", effectiveSize, foreground, face);
            int clockHeight = Math.max(dp(62),
                    measuredLineHeight(digitalClock, "0123456789:00", 2, effectiveSize));
            clockLineHeightPx = clockHeight;
            clockTextColumn.addView(digitalClock,
                    new LinearLayout.LayoutParams(-1, clockHeight));
        }

        float dateSize = effectiveSize * 0.26f;
        int dateRowHeight = dp(Math.max(24, Math.round(effectiveSize * 0.30f)));
        boolean showGregorian = prefs.getBoolean("showGregorian", true);
        boolean showHebrew = prefs.getBoolean("showHebrew", true);
        if (showGregorian) {
            gregorianDate = makeText("", dateSize, foreground, face);
            int height = Math.max(dateRowHeight,
                    measuredLineHeight(gregorianDate,
                            getString(R.string.main_activity_text_008), 0, 24));
            gregorianDate.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
            clockTextColumn.addView(gregorianDate);
        }
        if (showHebrew) {
            hebrewDate = makeText("", dateSize, foreground, face);
            int height = Math.max(dateRowHeight,
                    measuredLineHeight(hebrewDate,
                            getString(R.string.main_activity_text_009), 0, 24));
            hebrewDate.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
            clockTextColumn.addView(hebrewDate);
        }

        int numbersOnlyDateOverlap = analog && numbersOnly
                ? Math.round(analogDiameter * 0.17f) : 0;
        if (gregorianDate != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) gregorianDate.getLayoutParams();
            params.topMargin = analog ? -numbersOnlyDateOverlap : dp(20);
            gregorianDate.setLayoutParams(params);
        }
        if (hebrewDate != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) hebrewDate.getLayoutParams();
            params.topMargin = gregorianDate == null
                    ? (analog ? -numbersOnlyDateOverlap : dp(20)) : dp(10);
            hebrewDate.setLayoutParams(params);
        }

        LinearLayout.LayoutParams textColumnParams =
                new LinearLayout.LayoutParams(-2, -2);
        textColumnParams.gravity = Gravity.CENTER_HORIZONTAL;
        block.addView(clockTextColumn, textColumnParams);

        FrameLayout.LayoutParams blockParams =
                new FrameLayout.LayoutParams(-1, -2,
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        String position = prefs.getString("clockDatePosition", "upper");
        if ("middle".equals(position)) {
            blockParams.gravity = Gravity.CENTER;
        } else if ("upper".equals(position)) {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            blockParams.topMargin = Math.max(0,
                    Math.round(screenHeight / 4f - clockLineHeightPx / 2f));
        } else {
            blockParams.topMargin = 0;
        }
        root.addView(block, blockParams);

        chargingBatteryView = new BatteryChargeIndicatorView(this);
        FrameLayout.LayoutParams batteryParams =
                new FrameLayout.LayoutParams(-2, dp(28),
                        Gravity.TOP | Gravity.RIGHT);
        batteryParams.rightMargin = dp(12);
        batteryParams.topMargin = dp(12);
        root.addView(chargingBatteryView, batteryParams);

        setContentView(root);
        applyDreamBrightness();
        updateChargingBatteryDisplay();
        updateClockTextColumnWidth();
        root.post(this::applyClockHorizontalOffset);
    }

    private Drawable createDreamBackground(int base) {
        String style = prefs.getString("backgroundStyle", "solid");
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
                    new int[]{base, mixColor(base, Color.BLACK, 0.62f), Color.BLACK});
            vignette.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            vignette.setGradientRadius(dp(Math.max(250, Math.round(Math.max(
                    getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels) * 0.66f))));
            vignette.setGradientCenter(0.5f, 0.45f);
            return vignette;
        }
        return new ColorDrawable(base);
    }

    private int mixColor(int from, int to, float amount) {
        float f = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(from) * (1f - f) + Color.red(to) * f),
                Math.round(Color.green(from) * (1f - f) + Color.green(to) * f),
                Math.round(Color.blue(from) * (1f - f) + Color.blue(to) * f));
    }

    private boolean isAutomaticNightModeActive() {
        if (!prefs.getBoolean("nightModeEnabled", false)) return false;
        int start = Math.max(0, Math.min(23, prefs.getInt("nightModeStartHour", 22)));
        int end = Math.max(0, Math.min(23, prefs.getInt("nightModeEndHour", 7)));
        if (start == end) return false;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return start < end ? hour >= start && hour < end
                : hour >= start || hour < end;
    }

    private void applyDreamBrightness() {
        int percent = prefs.getInt("screenBrightness", -1);
        float brightness = percent < 0 ? -1f
                : Math.max(0.01f, Math.min(1f, percent / 100f));
        if (isAutomaticNightModeActive()) {
            brightness = brightness < 0f ? 0.20f : Math.min(brightness, 0.20f);
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = brightness < 0f
                ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE : brightness;
        getWindow().setAttributes(attributes);
    }

    private int measuredLineHeight(TextView view, String sample, int extraSpaceDp,
                                   int minimumHeightDp) {
        if (view == null) return dp(minimumHeightDp);
        android.graphics.Paint paint = view.getPaint();
        android.graphics.Paint.FontMetrics metrics = paint.getFontMetrics();
        int fontHeight = Math.round(metrics.descent - metrics.ascent);
        android.graphics.Rect bounds = new android.graphics.Rect();
        int visibleHeight = 0;
        if (sample != null && !sample.isEmpty()) {
            paint.getTextBounds(sample, 0, sample.length(), bounds);
            visibleHeight = Math.max(visibleHeight, bounds.height());
        }
        CharSequence current = view.getText();
        if (current != null && current.length() > 0) {
            paint.getTextBounds(current.toString(), 0, current.length(), bounds);
            visibleHeight = Math.max(visibleHeight, bounds.height());
        }
        return Math.max(dp(minimumHeightDp),
                Math.max(fontHeight, visibleHeight) + dp(extraSpaceDp));
    }

    private void updateClockTextColumnWidth() {
        if (clockTextColumn == null) return;
        float widest = 0f;
        TextView[] rows = {digitalClock, gregorianDate, hebrewDate};
        for (TextView row : rows) {
            if (row == null || row.getVisibility() != View.VISIBLE) continue;
            CharSequence value = row.getText();
            if (value == null || value.length() == 0) continue;
            widest = Math.max(widest,
                    row.getPaint().measureText(value, 0, value.length())
                            + row.getPaddingLeft() + row.getPaddingRight() + dp(32));
        }
        int available = Math.max(dp(1),
                getResources().getDisplayMetrics().widthPixels - dp(24));
        int width = Math.max(dp(1), Math.min(available, (int) Math.ceil(widest)));
        ViewGroup.LayoutParams raw = clockTextColumn.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams
                && raw.width != width) {
            raw.width = width;
            clockTextColumn.setLayoutParams(raw);
            clockTextColumn.requestLayout();
        }
    }

    private void applyClockHorizontalOffset() {
        if (clockBlock == null || clockTextColumn == null) return;
        int width = clockBlock.getWidth();
        if (width <= 0) width = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = clockTextColumn.getMeasuredWidth();
        if (analogClock != null && analogClock.getVisibility() == View.VISIBLE) {
            contentWidth = Math.max(contentWidth, analogClock.getMeasuredWidth());
        }
        float fraction = Math.max(-1f, Math.min(1f,
                prefs.getInt("clockHorizontalOffset", 0) / 20f));
        float translation = ((width - contentWidth) / 2f) * fraction;
        if (analogClock != null) analogClock.setTranslationX(translation);
        clockTextColumn.setTranslationX(translation);
    }

    private void updateChargingBatteryDisplay() {
        if (chargingBatteryView == null || prefs == null) return;
        int level = 0;
        boolean charging = false;
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                if (rawLevel >= 0 && scale > 0) {
                    level = Math.max(0, Math.min(100,
                            Math.round(rawLevel * 100f / scale)));
                }
                charging = plugged != 0
                        && (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);
            }
        } catch (Throwable ignored) { }
        chargingBatteryView.setBatteryState(
                prefs.getString("chargingBatteryDisplayMode", "off"),
                level, charging, prefs.getInt("fontColor", Color.WHITE));
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
        updateClockTextColumnWidth();
        if (clockBlock != null) clockBlock.post(this::applyClockHorizontalOffset);
        updateChargingBatteryDisplay();
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
