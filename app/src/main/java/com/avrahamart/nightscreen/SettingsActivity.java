package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
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
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
    private LinearLayout currentSettingsGroup;
    private LinearLayout currentSettingsGroupParent;
    private String currentSettingsGroupTitle;
private LinearLayout clockDateCard;
    private LinearLayout playerFrameCard;
    private LinearLayout notificationAccessCard;
    private LinearLayout overlayPermissionCard;
    private TextView overlayPermissionStatus;
    private Button overlayPermissionButton;
    private Switch showPlayerToggle;
    private Switch autoStartOnChargeToggle;
    private Switch autoStartOnDeviceLockToggle;
    private Switch autoStartOnBootToggle;
    private Switch autoStartOnWakeToggle;
    private TextView activityCapabilityNotice;
    private View activityHelpHeaderButton;
    private LinearLayout screenLockAdminCard;
    private TextView screenLockAdminStatus;
    private Button screenLockAdminButton;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName screenLockAdminComponent;
    private LinearLayout currentCard;






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
    private final int dateTextColor = Color.rgb(220, 220, 220);

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

        screenTitle = textView("הגדרות", 27, ON_SURFACE);
        screenTitle.setIncludeFontPadding(false);
        screenTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        screenTitle.setLetterSpacing(-0.015f);
        screenTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        screenTitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        headerTitleBlock.addView(screenTitle, new LinearLayout.LayoutParams(-1, -2));

        screenSubtitle = textView("התאמה אישית של שומר מסך", 16, MUTED);
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
            updateDeviceCapabilityNotice();
        }
    }

    private void buildDashboard() {
        currentCategory = CATEGORY_DASHBOARD;
        clockDateCard = null;
        playerFrameCard = null;
        activityCapabilityNotice = null;
        if (activityHelpHeaderButton != null
                && activityHelpHeaderButton.getParent() == screenHeader) {
            screenHeader.removeView(activityHelpHeaderButton);
        }
        activityHelpHeaderButton = null;
        if (content == null) return;
        if (settingsScroll != null) settingsScroll.setFillViewport(true);
        content.setPadding(0, 0, 0, 0);
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        content.removeAllViews();
        currentSettingsGroup = null;
        currentSettingsGroupParent = null;
        currentSettingsGroupTitle = null;
screenTitle.setText("הגדרות");
        screenSubtitle.setText("התאמה אישית של שומר מסך");

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

        addDashboardButton("הפעלה והתנהגות",
                "הגדרת הזמנים ואפשרויות ההפעלה",
                "power-category", CATEGORY_ACTIVITY);
        addDashboardButton("מראה ועיצוב",
                "התאמת גופנים צבעים ובהירות",
                "palette", CATEGORY_APPEARANCE);
        addDashboardButton("שעון ותאריך",
                "התאמת תצוגת השעה והתאריך",
                "calendar-clock", CATEGORY_CLOCK_DATE);
        addDashboardButton("נגן מדיה",
                "הצגת והתאמת נגן המערכת",
                "music-note", CATEGORY_PLAYER);
        addDashboardButton("הגדרות מתקדמות",
                "כלים מתקדמים וחיסכון בסוללה",
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
        addDashboardNavigationButton("ניהול הרשאות", "shield-alert",
                () -> startActivity(new Intent(this, PermissionsActivity.class)));
        addDashboardNavigationButton("מדריך שימוש", "book-open",
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
        int iconOffsetDp = "calendar-clock".equals(iconName) ? 1
                : "music-note".equals(iconName) ? -4
                : "palette".equals(iconName) ? 0 : 0;
        if (iconOffsetDp != 0) glyph.setTranslationX(dp(iconOffsetDp));
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
        if (activityHelpHeaderButton != null
                && activityHelpHeaderButton.getParent() == screenHeader) {
            screenHeader.removeView(activityHelpHeaderButton);
        }
        activityHelpHeaderButton = null;
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
        addCheck(content, "תאריך עברי", "showHebrew", true);
        String[] hebrewDateFormats = {"plain", "yearArticle", "monthBet", "monthBetYearArticle"};
        addSpinner(content, "תבנית התאריך העברי",
                currentHebrewDateSamples(), hebrewDateFormats, "hebrewDateFormat");
        addCheck(content, "תאריך לועזי", "showGregorian", true);
        String[] gregorianDateFormats = {
                "dd.MM.yyyy", "dd.MM.yy", "dd/MM/yy",
                "dd/MM/yyyy", "yyyy/MM/dd", "d 'ב'MMMM yyyy"
        };
        addSpinner(content, "תבנית התאריך הלועזי",
                currentGregorianDateSamples(gregorianDateFormats),
                gregorianDateFormats, "gregorianDateFormat");
        addSegmentedChoices(content, "תצוגת שעה",
                new String[]{"24 שעות", "12 שעות"},
                new String[]{"24", "12"}, "timeFormat");
        addSegmentedChoices(content, "סוג השעון",
                new String[]{"דיגיטלי", "אנלוגי"},
                new String[]{"digital", "analog"}, "clockDisplayMode");
        addSegmentedChoices(content, "עיצוב לוח השעון",
                new String[]{"קלאסי", "נקי", "ספרות"},
                new String[]{"classic", "minimal", "numerals"}, "clockDialStyle");
        addDiscreteSlider(content, "גודל",
                new String[]{"קטן", "בינוני", "רגיל", "גדול", "ענק"},
                new String[]{"60", "72", "84", "96", "108"}, "clockDateSize");
        addDiscreteSlider(content, "עובי הגופן",
                new String[]{"דק", "רגיל", "מודגש", "עבה"},
                new String[]{"-1", "0", "1", "2"}, "fontWeight");
        addSegmentedChoices(content, "מיקום",
                new String[]{"בראש המסך", "ברבע העליון", "במרכז המסך"},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");
        addLineSlider(content, "הזזה אופקית", "clockHorizontalOffset", -20, 20);
    }

    private String[] currentGregorianDateSamples(String[] patterns) {
        Calendar today = Calendar.getInstance();
        String[] samples = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            Locale locale = "d 'ב'MMMM yyyy".equals(patterns[i])
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
                .replace("מרחשוון", "חשוון")
                .replace("מרחשון", "חשוון")
                .replace("חשון", "חשוון")
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
            month = "ב" + month;
        }
        if ("yearArticle".equals(format) || "monthBetYearArticle".equals(format)) {
            year = "ה" + year;
        }
        return day + " " + month + " " + year;
    }

    private void addActivityHelpHeaderButton() {
        // The information button belongs to the page title bar, at its upper-left.
        FrameLayout button = new FrameLayout(this);
        button.setContentDescription("הסבר על הגדרות זמני הפעילות");
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackgroundColor(Color.TRANSPARENT);

        View glyph = makeIconGlyph("message-circle-question", ACCENT);
        button.addView(glyph,
                new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(38), dp(38), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        params.leftMargin = dp(2);
        screenHeader.addView(button, params);
        button.setOnClickListener(v -> showActivityTimeHelpDialog());
        activityHelpHeaderButton = button;
    }

    private void buildActivitySettings() {
        LinearLayout durationCard = addSegmentedChoices(content, "זמן מסך",
                new String[]{"לפי המכשיר", "1 דק'", "5 דק'", "ללא כיבוי"},
                new String[]{"device", "1", "5", "0"}, "screenSaverDuration");
        addActivityCapabilityNotice(durationCard);
        addCheck(content, "מניעת נעילה בזמן פעילות", "preventLockWhileActive", false);
        addCheck(content, "המשך הצגה במסך נעילה", "continueOnLockScreen", false);
        addCheck(content, "הפעלה אוטומטית בנעילה", "autoStartOnDeviceLock", false);
        addCheck(content, "הפעלה אוטומטית בהדלקה", "autoStartOnBoot", false);
        addCheck(content, "הפעלה אוטומטית בהתעוררות", "autoStartOnWake", false);
        addCheck(content, "הפעלה אוטומטית בטעינה ובעגינה", "autoStartOnCharge", false);
        addCheck(content, "ללא כיבוי בטעינה", "noShutdownWhileCharging", false);
        addSegmentedChoices(content, "תצוגת סוללה בטעינה",
                new String[]{"ללא", "אחוזים", "סמל", "סמל ואחוזים"},
                new String[]{"off", "percent", "icon", "icon_percent"},
                "chargingBatteryDisplayMode");
        addCheck(content, "השהיה באפליקציות מוחרגות",
                "pauseWhenExcludedAppActive", false);
        if (currentSettingsGroup != null && currentSettingsGroup.getChildCount() > 1) {
            View exclusionCard = currentSettingsGroup.getChildAt(
                    currentSettingsGroup.getChildCount() - 1);
            if (exclusionCard instanceof LinearLayout) {
                addExcludedAppsPicker((LinearLayout) exclusionCard);
            }
        }
    }

    private void addExcludedAppsPicker(LinearLayout parent) {
        LinearLayout card = parent;
        TextView description = textView(
                "אפשר לבחור אפליקציות שבהן שומר מסך לא יופעל אוטומטית. נדרשת הרשאת נתוני שימוש של Android.",
                14, MUTED);
        description.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        description.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        description.setTextDirection(View.TEXT_DIRECTION_RTL);
        description.setLineSpacing(dp(3), 1f);
        card.addView(description, new LinearLayout.LayoutParams(-1, -2));
        Button choose = makeActionButton("בחירת אפליקציות");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(42));
        buttonParams.topMargin = dp(8);
        card.addView(choose, buttonParams);
        choose.setOnClickListener(v -> {
            if (!hasUsageAccess()) {
                AppToast.makeText(this,
                        "כדי לזהות אפליקציה פעילה, יש לאפשר לשומר מסך גישה לנתוני שימוש במסך הבא.",
                        Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Throwable ignored) {
                    AppToast.makeText(this, "הגדרת נתוני שימוש אינה זמינה במכשיר זה.",
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
                .setTitle("אפליקציות מוחרגות")
                .setMultiChoiceItems(names.toArray(new String[0]), checked,
                        (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("שמירה", (d, which) -> {
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
        addColorChoices(content, "צבע המסך",
                new String[]{"שחור", "כחול", "כחול כהה", "טורקיז",
                        "אפור פחם", "ירוק יער", "כחול עמוק", "אפור אבן"},
                new String[]{"#000000", "#142438", "#091321", "#12313A",
                        "#252B33", "#20362E", "#193740", "#303943"},
                "screenBackgroundColor");
        addSegmentedChoices(content, "סגנון הרקע",
                new String[]{"אחיד", "מעבר צבע", "זוהר עדין", "הצללה"},
                new String[]{"solid", "gradient", "glow", "vignette"}, "backgroundStyle");
        addDiscreteSlider(content, "בהירות המסך",
                new String[]{"לפי המכשיר", "25%", "50%", "75%", "100%"},
                new String[]{"-1", "25", "50", "75", "100"}, "screenBrightness");
        addSpinner(content, "סוג הגופן",
                new String[]{"ברירת מחדל", "DL Adva", "Keter Aram Tsova", "Shofar",
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
        addColorChoices(content, "צבע הגופן",
                new String[]{"לבן", "כחול עדין", "כסף", "אפור", "אפור בהיר"},
                new String[]{"white", "blue", "silver", "gray", "light"}, "fontColor");
    }

    
    private void buildPlayerSettings() {
        addSegmentedChoices(content, "הפעלת הנגן",
                new String[]{"כבוי", "פועל", "פועל בתחילת שיר"},
                new String[]{"off", "on", "start"}, "playerDisplayMode");
        addCheck(content, "פרטי האמן", "showDetails", getBool("showDetails", true));
        addSegmentedChoices(content, "הצגת שורת מיקום ופקדי הנגן",
                new String[]{"כבוי", "פועל", "פועל בתחילת שיר"},
                new String[]{"off", "on", "start"}, "playerControlsDisplayMode");
        addDiscreteSlider(content, "גודל הנגן",
                new String[]{"קטן", "בינוני", "גדול", "ענק"},
                new String[]{"14", "17", "20", "23"}, "playerSize");
        addSegmentedChoices(content, "מיקום הנגן",
                new String[]{"ברבע התחתון", "בתחתית המסך"},
                new String[]{"lower", "bottom"}, "playerPosition");
        addLineSlider(content, "הזזה אופקית של הנגן", "playerHorizontalOffset", -20, 20);
        addCheck(content, "הצג מסגרת", "frameEnabled", false);
        addCheck(content, "תמונת האלבום כרקע פנימי", "frameUseAlbumArt",
                getBool("frameUseAlbumArt", true));
        addColorChoices(content, "צבע מסגרת",
                new String[]{"לבן", "כחול עדין", "כסף", "אפור", "אפור בהיר"},
                new String[]{"white", "blue", "silver", "gray", "light"}, "frameColor");
        addDiscreteSlider(content, "עובי מסגרת",
                new String[]{"דק", "רגיל", "עבה"},
                new String[]{"1", "2", "3"}, "frameWidth");
        addDiscreteSlider(content, "עיגול פינות",
                new String[]{"ישר", "עדין", "מעוגל"},
                new String[]{"0", "10", "20"}, "frameRadius");
    }

    
    private void buildAdvancedSettings() {
        if (!prefs.contains("batteryProtectionThreshold")) {
            prefs.edit().putInt("batteryProtectionThreshold", 20).apply();
        }
        if ("system".equals(getString("timeFormat", "24"))) {
            prefs.edit().putString("timeFormat", "24").apply();
        }
        addCheck(content, "מצב לילה אוטומטי", "nightModeEnabled", false);
        String[] hourNames = new String[24];
        String[] hourValues = new String[24];
        for (int hour = 0; hour < 24; hour++) {
            hourNames[hour] = String.format(Locale.US, "%02d:00", hour);
            hourValues[hour] = String.valueOf(hour);
        }
        addSpinner(content, "שעת התחלת מצב לילה", hourNames, hourValues, "nightModeStartHour");
        addSpinner(content, "שעת סיום מצב לילה", hourNames, hourValues, "nightModeEndHour");

        addCheck(content, "הגנה אוטומטית כשהסוללה מתרוקנת",
                "batteryProtectionEnabled", false);
        addDiscreteSlider(content, "סף סוללה להפעלת ההגנה",
                new String[]{"10%", "15%", "20%", "25%", "30%"},
                new String[]{"10", "15", "20", "25", "30"},
                "batteryProtectionThreshold");

        addCheck(content, "בחירת מסלול ההפעלה בטעינה ובעגינה דרך שומר המסך של Android",
                "useAndroidDreamForCharging", getBool("useAndroidDreamForCharging", true));
    }

    private String settingsGroupTitleFor(String key) {
        switch (currentCategory) {
            case CATEGORY_CLOCK_DATE:
                if ("timeFormat".equals(key) || "clockDisplayMode".equals(key)
                        || "clockDialStyle".equals(key)) return "תצוגת השעה";
                if ("showHebrew".equals(key) || "showGregorian".equals(key)
                        || "hebrewDateFormat".equals(key) || "gregorianDateFormat".equals(key)) {
                    return "הצגת תאריכים";
                }
                return "התאמת השעון והתאריך";
            case CATEGORY_ACTIVITY:
                if ("screenSaverDuration".equals(key)
                        || "preventLockWhileActive".equals(key)
                        || "continueOnLockScreen".equals(key)) {
                    return "הגדרת זמני הפעילות";
                }
                if ("autoStartOnDeviceLock".equals(key) || "autoStartOnBoot".equals(key)
                        || "autoStartOnWake".equals(key) || "autoStartOnCharge".equals(key)) {
                    return "הפעלות אוטומטיות";
                }
                return "שומר מסך במצב טעינה";
            case CATEGORY_APPEARANCE:
                        if ("screenBackgroundColor".equals(key) || "screenBrightness".equals(key)
                        || "backgroundStyle".equals(key)) {
                    return "התאמת מראה המסך";
                }
                return "גופן וצבע הטקסט";
            case CATEGORY_PLAYER:
                if ("frameEnabled".equals(key) || "frameUseAlbumArt".equals(key)
                        || "frameColor".equals(key) || "frameWidth".equals(key)
                        || "frameRadius".equals(key)) {
                    return "מסגרת לנגן";
                }
                return "הצגת הנגן";
            case CATEGORY_ADVANCED:
                if ("nightModeEnabled".equals(key) || "nightModeStartHour".equals(key)
                        || "nightModeEndHour".equals(key)) return "מצב לילה";
                if ("batteryProtectionEnabled".equals(key)
                        || "batteryProtectionThreshold".equals(key)) return "הגנה על הסוללה";
                return "הגדרות מתקדמות";
            default:
                return "הגדרות";
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
            infoButton.setContentDescription("הסבר על הגדרות זמני הפעילות");
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
                            && "הגדרת זמני הפעילות".equals(groupTitle));
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
        header.addView(makeIconBadge(iconForSetting(key), dp(30)), settingIconParams);

        card.addView(header, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private String settingDescription(String key) {
        if ("showHebrew".equals(key)) return "הצגת התאריך העברי לצד השעה";
        if ("showGregorian".equals(key)) return "הצגת התאריך הלועזי לצד השעה";
        if ("hebrewDateFormat".equals(key)) return "בחירת תבנית התצוגה של התאריך העברי";
        if ("gregorianDateFormat".equals(key)) return "בחירת תבנית התצוגה של התאריך הלועזי";
        if ("clockDateSize".equals(key)) return "בחירת גודל התצוגה של השעון והתאריך";
        if ("clockDisplayMode".equals(key)) return "בחירה בין תצוגת שעה דיגיטלית לבין שעון אנלוגי עם מחוגים";
        if ("clockDialStyle".equals(key)) return "בחירת מראה לוח השעון האנלוגי וסימוני השעות";
        if ("fontWeight".equals(key)) return "בחירת עובי גופן השעון והתאריך";
        if ("clockDatePosition".equals(key)) return "בחירת מיקום הצגת השעון והתאריך במסך";
        if ("screenSaverDuration".equals(key)) return "בחירת משך זמן הצגת השומר מסך";
        if ("preventLockWhileActive".equals(key)) return "מניעת נעילת המכשיר למשך כל זמן הפעילות שמוגדר לשומר מסך";
        if ("continueOnLockScreen".equals(key)) return "השארת השומר מסך על מסך הנעילה גם לאחר כיבוי המסך בסיום הפעילות";
        if ("autoStartOnDeviceLock".equals(key)) return "הפעלת שומר מסך באופן אוטומטי בעת נעילת המכשיר";
        if ("autoStartOnBoot".equals(key)) return "הפעלת שומר מסך באופן אוטומטי לאחר הפעלת המכשיר";
        if ("autoStartOnWake".equals(key)) return "הפעלת השומר מסך באופן אוטומטי כשהמכשיר מתעורר ממצב שינה";
        if ("noShutdownWhileCharging".equals(key)) return "השארת השומר מסך פועל כל עוד המכשיר מחובר לטעינה";
        if ("screenBackgroundColor".equals(key)) return "בחירת צבע הרקע של שומר המסך";
        if ("screenBrightness".equals(key)) return "התאמת בהירות התצוגה של שומר המסך";
        if ("fontFamily".equals(key)) return "בחירת סוג הגופן שיוצג במסך";
        if ("fontColor".equals(key)) return "בחירת צבע הגופנים והפריטים המופיעים במסך";
        if ("playerDisplayMode".equals(key)) return "הצגת נגן המערכת בזמן שהוא פועל";
        if ("showPlayerOnlyAtStart".equals(key)) return "הצגת הנגן רק בתחילת כל שיר";
        if ("showDetails".equals(key)) return "כשמופיע בפרטי השיר את שם האמן, הצגת שם האמן מעל שם השיר";
        if ("playerControlsDisplayMode".equals(key)) return "בחירת אופן הצגת שורת המיקום, זמני השיר ולחצני השליטה: תמיד, בתחילת כל שיר או כבוי";
        if ("chargingBatteryDisplayMode".equals(key)) return "בחירת האופן שבו תוצג רמת הסוללה כל עוד המכשיר מחובר לטעינה";
        if ("clockHorizontalOffset".equals(key)) return "הזזת כל תצוגת השעון והתאריך ימינה או שמאלה";
        if ("playerHorizontalOffset".equals(key)) return "הזזת הנגן ימינה או שמאלה";
        if ("backgroundStyle".equals(key)) return "בחירת אופן הצגת צבע הרקע";
        if ("nightModeEnabled".equals(key)) return "הפחתת בהירות מסך השומר מסך בשעות שנבחרו, בלי לשנות את בהירות המכשיר";
        if ("nightModeStartHour".equals(key)) return "השעה שבה מצב לילה אוטומטי מתחיל";
        if ("nightModeEndHour".equals(key)) return "השעה שבה מצב לילה אוטומטי מסתיים";
        if ("timeFormat".equals(key)) return "בחירת תצוגת שעה בת 12 או 24 שעות";
        if ("batteryProtectionEnabled".equals(key)) return "הפחתת בהירות המסך באופן אוטומטי כשהסוללה יורדת מתחת לסף שנבחר";
        if ("useAndroidDreamForCharging".equals(key)) return "קובע באיזה מסלול תפעל ההגדרה „הפעלה אוטומטית בטעינה ובעגינה”: שומר המסך של Android (ברירת מחדל) או הצגה מעל אפליקציות. במסלול Android יש לבחור את „שומר מסך” בהגדרות המערכת.";
        if ("batteryProtectionThreshold".equals(key)) return "מתחת לאחוז שנבחר תופעל הפחתת בהירות, כל עוד המכשיר אינו בטעינה";
        if ("playerSize".equals(key)) return "בחירת גודל התצוגה של הנגן";
        if ("playerPosition".equals(key)) return "בחירת מיקום הצגת הנגן במסך";
        if ("frameEnabled".equals(key)) return "הצגת הנגן בתוך חלונית ממוסגרת";
        if ("frameUseAlbumArt".equals(key)) return "הצגת תמונת האלבום כרקע פנימי בתוך מסגרת הנגן בלבד";
        if ("frameColor".equals(key)) return "בחירת צבע המסגרת סביב הנגן";
        if ("frameWidth".equals(key)) return "בחירת עובי קו המסגרת";
        if ("frameRadius".equals(key)) return "בחירת מידת עיגול פינות המסגרת";
        return "";
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
        bg.setStroke(dp(1), Color.rgb(30, 48, 69));
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(0f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 0);
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
        heading.setPadding(0, 0, dp(3), 0);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams headingTextParams =
                new LinearLayout.LayoutParams(0, dp(42), 1f);
        headingRow.addView(heading, headingTextParams);

        card.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(42)));

        View separator = new View(this);
        separator.setBackgroundColor(Color.rgb(30, 48, 69));
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.setMargins(dp(2), dp(5), dp(2), dp(10));
        card.addView(separator, separatorParams);
        return card;
    }

    private void showActivityTimeHelpDialog() {
        String help =
                "זמן פעילות\n"
                + "ההגדרה קובעת כמה זמן שומר מסך יוצג, או האם משך התצוגה ינוהל על ידי המכשיר.\n"
                + "• לפי המכשיר - אין לאפליקציה טיימר סיום עצמאי. שומר מסך נשאר מוצג עד שהמכשיר מכבה את המסך לפי ההגדרות שלו. אם המסך אינו נכבה אוטומטית, שומר מסך ימשיך להופיע ברציפות.\n"
                + "• 1 דק' / 5 דק' - שומר מסך יורד מהמסך בתום הזמן שנבחר. אם זמן כיבוי המסך של Android ארוך יותר, המסך עצמו עשוי להישאר דולק עד לכיבוי המערכת.\n"
                + "• ללא כיבוי - האפליקציה מונעת כיבוי מסך אוטומטי ומשאירה את שומר מסך מוצג ברציפות, עד ליציאה יזומה או לשינוי ההגדרה.\n\n"
                + "מניעת נעילה בזמן פעילות\n"
                + "ההגדרה קובעת האם Android רשאי להציג את מסך הנעילה מעל שומר מסך בזמן שהתצוגה פעילה.\n"
                + "• כשההגדרה פעילה - האפליקציה מונעת את הופעת מסך הנעילה מעל שומר מסך במהלך זמן הפעילות.\n"
                + "• כשההגדרה כבויה - Android רשאי להציג את מסך הנעילה כרגיל, ושומר מסך יכול להופיע מעליו.\n\n"
                + "המשך הצגה במסך נעילה\n"
                + "• ההגדרה קובעת אם שומר מסך יישאר פעיל ויופיע שוב מעל מסך הנעילה לאחר שהמסך נכבה והופעל מחדש.\n"
                + "• במצב לפי המכשיר, כיבוי המסך על ידי המערכת מסיים את מחזור התצוגה; כשההגדרה פעילה שומר מסך יופיע שוב בהפעלה הבאה.\n"
                + "• בבחירת 1 דק' או 5 דק', ההמשך יתאפשר רק אם המסך אכן כבה עד למועד סיום זמן הפעילות. אם זמן הכיבוי של המכשיר ארוך יותר, שומר מסך ייסגר בתום הזמן ולא ימתין לכיבוי מאוחר.\n"
                + "• במכשיר שאין בו מסך נעילה, להגדרה זו אין משמעות מעשית.\n\n"
                + "הפעלה אוטומטית בנעילה\n"
                + "• ההגדרה מפעילה את שומר מסך אוטומטית כאשר המכשיר ננעל, גם בנעילה יזומה וגם בנעילה אוטומטית, גם אם שומר מסך לא היה פעיל קודם.\n"
                + "• כשההגדרה כבויה - נעילת המכשיר לבדה לא מפעילה את שומר מסך.\n"
                + "• לצורך ההפעלה האוטומטית בנעילה, האפליקציה מציגה התראה קבועה ושקטה שאינה מופיעה במסך הנעילה.\n\n"
                + "הפעלה אוטומטית בהדלקה\n"
                + "• כשההגדרה פעילה, שומר מסך נפתח אוטומטית לאחר אתחול מלא של המכשיר, לאחר ש-Android מאפשר להפעיל את האפליקציה.\n"
                + "• כשההגדרה כבויה, אתחול המכשיר אינו מפעיל את שומר מסך.\n\n"
                + "הפעלה אוטומטית בהתעוררות\n"
                + "• כשההגדרה פעילה, שומר מסך נפתח כשהמסך נדלק מחדש לאחר שהיה כבוי.\n"
                + "• כשההגדרה כבויה, הדלקת המסך אינה מפעילה את שומר מסך.\n\n"                + "הרשאות שמשפיעות על התנהגות האפליקציה\n"
                + "• הצגה מעל אפליקציות אחרות - נדרשת להפעלה אוטומטית ברקע בחלק מגרסאות Android. אם ההרשאה נשללת, ההגדרות התלויות בה יכובו אוטומטית.\n"
                + "• גישה להתראות - נדרשת להצגת נגן המדיה. אם ההרשאה נשללת, הצגת הנגן תכובה אוטומטית.\n"
                + "• הרשאת מנהל מכשיר - מאפשרת לאפליקציה לבקש נעילה מיידית בסיום זמן פעילות מוגדר. היא אינה נדרשת להצגת שומר מסך עצמו ואינה מבטיחה כיבוי פיזי של התצוגה בכל סוגי המכשירים.";

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

        TextView title = textView("הגדרת זמני הפעילות", 20, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        title.setIncludeFontPadding(false);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView("הסבר קצר להבנת האפשרויות", 14, MUTED);
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
                    line.equals("זמן פעילות")
                    || line.equals("מניעת נעילה בזמן פעילות")
                    || line.equals("המשך הצגה במסך הנעילה")
                    || line.equals("הפעלה אוטומטית בנעילה")
                    || line.equals("הפעלה אוטומטית בהדלקה")
                    || line.equals("הפעלה אוטומטית בהתעוררות")
                    || line.equals("הרשאות שמשפיעות על התנהגות האפליקציה");

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
        close.setText("הבנתי");
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
        closeX.setContentDescription("סגירת חלונית ההסבר");
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
            try {
                prefs.edit().putBoolean(key, checked).apply();

                if ("autoStartOnCharge".equals(key)) {
                    NightScreenApplication.syncChargerReceiver(this);
                }
            } catch (Throwable ignored) {
            }
            if ("useAndroidDreamForCharging".equals(key)) {
                prefs.edit().putBoolean("autoStartOverlayPermissionPending", false).apply();
                NightScreenApplication.syncChargerReceiver(this);
                LockScreenMonitorService.sync(this);
                if (checked) {
                    try {
                        startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                    } catch (Throwable ignored) {
                        prefs.edit().putBoolean(key, false).apply();
                        toggle.setChecked(false);
                        AppToast.makeText(this,
                                "הגדרות שומר המסך של Android אינן זמינות במכשיר זה.",
                                Toast.LENGTH_LONG).show();
                    }
                } else if (getBool("autoStartOnCharge", false)
                        && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    prefs.edit().putBoolean("autoStartOverlayPermissionPending", true).apply();
                    requestChargerOverlayPermission();
                }
            } else if ("autoStartOnCharge".equals(key)) {
                if (checked && !getBool("useAndroidDreamForCharging", true)
                        && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
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
            } else if ("autoStartOnDeviceLock".equals(key)) {
                if (checked && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", true).apply();
                    requestLockStartOverlayPermission();
                } else {
                    prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", false).apply();
                }
                if (!checked) {
                    prefs.edit().putBoolean("autoStartOnLockOverlayPermissionPending", false).apply();
                }
                LockScreenMonitorService.sync(this);
                NightScreenApplication.syncChargerReceiver(this);
                updateOverlayPermissionCardVisibility();
            } else if ("autoStartOnBoot".equals(key) || "autoStartOnWake".equals(key)) {
                if (checked && Build.VERSION.SDK_INT >= 29
                        && !Settings.canDrawOverlays(this)) {
                    AppToast.makeText(this,
                            "כדי לאפשר הפעלה אוטומטית לאחר אתחול או התעוררות, יש לאפשר לשומר מסך הצגה מעל אפליקציות אחרות.",
                            Toast.LENGTH_LONG).show();
                    if (!openOverlayPermissionSettings()) {
                        prefs.edit().putBoolean(key, false).apply();
                        toggle.setChecked(false);
                    }
                }
                if ("autoStartOnWake".equals(key)) {
                    LockScreenMonitorService.sync(this);
                    NightScreenApplication.syncChargerReceiver(this);
                }
            } else if ("showPlayer".equals(key)) {
                if (checked && !hasNotificationAccess()) {
                    prefs.edit().putBoolean("showPlayerPermissionPending", true).apply();
                    requestPlayerNotificationAccessIfNeeded();
                } else {
                    prefs.edit().putBoolean("showPlayerPermissionPending", false).apply();
                }
            } else if ("pauseWhenExcludedAppActive".equals(key)
                    && checked && !hasUsageAccess()) {
                prefs.edit().putBoolean(key, false).apply();
                toggle.setChecked(false);
                AppToast.makeText(this,
                        "כדי להפעיל החרגת אפליקציות, יש לאפשר תחילה הרשאת נתוני שימוש.",
                        Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Throwable ignored) {
                }
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

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView valueText = textView("", 15, ACCENT);
        valueText.setGravity(Gravity.CENTER);
        valueText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        valueText.setTextDirection(View.TEXT_DIRECTION_LTR);
        valueRow.addView(valueText, new LinearLayout.LayoutParams(0, dp(30), 1f));

        TextView reset = textView("איפוס", 13, ON_SURFACE);
        reset.setGravity(Gravity.CENTER);
        reset.setIncludeFontPadding(false);
        reset.setPadding(dp(12), dp(5), dp(12), dp(5));
        reset.setBackground(roundOutline(CONTROL_SURFACE, dp(9), OUTLINE));
        reset.setClickable(true);
        reset.setFocusable(true);
        reset.setContentDescription("איפוס " + label + " לברירת המחדל");
        valueRow.addView(reset, new LinearLayout.LayoutParams(-2, dp(32)));
        card.addView(valueRow, new LinearLayout.LayoutParams(-1, dp(34)));

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(Math.max(0, Math.min(maxValue - minValue,
                getInt(key, 0) - minValue)));
        seekBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        seekBar.setContentDescription(label);
        seekBar.setMinimumHeight(dp(48));
        seekBar.setPadding(dp(8), 0, dp(8), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            seekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
            seekBar.setThumbTintList(ColorStateList.valueOf(ON_SURFACE));
            seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(TRACK));
        }
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(-1, dp(52));
        seekParams.leftMargin = dp(2);
        seekParams.rightMargin = dp(2);
        card.addView(seekBar, seekParams);

        LinearLayout range = new LinearLayout(this);
        range.setOrientation(LinearLayout.HORIZONTAL);
        range.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        TextView low = textView("−" + Math.abs(minValue) + "%", 12, MUTED);
        TextView center = textView("0% · ברירת מחדל", 12, MUTED);
        TextView high = textView("+" + maxValue + "%", 12, MUTED);
        low.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        center.setGravity(Gravity.CENTER);
        high.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        range.addView(low, new LinearLayout.LayoutParams(0, dp(22), 1f));
        range.addView(center, new LinearLayout.LayoutParams(0, dp(22), 1f));
        range.addView(high, new LinearLayout.LayoutParams(0, dp(22), 1f));
        card.addView(range, new LinearLayout.LayoutParams(-1, dp(22)));

        Runnable updateValue = () -> {
            int value = seekBar.getProgress() + minValue;
            valueText.setText(value == 0 ? "0% · מרכז" : (value > 0 ? "+" : "") + value + "%");
            reset.setAlpha(value == 0 ? 0.55f : 1f);
        };
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = progress + minValue;
                updateValue.run();
                if (fromUser) prefs.edit().putInt(key, value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        reset.setOnClickListener(v -> {
            int defaultValue = Math.max(minValue, Math.min(maxValue, 0));
            seekBar.setProgress(defaultValue - minValue);
            prefs.edit().putInt(key, defaultValue).apply();
            updateValue.run();
        });
        updateValue.run();
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
        int optionTileHeight = 37;

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
            tile.addView(option, new FrameLayout.LayoutParams(-1, -1));
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
                    || ("playerDisplayMode".equals(key) && i == 2);
            if (mayExpandOnNarrowScreens) {
                int textWidth = (int) Math.ceil(option.getPaint().measureText(names[i]));
                responsiveTextWidths[i] = textWidth + dp(2);
                int extraWidth = "playerDisplayMode".equals(key) && i == 2
                        ? 16 : 8;
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
            notices.add("בבדיקה האחרונה לא זוהה מסך נעילה בפועל. לכן ההגדרות הקשורות לנעילה ולהצגת שומר מסך במסך הנעילה עשויות שלא להיות רלוונטיות למכשיר זה.");
        } else if (automotive) {
            notices.add("זוהתה במכשיר תמיכה ב־Android Automotive. בחלק ממערכות המולטימדיה אין מסך נעילה רגיל, ולכן הגדרות הקשורות לנעילה עשויות שלא להיות רלוונטיות.");
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
                notices.add("לא ניתן לזהות את זמן כיבוי המסך שהוגדר במכשיר. בסיום זמן הפעילות שומר מסך ייסגר, אך כיבוי התצוגה נשאר באחריות Android.");
            } else if (timeoutMs >= Integer.MAX_VALUE - 1000L) {
                notices.add("לא זוהה במכשיר כיבוי מסך אוטומטי. בבחירת 1 דק' או 5 דק' שומר מסך ייסגר בתום הזמן, אך המסך עצמו עשוי להישאר דולק.");
            } else if (timeoutMs > requestedMs) {
                notices.add("זמן כיבוי המסך שמוגדר במכשיר ארוך מזמן הפעילות שבחרת. בסיום הזמן שומר מסך ייסגר, אך המסך עשוי להישאר דולק עד ש־Android יכבה אותו.");
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
        LinearLayout choiceRow = null;

        for (int i = 0; i < names.length; i++) {
            int columnsPerRow = names.length > 5 ? 4 : 5;
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
            });
            choiceRow.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        LinearLayout customColorRow = new LinearLayout(this);
        customColorRow.setOrientation(LinearLayout.HORIZONTAL);
        customColorRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        customColorRow.setGravity(Gravity.CENTER_VERTICAL);
        customColorRow.setPadding(dp(5), dp(7), dp(5), dp(2));
        View customColorSwatch = new View(this);
        customColorSwatch.setBackground(makeCustomColorDot(
                getInt(key, "screenBackgroundColor".equals(key) ? Color.BLACK : Color.WHITE)));
        customColorRow.addView(customColorSwatch,
                new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView customColorLabel = textView("בחירת צבע מותאם אישית", 14, ON_SURFACE);
        customColorLabel.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams customLabelParams =
                new LinearLayout.LayoutParams(-2, dp(32));
        customLabelParams.rightMargin = dp(9);
        customColorRow.addView(customColorLabel, customLabelParams);
        customColorRow.setClickable(true);
        customColorRow.setFocusable(true);
        customColorRow.setOnClickListener(v -> showCustomColorDialog(key, customColorSwatch));
        LinearLayout.LayoutParams customColorParams = new LinearLayout.LayoutParams(-1, -2);
        customColorParams.topMargin = dp(5);
        choices.addView(customColorRow, customColorParams);

        LinearLayout.LayoutParams choicesParams =
                new LinearLayout.LayoutParams(-1, -2);
        choicesParams.topMargin = dp(9);
        card.addView(choices, choicesParams);
    }

    private GradientDrawable makeCustomColorDot(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setStroke(dp(1), Color.rgb(145, 170, 196));
        return dot;
    }

    
    private void showCustomColorDialog(String key, View preview) {
        int current = getInt(key,
                "screenBackgroundColor".equals(key) ? Color.BLACK : Color.WHITE);
        final float[] hsv = new float[3];
        Color.colorToHSV(current, hsv);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(12), dp(16), dp(10));
        panel.setBackgroundColor(CARD_SURFACE);

        LinearLayout sampleRow = new LinearLayout(this);
        sampleRow.setOrientation(LinearLayout.HORIZONTAL);
        sampleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sampleRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout sampleCopy = new LinearLayout(this);
        sampleCopy.setOrientation(LinearLayout.VERTICAL);
        sampleCopy.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sampleCopy.setGravity(Gravity.CENTER_VERTICAL);
        TextView sampleTitle = textView("הצבע שנבחר", 15, ON_SURFACE);
        sampleTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sampleCopy.addView(sampleTitle, new LinearLayout.LayoutParams(-1, -2));
        TextView sampleHint = textView("כוון את המחוונים עד לקבלת הגוון הרצוי", 12, MUTED);
        sampleHint.setPadding(0, dp(3), 0, 0);
        sampleCopy.addView(sampleHint, new LinearLayout.LayoutParams(-1, -2));
        sampleRow.addView(sampleCopy, new LinearLayout.LayoutParams(0, -2, 1f));

        View colorPreview = new View(this);
        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        previewParams.leftMargin = dp(10);
        colorPreview.setBackground(makeCustomColorDot(current));
        sampleRow.addView(colorPreview, previewParams);
        panel.addView(sampleRow, new LinearLayout.LayoutParams(-1, dp(48)));

        View separator = new View(this);
        separator.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.topMargin = dp(8);
        separatorParams.bottomMargin = dp(7);
        panel.addView(separator, separatorParams);

        String[] captions = {"גוון", "רוויה", "בהירות"};
        int[] maxes = {359, 100, 100};

        Runnable updateSwatch = () -> {
            int selected = Color.HSVToColor(hsv);
            colorPreview.setBackground(makeCustomColorDot(selected));
            int luminance = Math.round(Color.red(selected) * 0.299f
                    + Color.green(selected) * 0.587f
                    + Color.blue(selected) * 0.114f);
            sampleTitle.setTextColor(luminance < 135 ? Color.WHITE : ON_SURFACE);
        };

        for (int i = 0; i < captions.length; i++) {
            final int index = i;
            LinearLayout control = new LinearLayout(this);
            control.setOrientation(LinearLayout.VERTICAL);
            control.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            LinearLayout captionRow = new LinearLayout(this);
            captionRow.setOrientation(LinearLayout.HORIZONTAL);
            captionRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            captionRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView caption = textView(captions[i], 14, ON_SURFACE);
            captionRow.addView(caption, new LinearLayout.LayoutParams(0, dp(24), 1f));
            TextView value = textView("", 13, ACCENT);
            value.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            value.setTextDirection(View.TEXT_DIRECTION_LTR);
            captionRow.addView(value, new LinearLayout.LayoutParams(-2, dp(24)));
            control.addView(captionRow, new LinearLayout.LayoutParams(-1, dp(24)));

            SeekBar bar = new SeekBar(this);
            bar.setMax(maxes[i]);
            bar.setProgress(Math.max(0, Math.min(maxes[i],
                    Math.round(hsv[i] * (i == 0 ? 1f : 100f)))));
            bar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            bar.setPadding(dp(4), 0, dp(4), 0);
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
            control.addView(bar, new LinearLayout.LayoutParams(-1, dp(38)));
            LinearLayout.LayoutParams controlParams =
                    new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) controlParams.topMargin = dp(3);
            panel.addView(control, controlParams);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("בחירת צבע מותאם אישית")
                .setView(panel)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("שמירה", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int selected = Color.HSVToColor(hsv);
                    persistOption(key, String.format(Locale.US, "#%08X", selected));
                    preview.setBackground(makeCustomColorDot(selected));
                    dialog.dismiss();
                }));
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            window.setLayout(Math.min(dp(390),
                    getResources().getDisplayMetrics().widthPixels - dp(36)),
                    Math.min(dp(380), Math.max(dp(250), screenHeight - dp(120))));
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

    private void requestPlayerNotificationAccessIfNeeded() {
        if (hasNotificationAccess()) return;
        AppToast.makeText(this,
                "כדי להציג את הנגן, צריך לאפשר לשומר מסך גישה להתראות במסך הבא. בחר את \"שומר מסך\" והפעל את ההרשאה.",
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
                "הצגת הנגן נשארה כבויה מפני שלא ניתנה הרשאת גישה להתראות.",
                Toast.LENGTH_LONG).show();
        if (currentCategory == CATEGORY_PLAYER) showCategory(CATEGORY_PLAYER);
    }

    private void requestChargerOverlayPermission() {
        if (getBool("useAndroidDreamForCharging", true)
                || Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        AppToast.makeText(this,
                "כדי לאפשר הפעלה אוטומטית בטעינה, בחר במסך הבא את \"שומר מסך\" ואפשר \"הצגה מעל אפליקציות אחרות\".",
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
                || getBool("useAndroidDreamForCharging", true)) return;

        prefs.edit().putBoolean("autoStartOnCharge", false).apply();
        if (autoStartOnChargeToggle != null && autoStartOnChargeToggle.isChecked()) {
            autoStartOnChargeToggle.setChecked(false);
        }
        AppToast.makeText(this,
                "המתג \"הפעלה אוטומטית בטעינה\" נשאר כבוי מפני שלא ניתנה הרשאת \"הצגה מעל אפליקציות אחרות\".",
                Toast.LENGTH_LONG).show();
    }

    private void requestLockStartOverlayPermission() {
        if (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        AppToast.makeText(this,
                "כדי לאפשר הפעלה אוטומטית בנעילה, אפשר במסך הבא לשומר מסך את ההרשאה \"הצגה מעל אפליקציות אחרות\".",
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
                "ההפעלה האוטומטית בנעילת המכשיר נשארה כבויה מפני שלא ניתנה הרשאת \"הצגה מעל אפליקציות אחרות\".",
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

    private String iconForCard(String title) {
        if ("תצוגה במסך".equals(title)) return "monitor";
        if ("שומר מסך בזמן טעינה".equals(title)) return "android-charge";
        if ("עיצוב".equals(title)) return "palette";
        if ("תצוגת שעון ותאריך".equals(title)) return "clock";
        if ("תצוגת הנגן".equals(title)) return "music";
        if ("מסגרת לנגן".equals(title)) return "frame";
        if ("הרשאת גישה להתראות".equals(title)) return "bell";
        if ("הרשאת תצוגה על אפליקציות".equals(title)) return "auto-start";
        if ("הרשאת מנהל מכשיר".equals(title)) return "shield";
        return "settings";
    }

    private String iconForSetting(String key) {
        if ("showHebrew".equals(key) || "dateDisplayMode".equals(key)) return "hebrew-date";
        if ("showGregorian".equals(key)) return "gregorian-date";
        if ("showPlayer".equals(key) || "playerDisplayMode".equals(key)) return "media-toggle";
        if ("screenSaverDuration".equals(key)) return "activity-history";
        if ("preventLockWhileActive".equals(key)) return "power-off";
        if ("autoStartOnCharge".equals(key)) return "usb";
        if ("autoStartOnDeviceLock".equals(key)) return "lock-play";
        if ("autoStartOnBoot".equals(key)) return "power";
        if ("autoStartOnWake".equals(key)) return "screen-wake";
        if ("continueOnLockScreen".equals(key)) return "shield";
        if ("noShutdownWhileCharging".equals(key)) return "phone-charging";
        if ("screenBackgroundColor".equals(key)) return "palette";
        if ("screenBrightness".equals(key)) return "sun-dim";
        if ("fontColor".equals(key)) return "paint-roller";
        if ("fontWeight".equals(key)) return "bold";
        if ("fontFamily".equals(key)) return "font-family";
        if ("clockDateSize".equals(key)) return "clock-size";
        if ("clockDisplayMode".equals(key)) return "clock";
        if ("clockDialStyle".equals(key)) return "clock-dial";
        if ("clockDatePosition".equals(key)) return "position-arrows";
        if ("showPlayerOnlyAtStart".equals(key)) return "circle-play";
        if ("showDetails".equals(key)) return "artist";
        if ("playerControlsDisplayMode".equals(key)) return "progress-time-digital";
        if ("chargingBatteryDisplayMode".equals(key)) return "icon-battery-percent";
        if ("playerSize".equals(key)) return "player-size-resize";
        if ("playerPosition".equals(key)) return "panel-bottom";
        if ("frameEnabled".equals(key)) return "frame-dashed";
        if ("frameColor".equals(key)) return "pipette";
        if ("frameWidth".equals(key)) return "border-width";
        if ("frameRadius".equals(key)) return "corners";
        return "settings";
    }

    private View makeIconGlyph(String iconName, int tint) {
        int resourceId;
        if ("activity-history".equals(iconName)) {
            // The APK's original _N resource is the dedicated activity-time/history glyph.
            resourceId = R.drawable.ic_settings_activity_history;
        } else {
            String resourceName = "icon_" + iconName.replace('-', '_');
            resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
            if (resourceId == 0) resourceId = R.drawable.icon_settings;
        }
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageResource(resourceId);
        // These two symbols contain separate foreground/background colors.
        // A whole-image tint would erase the play mark and the close X.
        if (!"lock-play".equals(iconName) && !"close-circle".equals(iconName)) {
            image.setColorFilter(tint);
        }
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        image.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        image.setContentDescription(iconName);
        return image;
    }

    private FrameLayout makeIconBadge(String iconName, int size) {
        FrameLayout badge = new FrameLayout(this);
        // Keep the standard square badge behind every settings glyph, including progress/time.
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
            heading.setPadding(dp(2), 0, dp(5), 0);
            heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
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
                return "screenBackgroundColor".equals(key) ? 0 : -1;
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

    private LinearLayout addOverlayPermissionCard(LinearLayout parent) {
        LinearLayout card = addCard(parent, "הרשאת תצוגה על אפליקציות");
        card.setPadding(dp(8), dp(9), dp(12), dp(15));
        setPermissionCardHeadingTypeface(card);

        TextView message = textView(
                "ההרשאה מאפשרת הפעלה אוטומטית של שומר המסך בחיבור לטעינה.",
                15, ON_SURFACE);
        message.setGravity(Gravity.CENTER);
        message.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        message.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        card.addView(message, messageParams);

        overlayPermissionStatus = textView("", 14, MUTED);
        overlayPermissionStatus.setGravity(Gravity.CENTER);
        overlayPermissionStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        overlayPermissionStatus.setTextDirection(View.TEXT_DIRECTION_RTL);
        overlayPermissionStatus.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        card.addView(overlayPermissionStatus, statusParams);

        overlayPermissionButton = makeActionButton("פתיחת הגדרות הרשאה");
        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(-2, dp(44));
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.topMargin = dp(10);
        card.addView(overlayPermissionButton, buttonParams);
        overlayPermissionButton.setOnClickListener(v -> openOverlayPermissionSettings());
        updateOverlayPermissionCardState();
        return card;
    }

    private void updateOverlayPermissionCardVisibility() {
        // Overlay permission management now lives in PermissionsActivity.
    }

    private void updateOverlayPermissionCardState() {
        if (overlayPermissionCard == null
                || overlayPermissionStatus == null
                || overlayPermissionButton == null) return;

        boolean granted = Settings.canDrawOverlays(this);
        boolean autoStartEnabled = (getBool("autoStartOnCharge", false)
                && !getBool("useAndroidDreamForCharging", true))
                || getBool("autoStartOnDeviceLock", false)
                || getBool("autoStartOnBoot", false)
                || getBool("autoStartOnWake", false);
        overlayPermissionStatus.setText(granted
                ? (autoStartEnabled
                    ? "ההרשאה פעילה — ההפעלה האוטומטית זמינה."
                    : "ההרשאה ניתנה, אך ההפעלה האוטומטית כבויה.")
                : "נדרשת הרשאה להפעלה אוטומטית.");
        overlayPermissionStatus.setTextColor(granted ? ACCENT : MUTED);
        overlayPermissionButton.setText(granted
                ? "ההרשאה פעילה"
                : "פתיחת הגדרות הרשאה");
        overlayPermissionButton.setEnabled(!granted);
        overlayPermissionButton.setAlpha(granted ? 0.65f : 1f);
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
                    "לא ניתן לפתוח את הגדרת ההרשאה. פתח בהגדרות Android את \"הצגה מעל אפליקציות אחרות\".",
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void updateNotificationAccessCardVisibility() {
        // Notification access management now lives in PermissionsActivity.
    }

    private void positionPermissionCards() {
        if (content == null) return;

        // Reinsert permission cards in a stable order: notification access,
        // charger auto-start overlay permission, then device-admin permission.
        if (notificationAccessCard != null && notificationAccessCard.getParent() == content) {
            content.removeView(notificationAccessCard);
        }
        if (overlayPermissionCard != null && overlayPermissionCard.getParent() == content) {
            content.removeView(overlayPermissionCard);
        }

        int frameIndex = playerFrameCard != null ? content.indexOfChild(playerFrameCard) : -1;
        int insertAt = frameIndex >= 0 ? frameIndex + 1 : content.getChildCount();

        if (notificationAccessCard != null) {
            insertAt = Math.min(insertAt, content.getChildCount());
            content.addView(notificationAccessCard, insertAt++);
        }
        if (overlayPermissionCard != null) {
            insertAt = Math.min(insertAt, content.getChildCount());
            content.addView(overlayPermissionCard, insertAt);
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

    private boolean openNotificationAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return true;
        } catch (ActivityNotFoundException ignored) {
            AppToast.makeText(this, "לא ניתן לפתוח את הגדרות ההתראות במכשיר.",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            AppToast.makeText(this, "לא ניתן לפתוח את הגדרות ההתראות במכשיר.",
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void updateScreenLockAdminCard() {
        if (screenLockAdminCard == null || screenLockAdminStatus == null
                || screenLockAdminButton == null) return;
        boolean active = isScreenLockAdminActive();
        screenLockAdminStatus.setText(active
                ? "הרשאה פעילה — המכשיר יינעל בסיום הזמן."
                : "ההרשאה עדיין לא ניתנה, כיבוי המסך תלוי בהגדרות המכשיר.");
        screenLockAdminStatus.setTextColor(active ? ACCENT : MUTED);
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
                    "ההרשאה מאפשרת לנעול את המכשיר בכל מצב בסיום הזמן שנבחר.\n"
                    + "ללא ההרשאה, כיבוי המסך לא יהיה לפני הזמן המוגדר במכשיר.");
            startActivityForResult(intent, REQUEST_ENABLE_SCREEN_LOCK_ADMIN);
        } catch (Throwable ex) {
            AppToast.makeText(this, "לא ניתן לפתוח את בקשת הרשאת מנהל המכשיר.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_SCREEN_LOCK_ADMIN) {
            updateScreenLockAdminCard();
            if (isScreenLockAdminActive()) {
                AppToast.makeText(this, "הרשאת נעילת המסך הופעלה.",
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
        private final String value;
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
            paint.setStrokeWidth(dp(selected ? 3 : 1));
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