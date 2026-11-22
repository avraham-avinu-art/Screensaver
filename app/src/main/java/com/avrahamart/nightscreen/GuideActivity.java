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
        root.setPadding(dp(10), dp(2), dp(10), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(dp(10), dp(2) + insets.getSystemWindowInsetTop(),
                        dp(10), insets.getSystemWindowInsetBottom());
                return insets;
            });
        }

        FrameLayout header = new FrameLayout(this);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(dp(2), dp(8), dp(2), dp(8));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titleBlock.setGravity(Gravity.CENTER);

        TextView title = textView(getString(R.string.guide_activity_text_001), 23, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(-0.015f);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView(getString(R.string.guide_activity_text_002), 16, MUTED);
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitle.setIncludeFontPadding(false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        subtitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = 0;
        titleBlock.addView(subtitle, subtitleParams);

        header.addView(titleBlock, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        sections.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sections.setPadding(0, dp(2), 0, dp(8));

        addSection(sections, getString(R.string.guide_activity_text_003),
                getString(R.string.guide_activity_text_004) +
                getString(R.string.guide_activity_text_005) +
                getString(R.string.guide_activity_text_006));

        addSection(sections, getString(R.string.guide_activity_text_007),
                getString(R.string.guide_activity_text_008) +
                getString(R.string.guide_activity_text_009) +
                getString(R.string.guide_activity_text_010) +
                getString(R.string.guide_activity_text_011));

        addSection(sections, getString(R.string.guide_activity_text_012),
                getString(R.string.guide_activity_text_013) +
                getString(R.string.guide_activity_text_014) +
                getString(R.string.guide_activity_text_015) +
                getString(R.string.guide_activity_text_016) +
                getString(R.string.guide_activity_text_017) +
                getString(R.string.guide_activity_text_018) +
                getString(R.string.guide_activity_text_019) +
                getString(R.string.guide_activity_text_020) +
                getString(R.string.guide_activity_text_021) +
                getString(R.string.guide_activity_text_022));

        addSection(sections, getString(R.string.guide_activity_text_023),
                getString(R.string.guide_activity_text_024) +
                getString(R.string.guide_activity_text_025) +
                getString(R.string.guide_activity_text_026) +
                getString(R.string.guide_activity_text_027) +
                getString(R.string.guide_activity_text_028) +
                getString(R.string.guide_activity_text_029) +
                getString(R.string.guide_activity_text_030));

        addSection(sections, getString(R.string.guide_activity_text_080),
                getString(R.string.guide_activity_text_081));

        addSection(sections, getString(R.string.guide_activity_text_031),
                getString(R.string.guide_activity_text_032) +
                getString(R.string.guide_activity_text_033) +
                getString(R.string.guide_activity_text_034) +
                getString(R.string.guide_activity_text_035) +
                getString(R.string.guide_activity_text_036) +
                getString(R.string.guide_activity_text_037));

        addSection(sections, getString(R.string.guide_activity_text_038),
                getString(R.string.guide_activity_text_039) +
                getString(R.string.guide_activity_text_040) +
                getString(R.string.guide_activity_text_041) +
                getString(R.string.guide_activity_text_042) +
                getString(R.string.guide_activity_text_043) +
                getString(R.string.guide_activity_text_044) +
                getString(R.string.guide_activity_text_045));

        addSection(sections, getString(R.string.guide_activity_text_046),
                getString(R.string.guide_activity_text_047) +
                getString(R.string.guide_activity_text_048) +
                getString(R.string.guide_activity_text_049) +
                getString(R.string.guide_activity_text_050) +
                getString(R.string.guide_activity_text_051) +
                getString(R.string.guide_activity_text_052));

        addSection(sections, getString(R.string.guide_activity_text_053),
                getString(R.string.guide_activity_text_054) +
                getString(R.string.guide_activity_text_055) +
                getString(R.string.guide_activity_text_056) +
                getString(R.string.guide_activity_text_057) +
                getString(R.string.guide_activity_text_058));

        addSection(sections, getString(R.string.guide_activity_text_059),
                getString(R.string.guide_activity_text_060) +
                getString(R.string.guide_activity_text_061) +
                getString(R.string.guide_activity_text_062) +
                getString(R.string.guide_activity_text_063) +
                getString(R.string.guide_activity_text_064) +
                getString(R.string.guide_activity_text_065));

        addSection(sections, getString(R.string.guide_activity_text_066),
                getString(R.string.guide_activity_text_067) +
                getString(R.string.guide_activity_text_068) +
                getString(R.string.guide_activity_text_069) +
                getString(R.string.guide_activity_text_070) +
                getString(R.string.guide_activity_text_071) +
                getString(R.string.guide_activity_text_072));

        addSection(sections, getString(R.string.guide_activity_text_082),
                getString(R.string.guide_activity_text_083));

        addSection(sections, getString(R.string.guide_activity_text_073),
                getString(R.string.guide_activity_text_074) +
                getString(R.string.guide_activity_text_075) +
                getString(R.string.guide_activity_text_076) +
                getString(R.string.guide_activity_text_077) +
                getString(R.string.guide_activity_text_078) +
                getString(R.string.guide_activity_text_079));

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

        TextView message = textView(body, 15, ON_SURFACE);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        message.setTextDirection(View.TEXT_DIRECTION_RTL);
        message.setLineSpacing(dp(3), 1f);
        message.setIncludeFontPadding(false);
        message.setTextIsSelectable(true);
        card.addView(message, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(7);
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
