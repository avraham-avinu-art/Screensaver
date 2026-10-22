package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final int REQUEST_ENABLE_SCREEN_LOCK_ADMIN = 8142;
    private static final int SURFACE = Color.rgb(9, 19, 33);
    private static final int CARD_SURFACE = Color.rgb(20, 36, 56);
    private static final int CONTROL_SURFACE = Color.rgb(27, 47, 70);
    private static final int ICON_SURFACE = Color.rgb(36, 57, 80);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int OUTLINE = Color.rgb(62, 88, 116);
    private static final int CARD_OUTLINE = Color.rgb(42, 67, 95);
    private static final int TRACK = Color.rgb(62, 82, 105);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    private SharedPreferences prefs;
    private LinearLayout content;
    private LinearLayout clockDateCard;
    private LinearLayout playerFrameCard;
    private LinearLayout notificationAccessCard;
    private LinearLayout screenLockAdminCard;
    private TextView screenLockAdminStatus;
    private Button screenLockAdminButton;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName screenLockAdminComponent;
    private LinearLayout currentCard;
    private final int dateTextColor = Color.rgb(220, 220, 220);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        Window window = getWindow();
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(SURFACE);
        window.setNavigationBarColor(SURFACE);
        window.getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(14), dp(4), dp(14), dp(8));
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                view.setPadding(dp(14), dp(4) + topInset, dp(14), dp(8) + bottomInset);
                return insets;
            });
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(2), dp(7), dp(2), dp(12));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titleBlock.setGravity(Gravity.CENTER);
        titleBlock.setPadding(0, 0, 0, 0);

        TextView title = textView("הגדרות", 28, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView("התאמה אישית של השומר מסך", 18, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        subtitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titleBlock.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        header.addView(titleBlock, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.setPadding(0, dp(1), 0, dp(4));

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scrollContent.setTextDirection(View.TEXT_DIRECTION_RTL);
        scrollContent.addView(header, new LinearLayout.LayoutParams(-1, -2));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setTextDirection(View.TEXT_DIRECTION_RTL);
        scrollContent.addView(content, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(scrollContent, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        buildSettingsCards();

        Button done = new Button(this);
        done.setText("סיום ושמירה");
        done.setAllCaps(false);
        done.setTextSize(17);
        done.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        done.setTextColor(Color.rgb(7, 31, 49));
        done.setMinWidth(0);
        done.setMinHeight(0);
        done.setPadding(dp(24), 0, dp(24), 0);
        done.setBackground(pillBackground(ACCENT, dp(22)));
        done.setElevation(dp(2));
        LinearLayout.LayoutParams doneParams =
                new LinearLayout.LayoutParams(-2, dp(50));
        doneParams.gravity = Gravity.CENTER_HORIZONTAL;
        doneParams.topMargin = dp(4);
        root.addView(done, doneParams);
        done.setOnClickListener(v -> finish());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) {
            updateNotificationAccessCardVisibility();
            updateScreenLockAdminCard();
        }
    }

    private void buildSettingsCards() {
        LinearLayout display = addCard(content, "תצוגה במסך");
        addCheck(display, "תאריך עברי", "showHebrew", true);
        addCheck(display, "תאריך לועזי", "showGregorian", true);
        addCheck(display, "הצגת הנגן", "showPlayer", true);
        addDiscreteSlider(display, "זמן פעילות",
                new String[]{"לפי המכשיר", "דקה אחת", "חמש דקות", "ללא כיבוי"},
                new String[]{"device", "1", "5", "0"}, "screenSaverDuration");

        LinearLayout charging = addCard(content, "שומר מסך בזמן טעינה");
        addCheck(charging, "הפעלה אוטומטית בטעינה", "autoStartOnCharge", false);
        addCheck(charging, "ללא כיבוי בטעינה", "noShutdownWhileCharging", false);
        addCheck(charging, "הצגת אחוז הסוללה", "showBatteryPercent", false);

        LinearLayout font = addCard(content, "גופן");
        addColorChoices(font, "צבע",
                new String[]{"לבן", "כסף", "אפור", "אפור בהיר", "כחול עדין"},
                new String[]{"white", "silver", "gray", "light", "blue"}, "fontColor");
        addDiscreteSlider(font, "עובי",
                new String[]{"דק", "רגיל", "בינוני", "מודגש"},
                new String[]{"-1", "0", "1", "2"}, "fontWeight");
        addSpinner(font, "סוג",
                new String[]{"Sans Serif", "DL Adva", "Keter Aram Tsova", "Shofar",
                        "Gveret Levin", "Yehuda CLM", "Stam Ashkenaz", "DL Apollo Black",
                        "DL Normador", "Ozrad CLM Bold", "Haim FB Black", "Coming Soon",
                        "Short Stack"},
                new String[]{"sans-serif", "custom:DL Adva.ttf",
                        "custom:KeterAramTsova.ttf", "custom:ShofarRegular.ttf",
                        "custom:gveretlevi-regular.ttf", "custom:yehudaclm-light.ttf",
                        "custom:Ashkenazi-Stam.ttf", "custom:DL ApolloBlack.ttf",
                        "custom:DL Normador.TTF", "custom:OzradCLM-Bold.otf",
                        "custom:fmhaimblack.ttf", "custom:ComingSoon.ttf",
                        "custom:ShortStack-Regular.ttf"}, "fontFamily");

        clockDateCard = addCard(content, "תצוגת שעון ותאריך");
        addDiscreteSlider(clockDateCard, "גודל",
                new String[]{"קטן", "בינוני", "רגיל", "גדול", "גדול מאוד"},
                new String[]{"48", "60", "72", "84", "96"}, "clockDateSize");
        addDiscreteSlider(clockDateCard, "מיקום",
                new String[]{"למעלה", "25% מלמעלה", "באמצע"},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");

        LinearLayout player = addCard(content, "תצוגת הנגן");
        addCheck(player, "פרטי אמן", "showDetails", getBool("showDetails", true));
        addCheck(player, "שורת מיקום + זמן", "showProgress", true);
        addDiscreteSlider(player, "גודל הנגן",
                new String[]{"קטן", "בינוני", "גדול", "גדול מאוד"},
                new String[]{"14", "17", "20", "23"}, "playerSize");
        addDiscreteSlider(player, "מיקום הנגן",
                new String[]{"25% מלמטה", "למטה"},
                new String[]{"lower", "bottom"}, "playerPosition");

        playerFrameCard = addCard(content, "מסגרת לנגן");
        LinearLayout.LayoutParams playerFrameCardParams =
                (LinearLayout.LayoutParams) playerFrameCard.getLayoutParams();
        playerFrameCardParams.bottomMargin += dp(5);
        playerFrameCard.setLayoutParams(playerFrameCardParams);
        addCheck(playerFrameCard, "הצג מסגרת", "frameEnabled", false);
        addColorChoices(playerFrameCard, "צבע מסגרת",
                new String[]{"לבן", "כסף", "אפור", "אפור בהיר", "כחול עדין"},
                new String[]{"white", "silver", "gray", "light", "blue"}, "frameColor");
        addDiscreteSlider(playerFrameCard, "עובי מסגרת",
                new String[]{"דקה", "בינונית", "עבה"},
                new String[]{"1", "2", "3"}, "frameWidth");
        addDiscreteSlider(playerFrameCard, "עיגול פינות",
                new String[]{"ישר", "עדין", "מעוגל"},
                new String[]{"0", "10", "20"}, "frameRadius");

        if (!hasNotificationAccess()) {
            notificationAccessCard = addNotificationAccessCard(content);
        }

        // Optional device-admin card remains the final settings card.
        screenLockAdminCard = addCard(content, "הרשאת מנהל מכשיר");
        setPermissionCardHeadingTypeface(screenLockAdminCard);
        screenLockAdminCard.setPadding(dp(13), dp(10), dp(13), dp(14));
        TextView explanation = textView(
                "ההרשאה נדרשת רק לכיבוי מסך ונעילת המכשיר מיד בתום פעילות השומר מסך.\n"
                + "ללא ההרשאה, בתום הזמן תסתיים תצוגת שומר המסך בלבד. "
                + "כיבוי המסך ונעילת המכשיר ימשיכו להתבצע לפי הגדרות המכשיר.",
                15, ON_SURFACE);
        explanation.setGravity(Gravity.CENTER);
        explanation.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        explanation.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        explanation.setTextDirection(View.TEXT_DIRECTION_RTL);
        explanation.setLineSpacing(dp(2), 1f);
        explanation.setPadding(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams explanationParams =
                new LinearLayout.LayoutParams(-1, -2);
        explanationParams.topMargin = dp(4);
        explanationParams.bottomMargin = dp(4);
        screenLockAdminCard.addView(explanation, explanationParams);

        screenLockAdminStatus = textView("", 16, ON_SURFACE);
        screenLockAdminStatus.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        screenLockAdminStatus.setGravity(Gravity.CENTER);
        screenLockAdminStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        screenLockAdminStatus.setTextDirection(View.TEXT_DIRECTION_RTL);
        screenLockAdminStatus.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams adminStatusParams =
                new LinearLayout.LayoutParams(-1, -2);
        adminStatusParams.topMargin = dp(6);
        adminStatusParams.bottomMargin = dp(6);
        screenLockAdminCard.addView(screenLockAdminStatus, adminStatusParams);

        screenLockAdminButton = makeActionButton("הפעל הרשאת מנהל מכשיר");
        LinearLayout.LayoutParams adminButtonParams =
                new LinearLayout.LayoutParams(-2, dp(46));
        adminButtonParams.gravity = Gravity.CENTER_HORIZONTAL;
        adminButtonParams.topMargin = dp(7);
        screenLockAdminCard.addView(screenLockAdminButton, adminButtonParams);
        screenLockAdminButton.setOnClickListener(v -> requestScreenLockAdmin());
        updateScreenLockAdminCard();
    }

    private LinearLayout addCard(LinearLayout parent, String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setTextDirection(View.TEXT_DIRECTION_RTL);
        card.setPadding(dp(13), dp(10), dp(13), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_SURFACE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), CARD_OUTLINE);
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(9));
        parent.addView(card, params);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = textView(title, 18, ON_SURFACE);
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams headingTextParams =
                new LinearLayout.LayoutParams(0, dp(42), 1f);
        headingRow.addView(heading, headingTextParams);

        FrameLayout icon = makeIconBadge(iconForCard(title), dp(36));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(36), dp(36));
        iconParams.leftMargin = dp(9);
        headingRow.addView(icon, iconParams);
        card.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(42)));

        View separator = new View(this);
        separator.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.setMargins(dp(2), dp(5), dp(2), dp(10));
        card.addView(separator, separatorParams);
        return card;
    }

    private void addCheck(LinearLayout card, String label, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // LTR child order places the switch at the left and the Hebrew label at the right.
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView text = textView(label, 16, ON_SURFACE);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        text.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.addView(text, new LinearLayout.LayoutParams(0, dp(42), 1f));

        FrameLayout settingIcon = makeIconBadge(iconForSetting(key), dp(34));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.leftMargin = dp(9);
        row.addView(settingIcon, iconParams);

        Switch toggle = new Switch(this);
        toggle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        toggle.setShowText(false);
        toggle.setContentDescription(label);
        toggle.setChecked(getBool(key, defaultValue));
        if (Build.VERSION.SDK_INT >= 21) {
            toggle.setThumbTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{ACCENT, Color.rgb(177, 191, 207)}));
            toggle.setTrackTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{Color.rgb(39, 123, 153), Color.rgb(49, 67, 88)}));
        }
        LinearLayout.LayoutParams switchParams =
                new LinearLayout.LayoutParams(dp(56), dp(42));
        switchParams.leftMargin = dp(7);
        row.addView(toggle, 0, switchParams);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            try {
                prefs.edit().putBoolean(key, checked).apply();
            } catch (Throwable ignored) {
            }
        });
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = dp(4);
        card.addView(row, rowParams);
    }

    private void addDiscreteSlider(LinearLayout card, String label,
                                   String[] names, String[] values, String key) {
        if (names == null || values == null || names.length == 0
                || names.length != values.length) return;

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        block.setPadding(dp(2), dp(2), dp(2), dp(3));

        final int initial = selectedIndex(key, values);
        TextView chosenValue = textView(names[initial], 14, ACCENT);
        chosenValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        chosenValue.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        chosenValue.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        chosenValue.setSingleLine(false);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(-2, dp(34));
        valueParams.rightMargin = dp(8);
        headingRow.addView(chosenValue, valueParams);

        TextView settingLabel = textView(label, 16, ON_SURFACE);
        settingLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        settingLabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        settingLabel.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        settingLabel.setTextDirection(View.TEXT_DIRECTION_RTL);
        headingRow.addView(settingLabel, new LinearLayout.LayoutParams(0, dp(34), 1f));

        FrameLayout settingIcon = makeIconBadge(iconForSetting(key), dp(34));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.leftMargin = dp(9);
        headingRow.addView(settingIcon, iconParams);

        block.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(36)));

        SeekBar slider = new SeekBar(this);
        slider.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        slider.setMax(Math.max(0, names.length - 1));
        slider.setProgress(initial);
        slider.setContentDescription(label);
        slider.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        slider.setPadding(dp(2), 0, dp(2), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            slider.setThumbTintList(ColorStateList.valueOf(ACCENT));
            slider.setProgressTintList(ColorStateList.valueOf(ACCENT));
            slider.setProgressBackgroundTintList(ColorStateList.valueOf(TRACK));
        }
        LinearLayout.LayoutParams sliderParams =
                new LinearLayout.LayoutParams(-1, dp(32));
        sliderParams.topMargin = dp(1);
        sliderParams.leftMargin = dp(8);
        sliderParams.rightMargin = dp(7);
        block.addView(slider, sliderParams);

        LinearLayout endpoints = new LinearLayout(this);
        endpoints.setOrientation(LinearLayout.HORIZONTAL);
        endpoints.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        endpoints.setGravity(Gravity.CENTER_VERTICAL);
        TextView first = textView(names[0], 12, MUTED);
        first.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        first.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        first.setSingleLine(true);
        TextView last = textView(names[names.length - 1], 12, MUTED);
        last.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        last.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        last.setSingleLine(true);
        endpoints.addView(first, new LinearLayout.LayoutParams(0, dp(20), 1f));
        endpoints.addView(last, new LinearLayout.LayoutParams(0, dp(20), 1f));
        block.addView(endpoints, new LinearLayout.LayoutParams(-1, dp(20)));

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int safeProgress = Math.max(0, Math.min(progress, names.length - 1));
                chosenValue.setText(names[safeProgress]);
                if (fromUser) persistOption(key, values[safeProgress]);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        LinearLayout.LayoutParams blockParams =
                new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = dp(5);
        card.addView(block, blockParams);
    }

    private void addColorChoices(LinearLayout card, String label,
                                 String[] names, String[] values, String key) {
        if (names == null || values == null || names.length == 0
                || names.length != values.length) return;

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        block.setPadding(dp(2), dp(2), dp(2), dp(5));

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = textView(label, 16, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        headingRow.addView(title, new LinearLayout.LayoutParams(0, dp(36), 1f));
        FrameLayout icon = makeIconBadge("color-options", dp(34));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.leftMargin = dp(9);
        headingRow.addView(icon, iconParams);
        block.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(36)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        choices.setGravity(Gravity.CENTER_VERTICAL);
        final int initial = selectedIndex(key, values);
        final ColorSwatchView[] swatches = new ColorSwatchView[names.length];
        final TextView[] captions = new TextView[names.length];

        for (int i = 0; i < names.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(1), dp(4), dp(1), dp(2));

            ColorSwatchView swatch = new ColorSwatchView(this, values[i], i == initial);
            swatches[i] = swatch;
            item.addView(swatch, new LinearLayout.LayoutParams(dp(34), dp(34)));

            TextView caption = textView(names[i], 12, i == initial ? ACCENT : MUTED);
            caption.setGravity(Gravity.CENTER);
            caption.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            caption.setSingleLine(true);
            captions[i] = caption;
            LinearLayout.LayoutParams captionParams =
                    new LinearLayout.LayoutParams(-1, dp(22));
            captionParams.topMargin = dp(2);
            item.addView(caption, captionParams);

            item.setOnClickListener(v -> {
                persistOption(key, values[index]);
                for (int j = 0; j < swatches.length; j++) {
                    swatches[j].setSwatchSelected(j == index);
                    captions[j].setTextColor(j == index ? ACCENT : MUTED);
                }
            });
            choices.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        block.addView(choices, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams blockParams =
                new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = dp(4);
        card.addView(block, blockParams);
    }

    private void persistOption(String key, String value) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            if ("fontColor".equals(key) || "frameColor".equals(key)) {
                editor.putInt(key, colorValue(value));
            } else if ("fontWeight".equals(key) || "clockDateSize".equals(key)
                    || "playerSize".equals(key) || "frameWidth".equals(key)
                    || "frameRadius".equals(key)) {
                editor.putInt(key, Integer.parseInt(value));
            } else {
                editor.putString(key, value);
            }
            editor.apply();
        } catch (Throwable ignored) {
        }
    }

    private String iconForCard(String title) {
        if ("תצוגה במסך".equals(title)) return "monitor";
        if ("שומר מסך בזמן טעינה".equals(title)) return "battery";
        if ("גופן".equals(title)) return "type";
        if ("תצוגת שעון ותאריך".equals(title)) return "clock";
        if ("תצוגת הנגן".equals(title)) return "music";
        if ("מסגרת לנגן".equals(title)) return "frame";
        if ("הרשאת גישה להתראות".equals(title)) return "bell";
        if ("הרשאת מנהל מכשיר".equals(title)) return "shield";
        return "settings";
    }

    private String iconForSetting(String key) {
        if ("showHebrew".equals(key)) return "hebrew-date";
        if ("showGregorian".equals(key)) return "gregorian-date";
        if ("showPlayer".equals(key)) return "media-toggle";
        if ("screenSaverDuration".equals(key)) return "duration";
        if ("autoStartOnCharge".equals(key)) return "battery";
        if ("noShutdownWhileCharging".equals(key)) return "battery";
        if ("showBatteryPercent".equals(key)) return "battery";
        if ("fontColor".equals(key)) return "text-color";
        if ("fontWeight".equals(key)) return "weight";
        if ("fontFamily".equals(key)) return "font-family";
        if ("clockDateSize".equals(key)) return "clock-size";
        if ("clockDatePosition".equals(key)) return "clock-position";
        if ("showDetails".equals(key)) return "artist";
        if ("showProgress".equals(key)) return "progress";
        if ("playerSize".equals(key)) return "player-size";
        if ("playerPosition".equals(key)) return "player-position";
        if ("frameEnabled".equals(key)) return "frame-toggle";
        if ("frameColor".equals(key)) return "frame-color";
        if ("frameWidth".equals(key)) return "border-width";
        if ("frameRadius".equals(key)) return "corners";
        return "settings";
    }

    private FrameLayout makeIconBadge(String iconName, int size) {
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(roundOutline(ICON_SURFACE, dp(10), CARD_OUTLINE));
        GraphicIcon graphic = new GraphicIcon(this, iconName, Color.rgb(168, 200, 232));
        FrameLayout.LayoutParams graphicParams =
                new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.CENTER);
        badge.addView(graphic, graphicParams);
        return badge;
    }

    private void setPermissionCardHeadingTypeface(LinearLayout card) {
        if (card == null || card.getChildCount() == 0) return;
        View first = card.getChildAt(0);
        if (!(first instanceof LinearLayout)) return;
        LinearLayout headingRow = (LinearLayout) first;
        if (headingRow.getChildCount() == 0) return;
        View headingView = headingRow.getChildAt(0);
        if (headingView instanceof TextView) {
            TextView heading = (TextView) headingView;
            heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            heading.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            heading.setPadding(dp(2), 0, dp(2), 0);
            heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
    }

    private void addSpinner(LinearLayout card, String label, String[] names,
                            String[] values, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(2), dp(2), dp(2), dp(4));

        TextView labelView = textView(label, 16, ON_SURFACE);
        labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        labelView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        labelView.setTextDirection(View.TEXT_DIRECTION_RTL);

        int selected = selectedIndex(key, values);
        if ("fontFamily".equals(key)) {
            String storedFamily = getString(key, values[0]);
            boolean knownFamily = false;
            for (String value : values) {
                if (value.equals(storedFamily)) {
                    knownFamily = true;
                    break;
                }
            }
            // Migrate previous Android built-in family values to the first newly supplied font.
            if (!knownFamily) {
                selected = 0;
                persistOption(key, values[0]);
            }
        }
        final int selectedPosition = selected;
        int spinnerWidth = dropdownWidthForOptions(names);
        Spinner spinner = new Spinner(this);
        spinner.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        spinner.setContentDescription(label);
        spinner.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, names) {
            @Override public View getView(int position, View convertView,
                                          android.view.ViewGroup parent) {
                TextView view = convertView instanceof TextView
                        ? (TextView) convertView : new TextView(SettingsActivity.this);
                view.setText(names[position]);
                styleSpinnerText(view, true, false);
                return view;
            }

            @Override public View getDropDownView(int position, View convertView,
                                                  android.view.ViewGroup parent) {
                if (parent instanceof View) ((View) parent).setVerticalScrollBarEnabled(false);
                TextView view = new TextView(SettingsActivity.this);
                view.setText(names[position]);
                styleDropdownOptionText(view, position == selectedIndex(key, values));
                view.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, dp(39)));
                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(7), 0, dp(7), 0);
        spinner.setBackgroundColor(Color.TRANSPARENT);
        spinner.setDropDownWidth(spinnerWidth);
        if (Build.VERSION.SDK_INT >= 16) {
            spinner.setPopupBackgroundDrawable(roundOutline(CARD_SURFACE, dp(12), OUTLINE));
        }
        spinner.setSelection(selectedPosition, false);

        FrameLayout spinnerContainer = new FrameLayout(this);
        spinnerContainer.setBackground(roundOutline(CONTROL_SURFACE, dp(12), OUTLINE));
        spinnerContainer.setTag(spinner);
        FrameLayout.LayoutParams spinnerInnerParams =
                new FrameLayout.LayoutParams(-1, -1);
        spinnerContainer.addView(spinner, spinnerInnerParams);
        GraphicIcon dropdownIcon = new GraphicIcon(this, "chevron", ACCENT);
        FrameLayout.LayoutParams dropdownIconParams =
                new FrameLayout.LayoutParams(dp(18), dp(18),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        dropdownIconParams.leftMargin = dp(8);
        spinnerContainer.addView(dropdownIcon, dropdownIconParams);
        spinnerContainer.setOnClickListener(v -> spinner.performClick());

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(spinnerWidth, dp(44));
        spinnerParams.leftMargin = dp(9);
        row.addView(spinnerContainer, spinnerParams);
        row.addView(labelView, new LinearLayout.LayoutParams(0, dp(44), 1f));
        card.addView(row, new LinearLayout.LayoutParams(-1, -2));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean initialSelectionPending = true;
            private int lastHandledPosition = selectedPosition;

            @Override public void onItemSelected(AdapterView<?> parent, View view,
                                                 int position, long id) {
                if (initialSelectionPending) {
                    initialSelectionPending = false;
                    if (position == selectedPosition) {
                        lastHandledPosition = position;
                        return;
                    }
                }
                if (position == lastHandledPosition) return;
                lastHandledPosition = position;
                persistOption(key, values[position]);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void styleSpinnerText(TextView view, boolean selected, boolean selectedOption) {
        view.setTextColor(ON_SURFACE);
        view.setTextSize(15);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setSingleLine(true);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(2), 0, dp(2), 0);
        view.setHeight(dp(42));
    }

    private void styleDropdownOptionText(TextView view, boolean selectedOption) {
        view.setTextColor(selectedOption ? Color.rgb(7, 31, 49) : ON_SURFACE);
        view.setTextSize(15);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setSingleLine(true);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(6), dp(3), dp(6), dp(3));
        view.setBackground(pillBackground(selectedOption ? ACCENT : CARD_SURFACE, dp(8)));
        view.setHeight(dp(39));
    }

    private int dropdownWidthForOptions(String[] names) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(15f * getResources().getDisplayMetrics().scaledDensity);
        float longestText = 0f;
        for (String name : names) {
            if (name != null) longestText = Math.max(longestText, paint.measureText(name));
        }
        int contentWidth = (int) Math.ceil(longestText) + dp(42);
        int minimumWidth = dp(124);
        int screenLimit = getResources().getDisplayMetrics().widthPixels - dp(100);
        int maximumWidth = Math.max(minimumWidth, screenLimit);
        return Math.min(maximumWidth, Math.max(minimumWidth, contentWidth));
    }

    private void normalizeSpinnerWidths(LinearLayout card) {
        if (card == null) return;
        int widest = dp(88);
        int screenLimit = getResources().getDisplayMetrics().widthPixels
                - dp(70 + 31 + 8 + 18 + 55);
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            if (row.getChildCount() == 0 || !(row.getChildAt(0) instanceof Spinner)) continue;
            View spinner = row.getChildAt(0);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
            widest = Math.max(widest, params.width);
        }
        widest = Math.min(widest, Math.max(dp(88), screenLimit));
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            if (row.getChildCount() == 0 || !(row.getChildAt(0) instanceof Spinner)) continue;
            Spinner spinner = (Spinner) row.getChildAt(0);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
            params.width = widest;
            spinner.setLayoutParams(params);
            spinner.setDropDownWidth(widest);
        }
    }

    private int selectedIndex(String key, String[] values) {
        int index = 0;
        if ("fontColor".equals(key) || "frameColor".equals(key)) {
            if (!prefs.contains(key)) return -1;
            int stored = getInt(key, colorValue(values[0]));
            for (int i = 0; i < values.length; i++) {
                if (colorValue(values[i]) == stored) return i;
            }
            return -1;
        } else if ("fontWeight".equals(key) || "clockDateSize".equals(key)
                || "playerSize".equals(key) || "frameWidth".equals(key)
                || "frameRadius".equals(key)) {
            int fallback = "clockDateSize".equals(key) ? 96 : parse(values[0], 0);
            int stored = getInt(key, fallback);
            for (int i = 0; i < values.length; i++) {
                if (parse(values[i], Integer.MIN_VALUE) == stored) return i;
            }
        } else {
            String stored = getString(key, values[0]);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(stored)) return i;
            }
        }
        return index;
    }

    private LinearLayout addNotificationAccessCard(LinearLayout parent) {
        LinearLayout card = addCard(parent, "הרשאת גישה להתראות");
        card.setPadding(dp(8), dp(9), dp(12), dp(15));
        setPermissionCardHeadingTypeface(card);
        TextView message = textView(
                "כדי להציג את נגן המערכת, יש לאפשר גישה להתראות.",
                15, ON_SURFACE);
        message.setGravity(Gravity.CENTER);
        message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        message.setLineSpacing(dp(1), 1f);
        message.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = 0;
        messageParams.bottomMargin = 0;
        card.addView(message, messageParams);

        Button grant = makeActionButton("פתיחת הגדרות הרשאה");
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-2, dp(44));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(12);
        card.addView(grant, params);
        grant.setOnClickListener(v -> openNotificationAccessSettings());
        return card;
    }

    private void updateNotificationAccessCardVisibility() {
        if (hasNotificationAccess()) {
            removeCard(notificationAccessCard);
            notificationAccessCard = null;
        } else if (notificationAccessCard == null && content != null) {
            notificationAccessCard = addNotificationAccessCard(content);
        }
        positionPermissionCards();
    }

    private void positionPermissionCards() {
        if (content == null) return;
        if (notificationAccessCard != null
                && notificationAccessCard.getParent() == content
                && playerFrameCard != null) {
            content.removeView(notificationAccessCard);
            int frameIndex = content.indexOfChild(playerFrameCard);
            int insertAt = frameIndex >= 0 ? frameIndex + 1 : content.getChildCount();
            if (screenLockAdminCard != null
                    && screenLockAdminCard.getParent() == content) {
                int adminIndex = content.indexOfChild(screenLockAdminCard);
                if (adminIndex >= 0) insertAt = Math.min(insertAt, adminIndex);
            }
            content.addView(notificationAccessCard,
                    Math.max(0, Math.min(insertAt, content.getChildCount())));
        }
        if (screenLockAdminCard != null && screenLockAdminCard.getParent() == content
                && content.indexOfChild(screenLockAdminCard) != content.getChildCount() - 1) {
            content.removeView(screenLockAdminCard);
            content.addView(screenLockAdminCard);
        }
    }

    private boolean hasNotificationAccess() {
        ComponentName component = new ComponentName(
                this, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                return manager != null
                        && manager.isNotificationListenerAccessGranted(component);
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

    private void openNotificationAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "לא ניתן לפתוח את הגדרות ההתראות במכשיר.",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
        }
    }

    private void updateScreenLockAdminCard() {
        if (screenLockAdminCard == null || screenLockAdminStatus == null
                || screenLockAdminButton == null) return;
        boolean active = isScreenLockAdminActive();
        screenLockAdminStatus.setText(active
                ? "ההרשאה הופעלה, וניתן לנעול את המסך בתום זמן הפעילות."
                : "ההרשאה עדיין לא הופעלה, ולכן כיבוי המסך והנעילה הם רק לפי הגדרות המכשיר.");
        screenLockAdminButton.setText(active
                ? "הרשאת מנהל מכשיר פעילה"
                : "הפעל הרשאת מנהל מכשיר");
        screenLockAdminButton.setEnabled(!active);
        screenLockAdminButton.setAlpha(active ? 0.65f : 1f);
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

    private void requestScreenLockAdmin() {
        try {
            if (screenLockAdminComponent == null) {
                screenLockAdminComponent = new ComponentName(
                        this, ScreenLockAdminReceiver.class);
            }
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    screenLockAdminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "ההרשאה נדרשת רק לכיבוי מסך ונעילת המכשיר מיד בתום פעילות השומר מסך. "
                    + "ללא ההרשאה, כיבוי המסך ונעילת המכשיר ימשיכו להתבצע לפי הגדרות המכשיר.");
            startActivityForResult(intent, REQUEST_ENABLE_SCREEN_LOCK_ADMIN);
        } catch (Throwable ex) {
            Toast.makeText(this, "לא ניתן לפתוח את בקשת הרשאת מנהל המכשיר.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_SCREEN_LOCK_ADMIN) {
            updateScreenLockAdminCard();
            if (isScreenLockAdminActive()) {
                Toast.makeText(this, "הרשאת נעילת המסך הופעלה.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void removeCard(LinearLayout card) {
        if (card != null && card.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) card.getParent()).removeView(card);
        }
    }

    private Button makeActionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(Color.rgb(7, 31, 49));
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setBackground(pillBackground(ACCENT, dp(22)));
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(1));
        return button;
    }

    private GradientDrawable pillBackground(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        return bg;
    }

    private GradientDrawable roundOutline(int color, int radius, int strokeColor) {
        GradientDrawable bg = pillBackground(color, radius);
        bg.setStroke(dp(1), strokeColor);
        return bg;
    }

    private int colorValue(String value) {
        if ("white".equals(value)) return Color.WHITE;
        if ("light".equals(value)) return Color.argb(102, 220, 220, 220);
        if ("gray".equals(value)) return Color.GRAY;
        if ("blue".equals(value)) return Color.rgb(120, 165, 205);
        if ("slate".equals(value)) return Color.rgb(100, 125, 150);
        if ("silver".equals(value)) return Color.rgb(155, 165, 175);
        if ("purple".equals(value)) return Color.rgb(145, 130, 165);
        if ("gold".equals(value)) return Color.rgb(190, 165, 105);
        return Color.WHITE;
    }

    private boolean getBool(String key, boolean fallback) {
        try {
            Object value = prefs.getAll().get(key);
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof String) return Boolean.parseBoolean((String) value);
            if (value instanceof Number) return ((Number) value).intValue() != 0;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private int getInt(String key, int fallback) {
        try {
            Object value = prefs.getAll().get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
            if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private String getString(String key, String fallback) {
        try {
            Object value = prefs.getAll().get(key);
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private int parse(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Throwable ignored) { return fallback; }
    }

    private TextView textView(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        return view;
    }


    private class GraphicIcon extends View {
        private final String type;
        private final int tint;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GraphicIcon(Context context, String type, int tint) {
            super(context);
            this.type = type;
            this.tint = tint;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 24f, getHeight() / 24f);
            paint.setColor(tint);
            paint.setStrokeWidth(1.8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            Path path = new Path();
            switch (type) {
                case "monitor":
                    canvas.drawRoundRect(new RectF(2.5f, 3.5f, 21.5f, 17f), 1.8f, 1.8f, paint);
                    canvas.drawLine(8f, 21f, 16f, 21f, paint);
                    canvas.drawLine(12f, 17f, 12f, 21f, paint);
                    break;
                case "calendar":
                    canvas.drawRoundRect(new RectF(3f, 5f, 21f, 21f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 9.5f, 20.5f, 9.5f, paint);
                    canvas.drawLine(8f, 3f, 8f, 7f, paint);
                    canvas.drawLine(16f, 3f, 16f, 7f, paint);
                    canvas.drawLine(8f, 13f, 8f, 13.1f, paint);
                    canvas.drawLine(12f, 13f, 12f, 13.1f, paint);
                    canvas.drawLine(16f, 13f, 16f, 13.1f, paint);
                    canvas.drawLine(8f, 17f, 8f, 17.1f, paint);
                    canvas.drawLine(12f, 17f, 12f, 17.1f, paint);
                    break;
                case "clock":
                    canvas.drawCircle(12f, 12f, 9f, paint);
                    canvas.drawLine(12f, 6.5f, 12f, 12f, paint);
                    canvas.drawLine(12f, 12f, 16.5f, 14.5f, paint);
                    break;
                case "music":
                    path.moveTo(14f, 5f);
                    path.lineTo(20f, 3.5f);
                    path.lineTo(20f, 16.5f);
                    canvas.drawPath(path, paint);
                    canvas.drawLine(14f, 5f, 14f, 17f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawOval(new RectF(8f, 15f, 14f, 20f), paint);
                    canvas.drawOval(new RectF(15f, 15f, 21f, 19.5f), paint);
                    break;
                case "type":
                case "font":
                    path.moveTo(3.5f, 20f);
                    path.lineTo(10f, 4f);
                    path.lineTo(13.5f, 4f);
                    path.lineTo(20.5f, 20f);
                    canvas.drawPath(path, paint);
                    canvas.drawLine(6.5f, 13.5f, 17.5f, 13.5f, paint);
                    break;
                case "palette":
                    path.moveTo(12f, 3f);
                    path.cubicTo(6f, 3f, 2.5f, 7.2f, 2.5f, 12f);
                    path.cubicTo(2.5f, 17.3f, 6.7f, 21f, 11.5f, 21f);
                    path.cubicTo(13.2f, 21f, 14f, 19.8f, 14f, 18.4f);
                    path.cubicTo(14f, 17.1f, 13.1f, 16.4f, 13.1f, 15.3f);
                    path.cubicTo(13.1f, 14.2f, 14f, 13.5f, 15.3f, 13.5f);
                    path.lineTo(18f, 13.5f);
                    path.cubicTo(20.3f, 13.5f, 21.5f, 11.8f, 21.5f, 9.9f);
                    path.cubicTo(21.5f, 5.8f, 17.2f, 3f, 12f, 3f);
                    path.close();
                    canvas.drawPath(path, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(7.5f, 9f, 1.2f, paint);
                    canvas.drawCircle(11.5f, 6.8f, 1.2f, paint);
                    canvas.drawCircle(16f, 7.5f, 1.2f, paint);
                    break;
                case "frame":
                    canvas.drawRoundRect(new RectF(3f, 3f, 21f, 21f), 3f, 3f, paint);
                    break;
                case "size":
                    canvas.drawLine(4f, 9f, 4f, 4f, paint);
                    canvas.drawLine(4f, 4f, 9f, 4f, paint);
                    canvas.drawLine(4f, 4f, 9f, 9f, paint);
                    canvas.drawLine(15f, 4f, 20f, 4f, paint);
                    canvas.drawLine(20f, 4f, 20f, 9f, paint);
                    canvas.drawLine(20f, 4f, 15f, 9f, paint);
                    canvas.drawLine(4f, 15f, 4f, 20f, paint);
                    canvas.drawLine(4f, 20f, 9f, 20f, paint);
                    canvas.drawLine(4f, 20f, 9f, 15f, paint);
                    canvas.drawLine(20f, 15f, 20f, 20f, paint);
                    canvas.drawLine(20f, 20f, 15f, 20f, paint);
                    canvas.drawLine(20f, 20f, 15f, 15f, paint);
                    break;
                case "bell":
                    path.moveTo(5f, 17.5f);
                    path.cubicTo(6.5f, 15.5f, 6.5f, 13.5f, 6.5f, 10.5f);
                    path.cubicTo(6.5f, 7f, 8.5f, 5f, 12f, 5f);
                    path.cubicTo(15.5f, 5f, 17.5f, 7f, 17.5f, 10.5f);
                    path.cubicTo(17.5f, 13.5f, 17.5f, 15.5f, 19f, 17.5f);
                    path.close();
                    canvas.drawPath(path, paint);
                    canvas.drawLine(4f, 18f, 20f, 18f, paint);
                    canvas.drawArc(new RectF(9.5f, 18f, 14.5f, 22f), 0f, 180f, false, paint);
                    break;
                case "shield":
                    path.moveTo(12f, 2.5f);
                    path.lineTo(20f, 5.5f);
                    path.lineTo(19f, 13.5f);
                    path.cubicTo(18.4f, 17f, 15.5f, 20f, 12f, 21.5f);
                    path.cubicTo(8.5f, 20f, 5.6f, 17f, 5f, 13.5f);
                    path.lineTo(4f, 5.5f);
                    path.close();
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(8.5f, 12f);
                    path.lineTo(11f, 14.5f);
                    path.lineTo(16f, 9.5f);
                    canvas.drawPath(path, paint);
                    break;
                case "line":
                case "minus":
                    canvas.drawLine(3f, 12f, 21f, 12f, paint);
                    break;
                case "chevron":
                    path.moveTo(5f, 9f);
                    path.lineTo(12f, 16f);
                    path.lineTo(19f, 9f);
                    canvas.drawPath(path, paint);
                    break;
                case "arrow":
                    canvas.drawLine(4f, 12f, 20f, 12f, paint);
                    path.moveTo(13f, 5f);
                    path.lineTo(20f, 12f);
                    path.lineTo(13f, 19f);
                    canvas.drawPath(path, paint);
                    break;
                case "hebrew-date":
                    canvas.drawRoundRect(new RectF(3f, 4f, 21f, 20f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 9f, 20.5f, 9f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(7f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("א", 12f, 17f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);
                    break;
                case "gregorian-date":
                    canvas.drawRoundRect(new RectF(3f, 4f, 21f, 20f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 9f, 20.5f, 9f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(7f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("31", 12f, 17f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);
                    break;
                case "media-toggle":
                    canvas.drawRoundRect(new RectF(3f, 5f, 21f, 19f), 3f, 3f, paint);
                    path.moveTo(10f, 8f);
                    path.lineTo(16f, 12f);
                    path.lineTo(10f, 16f);
                    path.close();
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawPath(path, paint);
                    break;
                case "duration":
                    canvas.drawCircle(12f, 12f, 9f, paint);
                    canvas.drawLine(12f, 6f, 12f, 12f, paint);
                    canvas.drawLine(12f, 12f, 17f, 12f, paint);
                    canvas.drawArc(new RectF(6f, 2f, 18f, 14f), 210f, 120f, false, paint);
                    break;
                case "battery":
                    canvas.drawRoundRect(new RectF(2.5f, 6f, 20f, 18f), 2f, 2f, paint);
                    canvas.drawRoundRect(new RectF(20.5f, 9.5f, 22.5f, 14.5f), 0.8f, 0.8f, paint);
                    path.moveTo(12f, 7.5f);
                    path.lineTo(8.5f, 12.5f);
                    path.lineTo(12f, 12.5f);
                    path.lineTo(10.5f, 16.5f);
                    path.lineTo(16f, 10.5f);
                    path.lineTo(12.5f, 10.5f);
                    canvas.drawPath(path, paint);
                    break;
                case "text-color":
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(18f);
                    canvas.drawText("A", 4f, 18f, paint);
                    canvas.drawCircle(18f, 7f, 2f, paint);
                    canvas.drawCircle(19f, 13f, 2f, paint);
                    break;
                case "weight":
                    paint.setStrokeWidth(3.2f);
                    canvas.drawLine(5f, 4f, 5f, 20f, paint);
                    canvas.drawLine(12f, 4f, 12f, 20f, paint);
                    canvas.drawLine(19f, 4f, 19f, 20f, paint);
                    break;
                case "font-family":
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(18f);
                    canvas.drawText("Aa", 2f, 17f, paint);
                    break;
                case "clock-size":
                    canvas.drawLine(3f, 7f, 21f, 7f, paint);
                    canvas.drawLine(3f, 12f, 17f, 12f, paint);
                    canvas.drawLine(3f, 17f, 13f, 17f, paint);
                    break;
                case "clock-position":
                    canvas.drawLine(12f, 3f, 12f, 21f, paint);
                    path.moveTo(7f, 8f);
                    path.lineTo(12f, 3f);
                    path.lineTo(17f, 8f);
                    canvas.drawPath(path, paint);
                    canvas.drawLine(5f, 21f, 19f, 21f, paint);
                    break;
                case "artist":
                    canvas.drawCircle(9f, 8f, 4f, paint);
                    canvas.drawArc(new RectF(2f, 13f, 16f, 24f), 180f, 180f, false, paint);
                    canvas.drawLine(17f, 6f, 22f, 6f, paint);
                    canvas.drawLine(17f, 11f, 22f, 11f, paint);
                    break;
                case "progress":
                    canvas.drawLine(3f, 12f, 21f, 12f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(8f, 12f, 3f, paint);
                    canvas.drawCircle(18f, 12f, 2f, paint);
                    break;
                case "player-size":
                    canvas.drawRoundRect(new RectF(3f, 5f, 21f, 19f), 2f, 2f, paint);
                    canvas.drawRoundRect(new RectF(7f, 8f, 17f, 16f), 1f, 1f, paint);
                    break;
                case "player-position":
                    canvas.drawLine(3f, 18f, 21f, 18f, paint);
                    path.moveTo(7f, 12f);
                    path.lineTo(12f, 17f);
                    path.lineTo(17f, 12f);
                    canvas.drawPath(path, paint);
                    break;
                case "frame-toggle":
                    canvas.drawRoundRect(new RectF(3f, 3f, 21f, 21f), 2f, 2f, paint);
                    canvas.drawLine(7f, 7f, 17f, 7f, paint);
                    canvas.drawLine(7f, 17f, 17f, 17f, paint);
                    break;
                case "frame-color":
                case "color-options":
                    canvas.drawCircle(12f, 12f, 8f, paint);
                    canvas.drawCircle(12f, 12f, 4f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(12f, 12f, 1.5f, paint);
                    break;
                case "border-width":
                    paint.setStrokeWidth(1f);
                    canvas.drawLine(4f, 5f, 20f, 5f, paint);
                    paint.setStrokeWidth(2.5f);
                    canvas.drawLine(4f, 12f, 20f, 12f, paint);
                    paint.setStrokeWidth(4f);
                    canvas.drawLine(4f, 19f, 20f, 19f, paint);
                    break;
                case "corners":
                    canvas.drawRoundRect(new RectF(3f, 3f, 21f, 21f), 7f, 7f, paint);
                    canvas.drawRoundRect(new RectF(7f, 7f, 17f, 17f), 2f, 2f, paint);
                    break;
                case "settings":
                    canvas.drawCircle(12f, 12f, 7.5f, paint);
                    canvas.drawCircle(12f, 12f, 2.5f, paint);
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2d * i) / 8d;
                        float x1 = 12f + (float) Math.cos(a) * 8f;
                        float y1 = 12f + (float) Math.sin(a) * 8f;
                        float x2 = 12f + (float) Math.cos(a) * 10f;
                        float y2 = 12f + (float) Math.sin(a) * 10f;
                        canvas.drawLine(x1, y1, x2, y2, paint);
                    }
                    break;
                default:
                    canvas.drawCircle(12f, 12f, 8f, paint);
                    break;
            }
            canvas.restore();
        }
    }

    private class ColorSwatchView extends View {
        private final String value;
        private boolean selected;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorSwatchView(Context context, String value, boolean selected) {
            super(context);
            this.value = value;
            this.selected = selected;
            setClickable(false);
        }

        void setSwatchSelected(boolean selected) {
            this.selected = selected;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - dp(selected ? 2 : 3);

            if (selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(ACCENT);
                canvas.drawCircle(cx, cy, radius, paint);
                radius -= dp(3);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colorValue(value));
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(CARD_OUTLINE);
            canvas.drawCircle(cx, cy, radius, paint);

            if (selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setColor(Color.rgb(8, 26, 42));
                Path check = new Path();
                check.moveTo(cx - dp(5), cy);
                check.lineTo(cx - dp(1), cy + dp(4));
                check.lineTo(cx + dp(6), cy - dp(5));
                canvas.drawPath(check, paint);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
