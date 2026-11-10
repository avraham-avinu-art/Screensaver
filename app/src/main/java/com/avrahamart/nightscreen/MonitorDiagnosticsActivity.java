package com.avrahamart.nightscreen;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MonitorDiagnosticsActivity extends Activity {
    private static final int SURFACE = Color.rgb(9, 19, 33);
    private static final int CARD_SURFACE = Color.rgb(20, 36, 56);
    private static final int CONTROL_SURFACE = Color.rgb(27, 47, 70);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int CARD_OUTLINE = Color.rgb(42, 67, 95);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    private TextView logView;

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
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(12), dp(12), dp(12), dp(10));

        TextView title = textView("אבחון התראת הרקע", 22, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        TextView instructions = textView(
                "כדי לבדוק את התקלה: נקה את הרישום, חזור למסך הראשי וודא שההתראה מופיעה. הסר את שומר מסך ממסך האפליקציות האחרונות, המתן כ־10 שניות, פתח שוב את האפליקציה וחזור לכאן. לאחר מכן העתק את נתוני האבחון ושלח אותם.",
                15, MUTED);
        instructions.setGravity(Gravity.CENTER);
        instructions.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams instructionsParams =
                new LinearLayout.LayoutParams(-1, -2);
        instructionsParams.topMargin = dp(4);
        instructionsParams.bottomMargin = dp(12);
        root.addView(instructions, instructionsParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setBackground(roundOutline(CARD_SURFACE, dp(16), CARD_OUTLINE));
        logView = textView("עדיין לא נאספו אירועים.", 12, ON_SURFACE);
        logView.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        logView.setGravity(Gravity.TOP | Gravity.RIGHT);
        logView.setTextDirection(View.TEXT_DIRECTION_LTR);
        logView.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(logView, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollParams.bottomMargin = dp(12);
        root.addView(scroll, scrollParams);

        Button copy = makeButton("העתקת נתוני אבחון");
        copy.setOnClickListener(v -> copyDiagnostics());
        root.addView(copy, new LinearLayout.LayoutParams(-1, dp(48)));

        Button clear = makeButton("נקה רישום והתחל בדיקה");
        clear.setBackground(roundOutline(CONTROL_SURFACE, dp(22), CARD_OUTLINE));
        clear.setTextColor(ON_SURFACE);
        LinearLayout.LayoutParams clearParams =
                new LinearLayout.LayoutParams(-1, dp(46));
        clearParams.topMargin = dp(8);
        root.addView(clear, clearParams);
        clear.setOnClickListener(v -> {
            MonitorDiagnostics.clear(this);
            MonitorDiagnostics.record(this, "===== USER STARTED NEW DIAGNOSTIC TEST =====");
            MonitorDiagnostics.recordNotificationState(this, "test start baseline");
            refreshLog();
            Toast.makeText(this, "הרישום נוקה. אפשר להתחיל בבדיקה.", Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
        refreshLog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MonitorDiagnostics.record(this, "diagnostics screen opened/resumed; appProcessPid="
                + android.os.Process.myPid());
        MonitorDiagnostics.recordNotificationState(this, "diagnostics screen opened");
        refreshLog();
    }

    private void refreshLog() {
        String value = MonitorDiagnostics.read(this);
        logView.setText(value.isEmpty() ? "עדיין לא נאספו אירועים." : value);
    }

    private void copyDiagnostics() {
        String value = MonitorDiagnostics.read(this);
        if (value.isEmpty()) {
            Toast.makeText(this, "אין עדיין נתוני אבחון להעתקה.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "לא ניתן לגשת ללוח ההעתקה.", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Screensaver diagnostics", value));
        Toast.makeText(this, "נתוני האבחון הועתקו. אפשר להדביק אותם כאן.", Toast.LENGTH_LONG).show();
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(Color.rgb(7, 31, 49));
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(roundOutline(ACCENT, dp(22), ACCENT));
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(1));
        return button;
    }

    private TextView textView(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        return view;
    }

    private GradientDrawable roundOutline(int color, int radius, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
