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
    private LinearLayout overlayPermissionCard;
    private TextView overlayPermissionStatus;
    private Button overlayPermissionButton;
    private Switch showPlayerToggle;
    private Switch autoStartOnChargeToggle;
    private Switch autoStartOnDeviceLockToggle;
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
        root.setPadding(dp(14), dp(2), dp(14), dp(8));
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                view.setPadding(dp(14), dp(2) + topInset, dp(14), dp(8) + bottomInset);
                return insets;
            });
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        // LTR child order pins the overflow button to the physical left and keeps
        // the title centered with an equal-width spacer on the right.
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(2), dp(5), dp(2), dp(8));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titleBlock.setGravity(Gravity.CENTER);
        titleBlock.setPadding(0, 0, 0, 0);

        TextView title = textView("הגדרות", 28, ON_SURFACE);
        title.setIncludeFontPadding(false);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView("התאמה אישית של השומר מסך", 18, MUTED);
        subtitle.setIncludeFontPadding(false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        subtitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titleBlock.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        // Keep the title and subtitle centered and unobstructed by utility buttons.
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

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

        FrameLayout footer = new FrameLayout(this);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, dp(50));
        footerParams.topMargin = dp(4);
        root.addView(footer, footerParams);

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
        FrameLayout.LayoutParams doneParams =
                new FrameLayout.LayoutParams(-2, dp(50), Gravity.CENTER);
        footer.addView(done, doneParams);
        done.setOnClickListener(v -> finish());

        FrameLayout permissionsButton = makeCircularIconButton("מעבר למסך הרשאות", "shield-alert");
        FrameLayout.LayoutParams permissionsParams =
                new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        footer.addView(permissionsButton, permissionsParams);
        permissionsButton.setOnClickListener(v ->
                startActivity(new Intent(this, PermissionsActivity.class)));

        FrameLayout backButton = makeCircularIconButton("חזרה למסך הקודם", "back-arrow");
        FrameLayout.LayoutParams backParams =
                new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        footer.addView(backButton, backParams);
        backButton.setOnClickListener(v -> finish());

        setContentView(root);
    }

    private FrameLayout makeCircularIconButton(String description, String iconType) {
        FrameLayout button = new FrameLayout(this);
        button.setContentDescription(description);
        button.setClickable(true);
        button.setFocusable(true);
        button.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        button.setBackground(pillBackground(ACCENT, dp(22)));
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(2));
        View glyph = makeIconGlyph(iconType, Color.rgb(7, 31, 49));
        button.addView(glyph,
                new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        return button;
    }

@Override
    protected void onResume() {
        super.onResume();
        if (content != null) {
            handlePlayerPermissionResult();
            handleChargerPermissionResult();
            handleLockStartPermissionResult();
            LockScreenMonitorService.sync(this);
            NightScreenApplication.syncChargerReceiver(this);
        }
    }

    private void buildSettingsCards() {
        LinearLayout display = addCard(content, "תצוגה במסך");
        addCheck(display, "תאריך עברי", "showHebrew", true);
        addCheck(display, "תאריך לועזי", "showGregorian", true);
        addCheck(display, "הצגת הנגן", "showPlayer", false);
        LinearLayout activityTimes = addCard(content, "הגדרת זמני הפעילות");
        addDiscreteSlider(activityTimes, "זמן פעילות",
                new String[]{"לפי המכשיר", "דקה אחת", "חמש דקות", "ללא כיבוי"},
                new String[]{"device", "1", "5", "0"}, "screenSaverDuration");
        addCheck(activityTimes, "מניעת נעילה בזמן פעילות",
                "preventLockWhileActive", false);
        addCheck(activityTimes, "הפעלה אוטומטית בנעילה",
                "autoStartOnDeviceLock", false);

        LinearLayout charging = addCard(content, "שומר מסך בזמן טעינה");
        addCheck(charging, "הפעלה אוטומטית בטעינה", "autoStartOnCharge", false);
        addCheck(charging, "ללא כיבוי בטעינה", "noShutdownWhileCharging", false);
        addCheck(charging, "הצגת אחוז הסוללה", "showBatteryPercent", false);

        LinearLayout design = addCard(content, "עיצוב");
        addColorChoices(design, "צבע המסך",
                new String[]{"שחור", "כחול", "כחול כהה", "טורקיז",
                        "אפור פחם", "ירוק יער", "כחול עמוק", "אפור אבן"},
                new String[]{"#000000", "#142438", "#091321", "#12313A",
                        "#252B33", "#20362E", "#193740", "#303943"},
                "screenBackgroundColor");
        addDiscreteSlider(design, "בהירות המסך",
                new String[]{"לפי המכשיר", "25%", "50%", "75%", "100%"},
                new String[]{"-1", "25", "50", "75", "100"}, "screenBrightness");
        addSpinner(design, "סוג הגופן",
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
        addColorChoices(design, "צבע הגופן",
                new String[]{"לבן", "כסף", "אפור", "אפור בהיר", "כחול עדין"},
                new String[]{"white", "silver", "gray", "light", "blue"}, "fontColor");

        clockDateCard = addCard(content, "תצוגת שעון ותאריך");
        addDiscreteSlider(clockDateCard, "גודל",
                new String[]{"קטן", "בינוני", "רגיל", "גדול", "גדול מאוד"},
                new String[]{"60", "72", "84", "96", "108"}, "clockDateSize");
        addDiscreteSlider(clockDateCard, "עובי הגופן",
                new String[]{"דק", "רגיל", "בינוני", "מודגש"},
                new String[]{"-1", "0", "1", "2"}, "fontWeight");
        addDiscreteSlider(clockDateCard, "מיקום",
                new String[]{"למעלה", "25% מלמעלה", "באמצע"},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");

        LinearLayout player = addCard(content, "תצוגת הנגן");
        addCheck(player, "הצגה רק בתחילת שיר", "showPlayerOnlyAtStart", false);
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

        // Permission cards are presented on the dedicated PermissionsActivity.

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
        heading.setPadding(0, 0, dp(3), 0);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams headingTextParams =
                new LinearLayout.LayoutParams(0, dp(42), 1f);
        headingRow.addView(heading, headingTextParams);

        // Only the activity-time card gets a compact information control at its
        // heading height; no other settings card changes.
        if ("הגדרת זמני הפעילות".equals(title)) {
            FrameLayout infoButton = new FrameLayout(this);
            infoButton.setContentDescription("הסבר על הגדרות זמני הפעילות");
            infoButton.setClickable(true);
            infoButton.setFocusable(true);
            infoButton.setBackgroundColor(Color.TRANSPARENT);
            View infoGlyph = makeIconGlyph("message-circle-question", ACCENT);
            infoButton.addView(infoGlyph,
                    new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
            LinearLayout.LayoutParams infoParams =
                    new LinearLayout.LayoutParams(dp(38), dp(38));
            infoParams.leftMargin = dp(3);
            headingRow.addView(infoButton, 0, infoParams);
            infoButton.setOnClickListener(v -> showActivityTimeHelpDialog());
        }

        card.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(42)));

        View separator = new View(this);
        separator.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.setMargins(dp(2), dp(5), dp(2), dp(10));
        card.addView(separator, separatorParams);
        return card;
    }

    private void showActivityTimeHelpDialog() {
        String help =
                "זמן פעילות\n"
                + "ההגדרה קובעת את משך הזמן שהשומר מסך יהיה מוצג על המסך.\n"
                + "• ניתן להגדיר אם משך זמן התצוגה יהיה לפי זמן המסך שמוגדר במכשיר, או לפי משך זמן שונה כפי שמוגדר באפליקציה.\n"
                + "• בבחירת האפשרות ללא כיבוי - השומר מסך נשאר מוצג ללא מגבלת זמן, עד ליציאה יזומה או עד לשינוי המצב.\n\n"
                + "מניעת נעילה בזמן פעילות\n"
                + "• כשההגדרה פעילה - המסך לא יינעל כל עוד השומר מסך פעיל.\n"
                + "• כשההגדרה כבויה - המכשיר רשאי להפעיל את הנעילה גם כשהשומר מסך עדיין מוצג.\n\n"
                + "הפעלה אוטומטית בנעילה\n"
                + "• ההגדרה מפעילה את השומר מסך אוטומטית כאשר המכשיר ננעל, גם בנעילה יזומה וגם בנעילה אוטומטית.\n"
                + "• כשההגדרה כבויה - נעילת המכשיר לבדה לא מפעילה את השומר מסך.\n"

                + "• לצורך ההפעלה האוטומטית בנעילה, האפליקציה מציגה התראה קבועה בזמן שהאפשרות פעילה. ההתראה שקטה ואינה מופיעה במסך הנעילה.\n\n"
                + "הרשאות שמשפיעות על ההתנהגות האפליקציה\n"
                + "• הצגה מעל אפליקציות אחרות - נדרשת להפעלה אוטומטית ברקע בחלק מגרסאות Android, לרבות Android 10 ומעלה. בלעדיה, ההפעלה האוטומטית עלולה לא לפעול.\n"
                + "• הרשאת מנהל מכשיר - הרשאה נפרדת שניתן להפעיל ממסך ההרשאות, ההרשאה אינה נדרשת לצורך הצגת השומר מסך עצמו.\n"
                + "היא נועדה רק כדי לאפשר לאפליקציה לכבות את המסך ולנעול את המכשיר בעצמה בכל מצב בסיום זמן הפעילות שמוגדר באפליקציה.\n"
                + "ללא ההרשאה, האפליקציה אינה נועלת את המכשיר, אלא רק מתאמת את כיבוי המסך ונעילת המסך לזמן המוגדר במכשיר, ולכן נעילה מיידית תתאפשר רק כשזמן הפעילות שמוגדר באפליקציה זהה לזמן המוגדר במכשיר או יותר ממנו, אבל כאשר זמן פעילות המוגדר קצר מהזמן המוגדר במכשיר לא תתבצע נעילה מיידית בסיום הפעילות.";

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
                new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        heading.addView(helpIconShell, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setGravity(Gravity.CENTER_VERTICAL);
        // Extra spacing between the icon and title block.
        titleBlock.setPadding(dp(18), 0, 0, 0);

        TextView title = textView("הגדרת זמני הפעילות", 20, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        title.setIncludeFontPadding(false);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView("הסבר קצר להבנת האפשרויות", 13, MUTED);
        subtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        subtitle.setIncludeFontPadding(false);
        subtitle.setPadding(0, 0, 0, 0);
        titleBlock.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams titleBlockParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        // Add spacing at the physical side next to the icon (the heading is RTL).
        titleBlockParams.rightMargin = dp(12);
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
        message.setLineSpacing(dp(4), 1.0f);
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
                    || line.equals("הפעלה אוטומטית בנעילה")
                    || line.equals("הרשאות שמשפיעות על ההתנהגות האפליקציה");

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

    private void addCheck(LinearLayout card, String label, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // LTR child order places the switch at the left and the Hebrew label at the right.
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(6), dp(2), dp(6), dp(2));

        TextView text = textView(label, 16, ON_SURFACE);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        text.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.addView(text, new LinearLayout.LayoutParams(0, dp(38), 1f));

        FrameLayout settingIcon = makeIconBadge(iconForSetting(key), dp(30));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        iconParams.leftMargin = dp(9);
        row.addView(settingIcon, iconParams);

        Switch toggle = new Switch(this);
        toggle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        toggle.setShowText(false);
        toggle.setContentDescription(label);
        if ("showPlayer".equals(key)) showPlayerToggle = toggle;
        if ("autoStartOnCharge".equals(key)) autoStartOnChargeToggle = toggle;
        if ("autoStartOnDeviceLock".equals(key)) autoStartOnDeviceLockToggle = toggle;
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
        switchParams.leftMargin = dp(7);
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
                // Device-lock activation is independent from the active-session lock policy.
                if (checked && Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this)) {
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
            } else if ("showPlayer".equals(key)) {
                if (checked && !hasNotificationAccess()) {
                    prefs.edit().putBoolean("showPlayerPermissionPending", true).apply();
                    requestPlayerNotificationAccessIfNeeded();
                } else {
                    prefs.edit().putBoolean("showPlayerPermissionPending", false).apply();
                }
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
        block.setPadding(dp(6), dp(2), dp(6), dp(3));

        final int initial = selectedIndex(key, values);
        TextView chosenValue = textView(names[initial], 16, ACCENT);
        chosenValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        chosenValue.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        chosenValue.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        chosenValue.setSingleLine(false);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(-2, dp(30));
        // Track starts after 8dp outer margin and 12dp SeekBar inner padding.
        valueParams.leftMargin = dp(20);
        valueParams.rightMargin = dp(8);
        headingRow.addView(chosenValue, valueParams);

        TextView settingLabel = textView(label, 16, ON_SURFACE);
        settingLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        settingLabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        settingLabel.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        settingLabel.setTextDirection(View.TEXT_DIRECTION_RTL);
        settingLabel.setPadding(0, 0, dp(3), 0);
        headingRow.addView(settingLabel, new LinearLayout.LayoutParams(0, dp(30), 1f));

        FrameLayout settingIcon = makeIconBadge(iconForSetting(key), dp(30));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        iconParams.leftMargin = dp(9);
        headingRow.addView(settingIcon, iconParams);

        block.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(32)));

        SeekBar slider = new SeekBar(this);
        slider.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        slider.setMax(Math.max(0, names.length - 1));
        slider.setProgress(initial);
        slider.setContentDescription(label);
        slider.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        slider.setPadding(dp(12), 0, dp(12), 0);
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
        block.setPadding(dp(6), dp(2), dp(6), dp(5));

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = textView(label, 16, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        title.setPadding(0, 0, dp(3), 0);
        headingRow.addView(title, new LinearLayout.LayoutParams(0, dp(32), 1f));
        FrameLayout icon = makeIconBadge(iconForSetting(key), dp(30));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        iconParams.leftMargin = dp(9);
        headingRow.addView(icon, iconParams);
        block.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        choices.setGravity(Gravity.CENTER);
        int initial = selectedIndex(key, values);
        // Migrate the previously offered brown background to graphite so existing
        // installations retain a valid selected swatch.
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
                if (i > 0) choiceRowParams.topMargin = dp(4);
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
            item.addView(swatch, new LinearLayout.LayoutParams(dp(34), dp(34)));

            TextView caption = textView(names[i], 12,
                    i == selectedInitially ? ACCENT : MUTED);
            caption.setGravity(Gravity.CENTER);
            caption.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            caption.setSingleLine(true);
            caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
            choiceRow.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        block.addView(choices, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams blockParams =
                new LinearLayout.LayoutParams(-1, -2);
        blockParams.bottomMargin = dp(4);
        card.addView(block, blockParams);
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
                    .apply();
            if (showPlayerToggle != null) showPlayerToggle.setChecked(false);
        }
    }

    private void handlePlayerPermissionResult() {
        if (!prefs.getBoolean("showPlayerPermissionPending", false)) return;

        prefs.edit().putBoolean("showPlayerPermissionPending", false).apply();
        if (hasNotificationAccess()) return;

        prefs.edit().putBoolean("showPlayer", false).apply();
        if (showPlayerToggle != null && showPlayerToggle.isChecked()) {
            showPlayerToggle.setChecked(false);
        }
        Toast.makeText(this,
                "הצגת הנגן נשארה כבויה מפני שלא ניתנה הרשאת גישה להתראות.",
                Toast.LENGTH_LONG).show();
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
            if ("fontColor".equals(key) || "frameColor".equals(key)
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
        if ("showHebrew".equals(key)) return "hebrew-date";
        if ("showGregorian".equals(key)) return "gregorian-date";
        if ("showPlayer".equals(key)) return "media-toggle";
        if ("screenSaverDuration".equals(key)) return "activity-history";
        if ("preventLockWhileActive".equals(key)) return "power-off";
        if ("autoStartOnCharge".equals(key)) return "usb";
        if ("autoStartOnDeviceLock".equals(key)) return "lock-play";
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
        if (!"hebrew-date".equals(iconName) && !"lock-play".equals(iconName)
                && !"close-circle".equals(iconName)) {
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
        View graphic = makeIconGlyph(iconName, Color.rgb(168, 200, 232));
        int glyphSize = "progress-time-digital".equals(iconName) ? dp(27)
                : "bold".equals(iconName) ? dp(24)
                : "phone-charging".equals(iconName) ? dp(24)
                : "power-off".equals(iconName) ? dp(19)
                : ("history".equals(iconName) || "panel-bottom".equals(iconName)) ? dp(24)
                : "position-arrows".equals(iconName) ? dp(27)
                : "gregorian-date".equals(iconName) ? dp(25)
                : dp(21);
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

    private void addSpinner(LinearLayout card, String label, String[] names,
                            String[] values, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(6), dp(2), dp(6), dp(4));

        TextView labelView = textView(label, 16, ON_SURFACE);
        labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        labelView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        labelView.setTextDirection(View.TEXT_DIRECTION_RTL);
        labelView.setPadding(0, 0, dp(3), 0);

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
        View dropdownIcon = makeIconGlyph("chevron", ACCENT);
        FrameLayout.LayoutParams dropdownIconParams =
                new FrameLayout.LayoutParams(dp(18), dp(18),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        dropdownIconParams.leftMargin = dp(8);
        spinnerContainer.addView(dropdownIcon, dropdownIconParams);
        spinnerContainer.setOnClickListener(v -> spinner.performClick());

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(spinnerWidth, dp(40));
        spinnerParams.leftMargin = dp(9);
        row.addView(spinnerContainer, spinnerParams);
        row.addView(labelView, new LinearLayout.LayoutParams(0, dp(40), 1f));
        FrameLayout settingIcon = makeIconBadge(iconForSetting(key), dp(30));
        LinearLayout.LayoutParams settingIconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        settingIconParams.leftMargin = dp(9);
        row.addView(settingIcon, settingIconParams);
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
                || getBool("autoStartOnDeviceLock", false);
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
            float radius = Math.min(getWidth(), getHeight()) / 2f - dp(selected ? 2 : 3);

            if (selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(ACCENT);
                canvas.drawCircle(cx, cy, radius, paint);
                radius -= dp(3);
            }

            int swatchColor = colorValue(value);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(swatchColor);
            canvas.drawCircle(cx, cy, radius, paint);

            // Only unselected screen-background swatches need the subtle outline.
            // The selected swatch already has its accent selection ring.
            if (screenBackgroundPalette && !selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(CARD_OUTLINE);
                canvas.drawCircle(cx, cy, radius - dp(1), paint);
            }

            if (selected) {
                // Use a contrasting checkmark: dark ink on light font-color
                // swatches, and white on the dark screen-background swatches.
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
