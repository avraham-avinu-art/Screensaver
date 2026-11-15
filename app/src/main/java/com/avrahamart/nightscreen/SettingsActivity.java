package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {
    private static final int REQUEST_AUTO_START_NOTIFICATION_PERMISSION = 8143;
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
    private LinearLayout currentSettingsGroup;
    private LinearLayout currentSettingsGroupParent;
    private String currentSettingsGroupTitle;
    private LinearLayout clockDateCard;
    private LinearLayout playerFrameCard;
    private Switch showPlayerToggle;
    private Switch autoStartOnChargeToggle;
    private Switch autoStartOnDeviceLockToggle;
    private Switch autoStartOnBootToggle;
    private Switch autoStartOnWakeToggle;
    private TextView activityCapabilityNotice;


    private TextView screenTitle;
    private TextView screenSubtitle;
    private ScrollView settingsScroll;
    private FrameLayout screenHeader;
    private LinearLayout headerTitleBlock;
    private View dashboardHeaderDivider;
    private int currentCategory = 0;
    private static final int CATEGORY_DASHBOARD = 0;
    private static final int CATEGORY_CLOCK_DATE = 1;
    private static final int CATEGORY_ACTIVITY = 2;
    private static final int CATEGORY_APPEARANCE = 3;
    private static final int CATEGORY_PLAYER = 4;
    private static final int CATEGORY_ADVANCED = 5;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        // Convert the previous inverse preference once, preserving an explicit user choice.
        LockBehaviorPreferences.isPreventionEnabled(this);

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
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setPadding(dp(10), dp(2), dp(10), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                view.setPadding(dp(10), dp(2) + topInset, dp(10), bottomInset);
                return insets;
            });
        }

        // FrameLayout keeps the heading independently positioned within the header.
        screenHeader = new FrameLayout(this);
        screenHeader.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        screenHeader.setPadding(dp(2), dp(8), dp(2), dp(14));

        headerTitleBlock = new LinearLayout(this);
        headerTitleBlock.setOrientation(LinearLayout.VERTICAL);
        headerTitleBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        headerTitleBlock.setGravity(Gravity.CENTER);
        headerTitleBlock.setPadding(0, 0, 0, 0);

        screenTitle = textView(getString(R.string.settings_activity_text_001), 27, ON_SURFACE);
        screenTitle.setIncludeFontPadding(false);
        screenTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        screenTitle.setLetterSpacing(-0.015f);
        screenTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        screenTitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        headerTitleBlock.addView(screenTitle, new LinearLayout.LayoutParams(-1, -2));

        screenSubtitle = textView(getString(R.string.settings_activity_text_002), 16, MUTED);
        screenSubtitle.setIncludeFontPadding(false);
        screenSubtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenSubtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        screenSubtitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        screenSubtitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        LinearLayout.LayoutParams headerSubtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        headerSubtitleParams.topMargin = 0;
        headerTitleBlock.addView(screenSubtitle, headerSubtitleParams);

        FrameLayout.LayoutParams titleBlockParams =
                new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        screenHeader.addView(headerTitleBlock, titleBlockParams);

        settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(true);
        settingsScroll.setVerticalScrollBarEnabled(false);
        settingsScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        settingsScroll.setClipToPadding(false);
        settingsScroll.setClipChildren(false);
        settingsScroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        settingsScroll.setPadding(0, dp(1), 0, 0);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scrollContent.setTextDirection(View.TEXT_DIRECTION_RTL);
        scrollContent.addView(screenHeader, new LinearLayout.LayoutParams(-1, -2));

        dashboardHeaderDivider = new View(this);
        GradientDrawable headerDividerGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{SURFACE, CARD_OUTLINE, ACCENT, CARD_OUTLINE, SURFACE});
        dashboardHeaderDivider.setBackground(headerDividerGradient);
        LinearLayout.LayoutParams headerDividerParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        headerDividerParams.bottomMargin = dp(17);
        scrollContent.addView(dashboardHeaderDivider, headerDividerParams);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setTextDirection(View.TEXT_DIRECTION_RTL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scrollContent.addView(content, new LinearLayout.LayoutParams(-1, -2));
        settingsScroll.addView(scrollContent, new ScrollView.LayoutParams(-1, -2));
        root.addView(settingsScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        buildDashboard();

        setContentView(root);
    }

@Override
    public void onBackPressed() {
        if (currentCategory != CATEGORY_DASHBOARD) {
            buildDashboard();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
if (content != null) {
            handlePlayerPermissionResult();
            handleChargerPermissionResult();
            handleLockStartPermissionResult();
            String disabledFeatures =
                    PermissionDependencySync.disableFeaturesWithoutPermissions(this);
            if (!disabledFeatures.isEmpty()) {
                AppToast.makeText(this, disabledFeatures, Toast.LENGTH_LONG).show();
            }
            refreshPermissionDependentToggles();
            LockScreenMonitorService.sync(this);
            NightScreenApplication.syncChargerReceiver(this);
            ensureAutoStartNotificationPermission();
            updateDeviceCapabilityNotice();
        }
    }

    private void buildDashboard() {
        currentCategory = CATEGORY_DASHBOARD;
        clockDateCard = null;
        playerFrameCard = null;
        activityCapabilityNotice = null;
if (content == null) return;
        if (settingsScroll != null) settingsScroll.setFillViewport(true);
        content.setPadding(0, 0, 0, 0);
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        content.removeAllViews();
        currentSettingsGroup = null;
        currentSettingsGroupParent = null;
        currentSettingsGroupTitle = null;
screenTitle.setText(getString(R.string.settings_activity_text_001));
        screenSubtitle.setText(getString(R.string.settings_activity_text_002));

        screenHeader.setVisibility(View.VISIBLE);
        // The dashboard uses only the right-aligned "הגדרות" title.
        screenHeader.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        screenHeader.setPadding(dp(2), dp(7), dp(2), dp(4));
        screenSubtitle.setVisibility(View.GONE);
        screenTitle.setTextSize(27);
        dashboardHeaderDivider.setVisibility(View.GONE);
        headerTitleBlock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        screenSubtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenSubtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);

        addDashboardButton(getString(R.string.settings_activity_text_003),
                getString(R.string.settings_activity_text_004),
                "power-category", CATEGORY_ACTIVITY);
        addDashboardButton(getString(R.string.settings_activity_text_005),
                getString(R.string.settings_activity_text_006),
                "palette", CATEGORY_APPEARANCE);
        addDashboardButton(getString(R.string.settings_activity_text_007),
                getString(R.string.settings_activity_text_008),
                "calendar-clock", CATEGORY_CLOCK_DATE);
        addDashboardButton(getString(R.string.settings_activity_text_009),
                getString(R.string.settings_activity_text_010),
                "music-note", CATEGORY_PLAYER);
        addDashboardButton(getString(R.string.settings_activity_text_011),
                getString(R.string.settings_activity_text_012),
                "tune", CATEGORY_ADVANCED);

        addDashboardNavigation();
        // Do not leave an extra gap below the final item at the bottom of the screen.
        if (content.getChildCount() > 0) {
            View lastItem = content.getChildAt(content.getChildCount() - 1);
            if (lastItem.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lastParams =
                        (LinearLayout.LayoutParams) lastItem.getLayoutParams();
                lastParams.bottomMargin = dp(12);
                lastItem.setLayoutParams(lastParams);
            }
        }

        // The dashboard and every inner category share this ScrollView.
        // Reset its position when returning from a scrolled category.
        if (settingsScroll != null) {
            settingsScroll.scrollTo(0, 0);
            settingsScroll.post(() -> settingsScroll.scrollTo(0, 0));
        }
    }

    private LinearLayout.LayoutParams dashboardButtonParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        params.bottomMargin = dp(7);
        return params;
    }

    private void addDashboardButton(String title, String description,
                                    String iconName, int category) {
        addDashboardActionButton(title, description, iconName,
                () -> showCategory(category));
    }

    private void addDashboardNavigation() {
        addDashboardNavigationButton(getString(R.string.settings_activity_text_013), "shield-alert",
                () -> startActivity(new Intent(this, PermissionsActivity.class)));
        addDashboardNavigationButton(getString(R.string.settings_activity_text_014), "book-open",
                () -> startActivity(new Intent(this, GuideActivity.class)));
    }

    private void addDashboardNavigationButton(String title, String iconName,
                                              Runnable action) {
        addDashboardActionButton(title, null, iconName, action);
    }

    private void addDashboardActionButton(String title, String description,
                                          String iconName, Runnable action) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(14), dp(12), dp(23), dp(12));
        button.setMinimumHeight(dp(99));
        button.setBackground(roundOutline(CARD_SURFACE, dp(19), CARD_OUTLINE));
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(description == null || description.isEmpty()
                ? title : title + ". " + description);

        FrameLayout iconBadge = new FrameLayout(this);
        iconBadge.setBackground(roundOutline(ICON_SURFACE, dp(24), ACCENT));
        View glyph = makeIconGlyph(iconName, ACCENT);
        glyph.setTranslationX(0f);
        glyph.setTranslationY(0f);
        iconBadge.addView(glyph,
                new FrameLayout.LayoutParams(dp(35), dp(35), Gravity.CENTER));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.leftMargin = dp(8);
        button.addView(iconBadge, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        copy.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = textView(title, 19, ON_SURFACE);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        titleView.setTextDirection(View.TEXT_DIRECTION_RTL);
        copy.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        if (description != null && !description.isEmpty()) {
            TextView descriptionView = textView(description, 15, MUTED);
            descriptionView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            descriptionView.setIncludeFontPadding(false);
            descriptionView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            descriptionView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            descriptionView.setTextDirection(View.TEXT_DIRECTION_RTL);
            descriptionView.setSingleLine(true);
            descriptionView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams descriptionParams =
                    new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(3);
            copy.addView(descriptionView, descriptionParams);
        }

        button.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        button.setOnClickListener(v -> action.run());
        content.addView(button, dashboardButtonParams());
    }

    private void showCategory(int category) {
        currentCategory = category;
screenHeader.setVisibility(View.GONE);
        screenSubtitle.setVisibility(View.GONE);
        if (settingsScroll != null) settingsScroll.setFillViewport(true);
        content.setPadding(0, dp(10), 0, 0);
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        content.removeAllViews();
        currentSettingsGroup = null;
        currentSettingsGroupParent = null;
        currentSettingsGroupTitle = null;
        clockDateCard = null;
        playerFrameCard = null;
        activityCapabilityNotice = null;


        switch (category) {
            case CATEGORY_CLOCK_DATE:
                buildClockDateSettings();
                break;
            case CATEGORY_ACTIVITY:
                buildActivitySettings();
                break;
            case CATEGORY_APPEARANCE:
                buildAppearanceSettings();
                break;
            case CATEGORY_PLAYER:
                buildPlayerSettings();
                break;
            case CATEGORY_ADVANCED:
                buildAdvancedSettings();
                break;
            default:
                buildDashboard();
                return;
        }

        finishSettingsGroup();
        if (settingsScroll != null) {
            settingsScroll.scrollTo(0, 0);
            settingsScroll.post(() -> settingsScroll.scrollTo(0, 0));
        }
    }

    private void finishSettingsGroup() {
        if (currentSettingsGroup != null
                && currentSettingsGroupParent == content
                && currentSettingsGroup.getParent() == content) {
            // Keep the frame's internal padding unchanged; put the clearance
            // outside the last frame, matching the horizontal screen inset.
            currentSettingsGroup.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) currentSettingsGroup.getLayoutParams();
            params.bottomMargin = dp(10);
            currentSettingsGroup.setLayoutParams(params);
        }
    }

    

    

    
    private void buildClockDateSettings() {
        addCheck(content, getString(R.string.settings_activity_text_015), "showHebrew", true);
        String[] hebrewDateFormats = {"plain", "yearArticle", "monthBet", "monthBetYearArticle"};
        addSpinner(content, getString(R.string.settings_activity_text_016),
                currentHebrewDateSamples(), hebrewDateFormats, "hebrewDateFormat");
        addCheck(content, getString(R.string.settings_activity_text_017), "showGregorian", true);
        String[] gregorianDateFormats = {
                "dd.MM.yyyy", "dd.MM.yy", "dd/MM/yy",
                "dd/MM/yyyy", "yyyy/MM/dd", getString(R.string.settings_activity_text_018)
        };
        addSpinner(content, getString(R.string.settings_activity_text_019),
                currentGregorianDateSamples(gregorianDateFormats),
                gregorianDateFormats, "gregorianDateFormat");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_020),
                new String[]{getString(R.string.settings_activity_text_021), getString(R.string.settings_activity_text_022)},
                new String[]{"24", "12"}, "timeFormat");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_023),
                new String[]{getString(R.string.settings_activity_text_024), getString(R.string.settings_activity_text_025)},
                new String[]{"digital", "analog"}, "clockDisplayMode");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_026),
                new String[]{getString(R.string.settings_activity_text_027), getString(R.string.settings_activity_text_028), getString(R.string.settings_activity_text_029)},
                new String[]{"numbers_only", "lines_only", "lines_numbers"}, "clockDialStyle");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_030),
                new String[]{getString(R.string.settings_activity_text_031), getString(R.string.settings_activity_text_032), getString(R.string.settings_activity_text_033), getString(R.string.settings_activity_text_034), getString(R.string.settings_activity_text_035)},
                new String[]{"60", "72", "84", "96", "108"}, "clockDateSize");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_036),
                new String[]{getString(R.string.settings_activity_text_037), getString(R.string.settings_activity_text_033), getString(R.string.settings_activity_text_038), getString(R.string.settings_activity_text_039)},
                new String[]{"-1", "0", "1", "2"}, "fontWeight");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_040),
                new String[]{getString(R.string.settings_activity_text_041), getString(R.string.settings_activity_text_042), getString(R.string.settings_activity_text_043)},
                new String[]{"right", "center", "left"}, "clockTextAlignment");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_044),
                new String[]{getString(R.string.settings_activity_text_045), getString(R.string.settings_activity_text_046), getString(R.string.settings_activity_text_047)},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");
        addLineSlider(content, getString(R.string.settings_activity_text_048), "clockHorizontalOffset", -20, 20);
    }

    private String[] currentGregorianDateSamples(String[] patterns) {
        Calendar today = Calendar.getInstance();
        String[] samples = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            Locale locale = getString(R.string.settings_activity_text_018).equals(patterns[i])
                    ? new Locale("he", "IL") : Locale.US;
            SimpleDateFormat formatter = new SimpleDateFormat(patterns[i], locale);
            formatter.setTimeZone(today.getTimeZone());
            samples[i] = formatter.format(today.getTime());
        }
        return samples;
    }

    private String[] currentHebrewDateSamples() {
        try {
            Calendar today = Calendar.getInstance();
            JewishDate jewishDate = new JewishDate(today);
            HebrewDateFormatter formatter = new HebrewDateFormatter();
            formatter.setHebrewFormat(true);
            formatter.setUseGershGershayim(true);
            formatter.setUseLongHebrewYears(false);
            String rawDate = formatter.format(jewishDate);
            return new String[]{
                    formatHebrewDateSample(rawDate, "plain"),
                    formatHebrewDateSample(rawDate, "yearArticle"),
                    formatHebrewDateSample(rawDate, "monthBet"),
                    formatHebrewDateSample(rawDate, "monthBetYearArticle")
            };
        } catch (Throwable ignored) {
            return new String[]{"", "", "", ""};
        }
    }

    private String formatHebrewDateSample(String rawDate, String format) {
        String normalized = rawDate
                .replace(getString(R.string.settings_activity_text_049), getString(R.string.settings_activity_text_050))
                .replace(getString(R.string.settings_activity_text_051), getString(R.string.settings_activity_text_050))
                .replace(getString(R.string.settings_activity_text_052), getString(R.string.settings_activity_text_050))
                .replace("׳", "'")
                .replace("״", "\"")
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
            month = getString(R.string.settings_activity_text_053) + month;
        }
        if ("yearArticle".equals(format) || "monthBetYearArticle".equals(format)) {
            year = getString(R.string.settings_activity_text_054) + year;
        }
        return day + " " + month + " " + year;
    }

    
    private void buildActivitySettings() {
        LinearLayout durationCard = addSegmentedChoices(content, getString(R.string.settings_activity_text_056),
                new String[]{getString(R.string.settings_activity_text_057), getString(R.string.settings_activity_text_058), getString(R.string.settings_activity_text_059), getString(R.string.settings_activity_text_060)},
                new String[]{"device", "1", "5", "0"}, "screenSaverDuration");
        addActivityCapabilityNotice(durationCard);
        addCheck(content, getString(R.string.settings_activity_text_061), "preventLockWhileActive", false);
        addCheck(content, getString(R.string.settings_activity_text_062), "continueOnLockScreen", false);
        addCheck(content, getString(R.string.settings_activity_text_063), "autoStartOnDeviceLock", false);
        addCheck(content, getString(R.string.settings_activity_text_064), "autoStartOnBoot", false);
        addCheck(content, getString(R.string.settings_activity_text_065), "autoStartOnWake", false);
        addCheck(content, getString(R.string.settings_activity_text_066), "autoStartOnCharge", false);
        addCheck(content, getString(R.string.settings_activity_text_068), "noShutdownWhileCharging", false);
        addSegmentedChoices(content, getString(R.string.settings_activity_text_069),
                new String[]{getString(R.string.settings_activity_text_070), getString(R.string.settings_activity_text_071), getString(R.string.settings_activity_text_072), getString(R.string.settings_activity_text_073)},
                new String[]{"off", "percent", "icon", "icon_percent"},
                "chargingBatteryDisplayMode");
    }

    private void addExcludedAppsPicker(LinearLayout parent) {
        LinearLayout card = parent;
        Button choose = makeActionButton(getString(R.string.settings_activity_text_074));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, dp(42));
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.topMargin = dp(14);
        card.addView(choose, buttonParams);
        choose.setOnClickListener(v -> {
            if (!hasUsageAccess()) {
                AppToast.makeText(this,
                        getString(R.string.settings_activity_text_075),
                        Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Throwable ignored) {
                    AppToast.makeText(this, getString(R.string.settings_activity_text_076),
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
            showExcludedAppsDialog();
        });
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager manager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            if (manager == null) return false;
            return manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showExcludedAppsDialog() {
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> resolved =
                getPackageManager().queryIntentActivities(launcher, 0);
        java.util.Map<String, CharSequence> labels = new java.util.TreeMap<>(
                (left, right) -> {
                    try {
                        return getPackageManager().getApplicationLabel(
                                getPackageManager().getApplicationInfo(left, 0)).toString()
                                .compareToIgnoreCase(getPackageManager().getApplicationLabel(
                                getPackageManager().getApplicationInfo(right, 0)).toString());
                    } catch (Throwable ignored) {
                        return left.compareToIgnoreCase(right);
                    }
                });
        for (android.content.pm.ResolveInfo info : resolved) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName)) continue;
            try {
                CharSequence label = info.loadLabel(getPackageManager());
                labels.put(packageName, label == null ? packageName : label);
            } catch (Throwable ignored) {
                labels.put(packageName, packageName);
            }
        }

        List<String> packages = new ArrayList<>(labels.keySet());
        List<String> names = new ArrayList<>();
        boolean[] checked = new boolean[packages.size()];
        Set<String> selected = new HashSet<>(
                prefs.getStringSet("excludedForegroundPackages", Collections.emptySet()));
        for (int i = 0; i < packages.size(); i++) {
            names.add(String.valueOf(labels.get(packages.get(i))));
            checked[i] = selected.contains(packages.get(i));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_activity_text_077))
                .setMultiChoiceItems(names.toArray(new String[0]), checked,
                        (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(getString(R.string.settings_activity_text_078), null)
                .setPositiveButton(getString(R.string.settings_activity_text_079), (d, which) -> {
                    Set<String> updated = new HashSet<>();
                    for (int i = 0; i < packages.size(); i++) {
                        if (checked[i]) updated.add(packages.get(i));
                    }
                    prefs.edit().putStringSet("excludedForegroundPackages", updated).apply();
                })
                .create();
        dialog.show();
    }

    
    private void buildAppearanceSettings() {
        addColorChoices(content, getString(R.string.settings_activity_text_080),
                new String[]{getString(R.string.settings_activity_text_081), getString(R.string.settings_activity_text_082), getString(R.string.settings_activity_text_083), getString(R.string.settings_activity_text_084),
                        getString(R.string.settings_activity_text_085), getString(R.string.settings_activity_text_086), getString(R.string.settings_activity_text_087), getString(R.string.settings_activity_text_088)},
                new String[]{"#000000", "#142438", "#091321", "#12313A",
                        "#252B33", "#20362E", "#193740", "#303943"},
                "screenBackgroundColor");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_089),
                new String[]{getString(R.string.settings_activity_text_090), getString(R.string.settings_activity_text_091), getString(R.string.settings_activity_text_092), getString(R.string.settings_activity_text_093)},
                new String[]{"solid", "gradient", "glow", "vignette"}, "backgroundStyle");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_094),
                new String[]{getString(R.string.settings_activity_text_057), "25%", "50%", "75%", "100%"},
                new String[]{"-1", "25", "50", "75", "100"}, "screenBrightness");
        addSpinner(content, getString(R.string.settings_activity_text_095),
                new String[]{getString(R.string.settings_activity_text_096), "DL Adva", "Keter Aram Tsova", "Shofar",
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
        addColorChoices(content, getString(R.string.settings_activity_text_097),
                new String[]{getString(R.string.settings_activity_text_098), getString(R.string.settings_activity_text_099), getString(R.string.settings_activity_text_100), getString(R.string.settings_activity_text_101), getString(R.string.settings_activity_text_102)},
                new String[]{"white", "blue", "silver", "gray", "light"}, "fontColor");
    }

    
    private void buildPlayerSettings() {
        addSegmentedChoices(content, getString(R.string.settings_activity_text_103),
                new String[]{getString(R.string.settings_activity_text_104), getString(R.string.settings_activity_text_105), getString(R.string.settings_activity_text_106)},
                new String[]{"off", "on", "start"}, "playerDisplayMode");
        addCheck(content, getString(R.string.settings_activity_text_107), "showDetails", getBool("showDetails", true));
        addSegmentedChoices(content, getString(R.string.settings_activity_text_108),
                new String[]{getString(R.string.settings_activity_text_104), getString(R.string.settings_activity_text_105), getString(R.string.settings_activity_text_106)},
                new String[]{"off", "on", "start"}, "playerControlsDisplayMode");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_109),
                new String[]{getString(R.string.settings_activity_text_031), getString(R.string.settings_activity_text_032), getString(R.string.settings_activity_text_034), getString(R.string.settings_activity_text_035)},
                new String[]{"14", "17", "20", "23"}, "playerSize");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_110),
                new String[]{getString(R.string.settings_activity_text_111), getString(R.string.settings_activity_text_112)},
                new String[]{"lower", "bottom"}, "playerPosition");
        addSegmentedChoices(content, getString(R.string.settings_activity_text_040),
                new String[]{getString(R.string.settings_activity_text_041), getString(R.string.settings_activity_text_042), getString(R.string.settings_activity_text_043)},
                new String[]{"right", "center", "left"}, "playerTextAlignment");
        addLineSlider(content, getString(R.string.settings_activity_text_113), "playerHorizontalOffset", -20, 20);
        addCheck(content, getString(R.string.settings_activity_text_114), "frameEnabled", false);
        addCheck(content, getString(R.string.settings_activity_text_115), "frameUseAlbumArt",
                getBool("frameUseAlbumArt", true));
        addColorChoices(content, getString(R.string.settings_activity_text_116),
                new String[]{getString(R.string.settings_activity_text_098), getString(R.string.settings_activity_text_099), getString(R.string.settings_activity_text_100), getString(R.string.settings_activity_text_101), getString(R.string.settings_activity_text_102)},
                new String[]{"white", "blue", "silver", "gray", "light"}, "frameColor");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_117),
                new String[]{getString(R.string.settings_activity_text_037), getString(R.string.settings_activity_text_033), getString(R.string.settings_activity_text_039)},
                new String[]{"1", "2", "3"}, "frameWidth");
        addDiscreteSlider(content, getString(R.string.settings_activity_text_118),
                new String[]{getString(R.string.settings_activity_text_119), getString(R.string.settings_activity_text_120), getString(R.string.settings_activity_text_121)},
                new String[]{"0", "10", "20"}, "frameRadius");
    }

    
    private void buildAdvancedSettings() {
        if (!prefs.contains("batteryProtectionThreshold")) {
            prefs.edit().putInt("batteryProtectionThreshold", 20).apply();
        }
        if ("system".equals(getString("timeFormat", "24"))) {
            prefs.edit().putString("timeFormat", "24").apply();
        }
        addCheck(content, getString(R.string.settings_activity_text_122), "nightModeEnabled", false);
        String[] hourNames = new String[24];
        String[] hourValues = new String[24];
        for (int hour = 0; hour < 24; hour++) {
            hourNames[hour] = String.format(Locale.US, "%02d:00", hour);
            hourValues[hour] = String.valueOf(hour);
        }
        addSpinner(content, getString(R.string.settings_activity_text_123), hourNames, hourValues, "nightModeStartHour");
        addSpinner(content, getString(R.string.settings_activity_text_124), hourNames, hourValues, "nightModeEndHour");

        addCheck(content, getString(R.string.settings_activity_text_125),
                "batteryProtectionEnabled", false);
        addDiscreteSlider(content, getString(R.string.settings_activity_text_126),
                new String[]{"10%", "15%", "20%", "25%", "30%"},
                new String[]{"10", "15", "20", "25", "30"},
                "batteryProtectionThreshold");

        addCheck(content, getString(R.string.settings_activity_text_127),
                "useAndroidDreamForCharging", getBool("useAndroidDreamForCharging", false));

        addCheck(content, getString(R.string.settings_activity_text_128),
                "pauseWhenExcludedAppActive", false);
        if (currentSettingsGroup != null && currentSettingsGroup.getChildCount() > 1) {
            View exclusionCard = currentSettingsGroup.getChildAt(
                    currentSettingsGroup.getChildCount() - 1);
            if (exclusionCard instanceof LinearLayout) {
                addExcludedAppsPicker((LinearLayout) exclusionCard);
            }
        }
        addCheck(content, getString(R.string.settings_activity_text_129),
                "preventAutoStartDuringUse", false);
        addCheck(content, getString(R.string.settings_activity_text_067),
                "autoStartOnlyWhenLocked", false);
        addCheck(content, getString(R.string.settings_activity_text_130),
                "settingsButtonAlwaysVisible", false);
    }

    private String settingsGroupTitleFor(String key) {
        switch (currentCategory) {
            case CATEGORY_CLOCK_DATE:
                if ("timeFormat".equals(key) || "clockDisplayMode".equals(key)
                        || "clockDialStyle".equals(key)) return getString(R.string.settings_activity_text_131);
                if ("showHebrew".equals(key) || "showGregorian".equals(key)
                        || "hebrewDateFormat".equals(key) || "gregorianDateFormat".equals(key)) {
                    return getString(R.string.settings_activity_text_132);
                }
                return getString(R.string.settings_activity_text_133);
            case CATEGORY_ACTIVITY:
                if ("screenSaverDuration".equals(key)
                        || "preventLockWhileActive".equals(key)
                        || "continueOnLockScreen".equals(key)) {
                    return getString(R.string.settings_activity_text_134);
                }
                if ("autoStartOnDeviceLock".equals(key) || "autoStartOnBoot".equals(key)
                        || "autoStartOnWake".equals(key)
                        || "autoStartOnCharge".equals(key)) {
                    return getString(R.string.settings_activity_text_135);
                }
                return getString(R.string.settings_activity_text_136);
            case CATEGORY_APPEARANCE:
                        if ("screenBackgroundColor".equals(key) || "screenBrightness".equals(key)
                        || "backgroundStyle".equals(key)) {
                    return getString(R.string.settings_activity_text_137);
                }
                return getString(R.string.settings_activity_text_138);
            case CATEGORY_PLAYER:
                if ("frameEnabled".equals(key) || "frameUseAlbumArt".equals(key)
                        || "frameColor".equals(key) || "frameWidth".equals(key)
                        || "frameRadius".equals(key)) {
                    return getString(R.string.settings_activity_text_139);
                }
                return getString(R.string.settings_activity_text_140);
            case CATEGORY_ADVANCED:
                if ("useAndroidDreamForCharging".equals(key)) return getString(R.string.settings_activity_text_066);
                if ("settingsButtonAlwaysVisible".equals(key)) return getString(R.string.settings_activity_text_141);
                if ("nightModeEnabled".equals(key) || "nightModeStartHour".equals(key)
                        || "nightModeEndHour".equals(key)) return getString(R.string.settings_activity_text_142);
                if ("batteryProtectionEnabled".equals(key)
                        || "batteryProtectionThreshold".equals(key)) return getString(R.string.settings_activity_text_143);
                if ("pauseWhenExcludedAppActive".equals(key)
                        || "preventAutoStartDuringUse".equals(key)
                        || "autoStartOnlyWhenLocked".equals(key)) {
                    return getString(R.string.settings_activity_text_144);
                }
                return getString(R.string.settings_activity_text_011);
            default:
                return getString(R.string.settings_activity_text_001);
        }
    }

    private void addSettingsGroupHeading(LinearLayout group, String label,
                                         boolean showActivityInfo) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView heading = textView(label, 18, Color.rgb(79, 195, 247));
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        heading.setIncludeFontPadding(false);
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        heading.setPadding(0, 0, dp(7), 0);
        row.addView(heading, new LinearLayout.LayoutParams(0, dp(26), 1f));

        if (showActivityInfo) {
            FrameLayout infoButton = new FrameLayout(this);
            infoButton.setContentDescription(getString(R.string.settings_activity_text_055));
            infoButton.setClickable(true);
            infoButton.setFocusable(true);
            infoButton.setBackgroundColor(Color.TRANSPARENT);
            View glyph = makeIconGlyph("message-circle-question", ACCENT);
            infoButton.addView(glyph,
                    new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
            infoButton.setOnClickListener(v -> showActivityTimeHelpDialog());
            row.addView(infoButton, new LinearLayout.LayoutParams(dp(36), dp(30)));
        }

        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, dp(26));
        headingParams.topMargin = dp(15);
        headingParams.bottomMargin = dp(8);
        headingParams.leftMargin = dp(14);
        headingParams.rightMargin = dp(5);
        group.addView(row, headingParams);

        // Match the heading rule to the separators between setting rows.
        View headingDivider = new View(this);
        headingDivider.setBackgroundColor(Color.rgb(54, 78, 102));
        LinearLayout.LayoutParams headingDividerParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        headingDividerParams.leftMargin = dp(13);
        headingDividerParams.rightMargin = dp(13);
        group.addView(headingDivider, headingDividerParams);
    }

    private LinearLayout addSettingCard(LinearLayout parent, String label, String key) {
        String groupTitle = settingsGroupTitleFor(key);
        boolean beginsNewGroup = currentSettingsGroup == null
                || currentSettingsGroupParent != parent
                || !groupTitle.equals(currentSettingsGroupTitle);

        if (beginsNewGroup) {
            if (currentSettingsGroup != null
                    && currentSettingsGroupParent == parent) {
                LinearLayout.LayoutParams previousGroupParams =
                        (LinearLayout.LayoutParams) currentSettingsGroup.getLayoutParams();
                previousGroupParams.bottomMargin = dp(11);
                currentSettingsGroup.setLayoutParams(previousGroupParams);
            }

            currentSettingsGroup = new LinearLayout(this);
            currentSettingsGroup.setOrientation(LinearLayout.VERTICAL);
            currentSettingsGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            currentSettingsGroup.setClipChildren(false);
            currentSettingsGroup.setClipToPadding(false);
            currentSettingsGroup.setBackground(
                    roundOutline(CARD_SURFACE, dp(17), CARD_OUTLINE));
            LinearLayout.LayoutParams groupParams =
                    new LinearLayout.LayoutParams(-1, -2);
            groupParams.bottomMargin = 0;
            parent.addView(currentSettingsGroup, groupParams);
            currentSettingsGroupParent = parent;
            currentSettingsGroupTitle = groupTitle;
            addSettingsGroupHeading(currentSettingsGroup, groupTitle,
                    currentCategory == CATEGORY_ACTIVITY
                            && getString(R.string.settings_activity_text_134).equals(groupTitle));
        } else if (currentSettingsGroup.getChildCount() > 1) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(54, 78, 102));
            LinearLayout.LayoutParams dividerParams =
                    new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.leftMargin = dp(13);
            dividerParams.rightMargin = dp(13);
            currentSettingsGroup.addView(divider, dividerParams);
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setTextDirection(View.TEXT_DIRECTION_RTL);
        card.setPadding(dp(7), dp(12), dp(7), dp(14));
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = 0;
        currentSettingsGroup.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClipChildren(false);
        header.setClipToPadding(false);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        textBlock.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = textView(label, 16, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        textBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        String descriptionText = settingDescription(key);
        if (descriptionText != null && !descriptionText.isEmpty()) {
            TextView description = textView(descriptionText, 13, MUTED);
            description.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            description.setIncludeFontPadding(false);
            description.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            description.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            LinearLayout.LayoutParams descriptionParams =
                    new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(3);
            textBlock.addView(description, descriptionParams);
        }

        LinearLayout.LayoutParams textBlockParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        textBlockParams.rightMargin = dp(7);
        header.addView(textBlock, textBlockParams);
        LinearLayout.LayoutParams settingIconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        settingIconParams.rightMargin = dp(5);
        header.addView(makeSettingIconBadge(key, label), settingIconParams);

        card.addView(header, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private String settingDescription(String key) {
        if ("showHebrew".equals(key)) return getString(R.string.settings_activity_text_145);
        if ("showGregorian".equals(key)) return getString(R.string.settings_activity_text_146);
        if ("hebrewDateFormat".equals(key)) return getString(R.string.settings_activity_text_147);
        if ("gregorianDateFormat".equals(key)) return getString(R.string.settings_activity_text_148);
        if ("clockDateSize".equals(key)) return getString(R.string.settings_activity_text_149);
        if ("clockDisplayMode".equals(key)) return getString(R.string.settings_activity_text_150);
        if ("clockDialStyle".equals(key)) return getString(R.string.settings_activity_text_151);
        if ("clockTextAlignment".equals(key)) return getString(R.string.settings_activity_text_152);
        if ("playerTextAlignment".equals(key)) return getString(R.string.settings_activity_text_153);
        if ("autoStartOnlyWhenLocked".equals(key)) return getString(R.string.settings_activity_text_154);
        if ("preventAutoStartDuringUse".equals(key)) return getString(R.string.settings_activity_text_155);
        if ("pauseWhenExcludedAppActive".equals(key)) return getString(R.string.settings_activity_text_156);
        if ("fontWeight".equals(key)) return getString(R.string.settings_activity_text_157);
        if ("clockDatePosition".equals(key)) return getString(R.string.settings_activity_text_158);
        if ("screenSaverDuration".equals(key)) return getString(R.string.settings_activity_text_159);
        if ("preventLockWhileActive".equals(key)) return getString(R.string.settings_activity_text_160);
        if ("continueOnLockScreen".equals(key)) return getString(R.string.settings_activity_text_161);
        if ("autoStartOnDeviceLock".equals(key)) return getString(R.string.settings_activity_text_162);
        if ("autoStartOnBoot".equals(key)) return getString(R.string.settings_activity_text_163);
        if ("autoStartOnWake".equals(key)) return getString(R.string.settings_activity_text_164);
        if ("autoStartOnCharge".equals(key)) return getString(R.string.settings_activity_text_165);
        if ("noShutdownWhileCharging".equals(key)) return getString(R.string.settings_activity_text_166);
        if ("screenBackgroundColor".equals(key)) return getString(R.string.settings_activity_text_167);
        if ("screenBrightness".equals(key)) return getString(R.string.settings_activity_text_168);
        if ("fontFamily".equals(key)) return getString(R.string.settings_activity_text_169);
        if ("fontColor".equals(key)) return getString(R.string.settings_activity_text_170);
        if ("settingsButtonAlwaysVisible".equals(key)) return getString(R.string.settings_activity_text_171);
        if ("playerDisplayMode".equals(key)) return getString(R.string.settings_activity_text_172);
        if ("showPlayerOnlyAtStart".equals(key)) return getString(R.string.settings_activity_text_173);
        if ("showDetails".equals(key)) return getString(R.string.settings_activity_text_174);
        if ("playerControlsDisplayMode".equals(key)) return getString(R.string.settings_activity_text_175);
        if ("chargingBatteryDisplayMode".equals(key)) return getString(R.string.settings_activity_text_176);
        if ("clockHorizontalOffset".equals(key)) return getString(R.string.settings_activity_text_177);
        if ("playerHorizontalOffset".equals(key)) return getString(R.string.settings_activity_text_178);
        if ("backgroundStyle".equals(key)) return getString(R.string.settings_activity_text_179);
        if ("nightModeEnabled".equals(key)) return getString(R.string.settings_activity_text_180);
        if ("nightModeStartHour".equals(key)) return getString(R.string.settings_activity_text_181);
        if ("nightModeEndHour".equals(key)) return getString(R.string.settings_activity_text_182);
        if ("timeFormat".equals(key)) return getString(R.string.settings_activity_text_183);
        if ("batteryProtectionEnabled".equals(key)) return getString(R.string.settings_activity_text_184);
        if ("useAndroidDreamForCharging".equals(key)) return getString(R.string.settings_activity_text_185);
        if ("batteryProtectionThreshold".equals(key)) return getString(R.string.settings_activity_text_186);
        if ("playerSize".equals(key)) return getString(R.string.settings_activity_text_187);
        if ("playerPosition".equals(key)) return getString(R.string.settings_activity_text_188);
        if ("frameEnabled".equals(key)) return getString(R.string.settings_activity_text_189);
        if ("frameUseAlbumArt".equals(key)) return getString(R.string.settings_activity_text_190);
        if ("frameColor".equals(key)) return getString(R.string.settings_activity_text_191);
        if ("frameWidth".equals(key)) return getString(R.string.settings_activity_text_192);
        if ("frameRadius".equals(key)) return getString(R.string.settings_activity_text_193);
        return "";
    }

    
    private void showActivityTimeHelpDialog() {
        String help =
                getString(R.string.settings_activity_text_194)
                + getString(R.string.settings_activity_text_195)
                + getString(R.string.settings_activity_text_196)
                + getString(R.string.settings_activity_text_197)
                + getString(R.string.settings_activity_text_198)
                + getString(R.string.settings_activity_text_199)
                + getString(R.string.settings_activity_text_200)
                + getString(R.string.settings_activity_text_201)
                + getString(R.string.settings_activity_text_202)
                + getString(R.string.settings_activity_text_203)
                + getString(R.string.settings_activity_text_204)
                + getString(R.string.settings_activity_text_205)
                + getString(R.string.settings_activity_text_206)
                + getString(R.string.settings_activity_text_207)
                + getString(R.string.settings_activity_text_208)
                + getString(R.string.settings_activity_text_209)
                + getString(R.string.settings_activity_text_210)
                + getString(R.string.settings_activity_text_211)
                + getString(R.string.settings_activity_text_212)
                + getString(R.string.settings_activity_text_213)
                + getString(R.string.settings_activity_text_214)
                + getString(R.string.settings_activity_text_215)
                + getString(R.string.settings_activity_text_216)
                + getString(R.string.settings_activity_text_217)                + getString(R.string.settings_activity_text_218)
                + getString(R.string.settings_activity_text_219)
                + getString(R.string.settings_activity_text_220)
                + getString(R.string.settings_activity_text_221);

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setTextDirection(View.TEXT_DIRECTION_RTL);
        panel.setPadding(dp(18), dp(14), dp(18), dp(13));
        panel.setBackground(roundOutline(CARD_SURFACE, dp(22), CARD_OUTLINE));
        if (Build.VERSION.SDK_INT >= 21) panel.setElevation(dp(3));

        // The heading and help text scroll as one. The close X stays fixed and accessible.
        FrameLayout scrollViewport = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);
        scrollViewport.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scrollContent.setTextDirection(View.TEXT_DIRECTION_RTL);
        scrollContent.setPadding(0, 0, 0, dp(4));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        // Reserve the physical upper-left corner for the fixed close button.
        heading.setPadding(dp(40), 0, 0, 0);

        FrameLayout helpIconShell = new FrameLayout(this);
        helpIconShell.setBackgroundColor(Color.TRANSPARENT);
        View helpIcon = makeIconGlyph("message-circle-question", ACCENT);
        helpIconShell.addView(helpIcon,
                new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER));
        heading.addView(helpIconShell, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setGravity(Gravity.CENTER_VERTICAL);
        // Extra spacing between the icon and title block.
        titleBlock.setPadding(0, 0, 0, 0);

        TextView title = textView(getString(R.string.settings_activity_text_134), 20, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        title.setIncludeFontPadding(false);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView(getString(R.string.settings_activity_text_222), 14, MUTED);
        subtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        subtitle.setIncludeFontPadding(false);
        subtitle.setPadding(0, 0, 0, 0);
        titleBlock.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams titleBlockParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        // Add spacing at the physical side next to the icon (the heading is RTL).
        titleBlockParams.rightMargin = dp(1);
        heading.addView(titleBlock, titleBlockParams);
        scrollContent.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        View divider = new View(this);
        divider.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(9);
        dividerParams.bottomMargin = dp(10);
        scrollContent.addView(divider, dividerParams);

        TextView message = textView("", 15, ON_SURFACE);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        message.setLineSpacing(dp(3), 1.0f);
        message.setPadding(dp(2), dp(2), dp(2), dp(4));

        SpannableStringBuilder styledHelp = new SpannableStringBuilder();
        String[] lines = help.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineStart = styledHelp.length();
            styledHelp.append(line);
            int lineEnd = styledHelp.length();

            boolean sectionHeading =
                    line.equals(getString(R.string.settings_activity_text_223))
                    || line.equals(getString(R.string.settings_activity_text_061))
                    || line.equals(getString(R.string.settings_activity_text_224))
                    || line.equals(getString(R.string.settings_activity_text_063))
                    || line.equals(getString(R.string.settings_activity_text_064))
                    || line.equals(getString(R.string.settings_activity_text_065))
                    || line.equals(getString(R.string.settings_activity_text_225));

            if (sectionHeading) {
                styledHelp.setSpan(new ForegroundColorSpan(ACCENT), lineStart, lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledHelp.setSpan(new StyleSpan(Typeface.BOLD), lineStart, lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledHelp.setSpan(new RelativeSizeSpan(1.04f), lineStart, lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.startsWith("•")) {
                styledHelp.setSpan(new ForegroundColorSpan(ACCENT), lineStart, lineStart + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < lines.length - 1) {
                int newlineStart = styledHelp.length();
                styledHelp.append('\n');
                if (line.isEmpty()) {
                    styledHelp.setSpan(new RelativeSizeSpan(0.55f), newlineStart, newlineStart + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        message.setText(styledHelp);
        scrollContent.addView(message, new LinearLayout.LayoutParams(-1, -2));

        Button close = new Button(this);
        close.setText(getString(R.string.settings_activity_text_226));
        close.setAllCaps(false);
        close.setTextSize(15);
        close.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        close.setTextColor(Color.rgb(7, 31, 49));
        close.setMinWidth(0);
        close.setMinHeight(0);
        close.setPadding(dp(28), 0, dp(28), 0);
        close.setBackground(pillBackground(ACCENT, dp(22)));
        LinearLayout.LayoutParams closeParams =
                new LinearLayout.LayoutParams(dp(124), dp(44));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(12);
        scrollContent.addView(close, closeParams);
        close.setOnClickListener(v -> dialog.dismiss());

        scroll.addView(scrollContent, new ScrollView.LayoutParams(-1, -2));

        FrameLayout closeX = new FrameLayout(this);
        closeX.setContentDescription(getString(R.string.settings_activity_text_227));
        closeX.setBackgroundColor(Color.TRANSPARENT);
        closeX.setClickable(true);
        closeX.setFocusable(true);
        closeX.setPadding(0, 0, 0, 0);
        closeX.setForegroundGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= 21) closeX.setElevation(dp(4));
        View closeXGlyph = makeIconGlyph("close-circle", ACCENT);
        FrameLayout.LayoutParams closeGlyphParams =
                new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER);
        closeGlyphParams.gravity = Gravity.CENTER;
        closeX.addView(closeXGlyph, closeGlyphParams);
        closeX.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams closeXParams =
                new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.LEFT | Gravity.TOP);
        scrollViewport.addView(closeX, closeXParams);

        // Keep the outer dialog height unchanged: the old fixed close button occupied 56dp.
        int maxBodyHeight = Math.max(dp(150),
                Math.min(dp(430), getResources().getDisplayMetrics().heightPixels - dp(265)));
        int scrollViewportHeight = maxBodyHeight + dp(56);
        panel.addView(scrollViewport, new LinearLayout.LayoutParams(-1, scrollViewportHeight));

        dialog.setView(panel);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(dp(520), metrics.widthPixels - dp(28));
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.62f;
            window.setAttributes(params);
        }
    }

    private void addCheck(LinearLayout parent, String label, String key, boolean defaultValue) {
        LinearLayout card = addSettingCard(parent, label, key);
        LinearLayout row = (LinearLayout) card.getChildAt(0);
        row.setClickable(true);
        row.setFocusable(true);

        Switch toggle = new Switch(this);
        toggle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        toggle.setMinimumWidth(dp(56));
        toggle.setShowText(false);
        toggle.setContentDescription(label);
        if ("showPlayer".equals(key)) showPlayerToggle = toggle;
        if ("autoStartOnCharge".equals(key)) autoStartOnChargeToggle = toggle;
        if ("autoStartOnDeviceLock".equals(key)) autoStartOnDeviceLockToggle = toggle;
        if ("autoStartOnBoot".equals(key)) autoStartOnBootToggle = toggle;
        if ("autoStartOnWake".equals(key)) autoStartOnWakeToggle = toggle;
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
                new LinearLayout.LayoutParams(dp(56), dp(38));
        // Keep the complete switch inside the row bounds to prevent edge clipping.
        switchParams.leftMargin = 0;
        row.addView(toggle, 0, switchParams);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            boolean openingSystemSettingsForAutoStart = false;
            try {
                prefs.edit().putBoolean(key, checked).apply();

            } catch (Throwable ignored) {
            }
            if ("useAndroidDreamForCharging".equals(key)) {
                prefs.edit().putBoolean("autoStartOverlayPermissionPending", false).apply();
                LockScreenMonitorService.sync(this);
                if (checked) {
                    openingSystemSettingsForAutoStart = getBool("autoStartOnCharge", false);
                    try {
                        startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                    } catch (Throwable ignored) {
                        prefs.edit().putBoolean(key, false).apply();
                        toggle.setChecked(false);
                        AppToast.makeText(this,
                                getString(R.string.settings_activity_text_228),
                                Toast.LENGTH_LONG).show();
                    }
                } else if (getBool("autoStartOnCharge", false)
                        && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    openingSystemSettingsForAutoStart = true;
                    prefs.edit().putBoolean("autoStartOverlayPermissionPending", true).apply();
                    requestChargerOverlayPermission();
                }
            } else if ("autoStartOnCharge".equals(key)) {
                if (checked && getBool("useAndroidDreamForCharging", false)) {
                    openingSystemSettingsForAutoStart = true;
                    AppToast.makeText(this,
                            getString(R.string.settings_activity_text_229),
                            Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                    } catch (Throwable ignored) {
                        AppToast.makeText(this,
                                getString(R.string.settings_activity_text_230),
                                Toast.LENGTH_LONG).show();
                    }
                }
                if (checked && !getBool("useAndroidDreamForCharging", false)
                        && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    openingSystemSettingsForAutoStart = true;
                    prefs.edit().putBoolean("autoStartOverlayPermissionPending", true).apply();
                    requestChargerOverlayPermission();
                } else {
                    prefs.edit().putBoolean("autoStartOverlayPermissionPending", false).apply();
                }
                if (!checked) {
                    prefs.edit()
                            .remove("autoStartOverlayPermissionAsked")
                            .remove("autoStartOverlayDenialNotified")
                            .putBoolean("autoStartOverlayPermissionPending", false)
                            .apply();
                }
                updateOverlayPermissionCardVisibility();
                LockScreenMonitorService.sync(this);
            } else if ("autoStartOnDeviceLock".equals(key)) {
                if (checked && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    openingSystemSettingsForAutoStart = true;
                    prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", true).apply();
                    requestLockStartOverlayPermission();
                } else {
                    prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", false).apply();
                }
                if (!checked) {
                    prefs.edit()
                            .putBoolean("autoStartOnLockOverlayPermissionPending", false)
                            .apply();
                    MonitorSettingsSnapshot.resetDeviceLockActivation(this);
                }
                LockScreenMonitorService.sync(this);
                updateOverlayPermissionCardVisibility();
            } else if ("autoStartOnBoot".equals(key) || "autoStartOnWake".equals(key)) {
                if (checked && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    AppToast.makeText(this,
                            getString(R.string.settings_activity_text_231),
                            Toast.LENGTH_LONG).show();
                    if (openOverlayPermissionSettings()) {
                        openingSystemSettingsForAutoStart = true;
                    } else {
                        prefs.edit().putBoolean(key, false).apply();
                        toggle.setChecked(false);
                    }
                }
                // Keep the same persistent notification for every enabled mode.
                // Actual event subscriptions remain independently gated.
                LockScreenMonitorService.sync(this);
            } else if ("showPlayer".equals(key)) {
                if (checked && !hasNotificationAccess()) {
                    prefs.edit().putBoolean("showPlayerPermissionPending", true).apply();
                    requestPlayerNotificationAccessIfNeeded();
                } else {
                    prefs.edit().putBoolean("showPlayerPermissionPending", false).apply();
                }
            } else if (("pauseWhenExcludedAppActive".equals(key)
                    || "preventAutoStartDuringUse".equals(key))
                    && checked && !hasUsageAccess()) {
                prefs.edit().putBoolean(key, false).apply();
                toggle.setChecked(false);
                AppToast.makeText(this,
                        getString(R.string.settings_activity_text_232),
                        Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Throwable ignored) {
                }
            }

            
            NightScreenApplication.syncChargerReceiver(this);

            // Do not open two Android permission/settings screens at the same time.
            // If an external settings screen was opened above, onResume() requests
            // the notification permission after the user returns.
            if (!openingSystemSettingsForAutoStart
                    && (isAutoStartSetting(key) || "useAndroidDreamForCharging".equals(key))) {
                ensureAutoStartNotificationPermission();
            }
        });
        row.setOnClickListener(v -> {
            if (v != toggle) toggle.setChecked(!toggle.isChecked());
        });
    }

    // Use one consistent segmented selector for all discrete settings.
    // Existing values and ranges are intentionally preserved.

    private void addLineSlider(LinearLayout parent, String label, String key,
                               int minValue, int maxValue) {
        LinearLayout card = addSettingCard(parent, label, key);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout resetRow = new LinearLayout(this);
        resetRow.setOrientation(LinearLayout.HORIZONTAL);
        resetRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        resetRow.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView reset = textView(getString(R.string.settings_activity_text_233), 13, ON_SURFACE);
        reset.setGravity(Gravity.CENTER);
        reset.setIncludeFontPadding(false);
        reset.setPadding(dp(12), dp(5), dp(12), dp(5));
        reset.setBackground(roundOutline(CONTROL_SURFACE, dp(9), OUTLINE));
        reset.setClickable(true);
        reset.setFocusable(true);
        reset.setContentDescription(getString(R.string.settings_activity_text_234) + label + getString(R.string.settings_activity_text_235));
        resetRow.addView(reset, new LinearLayout.LayoutParams(-2, dp(32)));
        LinearLayout adjustmentRow = new LinearLayout(this);
        adjustmentRow.setOrientation(LinearLayout.HORIZONTAL);
        adjustmentRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        adjustmentRow.setGravity(Gravity.CENTER_VERTICAL);
        adjustmentRow.setPadding(dp(8), 0, dp(8), 0);
        adjustmentRow.setClipChildren(false);
        adjustmentRow.setClipToPadding(false);

        TextView left = textView(getString(R.string.settings_activity_text_236), 14, MUTED);
        left.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        left.setSingleLine(true);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(-2, dp(34));
        leftParams.rightMargin = dp(7);
        adjustmentRow.addView(left, leftParams);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(Math.max(0, Math.min(maxValue - minValue,
                getInt(key, 0) - minValue)));
        seekBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        seekBar.setContentDescription(label);
        seekBar.setMinimumHeight(dp(44));
        seekBar.setPadding(dp(10), 0, dp(10), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            seekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
            seekBar.setThumbTintList(ColorStateList.valueOf(ON_SURFACE));
            seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(TRACK));
        }
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        adjustmentRow.addView(seekBar, seekParams);

        TextView right = textView(getString(R.string.settings_activity_text_237), 14, MUTED);
        right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        right.setSingleLine(true);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(-2, dp(34));
        rightParams.leftMargin = dp(7);
        adjustmentRow.addView(right, rightParams);

        LinearLayout.LayoutParams adjustmentParams = new LinearLayout.LayoutParams(-1, dp(46));
        adjustmentParams.topMargin = dp(2);
        card.addView(adjustmentRow, adjustmentParams);

        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, dp(38));
        resetParams.topMargin = dp(6);
        resetParams.bottomMargin = 0;
        card.addView(resetRow, resetParams);

        Runnable updateReset = () -> {
            int value = seekBar.getProgress() + minValue;
            reset.setAlpha(value == 0 ? 0.65f : 1f);
        };
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = progress + minValue;
                updateReset.run();
                if (fromUser) prefs.edit().putInt(key, value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        reset.setOnClickListener(v -> {
            int defaultValue = Math.max(minValue, Math.min(maxValue, 0));
            seekBar.setProgress(defaultValue - minValue);
            prefs.edit().putInt(key, defaultValue).apply();
            updateReset.run();
        });
        updateReset.run();
    }

    private LinearLayout addDiscreteSlider(LinearLayout parent, String label,
                                            String[] names, String[] values, String key) {
        return addSegmentedChoices(parent, label, names, values, key);
    }

    private LinearLayout addSegmentedChoices(LinearLayout parent, String label,
                                               String[] names, String[] values, String key) {
        if (names == null || values == null || names.length == 0
                || names.length != values.length) return null;

        LinearLayout card = addSettingCard(parent, label, key);
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tiles.setGravity(Gravity.CENTER_VERTICAL);
        tiles.setClipChildren(false);
        tiles.setClipToPadding(false);
        tiles.setPadding(dp(1), 0, dp(1), 0);

        final TextView[] options = new TextView[names.length];
        final FrameLayout[] optionTiles = new FrameLayout[names.length];
        final LinearLayout.LayoutParams[] optionLayoutParams =
                new LinearLayout.LayoutParams[names.length];
        // Only these long labels may receive extra width, and only on layouts
        // where an equally divided selector would actually cut their text.
        final int[] responsiveTextWidths = new int[names.length];
        final int[] responsiveExpandedWidths = new int[names.length];
        int initial = selectedIndex(key, values);
        int optionTileHeight = "clockDialStyle".equals(key) ? 116 : 37;

        for (int i = 0; i < names.length; i++) {
            final int index = i;
            boolean selected = i == initial;
            FrameLayout tile = new FrameLayout(this);
            tile.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            tile.setClipChildren(false);
            tile.setClipToPadding(false);
            tile.setBackground(roundOutline(
                    selected ? ACCENT : CONTROL_SURFACE,
                    dp(8), selected ? ACCENT : Color.rgb(48, 68, 91)));
            optionTiles[i] = tile;

            TextView option = textView(names[i], 14,
                    selected ? Color.rgb(7, 31, 49) : ON_SURFACE);
            option.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            option.setGravity(Gravity.CENTER);
            option.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            option.setTextDirection(View.TEXT_DIRECTION_RTL);
            option.setSingleLine(true);
            option.setIncludeFontPadding(false);
            option.setPadding(dp(1), 0, dp(1), 0);
            if (Build.VERSION.SDK_INT >= 26) {
                option.setAutoSizeTextTypeUniformWithConfiguration(
                        10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            }
            if ("clockDialStyle".equals(key)) {
                LinearLayout previewContent = new LinearLayout(this);
                previewContent.setOrientation(LinearLayout.VERTICAL);
                previewContent.setGravity(Gravity.CENTER);
                previewContent.setPadding(0, dp(3), 0, dp(2));
                AnalogClockView dialPreview = new AnalogClockView(this);
                dialPreview.setTime(10, 10);
                dialPreview.setColors(ON_SURFACE, CARD_SURFACE);
                dialPreview.setDialStyle(values[i]);
                previewContent.addView(dialPreview,
                        new LinearLayout.LayoutParams(dp(84), dp(84)));
                option.setGravity(Gravity.CENTER);
                option.setTextSize(12);
                previewContent.addView(option,
                        new LinearLayout.LayoutParams(-1, dp(22)));
                tile.addView(previewContent, new FrameLayout.LayoutParams(-1, -1));
            } else {
                tile.addView(option, new FrameLayout.LayoutParams(-1, -1));
            }
            options[i] = option;

            tile.setContentDescription(label + ": " + names[i]);
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setOnClickListener(v -> {
                persistOption(key, values[index]);
                if ("playerDisplayMode".equals(key)) {
                    boolean enabled = !"off".equals(values[index]);
                    boolean onlyAtStart = "start".equals(values[index]);
                    boolean permissionMissing = enabled && !hasNotificationAccess();
                    prefs.edit()
                            .putBoolean("showPlayer", enabled)
                            .putBoolean("showPlayerOnlyAtStart", onlyAtStart)
                            .putBoolean("showPlayerPermissionPending", permissionMissing)
                            .apply();
                    if (permissionMissing) requestPlayerNotificationAccessIfNeeded();
                }
                int selectedAfterAction = index;
                if ("playerDisplayMode".equals(key) && !getBool("showPlayer", false)) {
                    selectedAfterAction = 0;
                }
                for (int j = 0; j < options.length; j++) {
                    boolean isSelected = j == selectedAfterAction;
                    options[j].setTextColor(isSelected
                            ? Color.rgb(7, 31, 49) : ON_SURFACE);
                    optionTiles[j].setBackground(roundOutline(
                            isSelected ? ACCENT : CONTROL_SURFACE,
                            dp(8), isSelected ? ACCENT : Color.rgb(48, 68, 91)));
                    options[j].setSelected(isSelected);
                }
                if ("screenSaverDuration".equals(key)) updateDeviceCapabilityNotice();
            });

            boolean mayExpandOnNarrowScreens =
                    (("screenBrightness".equals(key)
                            || "screenSaverDuration".equals(key)) && i == 0)
                    || (("playerDisplayMode".equals(key)
                            || "playerControlsDisplayMode".equals(key)) && i == 2)
                    || ("chargingBatteryDisplayMode".equals(key) && i == 3);
            if (mayExpandOnNarrowScreens) {
                int textWidth = (int) Math.ceil(option.getPaint().measureText(names[i]));
                responsiveTextWidths[i] = textWidth + dp(2);
                int extraWidth = (("playerDisplayMode".equals(key)
                        || "playerControlsDisplayMode".equals(key)) && i == 2)
                        ? 16 : ("chargingBatteryDisplayMode".equals(key) && i == 3 ? 18 : 8);
                responsiveExpandedWidths[i] = textWidth + dp(extraWidth);
            }

            // Start with identical weighted widths. A measured narrow layout
            // may selectively widen only the long-label tiles above.
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(
                    0, dp(optionTileHeight), 1f);
            tileParams.leftMargin = dp(2);
            tileParams.rightMargin = dp(2);
            optionLayoutParams[i] = tileParams;
            tiles.addView(tile, tileParams);
            if (i < names.length - 1) {
                View segmentGap = new View(this);
                tiles.addView(segmentGap, new LinearLayout.LayoutParams(dp(1), 1));
            }
        }

        final int[] lastMeasuredWidth = {-1};
        tiles.addOnLayoutChangeListener((view, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) -> {
            int measuredWidth = right - left;
            if (measuredWidth <= 0 || measuredWidth == lastMeasuredWidth[0]) return;
            lastMeasuredWidth[0] = measuredWidth;

            int availableForTiles = measuredWidth
                    - tiles.getPaddingLeft() - tiles.getPaddingRight()
                    - dp(1) * Math.max(0, names.length - 1);
            int responsiveCount = 0;
            int expandedWidthTotal = 0;
            int widestLongLabel = 0;
            for (int i = 0; i < names.length; i++) {
                LinearLayout.LayoutParams params = optionLayoutParams[i];
                availableForTiles -= params.leftMargin + params.rightMargin;
                if (responsiveTextWidths[i] > 0) {
                    responsiveCount++;
                    expandedWidthTotal += responsiveExpandedWidths[i];
                    widestLongLabel = Math.max(widestLongLabel, responsiveTextWidths[i]);
                }
            }

            int equalTileWidth = availableForTiles / names.length;
            boolean allTextFitsEqually = equalTileWidth >= widestLongLabel;
            int remainingCount = names.length - responsiveCount;
            int remainingWidth = availableForTiles - expandedWidthTotal;
            boolean enoughRoomForOtherTiles = remainingWidth >= 0
                    && (remainingCount == 0
                    || remainingWidth / remainingCount >= dp(24));
            boolean useResponsiveWidths = responsiveCount > 0
                    && !allTextFitsEqually && enoughRoomForOtherTiles;

            for (int i = 0; i < names.length; i++) {
                LinearLayout.LayoutParams params = optionLayoutParams[i];
                if (useResponsiveWidths && responsiveExpandedWidths[i] > 0) {
                    params.width = responsiveExpandedWidths[i];
                    params.weight = 0f;
                } else {
                    params.width = 0;
                    params.weight = 1f;
                }
                optionTiles[i].setLayoutParams(params);
            }
        });

        LinearLayout.LayoutParams tilesParams =
                new LinearLayout.LayoutParams(-1, dp(optionTileHeight + 6));
        tilesParams.topMargin = dp(7);
        card.addView(tiles, tilesParams);
        return card;
    }

    private void addActivityCapabilityNotice(LinearLayout card) {
        activityCapabilityNotice = textView("", 13, MUTED);
        activityCapabilityNotice.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        activityCapabilityNotice.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        activityCapabilityNotice.setTextDirection(View.TEXT_DIRECTION_RTL);
        activityCapabilityNotice.setLineSpacing(dp(2), 1f);
        activityCapabilityNotice.setPadding(dp(11), dp(8), dp(11), dp(8));
        activityCapabilityNotice.setBackground(roundOutline(CONTROL_SURFACE, dp(11), OUTLINE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(4);
        params.bottomMargin = dp(3);
        card.addView(activityCapabilityNotice, params);
        updateDeviceCapabilityNotice();
    }

    private void updateDeviceCapabilityNotice() {
        if (activityCapabilityNotice == null) return;

        java.util.ArrayList<String> notices = new java.util.ArrayList<>();
        boolean automotive = getPackageManager().hasSystemFeature(
                "android.hardware.type.automotive");
        boolean keyguardObservationKnown =
                prefs.getBoolean("screenLockObservationKnown", false);
        boolean keyguardObserved = prefs.getBoolean("screenLockObserved", true);

        if (keyguardObservationKnown && !keyguardObserved) {
            notices.add(getString(R.string.settings_activity_text_238));
        } else if (automotive) {
            notices.add(getString(R.string.settings_activity_text_239));
        }

        String duration = getString("screenSaverDuration", "device");
        if ("1".equals(duration) || "5".equals(duration)) {
            long timeoutMs = -1L;
            try {
                timeoutMs = Settings.System.getLong(
                        getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
            } catch (Throwable ignored) {
            }
            long requestedMs = "1".equals(duration) ? 60_000L : 300_000L;
            if (timeoutMs <= 0L) {
                notices.add(getString(R.string.settings_activity_text_240));
            } else if (timeoutMs >= Integer.MAX_VALUE - 1000L) {
                notices.add(getString(R.string.settings_activity_text_241));
            } else if (timeoutMs > requestedMs) {
                notices.add(getString(R.string.settings_activity_text_242));
            }
        }

        if (notices.isEmpty()) {
            activityCapabilityNotice.setVisibility(View.GONE);
        } else {
            activityCapabilityNotice.setText(android.text.TextUtils.join("\n\n", notices));
            activityCapabilityNotice.setVisibility(View.VISIBLE);
        }
    }

    private void addColorChoices(LinearLayout parent, String label,
                                 String[] names, String[] values, String key) {
        if (names == null || values == null || names.length == 0
                || names.length != values.length) return;

        LinearLayout card = addSettingCard(parent, label, key);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        choices.setGravity(Gravity.CENTER);

        int initial = selectedIndex(key, values);
        if ("screenBackgroundColor".equals(key) && prefs.contains(key)
                && getInt(key, Color.TRANSPARENT) == colorValue("#332820")) {
            initial = 4;
            persistOption(key, values[initial]);
        }
        final int selectedInitially = initial;
        final ColorSwatchView[] swatches = new ColorSwatchView[names.length];
        final TextView[] captions = new TextView[names.length];
        final int columnsPerRow = names.length > 5 ? 4 : 5;

        ColorSwatchView customColorSwatch = new ColorSwatchView(
                this,
                String.format(Locale.US, "#%08X",
                        getInt(key, "screenBackgroundColor".equals(key)
                                ? Color.BLACK : Color.WHITE)),
                selectedInitially < 0,
                "screenBackgroundColor".equals(key));
        final ColorSwatchView selectedCustomSwatch = customColorSwatch;

        // The custom-color label stays neutral in every selection state.
        final TextView customCaption = textView(getString(R.string.settings_activity_text_243), 13, MUTED);
        customCaption.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        customCaption.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        customCaption.setSingleLine(true);
        customCaption.setIncludeFontPadding(false);
        customCaption.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        final int customCaptionWidth = (int) Math.ceil(
                customCaption.getPaint().measureText(customCaption.getText().toString()))
                + dp(4);

        LinearLayout choiceRow = null;
        for (int i = 0; i < names.length; i++) {
            if (i % columnsPerRow == 0) {
                choiceRow = new LinearLayout(this);
                choiceRow.setOrientation(LinearLayout.HORIZONTAL);
                choiceRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                choiceRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams choiceRowParams =
                        new LinearLayout.LayoutParams(-1, -2);
                if (i > 0) choiceRowParams.topMargin = dp(5);
                choices.addView(choiceRow, choiceRowParams);
            }

            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(1), dp(2), dp(1), 0);

            ColorSwatchView swatch = new ColorSwatchView(
                    this, values[i], i == selectedInitially,
                    "screenBackgroundColor".equals(key));
            swatches[i] = swatch;
            item.addView(swatch, new LinearLayout.LayoutParams(dp(38), dp(38)));

            TextView caption = textView(names[i], 12,
                    i == selectedInitially ? ACCENT : MUTED);
            caption.setGravity(Gravity.CENTER);
            caption.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            caption.setSingleLine(true);
            caption.setTypeface(Typeface.create("sans-serif-medium",
                    i == selectedInitially ? Typeface.BOLD : Typeface.NORMAL));
            caption.setIncludeFontPadding(false);
            if (Build.VERSION.SDK_INT >= 26) {
                caption.setAutoSizeTextTypeUniformWithConfiguration(
                        9, 12, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            }
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
                    captions[j].setTypeface(Typeface.create("sans-serif-medium",
                            j == index ? Typeface.BOLD : Typeface.NORMAL));
                }
                selectedCustomSwatch.setSwatchSelected(false);
            });
            choiceRow.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        LinearLayout customColorRow = new LinearLayout(this);
        customColorRow.setOrientation(LinearLayout.HORIZONTAL);
        customColorRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        customColorRow.setGravity(Gravity.CENTER_VERTICAL);
        customColorRow.setClipChildren(false);

        // Give the custom swatch the exact width of the first palette column.
        // Its center therefore stays directly beneath the right-most swatch above.
        FrameLayout customColorCell = new FrameLayout(this);
        customColorCell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        customColorCell.setClipChildren(false);
        customColorCell.addView(selectedCustomSwatch,
                new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));

        // The label is positioned to the physical left of the swatch without
        // affecting the cell width or shifting the swatch away from its column.
        FrameLayout.LayoutParams customCaptionParams = new FrameLayout.LayoutParams(
                customCaptionWidth, dp(38), Gravity.CENTER);
        customCaption.setTranslationX(-dp(27) - customCaptionWidth / 2f);
        customColorCell.addView(customCaption, customCaptionParams);

        Runnable openCustomColorPicker = () -> showCustomColorDialog(
                key, selectedCustomSwatch, () -> {
                    for (int j = 0; j < swatches.length; j++) {
                        swatches[j].setSwatchSelected(false);
                        captions[j].setTextColor(MUTED);
                        captions[j].setTypeface(Typeface.create(
                                "sans-serif-medium", Typeface.NORMAL));
                    }
                    selectedCustomSwatch.setSwatchSelected(true);
                    // Only the custom circle shows selection; its label remains neutral.
                });
        customColorCell.setClickable(true);
        customColorCell.setFocusable(true);
        customColorCell.setOnClickListener(v -> openCustomColorPicker.run());
        customCaption.setClickable(true);
        customCaption.setFocusable(true);
        customCaption.setOnClickListener(v -> openCustomColorPicker.run());

        customColorRow.addView(customColorCell,
                new LinearLayout.LayoutParams(0, dp(42), 1f));
        View remainingSpace = new View(this);
        int firstRowColumnCount = Math.min(names.length, columnsPerRow);
        customColorRow.addView(remainingSpace,
                new LinearLayout.LayoutParams(0, dp(42),
                        Math.max(0, firstRowColumnCount - 1)));
        LinearLayout.LayoutParams customColorParams =
                new LinearLayout.LayoutParams(-1, -2);
        customColorParams.topMargin = dp(4);
        choices.addView(customColorRow, customColorParams);

        LinearLayout.LayoutParams choicesParams =
                new LinearLayout.LayoutParams(-1, -2);
        choicesParams.topMargin = dp(7);
        card.addView(choices, choicesParams);
    }

    private GradientDrawable makeCustomColorDot(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setStroke(dp(1), Color.rgb(145, 170, 196));
        return dot;
    }

    private GradientDrawable makeCustomColorPreview(int color) {
        GradientDrawable preview = new GradientDrawable();
        preview.setShape(GradientDrawable.RECTANGLE);
        preview.setColor(color);
        preview.setCornerRadius(dp(7));
        preview.setStroke(dp(1), Color.rgb(145, 170, 196));
        return preview;
    }

    
    private void showCustomColorDialog(String key, View preview, Runnable onSaved) {
        int current = getInt(key,
                "screenBackgroundColor".equals(key) ? Color.BLACK : Color.WHITE);
        final float[] hsv = new float[3];
        Color.colorToHSV(current, hsv);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setPadding(dp(14), dp(9), dp(14), dp(9));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setBackground(roundOutline(CARD_SURFACE, dp(18), CARD_OUTLINE));

        TextView heading = textView(getString(R.string.settings_activity_text_243), 20, ON_SURFACE);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heading.setGravity(Gravity.CENTER);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout sampleRow = new LinearLayout(this);
        sampleRow.setOrientation(LinearLayout.HORIZONTAL);
        sampleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sampleRow.setGravity(Gravity.CENTER_VERTICAL);
        View colorPreview = new View(this);
        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(-1, dp(34));
        colorPreview.setBackground(makeCustomColorPreview(current));
        // Reduce only the preview width to 75%; preserve its original height.
        colorPreview.setScaleX(0.75f);
        sampleRow.addView(colorPreview, previewParams);
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(-1, dp(40));
        // Add a little breathing room between the dialog heading and the color sample.
        sampleParams.topMargin = dp(6);
        panel.addView(sampleRow, sampleParams);

        String[] captions = {getString(R.string.settings_activity_text_245), getString(R.string.settings_activity_text_246), getString(R.string.settings_activity_text_247)};
        int[] maxes = {359, 100, 100};

        Runnable updateSwatch = () -> {
            int selected = Color.HSVToColor(hsv);
            colorPreview.setBackground(makeCustomColorPreview(selected));
        };

        for (int i = 0; i < captions.length; i++) {
            final int index = i;
            LinearLayout control = new LinearLayout(this);
            control.setOrientation(LinearLayout.VERTICAL);
            control.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            control.setClipChildren(false);
            control.setClipToPadding(false);

            LinearLayout captionRow = new LinearLayout(this);
            captionRow.setOrientation(LinearLayout.HORIZONTAL);
            captionRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            captionRow.setGravity(Gravity.CENTER_VERTICAL);
            // Align both the right-side label and left-side value with the SeekBar's
            // usable track area, which is inset by 12dp on each side.
            captionRow.setPadding(dp(12), 0, dp(12), 0);
            TextView caption = textView(captions[i], 16, ON_SURFACE);
            captionRow.addView(caption, new LinearLayout.LayoutParams(0, dp(24), 1f));
            TextView value = textView("", 14, ACCENT);
            value.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            value.setTextDirection(View.TEXT_DIRECTION_LTR);
            captionRow.addView(value, new LinearLayout.LayoutParams(-2, dp(24)));
            control.addView(captionRow, new LinearLayout.LayoutParams(-1, dp(24)));

            SeekBar bar = new SeekBar(this);
            bar.setMax(maxes[i]);
            bar.setProgress(Math.max(0, Math.min(maxes[i],
                    Math.round(hsv[i] * (i == 0 ? 1f : 100f)))));
            bar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            // Reserve room for the circular thumb at both endpoints and vertically.
            bar.setPadding(dp(12), dp(5), dp(12), dp(5));
            if (Build.VERSION.SDK_INT >= 21) {
                bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
                bar.setThumbTintList(ColorStateList.valueOf(ON_SURFACE));
                bar.setProgressBackgroundTintList(ColorStateList.valueOf(TRACK));
            }
            value.setText(index == 0 ? bar.getProgress() + "°" : bar.getProgress() + "%");
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    hsv[index] = index == 0 ? progress : progress / 100f;
                    value.setText(index == 0 ? progress + "°" : progress + "%");
                    updateSwatch.run();
                }
                @Override public void onStartTrackingTouch(SeekBar seek) { }
                @Override public void onStopTrackingTouch(SeekBar seek) { }
            });
            control.addView(bar, new LinearLayout.LayoutParams(-1, dp(42)));
            LinearLayout.LayoutParams controlParams =
                    new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) controlParams.topMargin = dp(1);
            panel.addView(control, controlParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView save = textView(getString(R.string.settings_activity_text_079), 16, Color.rgb(7, 31, 49));
        save.setGravity(Gravity.CENTER);
        save.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        save.setBackground(roundOutline(ACCENT, dp(10), ACCENT));
        save.setClickable(true);
        save.setFocusable(true);
        TextView cancel = textView(getString(R.string.settings_activity_text_078), 16, ON_SURFACE);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(roundOutline(CONTROL_SURFACE, dp(10), OUTLINE));
        cancel.setClickable(true);
        cancel.setFocusable(true);
        LinearLayout.LayoutParams actionButtonParams =
                new LinearLayout.LayoutParams(0, dp(40), 1f);
        actions.addView(save, actionButtonParams);
        LinearLayout.LayoutParams cancelParams =
                new LinearLayout.LayoutParams(0, dp(40), 1f);
        cancelParams.rightMargin = dp(9);
        actions.addView(cancel, cancelParams);
        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(-1, dp(40));
        actionsParams.topMargin = dp(6);
        panel.addView(actions, actionsParams);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        save.setOnClickListener(v -> {
            int selected = Color.HSVToColor(hsv);
            persistOption(key, String.format(Locale.US, "#%08X", selected));
            if (preview instanceof ColorSwatchView) {
                ((ColorSwatchView) preview).setSwatchColor(
                        String.format(Locale.US, "#%08X", selected), true);
            } else {
                preview.setBackground(makeCustomColorDot(selected));
            }
            if (onSaved != null) onSaved.run();
            dialog.dismiss();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            window.setLayout(Math.min(dp(390),
                    getResources().getDisplayMetrics().widthPixels - dp(36)),
                    Math.min(dp(430), Math.max(dp(300), screenHeight - dp(100))));
        }
    }

    private void refreshPermissionDependentToggles() {
        if (showPlayerToggle != null) {
            showPlayerToggle.setChecked(getBool("showPlayer", false));
        }
        if (autoStartOnChargeToggle != null) {
            autoStartOnChargeToggle.setChecked(getBool("autoStartOnCharge", false));
        }
        if (autoStartOnDeviceLockToggle != null) {
            autoStartOnDeviceLockToggle.setChecked(getBool("autoStartOnDeviceLock", false));
        }
        if (autoStartOnBootToggle != null) {
            autoStartOnBootToggle.setChecked(getBool("autoStartOnBoot", false));
        }
        if (autoStartOnWakeToggle != null) {
            autoStartOnWakeToggle.setChecked(getBool("autoStartOnWake", false));
        }
    }

    private boolean isAutoStartSetting(String key) {
        return "autoStartOnCharge".equals(key)
                || "autoStartOnDeviceLock".equals(key)
                || "autoStartOnBoot".equals(key)
                || "autoStartOnWake".equals(key);
    }

    /**
     * Android 13+ requires the runtime notification permission for the
     * automatic-start notifications to appear in the drawer. The foreground
     * service and the automatic launch itself remain independent of this permission.
     */
    private void ensureAutoStartNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        boolean anyAutoStartEnabled = getBool("autoStartOnDeviceLock", false)
                || getBool("autoStartOnBoot", false)
                || getBool("autoStartOnWake", false)
                || getBool("autoStartOnCharge", false);
        if (!anyAutoStartEnabled
                || prefs.getBoolean("autoStartNotificationPermissionAsked", false)) return;

        prefs.edit().putBoolean("autoStartNotificationPermissionAsked", true).apply();
        requestPermissions(
                new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_AUTO_START_NOTIFICATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AUTO_START_NOTIFICATION_PERMISSION) return;
        LockScreenMonitorService.sync(this);
        if (grantResults.length == 0
                || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            AppToast.makeText(this,
                    getString(R.string.settings_activity_text_248),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestPlayerNotificationAccessIfNeeded() {
        if (hasNotificationAccess()) return;
        AppToast.makeText(this,
                getString(R.string.settings_activity_text_249),
                Toast.LENGTH_LONG).show();
        if (!openNotificationAccessSettings()) {
            prefs.edit()
                    .putBoolean("showPlayerPermissionPending", false)
                    .putBoolean("showPlayer", false)
                    .putBoolean("showPlayerOnlyAtStart", false)
                    .putString("playerDisplayMode", "off")
                    .apply();
            if (showPlayerToggle != null) showPlayerToggle.setChecked(false);
        }
    }

    private void handlePlayerPermissionResult() {
        if (!prefs.getBoolean("showPlayerPermissionPending", false)) return;

        prefs.edit().putBoolean("showPlayerPermissionPending", false).apply();
        if (hasNotificationAccess()) return;

        prefs.edit().putBoolean("showPlayer", false)
                .putBoolean("showPlayerOnlyAtStart", false)
                .putString("playerDisplayMode", "off").apply();
        if (showPlayerToggle != null && showPlayerToggle.isChecked()) {
            showPlayerToggle.setChecked(false);
        }
        AppToast.makeText(this,
                getString(R.string.settings_activity_text_250),
                Toast.LENGTH_LONG).show();
        if (currentCategory == CATEGORY_PLAYER) showCategory(CATEGORY_PLAYER);
    }

    private void requestChargerOverlayPermission() {
        if (getBool("useAndroidDreamForCharging", false)
                || Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        AppToast.makeText(this,
                getString(R.string.settings_activity_text_251),
                Toast.LENGTH_LONG).show();
        if (!openOverlayPermissionSettings()) {
            prefs.edit().putBoolean("autoStartOverlayPermissionPending", false)
                    .putBoolean("autoStartOnCharge", false).apply();
            if (autoStartOnChargeToggle != null) autoStartOnChargeToggle.setChecked(false);
        }
    }

    private void handleChargerPermissionResult() {
        if (!prefs.getBoolean("autoStartOverlayPermissionPending", false)) return;

        prefs.edit().putBoolean("autoStartOverlayPermissionPending", false).apply();
        if (Settings.canDrawOverlays(this)
                || getBool("useAndroidDreamForCharging", false)) return;

        prefs.edit().putBoolean("autoStartOnCharge", false).apply();
        LockScreenMonitorService.sync(this);
                if (autoStartOnChargeToggle != null && autoStartOnChargeToggle.isChecked()) {
            autoStartOnChargeToggle.setChecked(false);
        }
        AppToast.makeText(this,
                getString(R.string.settings_activity_text_252),
                Toast.LENGTH_LONG).show();
    }

    private void requestLockStartOverlayPermission() {
        if (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        AppToast.makeText(this,
                getString(R.string.settings_activity_text_253),
                Toast.LENGTH_LONG).show();
        if (!openOverlayPermissionSettings()) {
            prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", false)
                    .putBoolean("autoStartOnDeviceLock", false).apply();
            if (autoStartOnDeviceLockToggle != null) {
                autoStartOnDeviceLockToggle.setChecked(false);
            }
        }
    }

    private void handleLockStartPermissionResult() {
        if (!prefs.getBoolean("autoStartOnLockOverlayPermissionPending", false)) return;

        prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", false).apply();
        if (Settings.canDrawOverlays(this)) return;

        prefs.edit().putBoolean("autoStartOnDeviceLock", false).apply();
        if (autoStartOnDeviceLockToggle != null && autoStartOnDeviceLockToggle.isChecked()) {
            autoStartOnDeviceLockToggle.setChecked(false);
        }
        AppToast.makeText(this,
                getString(R.string.settings_activity_text_254),
                Toast.LENGTH_LONG).show();
    }

    private void persistOption(String key, String value) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            if ("dateDisplayMode".equals(key)) {
                editor.putString(key, value);
                editor.putBoolean("showHebrew",
                        "both".equals(value) || "hebrew".equals(value));
                editor.putBoolean("showGregorian",
                        "both".equals(value) || "gregorian".equals(value));
            } else if ("fontColor".equals(key) || "frameColor".equals(key)
                    || "screenBackgroundColor".equals(key)) {
                editor.putInt(key, colorValue(value));
            } else if ("fontWeight".equals(key) || "clockDateSize".equals(key)
                    || "clockHorizontalOffset".equals(key)
                    || "playerHorizontalOffset".equals(key)
                    || "batteryProtectionThreshold".equals(key)
                    || "nightModeStartHour".equals(key) || "nightModeEndHour".equals(key)
                    || "playerSize".equals(key) || "frameWidth".equals(key)
                    || "frameRadius".equals(key) || "screenBrightness".equals(key)) {
                editor.putInt(key, Integer.parseInt(value));
            } else {
                editor.putString(key, value);
            }
            editor.apply();

        } catch (Throwable ignored) {
        }
    }

    private View makeIconGlyph(String iconName, int tint) {
        int resourceId;
        if ("activity-history".equals(iconName)) {
            resourceId = R.drawable.ic_settings_activity_history;
        } else {
            String resourceName = "icon_" + iconName.replace('-', '_');
            resourceId = getResources().getIdentifier(
                    resourceName, "drawable", getPackageName());
            if (resourceId == 0) {
                throw new IllegalStateException("Missing UI glyph resource: " + resourceName);
            }
        }

        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageResource(resourceId);
        if (!"lock-play".equals(iconName) && !"close-circle".equals(iconName)) {
            image.setColorFilter(tint);
        }
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        image.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        image.setContentDescription(iconName);
        return image;
    }

    private FrameLayout makeSettingIconBadge(String settingKey, String settingLabel) {
        String iconName = SettingIconRegistry.iconNameForKey(settingKey);
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(roundOutline(ICON_SURFACE, dp(10), CARD_OUTLINE));

        View graphic = makeIconGlyph(iconName, ACCENT);
        int glyphSize = "progress-time-digital".equals(iconName) ? dp(27)
                : "bold".equals(iconName) ? dp(24)
                : "phone-charging".equals(iconName) ? dp(24)
                : "power-off".equals(iconName) ? dp(19)
                : ("history".equals(iconName) || "panel-bottom".equals(iconName)) ? dp(24)
                : "position-arrows".equals(iconName) ? dp(27)
                : "gregorian-date".equals(iconName) ? dp(25)
                : dp(22);
        FrameLayout.LayoutParams graphicParams =
                new FrameLayout.LayoutParams(glyphSize, glyphSize, Gravity.CENTER);
        badge.addView(graphic, graphicParams);
        graphic.setContentDescription(settingLabel);
        return badge;
    }

    private void addSpinner(LinearLayout parent, String label, String[] names,
                            String[] values, String key) {
        LinearLayout card = addSettingCard(parent, label, key);
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
            if (!knownFamily) {
                selected = 0;
                persistOption(key, values[0]);
            }
        }

        final int selectedPosition = selected;
        int dropdownWidth = dropdownWidthForOptions(names);
        int fontFamilyWidthExtra = "fontFamily".equals(key) ? dp(6) : 0;
        int visibleWidth = spinnerVisibleWidthForOption(names[selected]) + fontFamilyWidthExtra;
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
                if ("fontFamily".equals(key)) {
                }
                return view;
            }

            @Override public View getDropDownView(int position, View convertView,
                                                  android.view.ViewGroup parent) {
                if (parent instanceof View) ((View) parent).setVerticalScrollBarEnabled(false);
                TextView view = new TextView(SettingsActivity.this);
                view.setText(names[position]);
                styleDropdownOptionText(view, position == selectedIndex(key, values));
                if ("fontFamily".equals(key)) {
                }
                view.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, dp(39)));
                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(7), 0, dp(7), 0);
        spinner.setBackgroundColor(Color.TRANSPARENT);
        spinner.setDropDownWidth(Math.max(dropdownWidth, visibleWidth));
        if (Build.VERSION.SDK_INT >= 16) {
            spinner.setPopupBackgroundDrawable(roundOutline(CARD_SURFACE, dp(12), OUTLINE));
        }
        spinner.setSelection(selectedPosition, false);

        FrameLayout spinnerContainer = new FrameLayout(this);
        spinnerContainer.setBackground(roundOutline(ACCENT, dp(12), ACCENT));
        spinnerContainer.setTag(spinner);
        spinnerContainer.addView(spinner,
                new FrameLayout.LayoutParams(-1, -1));
        View dropdownIcon = makeIconGlyph("chevron", Color.rgb(7, 31, 49));
        FrameLayout.LayoutParams dropdownIconParams =
                new FrameLayout.LayoutParams(dp(18), dp(18),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        dropdownIconParams.leftMargin = dp(8);
        spinnerContainer.addView(dropdownIcon, dropdownIconParams);

        // Keep the title and icon in their normal header row. Put the selector
        // underneath, like the segmented controls used by the other settings.
        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(visibleWidth, dp(40));
        spinnerParams.gravity = Gravity.RIGHT;
        // Align the selector's right edge with the right-aligned title/description
        // block above it, accounting for the adjacent 30dp setting icon and margins.
        spinnerParams.rightMargin = dp(42);
        spinnerParams.topMargin = dp("fontFamily".equals(key) ? 9 : 7);
        card.addView(spinnerContainer, spinnerParams);

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
                LinearLayout.LayoutParams widthParams =
                        (LinearLayout.LayoutParams) spinnerContainer.getLayoutParams();
                int nextVisibleWidth = spinnerVisibleWidthForOption(names[position])
                        + fontFamilyWidthExtra;
                widthParams.width = nextVisibleWidth;
                spinnerContainer.setLayoutParams(widthParams);
                spinner.setDropDownWidth(Math.max(dropdownWidth, nextVisibleWidth));
                persistOption(key, values[position]);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    

    private void styleSpinnerText(TextView view, boolean selected, boolean selectedOption) {
        view.setTextColor(Color.rgb(7, 31, 49));
        view.setTextSize(15);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(2), 0, dp(2), 0);
        view.setHeight(dp(38));
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

    private int spinnerVisibleWidthForOption(String name) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(15f * getResources().getDisplayMetrics().scaledDensity);
        float textWidth = name == null ? 0f : paint.measureText(name);
        // Include the Spinner's horizontal padding and the custom dropdown chevron.
        int desiredWidth = (int) Math.ceil(textWidth) + dp(62);
        // Reserve the selector indent so even the longest font name stays inside the card.
        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(76);
        int maximumWidth = Math.max(dp(88), availableWidth);
        return Math.min(maximumWidth, Math.max(dp(124), desiredWidth));
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

    
    private int selectedIndex(String key, String[] values) {
        int index = 0;
        if ("playerDisplayMode".equals(key)) {
            if (!getBool("showPlayer", false)) return 0;
            String storedMode = getString(key, "");
            if ("start".equals(storedMode)) return 2;
            if ("on".equals(storedMode)) return 1;
            return getBool("showPlayerOnlyAtStart", false) ? 2 : 1;
        }
        if ("dateDisplayMode".equals(key)) {
            if (prefs.contains(key)) {
                String storedMode = getString(key, "both");
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(storedMode)) return i;
                }
            }
            boolean showHebrew = getBool("showHebrew", true);
            boolean showGregorian = getBool("showGregorian", true);
            if (showHebrew && showGregorian) return 0;
            if (showHebrew) return 1;
            if (showGregorian) return 2;
            return 0;
        }
        if ("fontColor".equals(key) || "frameColor".equals(key)
                || "screenBackgroundColor".equals(key)) {
            if (!prefs.contains(key)) {
                return "screenBackgroundColor".equals(key) || "fontColor".equals(key) ? 0 : -1;
            }
            int stored = getInt(key, colorValue(values[0]));
            for (int i = 0; i < values.length; i++) {
                if (colorValue(values[i]) == stored) return i;
            }
            return -1;
        } else if ("fontWeight".equals(key) || "clockDateSize".equals(key)
                || "clockHorizontalOffset".equals(key)
                || "playerHorizontalOffset".equals(key)
                || "batteryProtectionThreshold".equals(key)
                || "nightModeStartHour".equals(key) || "nightModeEndHour".equals(key)
                || "playerSize".equals(key) || "frameWidth".equals(key)
                || "frameRadius".equals(key) || "screenBrightness".equals(key)) {
            int fallback = "clockDateSize".equals(key) ? 84
                    : ("fontWeight".equals(key) ? 0 : parse(values[0], 0));
            int stored = getInt(key, fallback);
            for (int i = 0; i < values.length; i++) {
                if (parse(values[i], Integer.MIN_VALUE) == stored) return i;
            }
        } else {
            String defaultValue = ("clockTextAlignment".equals(key)
                    || "playerTextAlignment".equals(key)) ? "center" : values[0];
            String stored = getString(key, defaultValue);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(stored)) return i;
            }
        }
        return index;
    }

    
    
    private void updateOverlayPermissionCardVisibility() {
        // Overlay permission management now lives in PermissionsActivity.
    }

    
    private boolean openOverlayPermissionSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            // Android 11+ opens a system list, so the package URI is used only on Android 10.
            if (Build.VERSION.SDK_INT < 30) {
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
            return true;
        } catch (Throwable ex) {
            AppToast.makeText(this,
                    getString(R.string.settings_activity_text_264),
                    Toast.LENGTH_LONG).show();
            return false;
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

    private boolean openNotificationAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return true;
        } catch (ActivityNotFoundException ignored) {
            AppToast.makeText(this, getString(R.string.settings_activity_text_265),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            AppToast.makeText(this, getString(R.string.settings_activity_text_265),
                    Toast.LENGTH_LONG).show();
        }
        return false;
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
        if (value != null && value.startsWith("#")) {
            try { return Color.parseColor(value); }
            catch (IllegalArgumentException ignored) { return Color.WHITE; }
        }
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


    private class ColorSwatchView extends View {
        private String value;
        private boolean selected;
        private final boolean screenBackgroundPalette;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorSwatchView(Context context, String value, boolean selected,
                        boolean screenBackgroundPalette) {
            super(context);
            this.value = value;
            this.selected = selected;
            this.screenBackgroundPalette = screenBackgroundPalette;
            setClickable(false);
        }

        void setSwatchSelected(boolean selected) {
            this.selected = selected;
            invalidate();
        }

        void setSwatchColor(String value, boolean selected) {
            this.value = value;
            this.selected = selected;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - dp(3);

            int swatchColor = colorValue(value);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(swatchColor);
            canvas.drawCircle(cx, cy, radius, paint);

            // A clearly visible outline separates every color sample from the card.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(selected ? 2 : 1));
            paint.setColor(selected ? ACCENT : Color.rgb(151, 177, 202));
            canvas.drawCircle(cx, cy, radius, paint);

            if (selected) {
                int luminance = Math.round(
                        Color.red(swatchColor) * 0.299f
                        + Color.green(swatchColor) * 0.587f
                        + Color.blue(swatchColor) * 0.114f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setColor(luminance < 145 ? Color.WHITE : Color.rgb(8, 26, 42));
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