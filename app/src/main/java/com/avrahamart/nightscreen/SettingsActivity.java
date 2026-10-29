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
    private Switch showOnLockScreenToggle;
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
        // LTR child order pins the overflow button to the physical left and keeps
        // the title centered with an equal-width spacer on the right.
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        header.setGravity(Gravity.CENTER_VERTICAL);
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

        FrameLayout permissionsButton = makeCircularIconButton("מעבר למסך הרשאות", "more-vertical");
        FrameLayout.LayoutParams permissionsParams =
                new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        footer.addView(permissionsButton, permissionsParams);
        permissionsButton.setOnClickListener(v ->
                startActivity(new Intent(this, PermissionsActivity.class)));

        FrameLayout backButton = makeCircularIconButton("חזרה למסך הקודם", "arrow-right");
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
        button.setBackground(roundOutline(CARD_SURFACE, dp(21), CARD_OUTLINE));
        GraphicIcon glyph = new GraphicIcon(this, iconType, ACCENT);
        button.addView(glyph,
                new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
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
        addCheck(activityTimes, "הצגה במסך הנעילה", "showOnLockScreen",
                Build.VERSION.SDK_INT >= 26);
        addCheck(activityTimes, "הפעלה אוטומטית בנעילה",
                "autoStartOnDeviceLock", false);

        LinearLayout charging = addCard(content, "שומר מסך בזמן טעינה");
        addCheck(charging, "הפעלה אוטומטית בטעינה", "autoStartOnCharge", false);
        addCheck(charging, "ללא כיבוי בטעינה", "noShutdownWhileCharging", false);
        addCheck(charging, "הצגת אחוז הסוללה", "showBatteryPercent", false);

        LinearLayout font = addCard(content, "גופן");
        addColorChoices(font, "צבע",
                new String[]{"לבן", "כסף", "אפור", "אפור בהיר", "כחול עדין"},
                new String[]{"white", "silver", "gray", "light", "blue"}, "fontColor");
        addSpinner(font, "סוג",
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
            infoButton.setBackground(roundOutline(CONTROL_SURFACE, dp(17), OUTLINE));
            GraphicIcon infoGlyph = new GraphicIcon(this, "help-dialog", ACCENT);
            infoButton.addView(infoGlyph,
                    new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
            LinearLayout.LayoutParams infoParams =
                    new LinearLayout.LayoutParams(dp(34), dp(34));
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
                + "• לפי המכשיר — משך ההצגה נשען על זמן הכיבוי שמוגדר באנדרואיד.\n"
                + "• דקה אחת / חמש דקות — השומר מסך נשאר מוצג למשך הזמן שנבחר באפליקציה. "
                + "האפליקציה מתאמת את שחרור מניעת הכיבוי לזמן המכשיר, כדי למנוע תוספת זמן או כיבוי מוקדם.\n"
                + "• ללא כיבוי — השומר מסך נשאר מוצג ללא מגבלת זמן, עד ליציאה יזומה או שינוי מצב מתאים.\n\n"
                + "הצגה במסך הנעילה\n"
                + "• פעילה — השומר מסך יכול להישאר מעל מסך הנעילה גם אחרי שהמכשיר ננעל אוטומטית. "
                + "האפשרות הזו אינה מפעילה את השומר מסך מעצמה.\n"
                + "• כבויה — השומר מסך אינו אמור להופיע מעל מסך הנעילה. אם גם ההפעלה האוטומטית בנעילה פעילה, "
                + "היא תכובה כדי למנוע מצב סותר.\n\n"
                + "הפעלה אוטומטית בנעילה\n"
                + "• פעילה יחד עם הצגה במסך הנעילה — האפליקציה מנסה לפתוח את השומר מסך כאשר המכשיר ננעל.\n"
                + "• כבויה — נעילת המכשיר לבדה לא מפעילה את השומר מסך; אפשר עדיין להפעיל אותו ידנית.\n\n"
                + "איך ההגדרות פועלות יחד?\n"
                + "• זמן פעילות קובע כמה זמן השומר מסך מוצג; הצגה במסך הנעילה קובעת אם הוא יכול להישאר מעל מסך הנעילה. "
                + "אלה שתי מטרות שונות.\n"
                + "• הצגה במסך הנעילה פעילה, אך הפעלה אוטומטית בנעילה כבויה — השומר מסך יכול להמשיך להופיע אם הופעל קודם, "
                + "אך אינו מתחיל מעצמו בגלל הנעילה.\n"
                + "• שתי האפשרויות פעילות — השומר מסך יכול להתחיל אוטומטית בנעילה וגם להישאר מוצג מעליה.\n"
                + "הרשאות שמשפיעות על ההתנהגות\n"
                + "• הצגה מעל אפליקציות אחרות — נדרשת להפעלה אוטומטית ברקע בחלק מגרסאות Android, "
                + "לרבות Android 10 ומעלה. בלעדיה, ההפעלה האוטומטית עלולה לא לפעול.\n"
                + "• מנהל מכשיר — הרשאה נפרדת שמאפשרת לשומר מסך לבקש נעילה מיידית בסוף זמן הפעילות. "
                + "היא אינה נדרשת להצגת השומר מסך או לנעילה האוטומטית הרגילה של המכשיר. ללא ההרשאה, "
                + "המכשיר ממשיך לפעול לפי מדיניות הנעילה שלו.\n";


        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setTextDirection(View.TEXT_DIRECTION_RTL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(14));
        panel.setBackground(roundOutline(CARD_SURFACE, dp(22), CARD_OUTLINE));
        if (Build.VERSION.SDK_INT >= 21) panel.setElevation(dp(3));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout helpIconShell = new FrameLayout(this);
        helpIconShell.setBackground(roundOutline(CONTROL_SURFACE, dp(21), OUTLINE));
        GraphicIcon helpIcon = new GraphicIcon(this, "help-dialog", ACCENT);
        helpIconShell.addView(helpIcon,
                new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        heading.addView(helpIconShell, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setGravity(Gravity.CENTER_VERTICAL);
        titleBlock.setPadding(dp(11), 0, 0, 0);

        TextView title = textView("הגדרות זמני הפעילות", 20, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView("מדריך קצר להבנת האפשרויות", 13, MUTED);
        subtitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleBlock.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        heading.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        View divider = new View(this);
        divider.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(13);
        dividerParams.bottomMargin = dp(10);
        panel.addView(divider, dividerParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);

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
            int start = styledHelp.length();
            styledHelp.append(line);
            int end = styledHelp.length();
            if (!line.isEmpty() && !line.startsWith("•")) {
                styledHelp.setSpan(new ForegroundColorSpan(ACCENT), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledHelp.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledHelp.setSpan(new RelativeSizeSpan(1.04f), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.startsWith("•")) {
                styledHelp.setSpan(new ForegroundColorSpan(ACCENT), start, start + 1,
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
        scroll.addView(message, new ScrollView.LayoutParams(-1, -2));

        int maxBodyHeight = Math.max(dp(150),
                Math.min(dp(430), getResources().getDisplayMetrics().heightPixels - dp(265)));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, maxBodyHeight));

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
        panel.addView(close, closeParams);
        close.setOnClickListener(v -> dialog.dismiss());

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
        if ("showOnLockScreen".equals(key)) showOnLockScreenToggle = toggle;
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
                if (checked && !getBool("showOnLockScreen", Build.VERSION.SDK_INT >= 26)) {
                    // Lock-triggered activation must be allowed to draw above the keyguard.
                    prefs.edit().putBoolean("showOnLockScreen", true).apply();
                    if (showOnLockScreenToggle != null && !showOnLockScreenToggle.isChecked()) {
                        showOnLockScreenToggle.setChecked(true);
                    }
                    Toast.makeText(this,
                            "כדי להציג את השומר מסך בעת נעילה, הופעלה גם האפשרות \"הצגה במסך הנעילה\".",
                            Toast.LENGTH_LONG).show();
                }
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
                updateOverlayPermissionCardVisibility();
            } else if ("showOnLockScreen".equals(key) && !checked
                    && getBool("autoStartOnDeviceLock", false)) {
                prefs.edit().putBoolean("autoStartOnDeviceLock", false)
                        .putBoolean("autoStartOnLockOverlayPermissionPending", false).apply();
                if (autoStartOnDeviceLockToggle != null && autoStartOnDeviceLockToggle.isChecked()) {
                    autoStartOnDeviceLockToggle.setChecked(false);
                }
                LockScreenMonitorService.sync(this);
                Toast.makeText(this,
                        "ההפעלה האוטומטית בנעילה כובתה מפני שהצגה במסך הנעילה אינה פעילה.",
                        Toast.LENGTH_LONG).show();
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
        FrameLayout icon = makeIconBadge("color-options", dp(30));
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(30), dp(30));
        iconParams.leftMargin = dp(9);
        headingRow.addView(icon, iconParams);
        block.addView(headingRow, new LinearLayout.LayoutParams(-1, dp(32)));

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
            item.setPadding(dp(1), dp(2), dp(1), 0);

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
        if ("שומר מסך בזמן טעינה".equals(title)) return "android-charge";
        if ("גופן".equals(title)) return "type";
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
        if ("screenSaverDuration".equals(key)) return "hourglass";
        if ("showOnLockScreen".equals(key)) return "lockscreen-phone";
        if ("autoStartOnCharge".equals(key)) return "cable";
        if ("autoStartOnDeviceLock".equals(key)) return "lock-play";
        if ("noShutdownWhileCharging".equals(key)) return "battery-hold";
        if ("showBatteryPercent".equals(key)) return "percent";
        if ("fontColor".equals(key)) return "paint-roller";
        if ("fontWeight".equals(key)) return "uppercase-font";
        if ("fontFamily".equals(key)) return "font-family";
        if ("clockDateSize".equals(key)) return "clock-size";
        if ("clockDatePosition".equals(key)) return "position-arrows";
        if ("showDetails".equals(key)) return "artist";
        if ("showProgress".equals(key)) return "progress-time-digital";
        if ("playerSize".equals(key)) return "player-size-resize";
        if ("playerPosition".equals(key)) return "player-position-panel";
        if ("frameEnabled".equals(key)) return "frame-dashed";
        if ("frameColor".equals(key)) return "palette";
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
        GraphicIcon dropdownIcon = new GraphicIcon(this, "chevron", ACCENT);
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
                case "help-dialog":
                    // An elegant help bubble: speech outline with a clear question mark.
                    canvas.drawRoundRect(new RectF(3f, 3.5f, 21f, 18.5f), 4.5f, 4.5f, paint);
                    path.moveTo(7f, 18.2f);
                    path.lineTo(5.5f, 21f);
                    path.lineTo(11f, 18.2f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(9f, 8.2f);
                    path.cubicTo(9.2f, 6.2f, 10.7f, 5.3f, 12.5f, 5.3f);
                    path.cubicTo(14.7f, 5.3f, 16f, 6.5f, 16f, 8.1f);
                    path.cubicTo(16f, 9.5f, 14.9f, 10.4f, 13.3f, 11.3f);
                    path.cubicTo(12.4f, 11.8f, 12f, 12.4f, 12f, 13.3f);
                    canvas.drawPath(path, paint);
                    canvas.drawCircle(12f, 15.8f, 0.75f, paint);
                    break;
                case "lockscreen-phone":
                    // Choice #1: phone outline with one solid lock centered in its screen.
                    canvas.drawRoundRect(new RectF(5.8f, 1.2f, 18.2f, 22.8f), 2.3f, 2.3f, paint);
                    canvas.drawLine(9.6f, 4f, 14.4f, 4f, paint);
                    canvas.drawCircle(12f, 20.2f, 0.55f, paint);
                    canvas.drawArc(new RectF(9.2f, 7.8f, 14.8f, 14f), 180f, 180f, false, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(new RectF(8.2f, 11.1f, 15.8f, 17.2f), 1.2f, 1.2f, paint);
                    paint.setColor(ICON_SURFACE);
                    canvas.drawCircle(12f, 13.2f, 0.7f, paint);
                    canvas.drawRoundRect(new RectF(11.5f, 13.2f, 12.5f, 15.1f), 0.5f, 0.5f, paint);
                    paint.setColor(tint);
                    paint.setStyle(Paint.Style.STROKE);
                    break;
                case "lock-play":
                    // The play triangle is drawn inside the lock body as one combined glyph.
                    canvas.drawArc(new RectF(7f, 2.5f, 17f, 15f), 180f, 180f, false, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(new RectF(3.5f, 10f, 20.5f, 21f), 2.2f, 2.2f, paint);
                    paint.setColor(Color.rgb(20, 36, 56));
                    path.moveTo(10f, 13f);
                    path.lineTo(15f, 15.5f);
                    path.lineTo(10f, 18f);
                    path.close();
                    canvas.drawPath(path, paint);
                    paint.setColor(tint);
                    break;
                case "hourglass":
                    path.moveTo(5f, 3f);
                    path.lineTo(19f, 3f);
                    path.lineTo(19f, 6f);
                    path.lineTo(14f, 11.5f);
                    path.lineTo(19f, 17f);
                    path.lineTo(19f, 21f);
                    path.lineTo(5f, 21f);
                    path.lineTo(5f, 17f);
                    path.lineTo(10f, 11.5f);
                    path.lineTo(5f, 6f);
                    path.close();
                    canvas.drawPath(path, paint);
                    canvas.drawLine(7f, 4.5f, 17f, 4.5f, paint);
                    canvas.drawLine(7f, 19.5f, 17f, 19.5f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    path.reset();
                    path.moveTo(9f, 6f);
                    path.lineTo(15f, 6f);
                    path.lineTo(12f, 9.5f);
                    path.close();
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(8.5f, 18f);
                    path.lineTo(15.5f, 18f);
                    path.lineTo(12f, 13.5f);
                    path.close();
                    canvas.drawPath(path, paint);
                    break;
                case "cable":
                    // Choice #7: a connected cable with connector ends, drawn as one continuous glyph.
                    canvas.drawRoundRect(new RectF(2.8f, 3.2f, 9f, 9.1f), 1.2f, 1.2f, paint);
                    canvas.drawLine(4.2f, 1.5f, 4.2f, 3.2f, paint);
                    canvas.drawLine(7.3f, 1.5f, 7.3f, 3.2f, paint);
                    path.moveTo(9f, 6.2f);
                    path.cubicTo(13f, 6.2f, 13f, 11f, 13f, 13.5f);
                    path.cubicTo(13f, 16f, 16f, 16.5f, 18f, 16.5f);
                    canvas.drawPath(path, paint);
                    canvas.drawRoundRect(new RectF(17.2f, 14.1f, 21.2f, 19f), 1.2f, 1.2f, paint);
                    canvas.drawLine(18.1f, 19f, 18.1f, 21f, paint);
                    canvas.drawLine(20.2f, 19f, 20.2f, 21f, paint);
                    break;
                case "zap":
                    path.moveTo(13.5f, 1.5f);
                    path.lineTo(5.5f, 13f);
                    path.lineTo(10.5f, 13f);
                    path.lineTo(9f, 22.5f);
                    path.lineTo(18.5f, 9.5f);
                    path.lineTo(13.5f, 9.5f);
                    path.close();
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawPath(path, paint);
                    break;
                case "percent":
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.7f);
                    canvas.drawCircle(6.5f, 6.5f, 2.5f, paint);
                    canvas.drawCircle(17.5f, 17.5f, 2.5f, paint);
                    canvas.drawLine(7.5f, 17.5f, 16.5f, 6.5f, paint);
                    break;
                case "paint-roller":
                    canvas.drawRoundRect(new RectF(3f, 4f, 17f, 11f), 1.4f, 1.4f, paint);
                    canvas.drawLine(17f, 7.5f, 20.5f, 7.5f, paint);
                    canvas.drawLine(20.5f, 7.5f, 20.5f, 14f, paint);
                    canvas.drawLine(20.5f, 14f, 13f, 14f, paint);
                    canvas.drawLine(13f, 14f, 13f, 21f, paint);
                    canvas.drawLine(10f, 21f, 16f, 21f, paint);
                    break;
                case "uppercase-font":
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                    paint.setTextSize(19f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("A", 12f, 19f, paint);
                    paint.setTypeface(Typeface.DEFAULT);
                    paint.setTextAlign(Paint.Align.LEFT);
                    break;
                case "position-arrows":
                    canvas.drawLine(12f, 4f, 12f, 20f, paint);
                    path.moveTo(7f, 9f);
                    path.lineTo(12f, 4f);
                    path.lineTo(17f, 9f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(7f, 15f);
                    path.lineTo(12f, 20f);
                    path.lineTo(17f, 15f);
                    canvas.drawPath(path, paint);
                    break;
                case "player-position-panel":
                    // Choice #1: the panel-bottom glyph, with a divider above its bottom band.
                    canvas.drawRoundRect(new RectF(3f, 2.5f, 21f, 21.5f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 15f, 20.5f, 15f, paint);
                    break;
                case "frame-dashed":
                    // Choice #10: a square with distinct dashes on all four sides.
                    float[][] dashSegments = new float[][]{
                            {5f,4f,8f,4f},{11f,4f,14f,4f},{17f,4f,20f,4f},
                            {5f,20f,8f,20f},{11f,20f,14f,20f},{17f,20f,20f,20f},
                            {4f,5f,4f,8f},{4f,11f,4f,14f},{4f,17f,4f,20f},
                            {21f,5f,21f,8f},{21f,11f,21f,14f},{21f,17f,21f,20f}
                    };
                    for (float[] dash : dashSegments) {
                        canvas.drawLine(dash[0], dash[1], dash[2], dash[3], paint);
                    }
                    break;
                case "player-size-resize":
                    // Choice #1: musical-note player tile with four outward resize arrows.
                    canvas.drawRoundRect(new RectF(5f, 6f, 19f, 18f), 2f, 2f, paint);
                    paint.setStrokeWidth(1.45f);
                    path.moveTo(11.5f, 8.2f);
                    path.lineTo(16f, 7.3f);
                    path.lineTo(16f, 12.1f);
                    canvas.drawPath(path, paint);
                    canvas.drawLine(11.5f, 8.2f, 11.5f, 13.3f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawOval(new RectF(8.4f, 12.1f, 12.1f, 14.9f), paint);
                    canvas.drawOval(new RectF(13.1f, 11.5f, 16.8f, 14.3f), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1.5f);
                    path.reset();
                    path.moveTo(3f, 8f);
                    path.lineTo(3f, 3f);
                    path.lineTo(8f, 3f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(16f, 3f);
                    path.lineTo(21f, 3f);
                    path.lineTo(21f, 8f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(3f, 16f);
                    path.lineTo(3f, 21f);
                    path.lineTo(8f, 21f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(16f, 21f);
                    path.lineTo(21f, 21f);
                    path.lineTo(21f, 16f);
                    canvas.drawPath(path, paint);
                    break;
                case "more-vertical":
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(12f, 5f, 1.8f, paint);
                    canvas.drawCircle(12f, 12f, 1.8f, paint);
                    canvas.drawCircle(12f, 19f, 1.8f, paint);
                    break;
                case "arrow-right":
                    // Centered right-pointing back/navigation arrow for the RTL layout.
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.2f);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    canvas.drawLine(4.5f, 12f, 19.5f, 12f, paint);
                    path.reset();
                    path.moveTo(13f, 5.5f);
                    path.lineTo(19.5f, 12f);
                    path.lineTo(13f, 18.5f);
                    canvas.drawPath(path, paint);
                    break;
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
                case "music-player":
                    // A compact music-player tile: album/note, track lines, and progress.
                    canvas.drawRoundRect(new RectF(2.8f, 3.6f, 21.2f, 20.4f), 2.8f, 2.8f, paint);
                    canvas.drawRoundRect(new RectF(4.8f, 6.1f, 11.2f, 13.5f), 1.1f, 1.1f, paint);
                    paint.setStrokeWidth(1.45f);
                    canvas.drawLine(8.5f, 7.7f, 8.5f, 10.8f, paint);
                    path.moveTo(8.5f, 7.7f);
                    path.lineTo(10f, 7.3f);
                    path.lineTo(10f, 10.2f);
                    canvas.drawPath(path, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawOval(new RectF(6.9f, 10.1f, 8.7f, 11.5f), paint);
                    canvas.drawOval(new RectF(8.9f, 9.6f, 10.5f, 11f), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(13.1f, 7.5f, 18.9f, 7.5f, paint);
                    canvas.drawLine(13.1f, 10.6f, 17.7f, 10.6f, paint);
                    canvas.drawLine(5f, 16.5f, 19f, 16.5f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(10.2f, 16.5f, 1.15f, paint);
                    break;
                case "music":
                    // Retained for any legacy references to the music-note glyph.
                    paint.setStrokeWidth(1.8f);
                    path.moveTo(13.5f, 5f);
                    path.lineTo(18f, 4f);
                    path.lineTo(18f, 15.5f);
                    canvas.drawPath(path, paint);
                    canvas.drawLine(13.5f, 5f, 13.5f, 16f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawOval(new RectF(8.2f, 14.8f, 13.5f, 19.2f), paint);
                    canvas.drawOval(new RectF(12.8f, 14f, 18.1f, 18.4f), paint);
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
                case "lock":
                    canvas.drawRoundRect(new RectF(4.5f, 10f, 19.5f, 21f), 2f, 2f, paint);
                    canvas.drawArc(new RectF(7f, 3f, 17f, 15f), 180f, 180f, false, paint);
                    canvas.drawCircle(12f, 15f, 1.2f, paint);
                    canvas.drawLine(12f, 16f, 12f, 18f, paint);
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
                    // Choice #1: a calendar with the Hebrew day marker א׳.
                    canvas.drawRoundRect(new RectF(3f, 4f, 21f, 20f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 9f, 20.5f, 9f, paint);
                    canvas.drawLine(8f, 2.5f, 8f, 7f, paint);
                    canvas.drawLine(16f, 2.5f, 16f, 7f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(7.5f);
                    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("א׳", 12f, 17f, paint);
                    paint.setTypeface(Typeface.DEFAULT);
                    paint.setTextAlign(Paint.Align.LEFT);
                    break;
                case "gregorian-date":
                    // Choice #2: the calendar-days glyph, using a grid rather than the old "31".
                    canvas.drawRoundRect(new RectF(3f, 4f, 21f, 20f), 2f, 2f, paint);
                    canvas.drawLine(3.5f, 9f, 20.5f, 9f, paint);
                    canvas.drawLine(8f, 2.5f, 8f, 7f, paint);
                    canvas.drawLine(16f, 2.5f, 16f, 7f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    float[] dayXs = new float[]{7f, 12f, 17f};
                    float[] dayYs = new float[]{12f, 15.5f, 18.5f};
                    for (float dayY : dayYs) {
                        for (float dayX : dayXs) {
                            canvas.drawCircle(dayX, dayY, 0.85f, paint);
                        }
                    }
                    paint.setStyle(Paint.Style.STROKE);
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
                case "android-charge":
                    // One outer phone frame; the charging bolt sits directly inside it.
                    canvas.drawRoundRect(new RectF(5.8f, 1.5f, 18.2f, 22.5f), 2.2f, 2.2f, paint);
                    canvas.drawLine(10f, 4f, 14f, 4f, paint);
                    path.moveTo(13.2f, 7f);
                    path.lineTo(9.5f, 12.2f);
                    path.lineTo(11.8f, 12.2f);
                    path.lineTo(10.8f, 17f);
                    path.lineTo(14.8f, 10.8f);
                    path.lineTo(12.5f, 10.8f);
                    path.close();
                    canvas.drawPath(path, paint);
                    canvas.drawCircle(12f, 20.4f, 0.65f, paint);
                    break;
                case "android-lit":
                    // One outer phone frame; a brightness symbol sits directly inside it.
                    canvas.drawRoundRect(new RectF(5.8f, 1.5f, 18.2f, 22.5f), 2.2f, 2.2f, paint);
                    canvas.drawLine(10f, 4f, 14f, 4f, paint);
                    canvas.drawCircle(12f, 12f, 2.7f, paint);
                    canvas.drawLine(12f, 7.8f, 12f, 6.8f, paint);
                    canvas.drawLine(12f, 17.2f, 12f, 16.2f, paint);
                    canvas.drawLine(7.8f, 12f, 8.8f, 12f, paint);
                    canvas.drawLine(15.2f, 12f, 16.2f, 12f, paint);
                    canvas.drawLine(9f, 9f, 8.3f, 8.3f, paint);
                    canvas.drawLine(15f, 9f, 15.7f, 8.3f, paint);
                    canvas.drawLine(9f, 15f, 8.3f, 15.7f, paint);
                    canvas.drawLine(15f, 15f, 15.7f, 15.7f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(12f, 12f, 1f, paint);
                    canvas.drawCircle(12f, 20.4f, 0.65f, paint);
                    break;
                case "auto-start":
                    canvas.drawCircle(11f, 12f, 8f, paint);
                    canvas.drawLine(11f, 6f, 11f, 12f, paint);
                    canvas.drawLine(11f, 12f, 15f, 14.5f, paint);
                    path.moveTo(17f, 5f);
                    path.lineTo(21f, 5f);
                    path.lineTo(21f, 9f);
                    canvas.drawPath(path, paint);
                    path.moveTo(21f, 5f);
                    path.lineTo(17f, 9f);
                    canvas.drawPath(path, paint);
                    break;
                case "battery-hold":
                    canvas.drawRoundRect(new RectF(2.5f, 6f, 20f, 18f), 2f, 2f, paint);
                    canvas.drawRoundRect(new RectF(20.5f, 9.5f, 22.5f, 14.5f), 0.8f, 0.8f, paint);
                    canvas.drawCircle(11f, 12f, 3.5f, paint);
                    canvas.drawLine(11f, 9.5f, 11f, 12f, paint);
                    canvas.drawLine(11f, 12f, 13f, 13.2f, paint);
                    break;
                case "battery-percent":
                    canvas.drawRoundRect(new RectF(2.5f, 6f, 20f, 18f), 2f, 2f, paint);
                    canvas.drawRoundRect(new RectF(20.5f, 9.5f, 22.5f, 14.5f), 0.8f, 0.8f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(8.5f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("%", 11f, 14.7f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);
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
                    // Choice #1: clock face and outward diagonal resize arrows in a single glyph.
                    canvas.drawCircle(12f, 12f, 6.5f, paint);
                    canvas.drawLine(12f, 7.5f, 12f, 12f, paint);
                    canvas.drawLine(12f, 12f, 15.5f, 14f, paint);
                    path.moveTo(3f, 9f);
                    path.lineTo(3f, 3f);
                    path.lineTo(9f, 3f);
                    path.moveTo(3f, 3f);
                    path.lineTo(7f, 7f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(15f, 3f);
                    path.lineTo(21f, 3f);
                    path.lineTo(21f, 9f);
                    path.moveTo(21f, 3f);
                    path.lineTo(17f, 7f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(3f, 15f);
                    path.lineTo(3f, 21f);
                    path.lineTo(9f, 21f);
                    path.moveTo(3f, 21f);
                    path.lineTo(7f, 17f);
                    canvas.drawPath(path, paint);
                    path.reset();
                    path.moveTo(15f, 21f);
                    path.lineTo(21f, 21f);
                    path.lineTo(21f, 15f);
                    path.moveTo(21f, 21f);
                    path.lineTo(17f, 17f);
                    canvas.drawPath(path, paint);
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
                case "progress-time-digital":
                    // One icon: a digital time readout with the playback progress line integrated below.
                    canvas.drawRoundRect(new RectF(2.5f, 2.5f, 21.5f, 21.5f), 2.2f, 2.2f, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(6.5f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                    canvas.drawText("1:23", 12f, 12.1f, paint);
                    paint.setTypeface(Typeface.DEFAULT);
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1.15f);
                    canvas.drawLine(5f, 16.9f, 19f, 16.9f, paint);
                    paint.setStrokeWidth(2.25f);
                    canvas.drawLine(5f, 16.9f, 12f, 16.9f, paint);
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
