package com.avrahamart.nightscreen;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class GuideActivity extends Activity {
    private static final int SURFACE = Color.rgb(9, 19, 33);
    private static final int CARD_SURFACE = Color.rgb(20, 36, 56);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int OUTLINE = Color.rgb(42, 67, 95);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SURFACE);
        window.setNavigationBarColor(SURFACE);
        window.getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(14), dp(6), dp(14), dp(8));
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(dp(14), dp(6) + insets.getSystemWindowInsetTop(),
                        dp(14), dp(8) + insets.getSystemWindowInsetBottom());
                return insets;
            });
        }

        FrameLayout header = new FrameLayout(this);
        TextView title = textView("מדריך שימוש", 25, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        header.addView(title, new FrameLayout.LayoutParams(-1, dp(44), Gravity.CENTER));

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView subtitle = textView("הסברים וטיפים לשימוש באפליקציה", 14, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.bottomMargin = dp(9);
        root.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        sections.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sections.setPadding(0, dp(2), 0, dp(8));

        addSection(sections, "1. היכרות עם האפליקציה",
                "שומר מסך מציג שעון גדול, תאריך עברי ותאריך לועזי, ובאפשרותך להוסיף גם נגן מדיה ואחוז סוללה.\n\n" +
                "ההגדרות מחולקות לארבעה מסכים עיקריים: שעון ותאריך; הפעלה והתנהגות; מראה ועיצוב; נגן מדיה. " +
                "בכל מסך כל הגדרה מוצגת בחלונית נפרדת. בנוסף קיימים מסך ניהול הרשאות ומדריך שימוש. כל שינוי נשמר בהגדרות האפליקציה.");

        addSection(sections, "2. שימוש במסך הראשי",
                "סמל ההגדרות אינו מוצג כל הזמן. הקש באזור העליון של המסך כדי להציג את סמל ההגדרות למשך כשלוש שניות, " +
                "ולאחר מכן הקש על סמל גלגל השיניים שמופיע בפינה העליונה.\n\n" +
                "אפשר לפתוח את ההגדרות גם באמצעות מקש התפריט הפיזי במכשירים שתומכים בו. כדי לצאת מהשומר, " +
                "השתמש בלחצן החזרה של המכשיר; נדרשות שתי לחיצות רצופות.");

        addSection(sections, "3. זמן פעילות ונעילת המכשיר",
                "במסך „הפעלה והתנהגות” נמצאת בחירת זמן הפעילות. לכל אפשרות משמעות שונה:\n\n" +
                "• לפי המכשיר — Android מנהל את זמן כיבוי המסך. השומר נשאר מוצג כל עוד המערכת משאירה את המסך פעיל. " +
                "אם המכשיר אינו מכבה את המסך אוטומטית, השומר ימשיך להיות מוצג.\n\n" +
                "• דקה אחת / חמש דקות — השומר מסיים את הפעילות בתום הזמן שנבחר. אם זמן כיבוי המסך של המכשיר ארוך יותר, " +
                "השומר עשוי להיסגר לפני שהמסך עצמו ייכבה. המסך נשאר בשליטת Android.\n\n" +
                "• ללא כיבוי — השומר נשאר מוצג ומבקש להשאיר את המסך פעיל ללא הגבלת זמן.\n\n" +
                "שלוש ההגדרות הנוספות אינן זהות זו לזו:\n" +
                "• מניעת נעילה בזמן פעילות — קובעת אם מסך הנעילה של Android יכול להופיע מעל השומר בזמן שהוא פעיל.\n" +
                "• המשך הצגה במסך הנעילה — מאפשרת לשומר להמשיך ולהופיע לאחר כיבוי המסך והפעלתו מחדש, בהתאם להתנהגות המכשיר ולתנאי זמן הפעילות.\n" +
                "• הפעלה אוטומטית בנעילה — מפעילה את השומר בעקבות נעילת המכשיר גם כאשר הוא לא היה פעיל קודם.");

        addSection(sections, "4. הפעלה אוטומטית",
                "הפעלה אוטומטית בנעילה והפעלה אוטומטית בטעינה הן שתי אפשרויות נפרדות.\n\n" +
                "• הפעלה אוטומטית בנעילה — כאשר האפשרות פעילה, האפליקציה עוקבת אחר אירועי נעילת המכשיר ומנסה להציג את השומר מעל מסך הנעילה. " +
                "בגרסאות Android שבהן נדרשת הרשאת הצגה מעל אפליקציות אחרות, יש לאפשר אותה במסך ההרשאות.\n\n" +
                "• הפעלה אוטומטית בטעינה — מפעילה את השומר בעת חיבור המטען. גם כאן, בגרסאות Android שבהן נדרשת הרשאת הצגה מעל אפליקציות אחרות, " +
                "יש להעניק אותה.\n\n" +
                "הפעלה אוטומטית בנעילה אינה תחליף ל„המשך הצגה במסך הנעילה”: הראשונה מפעילה את השומר בעקבות נעילה, " +
                "והשנייה עוסקת בהמשך הצגתו לאחר מחזור כיבוי והפעלה של המסך.");

        addSection(sections, "5. שעון ותאריך",
                "במסך „שעון ותאריך” אפשר לבחור בין שלושה מצבים: עברי ולועזי, רק עברי או רק לועזי.\n\n" +
                "• גודל — בחירה בין קטן, בינוני, רגיל, גדול וענק.\n" +
                "• עובי הגופן — בחירה בין דק, רגיל, מודגש ועבה.\n" +
                "• מיקום — בראש המסך, ברבע העליון או במרכז המסך.\n\n" +
                "הגודל והמשקל משפיעים על תצוגת השעון והתאריכים. בחלק מהגופנים קיימות התאמות תצוגה מיוחדות כדי לשמור על קריאות ואחידות.");

        addSection(sections, "6. נגן מדיה",
                "במסך „נגן מדיה” אפשר להציג את נגן המדיה של Android כאשר קיימת הפעלת מדיה זמינה.\n\n" +
                "• הצגת הנגן — מפעילה או מסתירה את הנגן.\n" +
                "• הצגה רק בתחילת שיר — מציגה את הנגן למשך חלון הזמן הראשוני של השיר.\n" +
                "• פרטי אמן — מציגה את שם האמן כאשר הוא נמסר על ידי נגן המדיה.\n" +
                "• זמני השיר ופקדי הנגן — מציגה את זמני השיר ואת סרגל ההתקדמות לצד כפתורי הניגון.\n" +
                "• גודל הנגן ומיקום הנגן — קובעים את גודל הכרטיס ואת מיקומו ברבע התחתון או בתחתית המסך.\n\n" +
                "אפשר גם להציג מסגרת, לבחור את צבעה, את עובייה ואת מידת עיגול הפינות. האפשרויות האלה נמצאות תחת „מסגרת לנגן”.");

        addSection(sections, "7. מראה ועיצוב",
                "במסך „מראה ועיצוב” אפשר להתאים את הרקע, הבהירות והגופן:\n\n" +
                "• צבע המסך — בחירה בין שחור, כחול, כחול כהה, טורקיז, אפור פחם, ירוק יער, כחול עמוק ואפור אבן.\n" +
                "• בהירות המסך — לפי המכשיר או אחת מדרגות הבהירות הזמינות.\n" +
                "• סוג הגופן — בחירה מבין הגופנים המותקנים באפליקציה.\n" +
                "• צבע הגופן — בחירת צבע הטקסט מבין הדוגמיות.\n\n" +
                "הדוגמית המסומנת בטורקיז היא הצבע הפעיל.");

        addSection(sections, "8. שומר מסך בזמן טעינה",
                "הגדרות הטעינה נמצאות במסך „הפעלה והתנהגות”:\n\n" +
                "• הפעלה אוטומטית בטעינה — מפעילה את השומר כאשר המטען מתחבר.\n" +
                "• ללא כיבוי בטעינה — כאשר המכשיר בטעינה והשומר פעיל, מונעת כיבוי מסך אוטומטי.\n" +
                "• הצגת אחוז הסוללה — מציגה את אחוז הסוללה בזמן טעינה.\n\n" +
                "הגדרות אלה נפרדות מבחירת זמן הפעילות הרגילה. ניתוק המטען מפסיק את התנאי של „ללא כיבוי בטעינה”.");

        addSection(sections, "9. ניהול הרשאות",
                "במסך „ניהול הרשאות” אפשר לבדוק את מצב ההרשאות ולפתוח את מסכי Android המתאימים גם לאחר שההרשאה כבר ניתנה.\n\n" +
                "• גישה להתראות — נדרשת להצגת נגן המדיה של המערכת.\n" +
                "• הצגה מעל אפליקציות אחרות — נדרשת להפעלה אוטומטית בתנאים שבהם Android מחייב הרשאה זו.\n" +
                "• מנהל מכשיר — מאפשר לאפליקציה לבקש נעילה בסיום זמן פעילות מוגדר.\n\n" +
                "אם הרשאה הנדרשת להגדרה פעילה מבוטלת, האפליקציה בודקת זאת בחזרה אליה, מכבה את המתג התלוי ומציגה הודעה שמסבירה מדוע. " +
                "ביטול הרשאת מנהל מכשיר אינו מכבה הגדרות שאינן תלויות בה.");

        addSection(sections, "10. מכשירים שונים והתנהגות המסך",
                "לא בכל מכשיר קיימים מסך נעילה רגיל או כיבוי מסך אוטומטי. מערכות מולטימדיה מסוימות מבוססות Android עשויות לפעול ברצף, " +
                "ולא להציג מסך נעילה רגיל.\n\n" +
                "האפליקציה בודקת את מאפייני Android ואת התנהגות המסך שנצפתה בפועל, ועשויה להציג הודעה כאשר הגדרות נעילה אינן רלוונטיות " +
                "או כאשר זמן כיבוי המסך של המכשיר ארוך מזמן הפעילות שנבחר. זיהוי Android Automotive לבדו אינו מזהה את כל מערכות המולטימדיה.\n\n" +
                "אפליקציה רגילה אינה יכולה להבטיח כיבוי פיזי של כל מסך. בסיום דקה אחת או חמש דקות השומר יכול להיסגר, " +
                "אבל התצוגה עצמה תישאר בשליטת Android והיצרן.");

        addSection(sections, "11. פעולות ומחוות ופתרון תקלות",
                "• הצגת ההגדרות — הקש באזור העליון של המסך כדי להציג את סמל גלגל השיניים למשך כשלוש שניות, ואז הקש עליו.\n" +
                "• יציאה מהשומר — החלק כלפי מעלה מאזורו התחתון של המסך.\n\n" +
                "אם הנגן אינו מופיע, בדוק שהאפשרות „הצגת הנגן” פעילה ושניתנה גישה להתראות.\n\n" +
                "אם השומר אינו מופעל אוטומטית בנעילה או בטעינה, בדוק שהמתג המתאים פעיל ושכל הרשאה נדרשת ניתנה.\n\n" +
                "אם המסך נשאר דולק אחרי סיום דקה או חמש דקות, בדוק את זמן כיבוי המסך בהגדרות Android. סגירת השומר וכיבוי התצוגה הן פעולות נפרדות. " +
                "ההודעה שמופיעה בכרטיס „זמן פעילות” במסך „הפעלה והתנהגות” מסבירה כאשר זמן הכיבוי במכשיר ארוך יותר מהזמן שנבחר.");

        scroll.addView(sections, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void addSection(LinearLayout parent, String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(11), dp(14), dp(13));
        card.setBackground(roundOutline(CARD_SURFACE, dp(17), OUTLINE));

        TextView heading = textView(title, 18, ACCENT);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, -2);
        headingParams.bottomMargin = dp(7);
        card.addView(heading, headingParams);

        View divider = new View(this);
        divider.setBackgroundColor(OUTLINE);
        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.bottomMargin = dp(9);
        card.addView(divider, dividerParams);

        TextView message = textView(body, 14, ON_SURFACE);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setLineSpacing(dp(3), 1f);
        message.setIncludeFontPadding(false);
        message.setTextIsSelectable(true);
        card.addView(message, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(9);
        parent.addView(card, params);
    }

    private GradientDrawable roundOutline(int color, int radius, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private TextView textView(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
