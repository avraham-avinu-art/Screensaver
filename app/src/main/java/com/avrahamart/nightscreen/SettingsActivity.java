package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.PopupMenu;
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
    private LinearLayout currentSettingsGroup;
    private LinearLayout currentSettingsGroupParent;
    private boolean settingsGroupBreakAfterPrevious;
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

        screenSubtitle = textView("התאמה אישית של השומר מסך", 15, MUTED);
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
                Toast.makeText(this, disabledFeatures, Toast.LENGTH_LONG).show();
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
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        content.removeAllViews();
        currentSettingsGroup = null;
        currentSettingsGroupParent = null;
        settingsGroupBreakAfterPrevious = false;
        screenTitle.setText("הגדרות");
        screenSubtitle.setText("התאמה אישית של השומר מסך");

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
                "power", CATEGORY_ACTIVITY);
        addDashboardButton("מראה ועיצוב",
                "התאמת צבעים, גופנים ובהירות המסך",
                "palette", CATEGORY_APPEARANCE);
        addDashboardButton("שעון ותאריך",
                "התאמת תצוגת השעה והתאריך",
                "calendar-clock", CATEGORY_CLOCK_DATE);
        addDashboardButton("נגן מדיה",
                "הצגת והתאמת נגן המערכת",
                "music-note", CATEGORY_PLAYER);

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
    }

    private LinearLayout.LayoutParams dashboardButtonParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        params.bottomMargin = dp(6);
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
        button.setPadding(dp(14), dp(12), dp(14), dp(12));
        button.setMinimumHeight(dp(99));
        button.setBackground(roundOutline(CARD_SURFACE, dp(19), CARD_OUTLINE));
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(description == null || description.isEmpty()
                ? title : title + ". " + description);

        FrameLayout iconBadge = new FrameLayout(this);
        iconBadge.setBackground(roundOutline(ICON_SURFACE, dp(12), CARD_OUTLINE));
        View glyph = makeIconGlyph(iconName, ACCENT);
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
        // Show the page heading on every internal settings page.
        screenHeader.setVisibility(View.VISIBLE);
        screenHeader.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        screenHeader.setPadding(dp(2), dp(8), dp(2), dp(8));
        screenSubtitle.setVisibility(View.VISIBLE);
        screenTitle.setTextSize(23);
        dashboardHeaderDivider.setVisibility(View.GONE);
        headerTitleBlock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenTitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        screenSubtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        screenSubtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        if (settingsScroll != null) settingsScroll.setFillViewport(true);
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        content.removeAllViews();
        currentSettingsGroup = null;
        currentSettingsGroupParent = null;
        settingsGroupBreakAfterPrevious = false;
        clockDateCard = null;
        playerFrameCard = null;
        activityCapabilityNotice = null;

        switch (category) {
            case CATEGORY_CLOCK_DATE:
                screenTitle.setText("שעון ותאריך");
                screenSubtitle.setText("התאמת תצוגת השעה והתאריך");
                buildClockDateSettings();
                break;
            case CATEGORY_ACTIVITY:
                screenTitle.setText("הפעלה והתנהגות");
                screenSubtitle.setText("הגדרת זמני הפעילות ואפשרויות ההפעלה");
                addActivityHelpHeaderButton();
                buildActivitySettings();
                break;
            case CATEGORY_APPEARANCE:
                screenTitle.setText("מראה ועיצוב");
                screenSubtitle.setText("התאמת צבעים, גופנים ובהירות המסך");
                buildAppearanceSettings();
                break;
            case CATEGORY_PLAYER:
                screenTitle.setText("נגן מדיה");
                screenSubtitle.setText("הצגת והתאמת נגן המערכת");
                buildPlayerSettings();
                break;
            default:
                buildDashboard();
                return;
        }

        // Leave a bottom inset inside the final shared frame, matching its side inset.
        finishSettingsGroup();

        // Every internal category must open at the start, regardless of the last
        // scroll position used in another category.
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
        addSegmentedChoices(content, "תצוגת תאריך",
                new String[]{"עברי ולועזי", "רק עברי", "רק לועזי"},
                new String[]{"both", "hebrew", "gregorian"}, "dateDisplayMode");
        addDiscreteSlider(content, "גודל",
                new String[]{"קטן", "בינוני", "רגיל", "גדול", "ענק"},
                new String[]{"60", "72", "84", "96", "108"}, "clockDateSize");
        addDiscreteSlider(content, "עובי הגופן",
                new String[]{"דק", "רגיל", "מודגש", "עבה"},
                new String[]{"-1", "0", "1", "2"}, "fontWeight");
        addSegmentedChoices(content, "מיקום",
                new String[]{"בראש המסך", "ברבע העליון", "במרכז המסך"},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");
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
        LinearLayout durationCard = addSegmentedChoices(content, "זמן פעילות",
                new String[]{"לפי המכשיר", "דקה אחת", "חמש דקות", "ללא כיבוי"},
                new String[]{"device", "1", "5", "0"}, "screenSaverDuration");
        addActivityCapabilityNotice(durationCard);
        addCheck(content, "מניעת נעילה בזמן פעילות", "preventLockWhileActive", false);
        addCheck(content, "המשך הצגה במסך נעילה", "continueOnLockScreen", false);
        addCheck(content, "הפעלה אוטומטית בנעילה", "autoStartOnDeviceLock", false);
        addCheck(content, "הפעלה אוטומטית בהדלקה", "autoStartOnBoot", false);
        addCheck(content, "הפעלה אוטומטית בהתעוררות", "autoStartOnWake", false);
        addCheck(content, "הפעלה אוטומטית בטעינה", "autoStartOnCharge", false);
        addCheck(content, "ללא כיבוי בטעינה", "noShutdownWhileCharging", false);
        addCheck(content, "הצגת אחוז הסוללה", "showBatteryPercent", false);
    }

    private void buildAppearanceSettings() {
        addColorChoices(content, "צבע המסך",
                new String[]{"שחור", "כחול", "כחול כהה", "טורקיז",
                        "אפור פחם", "ירוק יער", "כחול עמוק", "אפור אבן"},
                new String[]{"#000000", "#142438", "#091321", "#12313A",
                        "#252B33", "#20362E", "#193740", "#303943"},
                "screenBackgroundColor");
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
        addSegmentedChoices(content, "הצגת הנגן",
                new String[]{"כבוי", "פועל", "פועל בתחילת שיר"},
                new String[]{"off", "on", "start"}, "playerDisplayMode");
        addCheck(content, "פרטי אמן", "showDetails", getBool("showDetails", true));
        addCheck(content, "זמני השיר ופקדי הנגן", "showProgress", true);
        addDiscreteSlider(content, "גודל הנגן",
                new String[]{"קטן", "בינוני", "גדול", "ענק"},
                new String[]{"14", "17", "20", "23"}, "playerSize");
        addSegmentedChoices(content, "מיקום הנגן",
                new String[]{"ברבע התחתון", "בתחתית המסך"},
                new String[]{"lower", "bottom"}, "playerPosition");
        addCheck(content, "הצג מסגרת", "frameEnabled", false);
        addColorChoices(content, "צבע מסגרת",
                new String[]{"לבן", "כחול עדין", "כסף", "אפור", "אפור בהיר"},
                new String[]{"white", "blue", "silver", "gray", "light"}, "frameColor");
        addDiscreteSlider(content, "עובי מסגרת",
                new String[]{"דקה", "בינונית", "עבה"},
                new String[]{"1", "2", "3"}, "frameWidth");
        playerFrameCard = addDiscreteSlider(content, "עיגול פינות",
                new String[]{"ישר", "עדין", "מעוגל"},
                new String[]{"0", "10", "20"}, "frameRadius");
    }

    private LinearLayout addSettingCard(LinearLayout parent, String label, String key) {
        boolean beginsNewGroup = currentSettingsGroup == null
                || currentSettingsGroupParent != parent
                || settingsGroupBreakAfterPrevious;

        if (beginsNewGroup) {
            if (currentSettingsGroup != null
                    && currentSettingsGroupParent == parent) {
                LinearLayout.LayoutParams previousGroupParams =
                        (LinearLayout.LayoutParams) currentSettingsGroup.getLayoutParams();
                // Space is reserved only between distinct groups, not between rows.
                previousGroupParams.bottomMargin = dp(11);
                currentSettingsGroup.setLayoutParams(previousGroupParams);
            }

            currentSettingsGroup = new LinearLayout(this);
            currentSettingsGroup.setOrientation(LinearLayout.VERTICAL);
            currentSettingsGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            currentSettingsGroup.setBackground(
                    roundOutline(CARD_SURFACE, dp(17), CARD_OUTLINE));
            LinearLayout.LayoutParams groupParams =
                    new LinearLayout.LayoutParams(-1, -2);
            groupParams.bottomMargin = 0;
            parent.addView(currentSettingsGroup, groupParams);
            currentSettingsGroupParent = parent;
            settingsGroupBreakAfterPrevious = false;
        } else if (currentSettingsGroup.getChildCount() > 0) {
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
        // Individual settings are now rows inside one shared outer group frame.
        card.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = 0;
        currentSettingsGroup.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        textBlock.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = textView(label, 18, ON_SURFACE);
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

        // These are the intentional boundaries between the setting groups.
        settingsGroupBreakAfterPrevious =
                "continueOnLockScreen".equals(key)
                || "autoStartOnWake".equals(key)
                || "screenBrightness".equals(key)
                || "playerPosition".equals(key);
        return card;
    }

    private String settingDescription(String key) {
        if ("dateDisplayMode".equals(key)) return "בחירת התאריך שיופיע לצד השעה";
        if ("clockDateSize".equals(key)) return "בחירת גודל השעון והתאריכים";
        if ("fontWeight".equals(key)) return "בחירת עובי האותיות והספרות";
        if ("clockDatePosition".equals(key)) return "בחירת מיקום השעון והתאריכים במסך";
        if ("screenSaverDuration".equals(key)) return "בחירת משך הצגת שומר המסך";
        if ("preventLockWhileActive".equals(key)) return "מניעת הופעת מסך הנעילה בזמן הפעילות";
        if ("continueOnLockScreen".equals(key)) return "המשך הצגת השומר לאחר כיבוי והדלקת המסך";
        if ("autoStartOnDeviceLock".equals(key)) return "הפעלת השומר באופן אוטומטי בעת נעילת המכשיר";
        if ("autoStartOnBoot".equals(key)) return "הפעלת השומר באופן אוטומטי לאחר הפעלת המכשיר";
        if ("autoStartOnWake".equals(key)) return "הפעלת השומר כשהמסך מתעורר ממצב שינה";
        if ("autoStartOnCharge".equals(key)) return "הפעלת השומר אוטומטית בעת חיבור מטען";
        if ("noShutdownWhileCharging".equals(key)) return "השארת המסך פעיל כל עוד המכשיר בטעינה";
        if ("showBatteryPercent".equals(key)) return "הצגת אחוז הסוללה בזמן טעינה";
        if ("screenBackgroundColor".equals(key)) return "בחירת צבע הרקע של שומר המסך";
        if ("screenBrightness".equals(key)) return "התאמת בהירות התצוגה של שומר המסך";
        if ("fontFamily".equals(key)) return "בחירת סוג הגופן שיוצג במסך";
        if ("fontColor".equals(key)) return "בחירת צבע השעה והתאריכים";
        if ("playerDisplayMode".equals(key)) return "בחירת אופן הצגת נגן המדיה";
        if ("showPlayerOnlyAtStart".equals(key)) return "הצגת הנגן רק בתחילת כל שיר";
        if ("showDetails".equals(key)) return "הצגת שם האמן מתחת לפרטי השיר";
        if ("showProgress".equals(key)) return "הצגת זמני השיר וסרגל ההתקדמות שליד פקדי הנגן";
        if ("playerSize".equals(key)) return "בחירת גודל נגן המדיה";
        if ("playerPosition".equals(key)) return "בחירת מיקום נגן המדיה במסך";
        if ("frameEnabled".equals(key)) return "הצגת מסגרת סביב נגן המדיה";
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
                + "ההגדרה קובעת כמה זמן השומר מסך יוצג, או האם משך התצוגה ינוהל על ידי המכשיר.\n"
                + "• לפי המכשיר - אין לאפליקציה טיימר סיום עצמאי. השומר נשאר מוצג עד שהמכשיר מכבה את המסך לפי ההגדרות שלו. אם המסך אינו נכבה אוטומטית, השומר ימשיך להופיע ברציפות.\n"
                + "• דקה אחת / חמש דקות - השומר יורד מהמסך בתום הזמן שנבחר. אם זמן כיבוי המסך של Android ארוך יותר, המסך עצמו עשוי להישאר דולק עד לכיבוי המערכת.\n"
                + "• ללא כיבוי - האפליקציה מונעת כיבוי מסך אוטומטי ומשאירה את השומר מוצג ברציפות, עד ליציאה יזומה או לשינוי ההגדרה.\n\n"
                + "מניעת נעילה בזמן פעילות\n"
                + "ההגדרה קובעת האם Android רשאי להציג את מסך הנעילה מעל השומר בזמן שהתצוגה פעילה.\n"
                + "• כשההגדרה פעילה - האפליקציה מונעת את הופעת מסך הנעילה מעל השומר במהלך זמן הפעילות.\n"
                + "• כשההגדרה כבויה - Android רשאי להציג את מסך הנעילה כרגיל, והשומר יכול להופיע מעליו.\n\n"
                + "המשך הצגה במסך נעילה\n"
                + "• ההגדרה קובעת אם השומר יישאר פעיל ויופיע שוב מעל מסך הנעילה לאחר שהמסך נכבה והופעל מחדש.\n"
                + "• במצב לפי המכשיר, כיבוי המסך על ידי המערכת מסיים את מחזור התצוגה; כשההגדרה פעילה השומר יופיע שוב בהפעלה הבאה.\n"
                + "• בבחירת דקה אחת או חמש דקות, ההמשך יתאפשר רק אם המסך אכן כבה עד למועד סיום זמן הפעילות. אם זמן הכיבוי של המכשיר ארוך יותר, השומר ייסגר בתום הזמן ולא ימתין לכיבוי מאוחר.\n"
                + "• במכשיר שאין בו מסך נעילה, להגדרה זו אין משמעות מעשית.\n\n"
                + "הפעלה אוטומטית בנעילה\n"
                + "• ההגדרה מפעילה את השומר מסך אוטומטית כאשר המכשיר ננעל, גם בנעילה יזומה וגם בנעילה אוטומטית, גם אם השומר לא היה פעיל קודם.\n"
                + "• כשההגדרה כבויה - נעילת המכשיר לבדה לא מפעילה את השומר מסך.\n"
                + "• לצורך ההפעלה האוטומטית בנעילה, האפליקציה מציגה התראה קבועה ושקטה שאינה מופיעה במסך הנעילה.\n\n"
                + "הפעלה אוטומטית בהדלקה\n"
                + "• כשההגדרה פעילה, השומר נפתח אוטומטית לאחר אתחול מלא של המכשיר, לאחר ש-Android מאפשר להפעיל את האפליקציה.\n"
                + "• כשההגדרה כבויה, אתחול המכשיר אינו מפעיל את השומר.\n\n"
                + "הפעלה אוטומטית בהתעוררות\n"
                + "• כשההגדרה פעילה, השומר נפתח כשהמסך נדלק מחדש לאחר שהיה כבוי.\n"
                + "• כשההגדרה כבויה, הדלקת המסך אינה מפעילה את השומר.\n\n"
                + "הרשאות שמשפיעות על התנהגות האפליקציה\n"
                + "• הצגה מעל אפליקציות אחרות - נדרשת להפעלה אוטומטית ברקע בחלק מגרסאות Android. אם ההרשאה נשללת, ההגדרות התלויות בה יכובו אוטומטית.\n"
                + "• גישה להתראות - נדרשת להצגת נגן המדיה. אם ההרשאה נשללת, הצגת הנגן תכובה אוטומטית.\n"
                + "• הרשאת מנהל מכשיר - מאפשרת לאפליקציה לבקש נעילה מיידית בסיום זמן פעילות מוגדר. היא אינה נדרשת להצגת השומר עצמו ואינה מבטיחה כיבוי פיזי של התצוגה בכל סוגי המכשירים.";

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
                    || line.equals("הפעלה אוטומטית בהפעלת המכשיר")
                    || line.equals("הפעלה אוטומטית בהתעוררות ממצב שינה")
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
                new LinearLayout.LayoutParams(dp(50), dp(38));
        // Move the switch toward the outer left edge, away from the text block.
        switchParams.leftMargin = -dp(6);
        row.addView(toggle, 0, switchParams);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            try {
                prefs.edit().putBoolean(key, checked).apply();
                if ("autoStartOnCharge".equals(key)) {
                    NightScreenApplication.syncChargerReceiver(this);
                }
            } catch (Throwable ignored) {
            }
            if ("autoStartOnCharge".equals(key)) {
                if (checked && Build.VERSION.SDK_INT >= 29
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
                    Toast.makeText(this,
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
            }
        });
        row.setOnClickListener(v -> {
            if (v != toggle) toggle.setChecked(!toggle.isChecked());
        });
    }

    // Use one consistent segmented selector for all discrete settings.
    // Existing values and ranges are intentionally preserved.
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
            option.setPadding(dp(2), 0, dp(2), 0);
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

            LinearLayout.LayoutParams tileParams;
            boolean needsFullLabelWidth =
                    (("screenBrightness".equals(key)
                            || "screenSaverDuration".equals(key)) && i == 0)
                    || ("playerDisplayMode".equals(key) && i == 2);
            if (needsFullLabelWidth) {
                // Reserve only the width needed for the long label; the remaining
                // options split the remaining row width equally.
                int labelWidth = (int) Math.ceil(option.getPaint().measureText(names[i]))
                        + dp(10);
                tileParams = new LinearLayout.LayoutParams(
                        labelWidth, dp(optionTileHeight));
            } else {
                tileParams = new LinearLayout.LayoutParams(
                        0, dp(optionTileHeight), 1f);
            }
            tileParams.leftMargin = dp(2);
            tileParams.rightMargin = dp(2);
            tiles.addView(tile, tileParams);
            if (i < names.length - 1) {
                View segmentGap = new View(this);
                tiles.addView(segmentGap, new LinearLayout.LayoutParams(dp(1), 1));
            }
        }

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
            notices.add("בבדיקה האחרונה לא זוהה מסך נעילה בפועל. לכן ההגדרות הקשורות לנעילה ולהצגת השומר במסך הנעילה עשויות שלא להיות רלוונטיות למכשיר זה.");
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
                notices.add("לא ניתן לזהות את זמן כיבוי המסך שהוגדר במכשיר. בסיום זמן הפעילות השומר ייסגר, אך כיבוי התצוגה נשאר באחריות Android.");
            } else if (timeoutMs >= Integer.MAX_VALUE - 1000L) {
                notices.add("לא זוהה במכשיר כיבוי מסך אוטומטי. בבחירת דקה אחת או חמש דקות השומר ייסגר בתום הזמן, אך המסך עצמו עשוי להישאר דולק.");
            } else if (timeoutMs > requestedMs) {
                notices.add("זמן כיבוי המסך שמוגדר במכשיר ארוך מזמן הפעילות שבחרת. בסיום הזמן השומר ייסגר, אך המסך עשוי להישאר דולק עד ש־Android יכבה אותו.");
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

        LinearLayout.LayoutParams choicesParams =
                new LinearLayout.LayoutParams(-1, -2);
        choicesParams.topMargin = dp(9);
        card.addView(choices, choicesParams);
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
        Toast.makeText(this,
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
        Toast.makeText(this,
                "הצגת הנגן נשארה כבויה מפני שלא ניתנה הרשאת גישה להתראות.",
                Toast.LENGTH_LONG).show();
        if (currentCategory == CATEGORY_PLAYER) showCategory(CATEGORY_PLAYER);
    }

    private void requestChargerOverlayPermission() {
        if (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        Toast.makeText(this,
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
        if (Settings.canDrawOverlays(this)) return;

        prefs.edit().putBoolean("autoStartOnCharge", false).apply();
        if (autoStartOnChargeToggle != null && autoStartOnChargeToggle.isChecked()) {
            autoStartOnChargeToggle.setChecked(false);
        }
        Toast.makeText(this,
                "המתג \"הפעלה אוטומטית בטעינה\" נשאר כבוי מפני שלא ניתנה הרשאת \"הצגה מעל אפליקציות אחרות\".",
                Toast.LENGTH_LONG).show();
    }

    private void requestLockStartOverlayPermission() {
        if (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) return;

        Toast.makeText(this,
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
        Toast.makeText(this,
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
        if ("showBatteryPercent".equals(key)) return "battery-medium";
        if ("screenBackgroundColor".equals(key)) return "palette";
        if ("screenBrightness".equals(key)) return "sun-dim";
        if ("fontColor".equals(key)) return "paint-roller";
        if ("fontWeight".equals(key)) return "bold";
        if ("fontFamily".equals(key)) return "font-family";
        if ("clockDateSize".equals(key)) return "clock-size";
        if ("clockDatePosition".equals(key)) return "position-arrows";
        if ("showPlayerOnlyAtStart".equals(key)) return "circle-play";
        if ("showDetails".equals(key)) return "artist";
        if ("showProgress".equals(key)) return "progress-time-digital";
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
        int visibleWidth = Math.min(dropdownWidth, dp(150));
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
        spinner.setDropDownWidth(dropdownWidth);
        if (Build.VERSION.SDK_INT >= 16) {
            spinner.setPopupBackgroundDrawable(roundOutline(CARD_SURFACE, dp(12), OUTLINE));
        }
        spinner.setSelection(selectedPosition, false);

        FrameLayout spinnerContainer = new FrameLayout(this);
        spinnerContainer.setBackground(roundOutline(CONTROL_SURFACE, dp(12), OUTLINE));
        spinnerContainer.setTag(spinner);
        spinnerContainer.addView(spinner,
                new FrameLayout.LayoutParams(-1, -1));
        View dropdownIcon = makeIconGlyph("chevron", ACCENT);
        FrameLayout.LayoutParams dropdownIconParams =
                new FrameLayout.LayoutParams(dp(18), dp(18),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        dropdownIconParams.leftMargin = dp(8);
        spinnerContainer.addView(dropdownIcon, dropdownIconParams);

        // Place the font selector on the same horizontal row as its title and icon.
        LinearLayout header = (LinearLayout) card.getChildAt(0);
        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(visibleWidth, dp(40));
        spinnerParams.gravity = Gravity.CENTER_VERTICAL;
        spinnerParams.rightMargin = dp(8);
        header.addView(spinnerContainer, Math.max(0, header.getChildCount() - 1),
                spinnerParams);

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
        boolean autoStartEnabled = getBool("autoStartOnCharge", false)
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
            Toast.makeText(this,
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
            Toast.makeText(this, "לא ניתן לפתוח את הגדרות ההתראות במכשיר.",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            Toast.makeText(this, "לא ניתן לפתוח את הגדרות ההתראות במכשיר.",
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