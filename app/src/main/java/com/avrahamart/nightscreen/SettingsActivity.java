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
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final int REQUEST_ENABLE_SCREEN_LOCK_ADMIN = 8142;
    private static final int SURFACE = Color.rgb(247, 248, 252);
    private static final int ON_SURFACE = Color.rgb(36, 41, 59);
    private static final int MUTED = Color.rgb(100, 106, 127);
    private static final int OUTLINE = Color.rgb(205, 210, 226);
    private static final int ACCENT = Color.rgb(58, 76, 160);

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
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(30), dp(5), dp(30), dp(8));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        TextView title = textView("הגדרות", 26, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);
        header.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));

        TextView subtitle = textView("התאמה אישית של השעון, התאריך והנגן", 14, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        subtitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        header.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(24)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.setPadding(0, dp(1), 0, dp(4));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setTextDirection(View.TEXT_DIRECTION_RTL);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        buildSettingsCards();

        Button done = new Button(this);
        done.setText("סיום ושמירה");
        done.setAllCaps(false);
        done.setTextSize(16);
        done.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        done.setTextColor(Color.WHITE);
        done.setMinWidth(0);
        done.setMinHeight(0);
        done.setPadding(dp(24), 0, dp(24), 0);
        done.setBackground(pillBackground(ACCENT, dp(24)));
        done.setElevation(dp(2));
        LinearLayout.LayoutParams doneParams =
                new LinearLayout.LayoutParams(-1, dp(50));
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
        addSpinner(display, "זמן פעילות",
                new String[]{"דקה אחת", "חמש דקות", "ללא כיבוי",
                        "ללא כיבוי בטעינה", "לפי המכשיר"},
                new String[]{"1", "5", "0", "charging", "device"}, "screenSaverDuration");

        LinearLayout font = addCard(content, "גופן");
        addSpinner(font, "צבע",
                new String[]{"לבן", "אפור בהיר", "אפור", "כחול עדין"},
                new String[]{"white", "light", "gray", "blue"}, "fontColor");
        addSpinner(font, "עובי",
                new String[]{"רגיל", "בינוני", "מודגש"},
                new String[]{"0", "1", "2"}, "fontWeight");
        addSpinner(font, "סוג",
                new String[]{"Sans Serif", "Serif", "Monospace", "Casual", "Cursive",
                        "Sans Serif Condensed", "Sans Serif Rounded", "Sans Serif Small Caps",
                        "Serif Monospace", "Serif Condensed"},
                new String[]{"sans-serif", "serif", "monospace", "casual", "cursive",
                        "sans-serif-condensed", "sans-serif-rounded", "sans-serif-smallcaps",
                        "serif-monospace", "serif-condensed"}, "fontFamily");

        clockDateCard = addCard(content, "תצוגת שעון ותאריך");
        LinearLayout clock = clockDateCard;
        addSpinner(clock, "גודל",
                new String[]{"קטן", "בינוני", "רגיל", "גדול", "גדול מאוד"},
                new String[]{"48", "60", "72", "84", "96"}, "clockDateSize");
        addSpinner(clock, "מיקום",
                new String[]{"הכי למעלה", "ברבע העליון", "באמצע המסך"},
                new String[]{"top", "upper", "middle"}, "clockDatePosition");

        LinearLayout player = addCard(content, "תצוגת הנגן");
        addCheck(player, "פרטי אמן", "showDetails", getBool("showDetails", true));
        addCheck(player, "שורת מיקום + זמן", "showProgress", true);
        addSpinner(player, "גודל הנגן",
                new String[]{"קטן", "בינוני", "גדול", "גדול מאוד"},
                new String[]{"14", "17", "20", "23"}, "playerSize");
        addSpinner(player, "מיקום הנגן",
                new String[]{"ברבע התחתון", "הכי למטה"},
                new String[]{"lower", "bottom"}, "playerPosition");

        playerFrameCard = addCard(content, "מסגרת לנגן");
        LinearLayout frame = playerFrameCard;
        addCheck(frame, "הצג מסגרת", "frameEnabled", false);
        addSpinner(frame, "צבע מסגרת",
                new String[]{"לבן", "אפור", "אפור בהיר", "כסף"},
                new String[]{"white", "gray", "light", "silver"}, "frameColor");
        addSpinner(frame, "עובי מסגרת",
                new String[]{"דקה", "בינונית", "עבה"},
                new String[]{"1", "2", "3"}, "frameWidth");
        addSpinner(frame, "עיגול פינות",
                new String[]{"ישר", "עדין", "מעוגל"},
                new String[]{"0", "10", "20"}, "frameRadius");

        normalizeSpinnerWidths(display);
        normalizeSpinnerWidths(font);
        normalizeSpinnerWidths(clockDateCard);
        normalizeSpinnerWidths(player);
        normalizeSpinnerWidths(playerFrameCard);

        if (!hasNotificationAccess()) {
            notificationAccessCard = addNotificationAccessCard(content);
        }

        // Optional device-admin card is deliberately the final settings card.
        screenLockAdminCard = addCard(content, "הרשאת מנהל מכשיר");
        setPermissionCardHeadingTypeface(screenLockAdminCard);
        // Reduce horizontal inner padding by half so the explanation uses the card width better.
        screenLockAdminCard.setPadding(dp(7), dp(4), dp(8), dp(6));
        TextView explanation = textView(
                "ההרשאה נדרשת רק אם רוצים כיבוי מסך ונעילת המכשיר מיד בתום פעילות השומר מסך.\n"
                + "ללא ההרשאה, בתום הזמן תסתיים תצוגת שומר המסך בלבד. "
                + "כיבוי המסך ונעילת המכשיר ימשיכו להתבצע לפי הגדרות המכשיר.",
                16, ON_SURFACE);
        explanation.setGravity(Gravity.CENTER);
        explanation.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        explanation.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        explanation.setTextDirection(View.TEXT_DIRECTION_RTL);
        explanation.setLineSpacing(dp(1), 1f);
        explanation.setPadding(0, dp(5), 0, dp(5));
        LinearLayout.LayoutParams explanationParams =
                new LinearLayout.LayoutParams(-1, -2);
        explanationParams.topMargin = dp(1);
        explanationParams.bottomMargin = dp(1);
        screenLockAdminCard.addView(explanation, explanationParams);

        screenLockAdminStatus = textView("", 16, ON_SURFACE);
        screenLockAdminStatus.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        screenLockAdminStatus.setGravity(Gravity.CENTER);
        screenLockAdminStatus.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        screenLockAdminStatus.setTextDirection(View.TEXT_DIRECTION_RTL);
        screenLockAdminStatus.setPadding(0, dp(5), 0, dp(5));
        LinearLayout.LayoutParams adminStatusParams =
                new LinearLayout.LayoutParams(-1, -2);
        adminStatusParams.topMargin = dp(4);
        adminStatusParams.bottomMargin = dp(4);
        screenLockAdminCard.addView(screenLockAdminStatus, adminStatusParams);

        screenLockAdminButton = makeActionButton("הפעל הרשאת מנהל מכשיר");
        LinearLayout.LayoutParams adminButtonParams =
                new LinearLayout.LayoutParams(-2, dp(44));
        adminButtonParams.gravity = Gravity.CENTER_HORIZONTAL;
        adminButtonParams.topMargin = dp(5);
        screenLockAdminCard.addView(screenLockAdminButton, adminButtonParams);
        screenLockAdminButton.setOnClickListener(v -> requestScreenLockAdmin());
        updateScreenLockAdminCard();
    }

    private LinearLayout addCard(LinearLayout parent, String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setTextDirection(View.TEXT_DIRECTION_RTL);
        card.setPadding(dp(12), dp(4), dp(12), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), OUTLINE);
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(dp(1));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(3));
        parent.addView(card, params);

        TextView heading = textView(title, 18, Color.rgb(49, 58, 92));
        heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        heading.setGravity(Gravity.CENTER);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        heading.setTextDirection(View.TEXT_DIRECTION_RTL);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        heading.setPadding(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, dp(32));
        headingParams.bottomMargin = dp(1);
        card.addView(heading, headingParams);

        View separator = new View(this);
        separator.setBackgroundColor(OUTLINE);
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.setMargins(dp(2), dp(1), dp(2), dp(3));
        card.addView(separator, separatorParams);
        return card;
    }

    private void addCheck(LinearLayout card, String label, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // Position controls explicitly on the left and Hebrew labels on the right.
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(4), 0, dp(4), 0);

        TextView text = textView(label, 16, ON_SURFACE);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        text.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        row.addView(text, new LinearLayout.LayoutParams(0, dp(40), 1f));

        Switch toggle = new Switch(this);
        toggle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        toggle.setShowText(false);
        toggle.setChecked(getBool(key, defaultValue));
        if (Build.VERSION.SDK_INT >= 21) {
            toggle.setThumbTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{ACCENT, Color.rgb(238, 239, 244)}));
            toggle.setTrackTintList(new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{Color.rgb(173, 183, 232), Color.rgb(194, 198, 211)}));
        }
        LinearLayout.LayoutParams switchParams =
                new LinearLayout.LayoutParams(dp(58), dp(40));
        switchParams.leftMargin = dp(12);
        switchParams.rightMargin = dp(4);
        row.addView(toggle, 0, switchParams);

        toggle.setOnCheckedChangeListener((button, checked) -> {
            try {
                prefs.edit().putBoolean(key, checked).apply();
            } catch (Throwable ignored) {
            }
        });
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(40)));
    }

    private void setPermissionCardHeadingTypeface(LinearLayout card) {
        if (card == null || card.getChildCount() == 0) return;
        View first = card.getChildAt(0);
        if (first instanceof TextView) {
            TextView heading = (TextView) first;
            heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
    }

    private void addSpinner(LinearLayout card, String label, String[] names,
                            String[] values, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // The spinner stays on the left while every Hebrew label shares the same right edge.
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.setPadding(dp(4), 0, dp(4), 0);

        TextView l = textView(label, 16, ON_SURFACE);
        l.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        l.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        l.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        l.setTextDirection(View.TEXT_DIRECTION_RTL);
        row.addView(l, new LinearLayout.LayoutParams(0, dp(36), 1f));

        int selected = selectedIndex(key, values);
        int spinnerWidth = dropdownWidthForOptions(names);
        Spinner spinner = new Spinner(this);
        spinner.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        spinner.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
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
                TextView view = convertView instanceof TextView
                        ? (TextView) convertView : new TextView(SettingsActivity.this);
                view.setText(names[position]);
                styleSpinnerText(view, false, position == selectedIndex(key, values));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(8), 0, dp(8), 0);
        spinner.setDropDownWidth(spinnerWidth);
        spinner.setBackground(roundOutline(Color.rgb(249, 249, 253), dp(12), OUTLINE));
        spinner.setSelection(selected, false);

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(spinnerWidth, dp(34));
        spinnerParams.leftMargin = dp(12);
        spinnerParams.rightMargin = dp(6);
        row.addView(spinner, 0, spinnerParams);
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(36)));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean initialSelectionPending = true;
            private int lastHandledPosition = selected;

            @Override public void onItemSelected(AdapterView<?> parent, View view,
                                                 int position, long id) {
                if (initialSelectionPending) {
                    initialSelectionPending = false;
                    if (position == selected) {
                        lastHandledPosition = position;
                        return;
                    }
                }
                if (position == lastHandledPosition) return;
                lastHandledPosition = position;
                try {
                    String value = values[position];
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
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void styleSpinnerText(TextView view, boolean selected, boolean selectedOption) {
        // Use one identical text style for the closed spinner and every dropdown option.
        view.setTextColor(ON_SURFACE);
        // Dropdown options need to be visibly larger and separated; keep the closed
        // spinner value aligned with the standard settings-label typography.
        view.setTextSize(selected ? 16 : 18);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(!selected);
        view.setGravity(Gravity.CENTER);
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setSingleLine(true);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        if (selected) {
            view.setPadding(dp(3), 0, dp(3), 0);
            view.setHeight(dp(34));
        } else {
            // Taller dropdown rows, with breathing room above and below each option.
            view.setPadding(dp(8), dp(8), dp(8), dp(8));
            view.setHeight(dp(44));
            view.setBackgroundColor(selectedOption
                    ? Color.rgb(234, 238, 251) : Color.WHITE);
            view.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                    -1, dp(44)));
        }
    }

    private int dropdownWidthForOptions(String[] names) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(18f * getResources().getDisplayMetrics().scaledDensity);
        float longestText = 0f;
        for (String name : names) {
            if (name != null) longestText = Math.max(longestText, paint.measureText(name));
        }
        int contentWidth = (int) Math.ceil(longestText) + dp(24);
        int minimumWidth = dp(88);
        // Preserve room for the label on narrow screens while fitting the longest option when possible.
        int screenLimit = getResources().getDisplayMetrics().widthPixels
                - dp(70 + 31 + 8 + 18 + 55);
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
            int stored = getInt(key, colorValue(values[0]));
            for (int i = 0; i < values.length; i++) {
                if (colorValue(values[i]) == stored) return i;
            }
        } else if ("fontWeight".equals(key) || "clockDateSize".equals(key)
                || "playerSize".equals(key) || "frameWidth".equals(key)
                || "frameRadius".equals(key)) {
            int stored = getInt(key, parse(values[0], 0));
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
        LinearLayout card = addCard(parent, "גישה להתראות");
        setPermissionCardHeadingTypeface(card);
        TextView message = textView(
                "כדי להציג את נגן המערכת, יש לאפשר גישה להתראות.",
                16, ON_SURFACE);
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
        params.topMargin = dp(8);
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
                : "ההרשאה עדיין לא הופעלה, ולכן כיום כיבוי המסך והנעילה הם רק לפי הגדרות המכשיר.");
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
                    "ההרשאה נדרשת רק אם רוצים כיבוי מסך ונעילת המכשיר מיד בתום פעילות השומר מסך. "
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
        button.setTextSize(15);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(Color.WHITE);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
