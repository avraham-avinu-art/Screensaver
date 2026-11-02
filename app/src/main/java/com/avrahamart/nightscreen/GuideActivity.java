package com.avrahamart.nightscreen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class GuideActivity extends Activity {
    public static final String EXTRA_CHAPTER_ID =
            "com.avrahamart.nightscreen.EXTRA_GUIDE_CHAPTER_ID";

    private static final int SURFACE = Color.rgb(9, 19, 33);
    private static final int CARD_SURFACE = Color.rgb(20, 36, 56);
    private static final int CONTROL_SURFACE = Color.rgb(27, 47, 70);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int OUTLINE = Color.rgb(42, 67, 95);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    private LinearLayout root;
    private LinearLayout content;
    private TextView title;
    private TextView subtitle;
    private FrameLayout backButton;
    private int currentChapterId = -1;
    private final List<Chapter> chapters = createChapters();

    private static final class Chapter {
        final int id;
        final String title;
        final String summary;
        final String purpose;
        final String steps;
        final String recommended;
        final String system;
        final String troubleshooting;
        final int settingsCategory;
        final boolean permissionsPage;

        Chapter(int id, String title, String summary, String purpose, String steps,
                String recommended, String system, String troubleshooting,
                int settingsCategory, boolean permissionsPage) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.purpose = purpose;
            this.steps = steps;
            this.recommended = recommended;
            this.system = system;
            this.troubleshooting = troubleshooting;
            this.settingsCategory = settingsCategory;
            this.permissionsPage = permissionsPage;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SURFACE);
        window.setNavigationBarColor(SURFACE);
        window.getDecorView().setSystemUiVisibility(0);

        root = new LinearLayout(this);
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
        title = textView("מדריך שימוש", 24, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        header.addView(title, new FrameLayout.LayoutParams(-1, dp(44), Gravity.CENTER));

        backButton = circularBackButton();
        FrameLayout.LayoutParams backParams =
                new FrameLayout.LayoutParams(dp(42), dp(42),
                        Gravity.LEFT | Gravity.CENTER_VERTICAL);
        header.addView(backButton, backParams);
        backButton.setOnClickListener(v -> handleBack());
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        subtitle = textView("מדריך מפורט לפי נושאים", 14, MUTED);
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
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(0, dp(2), 0, dp(8));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);

        int requested = getIntent().getIntExtra(EXTRA_CHAPTER_ID, -1);
        if (findChapter(requested) != null) showChapter(requested);
        else showIndex();
    }

    private void showIndex() {
        currentChapterId = -1;
        title.setText("מדריך שימוש");
        subtitle.setText("בחר נושא כדי לפתוח פרק הסבר מלא");
        backButton.setVisibility(View.GONE);
        content.removeAllViews();

        addIntroCard("מדריך השימוש בשומר מסך",
                "המדריך מחולק ל־13 פרקים. בכל פרק תמצא הסבר על מטרת ההגדרה, הוראות שימוש, " +
                "המלצות, התאמה להגדרות Android ופתרון תקלות. מתוך פרק אפשר לעבור ישירות " +
                "למסך ההגדרה הרלוונטי.");
        for (Chapter chapter : chapters) {
            addChapterRow(chapter);
        }
    }

    private void showChapter(int chapterId) {
        Chapter chapter = findChapter(chapterId);
        if (chapter == null) {
            showIndex();
            return;
        }
        currentChapterId = chapterId;
        title.setText("פרק " + chapter.id);
        subtitle.setText(chapter.title);
        backButton.setVisibility(View.VISIBLE);
        content.removeAllViews();

        addIntroCard(chapter.title, chapter.summary);
        addSection(content, "1. מה ההגדרה עושה", chapter.purpose);
        addSection(content, "2. איך משתמשים בה", chapter.steps);
        addSection(content, "3. איך מומלץ להגדיר אותה", chapter.recommended);
        addSection(content, "4. התאמה להגדרות המערכת והרשאות נדרשות",
                chapter.system);
        addSection(content, "5. תקלות נפוצות ומה לבדוק", chapter.troubleshooting);

        if (chapter.permissionsPage) {
            addActionButton("פתיחת מסך ניהול הרשאות", () ->
                    startActivity(new Intent(this, PermissionsActivity.class)));
        } else if (chapter.settingsCategory >= 0) {
            addActionButton("פתיחת ההגדרות הרלוונטיות", () -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                if (chapter.settingsCategory > 0) {
                    intent.putExtra(SettingsActivity.EXTRA_OPEN_CATEGORY,
                            chapter.settingsCategory);
                }
                startActivity(intent);
            });
        }
        addActionButton("חזרה לרשימת הפרקים", this::showIndex);
    }

    private void handleBack() {
        if (currentChapterId >= 0) showIndex();
        else finish();
    }

    @Override
    public void onBackPressed() {
        if (currentChapterId >= 0) showIndex();
        else super.onBackPressed();
    }

    private Chapter findChapter(int id) {
        for (Chapter chapter : chapters) {
            if (chapter.id == id) return chapter;
        }
        return null;
    }

    private List<Chapter> createChapters() {
        List<Chapter> result = new ArrayList<>();
        result.add(new Chapter(1, "היכרות והפעלה ראשונית",
                "היכרות עם מסך השומר, מבנה ההגדרות ודרך הכניסה לאפשרויות שאינן מוצגות תמיד.",
                "האפליקציה מציגה שעון, תאריך עברי ותאריך לועזי, ובאפשרותך להציג גם נגן מדיה ואחוז סוללה. " +
                "מסך השומר נשאר נקי; כפתור ההגדרות מוסתר בכוונה ומופיע רק לאחר מחווה.",
                "1. פתח את שומר המסך.\n2. הקש באזור העליון של המסך כדי לחשוף את סמל גלגל השיניים למשך כשלוש שניות.\n" +
                "3. הקש על סמל גלגל השיניים.\n4. בחר אחת מארבע הקטגוריות: שעון ותאריך, הפעלה והתנהגות, מראה ועיצוב או נגן מדיה.\n" +
                "5. לסיום, השתמש ב„סיום ושמירה” כדי לחזור למסך השומר.",
                "להתחלה מומלץ להשאיר את ברירות המחדל: זמן פעילות לפי המכשיר, גודל שעון רגיל, גופן רגיל, " +
                "ומיקום השעון ברבע העליון. לאחר מכן שנה כל פעם הגדרה אחת ובדוק את התוצאה.",
                "אין צורך בהרשאה מיוחדת כדי לפתוח את מסך ההגדרות. הרשאות נוספות נדרשות רק לתכונות מסוימות, " +
                "כגון הצגת נגן המדיה והפעלה אוטומטית ברקע. פירוט מלא נמצא בפרק 11.",
                "אם סמל ההגדרות נעלם, הקש שוב באזור העליון והפעל אותו בתוך חלון שלוש השניות. " +
                "אם לחיצה על כרטיס קטגוריה אינה פותחת אותו, חזור למסך הראשי ונסה שוב. " +
                "שינויים נשמרים מיד ואין צורך לשמור כל הגדרה בנפרד.",
                0, false));

        result.add(new Chapter(2, "הגדרת זמן הפעילות",
                "ארבע אפשרויות זמן הפעילות קובעות כמה זמן השומר נשאר מוצג ומה תפקידו של זמן כיבוי המסך של Android.",
                "„לפי המכשיר” אינו פרק זמן עצמאי: השומר נשאר מוצג כל עוד Android משאיר את המסך פעיל. " +
                "„דקה אחת” ו„חמש דקות” מסיימות את הצגת השומר בתום הזמן שנבחר. „ללא כיבוי” מבקש להשאיר את השומר ואת המסך פעילים ברציפות.",
                "1. פתח הגדרות > הפעלה והתנהגות.\n2. אתר את „זמן פעילות”.\n" +
                "3. בחר „לפי המכשיר”, „דקה אחת”, „חמש דקות” או „ללא כיבוי”.\n" +
                "4. הבחירה נשמרת מיד.\n5. חזור למסך השומר ובדוק את ההתנהגות.",
                "לשימוש רגיל מומלץ „לפי המכשיר”. לשימוש קצר ומוגדר בחר דקה או חמש דקות. " +
                "לתצוגה רציפה, למשל כשעון שולחני, בחר „ללא כיבוי” ודאג לאספקת חשמל מתאימה.",
                "ב„לפי המכשיר” יש להגדיר ב־Android את זמן כיבוי המסך הרצוי. האפליקציה אינה משנה אותו. " +
                "בדקה אחת או בחמש דקות השומר ייסגר בתום הזמן גם אם Android עדיין משאיר את התצוגה דולקת. " +
                "כיבוי פיזי של המסך תלוי במערכת וביצרן ואינו מובטח על ידי האפליקציה.",
                "אם המסך נשאר דולק אחרי שהשומר נסגר, בדוק את זמן הכיבוי בהגדרות Android: ייתכן שהמסך מוגדר להיכבות מאוחר יותר. " +
                "אם השומר נכבה מוקדם מהצפוי, בדוק אם נבחר „דקה אחת” או „חמש דקות” ואם קיימת הגדרת טעינה שמשאירה את המסך פעיל.",
                2, false));

        result.add(new Chapter(3, "כיבוי מסך ונעילת המכשיר",
                "שלוש הגדרות נפרדות קובעות אם Android רשאי להציג מסך נעילה, אם השומר יוצג שוב לאחר התעוררות המסך, ואם יופעל בעקבות נעילה.",
                "„מניעת נעילה בזמן פעילות” קובעת אם מסך הנעילה יכול להופיע מעל השומר בזמן שהוא פעיל. " +
                "„המשך הצגה במסך הנעילה” משאירה את השומר מוכן להופעה לאחר כיבוי המסך והפעלתו מחדש, בכפוף למשך הפעילות. " +
                "„הפעלה אוטומטית בנעילה” מפעילה את השומר בעקבות אירוע נעילה גם אם לא היה פעיל קודם.",
                "1. פתח הגדרות > הפעלה והתנהגות.\n2. קבע את „מניעת נעילה בזמן פעילות” לפי הצורך.\n" +
                "3. קבע בנפרד את „המשך הצגה במסך הנעילה”.\n4. הפעל „הפעלה אוטומטית בנעילה” רק אם ברצונך שהשומר יעלה בעקבות נעילה כשהוא אינו פעיל.\n" +
                "5. בדוק את ההתנהגות לאחר כיבוי והדלקה של המסך.",
                "אם ברצונך שהשומר יופיע מעל מסך הנעילה בזמן הצגתו, בדוק את מניעת הנעילה. " +
                "אם ברצונך שהשומר יחזור לאחר שהמסך כבה והתעורר, השתמש בהמשך ההצגה. " +
                "אם ברצונך להפעיל אותו גם כאשר לא היה פעיל, השתמש בהפעלה האוטומטית בנעילה.",
                "הרשאת מנהל מכשיר מאפשרת לאפליקציה לבקש נעילה יזומה בסיום זמן הפעילות. היא אינה זהה להרשאת הצגה מעל אפליקציות אחרות. " +
                "מסכי Android ושמות התפריטים עשויים להשתנות בין יצרנים וגרסאות.",
                "אם מסך הנעילה אינו מופיע, בדוק את הגדרת מניעת הנעילה ואת הגדרות האבטחה של המכשיר. " +
                "אם השומר אינו חוזר לאחר התעוררות המסך, בדוק שהמשך ההצגה פעיל ושמשך הפעילות שנבחר עדיין מאפשר זאת. " +
                "אין להסיק שהפעלה אוטומטית בנעילה זהה להמשך הצגה.",
                2, false));

        result.add(new Chapter(4, "התאמה להגדרות Android",
                "הפרק מסביר את הקשר בין האפליקציה לבין זמן כיבוי המסך, מצב הנעילה, בהירות המערכת והבדלים בין מכשירים.",
                "Android מנהל את זמן כיבוי התצוגה ואת מסך הנעילה. האפליקציה יכולה להתאים את משך הצגת השומר ולשחרר או לבקש השארת מסך פעיל, " +
                "אך אינה יכולה להבטיח שכל מכשיר יכבה פיזית את המסך לפי בקשתה.",
                "1. פתח בהגדרות Android את תצוגה או מסך.\n2. אתר את „זמן כיבוי מסך” או „כיבוי אוטומטי”.\n" +
                "3. בדוק את משך הזמן המוגדר.\n4. בדוק בנפרד את סוג הנעילה ואת הגדרות חיסכון בסוללה.\n" +
                "5. חזור לאפליקציה ובחר זמן פעילות המתאים להגדרות המכשיר.",
                "לשימוש רגיל, בחר „לפי המכשיר” והשאר את זמן הכיבוי במערכת לפי העדפתך. " +
                "לזמן פעילות קבוע, ודא שזמן הכיבוי במכשיר אינו ארוך ממנו אם חשוב שגם התצוגה תכבה בסמוך לסיום.",
                "מסלול ההגדרות משתנה בין יצרנים וגרסאות Android. מכשירי מולטימדיה מבוססי Android עשויים שלא לכלול מסך נעילה רגיל או כיבוי מסך אוטומטי. " +
                "זיהוי Android Automotive הוא אינדיקציה בלבד ואינו מזהה כל יחידת מולטימדיה.",
                "אם הערך בהגדרות Android קצר מהמצופה אך המסך אינו נכבה, בדוק חיסכון בסוללה, מצב תצוגה רציפה והגדרות יצרן. " +
                "אל תסיק מקריאת הערך בלבד שהכיבוי אכן מתרחש; בדוק את התנהגות המסך בפועל.",
                2, false));

        result.add(new Chapter(5, "הפעלה אוטומטית בנעילה",
                "הגדרה זו מפעילה את שומר המסך בעת נעילת המכשיר גם כאשר השומר לא הוצג לפני כן.",
                "השירות עוקב אחר אירועי נעילת המכשיר ומנסה לפתוח את השומר מעל מסך הנעילה. " +
                "זו פעולה נפרדת מהמשך הצגה לאחר כיבוי מסך.",
                "1. פתח הגדרות > הפעלה והתנהגות.\n2. הפעל „הפעלה אוטומטית בנעילה”.\n" +
                "3. כאשר מוצגת בקשת הרשאה, קרא את ההסבר.\n4. אפשר „הצגה מעל אפליקציות אחרות” אם Android דורש זאת.\n" +
                "5. חזור לאפליקציה, ודא שהמתג נשאר פעיל ובדוק נעילה.",
                "הפעל אפשרות זו רק אם נדרשת הצגה אוטומטית בעקבות נעילה. אם השומר צריך לחזור רק לאחר מחזור כיבוי והדלקה בזמן שכבר פעל, עדיף להשתמש בהמשך ההצגה ולא להפעיל שירות אוטומטי נוסף.",
                "בגרסאות Android 10 ומעלה נדרשת בתרחישים הנתמכים הרשאת הצגה מעל אפליקציות אחרות. " +
                "השירות משתמש בהתראה שקטה ומתמשכת שאינה מוצגת במסך הנעילה.",
                "אם השומר אינו עולה, בדוק שהמתג פעיל, שההרשאה לא בוטלה וששירות ההפעלה האוטומטית פועל. " +
                "בדוק גם הגבלות סוללה והרשאות רקע של היצרן. אם ההרשאה בוטלה, המתג התלוי אמור להיכבות.",
                2, false));

        result.add(new Chapter(6, "שומר מסך בזמן טעינה",
                "הגדרות הטעינה מאפשרות להפעיל את השומר בחיבור מטען, להשאיר את התצוגה פעילה בזמן טעינה ולהציג את אחוז הסוללה.",
                "„הפעלה אוטומטית בטעינה” מפעילה את השומר בחיבור חשמל. „ללא כיבוי בטעינה” מונעת כיבוי מסך אוטומטי בזמן שהמכשיר נטען והשומר פעיל. " +
                "„הצגת אחוז הסוללה” מוסיפה את נתון הסוללה לתצוגה.",
                "1. פתח הגדרות > הפעלה והתנהגות.\n2. הפעל „הפעלה אוטומטית בטעינה” אם ברצונך שהשומר ייפתח עם חיבור מטען.\n" +
                "3. הפעל „ללא כיבוי בטעינה” רק אם ברצונך שהתצוגה תישאר פעילה כל זמן הטעינה.\n" +
                "4. הפעל „הצגת אחוז הסוללה” לפי הצורך.\n5. חבר ונתק מטען כדי לבדוק את ההתנהגות.",
                "לשעון שולחני בזמן טעינה אפשר להפעיל הפעלה אוטומטית וללא כיבוי בטעינה. " +
                "אם ברצונך רק לפתוח את השומר בחיבור מטען אך לא להשאיר את המסך דולק, השאר את „ללא כיבוי בטעינה” כבוי.",
                "הפעלה אוטומטית בטעינה עשויה לדרוש הרשאת הצגה מעל אפליקציות אחרות ב־Android 10 ומעלה. " +
                "האפשרות „ללא כיבוי בטעינה” אינה אותה הגדרה כמו זמן הפעילות הרגיל.",
                "אם חיבור המטען אינו מפעיל את השומר, בדוק שהמתג פעיל ושהרשאת ההצגה ניתנה. " +
                "אם המסך אינו נשאר פעיל, בדוק את „ללא כיבוי בטעינה” ואת זיהוי מצב הטעינה של המכשיר.",
                2, false));

        result.add(new Chapter(7, "שעון ותאריך",
                "בפרק זה ניתן ללמוד כיצד לבחור תאריכים, גודל, עובי ומיקום של השעון והתאריך.",
                "קטגוריית „שעון ותאריך” שולטת בהצגת התאריך העברי והלועזי, בגודל השעון והתאריכים, בעובי הגופן ובמיקום בלוח המסך.",
                "1. פתח הגדרות > שעון ותאריך.\n2. הפעל או כבה בנפרד תאריך עברי ותאריך לועזי.\n" +
                "3. בחר גודל מתוך האפשרויות.\n4. בחר עובי גופן.\n5. בחר מיקום: בראש המסך, ברבע העליון או במרכז המסך.",
                "ברירת המחדל היא גודל „רגיל”, עובי „רגיל” ומיקום „ברבע העליון”. " +
                "למסך קטן מומלץ לבדוק את הגודל בפועל; לגופן ייחודי ייתכנו התאמות מידות כדי לשמור על קריאות.",
                "ההגדרות האלה אינן דורשות הרשאה מיוחדת. שעת המכשיר, אזור הזמן והגדרות התאריך של Android משפיעים על הנתונים המוצגים.",
                "אם תאריך אינו מופיע, בדוק שהמתג המתאים פעיל. אם השעה או התאריך אינם נכונים, בדוק את שעון המערכת ואזור הזמן. " +
                "אם גופן מסוים נראה קטן או גדול מהאחרים, נסה גופן אחר או גודל סמוך.",
                1, false));

        result.add(new Chapter(8, "מראה ועיצוב",
                "כל הגדרות המראה החזותי מרוכזות במקום אחד: רקע, בהירות, משפחת גופן וצבע הטקסט.",
                "צבע המסך קובע את צבע הרקע. הבהירות קובעת את רמת ההארה שהאפליקציה מבקשת. סוג הגופן וצבע הגופן משפיעים על השעה והתאריכים.",
                "1. פתח הגדרות > מראה ועיצוב.\n2. בחר צבע מסך באמצעות דוגמית הצבע.\n" +
                "3. בחר בהירות או „לפי המכשיר”.\n4. פתח את רשימת הגופנים ובחר את הגופן הרצוי.\n" +
                "5. בחר צבע גופן באמצעות הדוגמיות. הבחירה נשמרת מיד.",
                "לשימוש ממושך מומלץ לבחור רקע כהה ובהירות מתונה. בחר צבע טקסט בעל ניגודיות גבוהה מול הרקע. " +
                "בדוק את קריאות הגופן גם במרחק שבו המכשיר מוצב בדרך כלל.",
                "בחירת צבע וגופן אינה דורשת הרשאה נפרדת. בהירות בפועל יכולה להיות מושפעת מהגדרות מערכת, הרשאות או מגבלות היצרן.",
                "אם צבע אינו משתנה, ודא שנבחרה הדוגמית הנכונה וחזור למסך השומר. אם הטקסט אינו קריא, בחר צבע בעל ניגודיות גבוהה יותר או גופן אחר.",
                3, false));

        result.add(new Chapter(9, "נגן המדיה",
                "הנגן מציג מידע על השמעה פעילה ומאפשר להתאים מה יוצג, מתי, באיזה גודל ובאיזה מיקום.",
                "נגן המדיה משקף את נתוני נגן Android הפעיל. ניתן לבחור אם להציג את הנגן, להציגו רק בתחילת שיר, להציג פרטי אמן ושורת התקדמות, ולשנות גודל ומיקום.",
                "1. פתח הגדרות > נגן מדיה.\n2. הפעל „הצגת הנגן”.\n3. אם מתבקשת הרשאת גישה להתראות, אפשר אותה.\n" +
                "4. קבע אם להציג רק בתחילת שיר.\n5. בחר אם להציג פרטי אמן ושורת מיקום + זמן.\n6. קבע גודל ומיקום.",
                "אם ברצונך לראות את הנגן לאורך ההשמעה, השאר „הצגה רק בתחילת שיר” כבוי. " +
                "הפעל שורת מיקום + זמן אם הנגן מספק נתוני התקדמות; לא כל יישום מדיה מספק את כל הנתונים.",
                "גישה להתראות נדרשת לקריאת נתוני המדיה ולשליטה בנגן המערכת. הרשאה זו ניתנת במסך הגדרות Android הייעודי לגישה להתראות.",
                "אם הנגן אינו מופיע, בדוק שהצגת הנגן פעילה ושהרשאת גישה להתראות ניתנה. " +
                "ודא שיש הפעלת מדיה פעילה ושהיישום המנגן מפרסם הפעלת מדיה למערכת. נתוני אמן או התקדמות חסרים עשויים לנבוע מהנגן עצמו.",
                4, false));

        result.add(new Chapter(10, "מסגרת הנגן",
                "המסגרת היא שכבת עיצוב נפרדת לנגן, עם שליטה בהפעלה, צבע, עובי ועיגול הפינות.",
                "כאשר המסגרת מופעלת, היא מקיפה את אזור הנגן. ניתן לבחור צבע, עובי ורדיוס פינות. כיבוי המסגרת אינו אמור למחוק את הערכים שנבחרו.",
                "1. פתח הגדרות > נגן מדיה.\n2. גלול לכרטיס „מסגרת לנגן”.\n3. הפעל „הצג מסגרת”.\n" +
                "4. בחר צבע מסגרת.\n5. קבע עובי ועיגול פינות.\n6. כבה את המסגרת כדי להסתיר אותה בלי לאפס את הבחירות.",
                "בחר מסגרת עדינה כאשר ברצונך להפריד את הנגן מהרקע בלי להכביד על התצוגה. " +
                "אם הרקע והנגן דומים בצבעם, בחר מסגרת בעלת ניגודיות מספקת.",
                "המסגרת היא הגדרה חזותית ואינה דורשת הרשאה. כשהמסגרת כבויה, אפשרויות העיצוב שלה אינן פעילות אך הבחירות נשמרות.",
                "אם המסגרת אינה נראית, בדוק שהמתג „הצג מסגרת” פעיל ושעובי המסגרת אינו מינימלי מדי. " +
                "אם שינוי הצבע לא נראה, בדוק את ניגודיות הצבע מול רקע הנגן.",
                4, false));

        result.add(new Chapter(11, "הרשאות",
                "מסך ניהול ההרשאות מרכז את מצב ההרשאות ואת הדרך לפתוח את מסכי Android המתאימים.",
                "גישה להתראות מאפשרת לקרוא נתוני מדיה ולשלוט בנגן. הצגה מעל אפליקציות אחרות נדרשת להפעלה אוטומטית ברקע בתרחישים הנתמכים. " +
                "הרשאת מנהל מכשיר מאפשרת לבקש נעילה יזומה בסיום הפעילות.",
                "1. פתח הגדרות > ניהול הרשאות.\n2. בדוק את מצב כל הרשאה.\n" +
                "3. לחץ על ניהול או הפעלה של ההרשאה המבוקשת.\n4. במסך Android בחר באפליקציית „שומר מסך” ואשר או בטל את ההרשאה לפי הצורך.\n" +
                "5. חזור לאפליקציה ובדוק שהמצב עודכן.",
                "הענק רק הרשאות הנדרשות לתכונות שבהן אתה משתמש. לנגן נדרשת גישה להתראות. להפעלה אוטומטית ברקע נדרשת לעיתים הצגה מעל אפליקציות אחרות. " +
                "לנעילה יזומה בסיום הפעילות נדרשת הרשאת מנהל מכשיר.",
                "נתיבי התפריטים משתנים בין יצרנים וגרסאות. מסך ההרשאות באפליקציה מציג את מצב ההרשאות ואת הכפתור המתאים. " +
                "ביטול הרשאה תלויה גורם לכיבוי ההגדרה התלויה ולהודעת הסבר. ביטול מנהל מכשיר אינו אמור לכבות הגדרות שאינן תלויות בו.",
                "אם הרשאה נראית פעילה אך התכונה אינה פועלת, חזור למסך ניהול ההרשאות ובדוק את המצב בפועל. " +
                "אם Android אינו פותח את המסך המתאים, חפש בהגדרות המערכת את שם ההרשאה ואת „שומר מסך”.",
                -1, true));

        result.add(new Chapter(12, "פעולות ומחוות",
                "הפרק מתעד את פעולות מסך השומר ואת מחוות המגע הזמינות בלי להוסיף כפתורים קבועים למסך הראשי.",
                "כפתור ההגדרות מוסתר ונחשף זמנית. מחוות החלקה כלפי מעלה מאפשרת לצאת מהשומר. כאשר נגן המדיה מוצג, מופיעים בו כפתורי השליטה שהנגן מספק.",
                "1. כדי לחשוף את ההגדרות, הקש באזור העליון של המסך.\n2. הקש על גלגל השיניים שמופיע למשך כשלוש שניות.\n" +
                "3. כדי לצאת מהשומר, התחל באזור השליש התחתון והחלק כלפי מעלה לכיוון השליש העליון.\n" +
                "4. כאשר הנגן מוצג, השתמש בכפתורי הקודם, הפעלה/השהיה והבא.",
                "במחוות יציאה, בצע תנועה ברורה כלפי מעלה ולא תנועה אופקית. במסך הראשי מומלץ להשאיר את אזור השומר נקי ולא להוסיף קיצורי דרך קבועים.",
                "המחוות אינן דורשות הרשאה. כפתורי נגן המדיה תלויים בכך שנגן פעיל ובגישה להתראות.",
                "אם ההחלקה אינה יוצאת, התחל נמוך יותר והמשך בתנועה אנכית. אם כפתור ההגדרות נעלם, חשוף אותו שוב והקש עליו מיד.",
                0, false));

        result.add(new Chapter(13, "פתרון בעיות",
                "רשימת בדיקות מסודרת לבעיות נפוצות בזמן פעילות, נעילה, טעינה, נגן והרשאות.",
                "פרק זה עוזר להבחין בין בעיה בהצגת השומר לבין פעולה של מערכת Android, כגון כיבוי פיזי של התצוגה או נעילת המכשיר.",
                "1. זהה מה בדיוק לא פועל: הצגת השומר, כיבוי התצוגה, נעילה, הפעלה אוטומטית או נגן.\n" +
                "2. בדוק את ההגדרה המתאימה במסך ההגדרות.\n3. בדוק את מצב ההרשאה במסך ניהול הרשאות.\n" +
                "4. בדוק את הגדרות Android ואת זמן כיבוי המסך.\n5. נסה שוב ובדוק אם ההתנהגות חזרה.",
                "לשימוש רגיל התחל ב„לפי המכשיר”. הפעל אוטומציה רק לאחר שבדקת את ההרשאות. " +
                "הפרד בין מניעת נעילה, המשך הצגה והפעלה אוטומטית — לכל אחת תפקיד שונה.",
                "בדוק זמן כיבוי מסך, נעילה אוטומטית, חיסכון בסוללה, גישה להתראות, הצגה מעל אפליקציות אחרות ומנהל מכשיר לפי התכונה. " +
                "יצרנים עשויים לשנות את שמות התפריטים ואת מגבלות הרקע.",
                "אם השומר נסגר אך המסך נשאר דולק, מדובר בשתי פעולות נפרדות; בדוק את זמן הכיבוי ב־Android. " +
                "אם ההפעלה בנעילה או בטעינה לא מתבצעת, בדוק מתג והרשאת הצגה. אם הנגן חסר, בדוק גישה להתראות ונגן פעיל. " +
                "אם אין מסך נעילה או כיבוי אוטומטי במכשיר, חלק מהאפשרויות עשויות להיות לא רלוונטיות.",
                2, false));
        return result;
    }

    private void addChapterRow(Chapter chapter) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundOutline(CARD_SURFACE, dp(15), OUTLINE));
        row.setClickable(true);
        row.setFocusable(true);

        TextView heading = textView("פרק " + chapter.id + " — " + chapter.title,
                17, ON_SURFACE);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView detail = textView(chapter.summary, 13, MUTED);
        detail.setGravity(Gravity.RIGHT | Gravity.TOP);
        detail.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(5);
        row.addView(detail, detailParams);

        row.setOnClickListener(v -> showChapter(chapter.id));
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        content.addView(row, params);
    }

    private void addIntroCard(String heading, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(12), dp(14), dp(13));
        card.setBackground(roundOutline(CONTROL_SURFACE, dp(15), OUTLINE));

        TextView titleView = textView(heading, 17, ACCENT);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        card.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        TextView message = textView(body, 14, ON_SURFACE);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        message.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(7);
        card.addView(message, messageParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        content.addView(card, params);
    }

    private void addSection(LinearLayout parent, String headingText, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(11), dp(14), dp(13));
        card.setBackground(roundOutline(CARD_SURFACE, dp(15), OUTLINE));

        TextView heading = textView(headingText, 16, ACCENT);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
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
        params.bottomMargin = dp(8);
        parent.addView(card, params);
    }

    private void addActionButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(Color.rgb(7, 31, 49));
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setBackground(pillBackground(ACCENT, dp(22)));
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(46));
        params.topMargin = dp(3);
        params.bottomMargin = dp(7);
        content.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private FrameLayout circularBackButton() {
        FrameLayout button = new FrameLayout(this);
        button.setContentDescription("חזרה");
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(pillBackground(ACCENT, dp(22)));
        TextView arrow = textView("‹", 34, Color.rgb(7, 31, 49));
        arrow.setGravity(Gravity.CENTER);
        arrow.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.addView(arrow, new FrameLayout.LayoutParams(-1, -1));
        return button;
    }

    private GradientDrawable roundOutline(int color, int radius, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private GradientDrawable pillBackground(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
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
