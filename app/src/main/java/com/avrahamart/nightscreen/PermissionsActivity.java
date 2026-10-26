package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class PermissionsActivity extends Activity {
    private static final int SURFACE = Color.rgb(9, 19, 33);
    private static final int CARD_SURFACE = Color.rgb(20, 36, 56);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int CARD_OUTLINE = Color.rgb(42, 67, 95);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    private TextView notificationStatus;
    private TextView overlayStatus;
    private TextView adminStatus;
    private Button notificationButton;
    private Button overlayButton;
    private Button adminButton;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(SURFACE);
        window.setNavigationBarColor(SURFACE);
        window.getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(14), dp(6), dp(14), dp(10));

        FrameLayout header = new FrameLayout(this);
        header.setPadding(dp(4), dp(3), dp(4), dp(12));
        TextView title = textView("הרשאות", 26, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        header.addView(title, new FrameLayout.LayoutParams(-1, dp(44), Gravity.CENTER));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        cards.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        cards.setTextDirection(View.TEXT_DIRECTION_RTL);

        addNotificationPermissionCard(cards);
        addOverlayPermissionCard(cards);
        addDeviceAdminPermissionCard(cards);

        scroll.addView(cards, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button done = makeButton("חזרה להגדרות");
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(-2, dp(48));
        doneParams.gravity = Gravity.CENTER_HORIZONTAL;
        doneParams.topMargin = dp(8);
        root.addView(done, doneParams);
        done.setOnClickListener(v -> finish());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
    }

    private void addNotificationPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, "הרשאת גישה להתראות",
                "כדי להציג את נגן המערכת, יש לאפשר גישה להתראות.");
        notificationStatus = addStatus(card);
        notificationButton = addPermissionButton(card,
                "הפעל הרשאת גישה להתראות", v -> {
                    if (hasNotificationAccess()) {
                        Toast.makeText(this,
                                "כדי לשנות או לבטל את ההרשאה, שנה במסך הבא את גישת ההתראות של שומר מסך.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "כדי להציג את הנגן, צריך לאפשר לשומר מסך גישה להתראות במסך הבא. בחר את \"שומר מסך\" והפעל את ההרשאה.",
                                Toast.LENGTH_LONG).show();
                    }
                    openNotificationAccessSettings();
                });
    }

    private void addOverlayPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, "הרשאת תצוגה על אפליקציות",
                "ההרשאה נדרשת להפעלה אוטומטית של שומר המסך בטעינה או בנעילת המכשיר.");
        overlayStatus = addStatus(card);
        overlayButton = addPermissionButton(card,
                "הפעל הרשאת הצגה מעל אפליקציות", v -> {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this,
                                "כדי לשנות או לבטל את ההרשאה, שנה במסך הבא את הגדרת ההצגה מעל אפליקציות אחרות עבור שומר מסך.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "כדי לאפשר הפעלה אוטומטית בטעינה, בחר במסך הבא את \"שומר מסך\" ואפשר \"הצגה מעל אפליקציות אחרות\".",
                                Toast.LENGTH_LONG).show();
                    }
                    openOverlayPermissionSettings();
                });
    }

    private void addDeviceAdminPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, "הרשאת מנהל מכשיר",
                "ההרשאה מאפשרת לנעול את המכשיר בסיום זמן הפעילות שנבחר.");
        adminStatus = addStatus(card);
        adminButton = addPermissionButton(card,
                "הפעל הרשאת מנהל מכשיר", v -> {
                    if (isAdminActive()) {
                        Toast.makeText(this,
                                "כדי לבטל את ההרשאה, בחר את שומר מסך במסך ניהול מנהלי המכשיר.",
                                Toast.LENGTH_LONG).show();
                        openDeviceAdminSettings();
                    } else {
                        Toast.makeText(this,
                                "כדי לאפשר נעילת המכשיר בסיום זמן הפעילות, אשר במסך הבא את הרשאת מנהל המכשיר.",
                                Toast.LENGTH_LONG).show();
                        requestDeviceAdmin();
                    }
                });
    }

    private LinearLayout makeCard(LinearLayout parent, String titleText, String descriptionText) {
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

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(9);
        parent.addView(card, cardParams);

        TextView heading = textView(titleText, 18, ON_SURFACE);
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        heading.setGravity(Gravity.CENTER);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(heading, new LinearLayout.LayoutParams(-1, dp(42)));

        View separator = new View(this);
        separator.setBackgroundColor(CARD_OUTLINE);
        LinearLayout.LayoutParams separatorParams =
                new LinearLayout.LayoutParams(-1, dp(1));
        separatorParams.setMargins(dp(2), dp(5), dp(2), dp(10));
        card.addView(separator, separatorParams);

        TextView description = textView(descriptionText, 15, ON_SURFACE);
        description.setGravity(Gravity.CENTER);
        description.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        description.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        description.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams descriptionParams =
                new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.bottomMargin = dp(3);
        card.addView(description, descriptionParams);
        return card;
    }

    private TextView addStatus(LinearLayout card) {
        TextView status = textView("", 14, MUTED);
        status.setGravity(Gravity.CENTER);
        status.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        status.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(7);
        params.bottomMargin = dp(1);
        card.addView(status, params);
        return status;
    }

    private Button addPermissionButton(LinearLayout card, String label,
                                       android.view.View.OnClickListener listener) {
        Button button = makeButton(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(9);
        card.addView(button, params);
        // Ongoing permission management: button remains clickable after it is granted.
        button.setEnabled(true);
        button.setOnClickListener(listener);
        return button;
    }

    private void updatePermissionStatuses() {
        boolean notificationGranted = hasNotificationAccess();
        notificationStatus.setText(notificationGranted
                ? "ההרשאה ניתנה — גישה להתראות פעילה."
                : "ההרשאה עדיין לא ניתנה.");
        notificationStatus.setTextColor(notificationGranted ? ACCENT : MUTED);
        notificationButton.setText(notificationGranted
                ? "ניהול הרשאת גישה להתראות"
                : "הפעל הרשאת גישה להתראות");
        notificationButton.setEnabled(true);

        boolean overlayGranted = Settings.canDrawOverlays(this);
        overlayStatus.setText(overlayGranted
                ? "ההרשאה ניתנה — הצגה מעל אפליקציות אחרות פעילה."
                : "ההרשאה עדיין לא ניתנה.");
        overlayStatus.setTextColor(overlayGranted ? ACCENT : MUTED);
        overlayButton.setText(overlayGranted
                ? "ניהול הרשאת הצגה מעל אפליקציות"
                : "הפעל הרשאת הצגה מעל אפליקציות");
        overlayButton.setEnabled(true);

        boolean adminGranted = isAdminActive();
        adminStatus.setText(adminGranted
                ? "ההרשאה ניתנה — הרשאת מנהל המכשיר פעילה."
                : "ההרשאה עדיין לא ניתנה; נעילת המכשיר תתנהל לפי הגדרות המכשיר.");
        adminStatus.setTextColor(adminGranted ? ACCENT : MUTED);
        adminButton.setText(adminGranted
                ? "ניהול הרשאת מנהל מכשיר"
                : "הפעל הרשאת מנהל מכשיר");
        adminButton.setEnabled(true);
    }

    private boolean hasNotificationAccess() {
        ComponentName component = new ComponentName(this, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null && manager.isNotificationListenerAccessGranted(component)) {
                    return true;
                }
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

    private boolean openOverlayPermissionSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
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

    private void requestDeviceAdmin() {
        try {
            if (adminComponent == null) {
                adminComponent = new ComponentName(this, ScreenLockAdminReceiver.class);
            }
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "ההרשאה מאפשרת לנעול את המכשיר בסיום זמן הפעילות שנבחר.");
            startActivity(intent);
        } catch (Throwable ex) {
            Toast.makeText(this, "לא ניתן לפתוח את בקשת הרשאת מנהל המכשיר.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openDeviceAdminSettings() {
        try {
            startActivity(new Intent("android.settings.DEVICE_ADMIN_SETTINGS"));
        } catch (Throwable first) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Throwable ignored) {
                Toast.makeText(this, "לא ניתן לפתוח את הגדרות אבטחת המכשיר.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isAdminActive() {
        try {
            if (devicePolicyManager == null) {
                devicePolicyManager = (DevicePolicyManager)
                        getSystemService(Context.DEVICE_POLICY_SERVICE);
            }
            if (adminComponent == null) {
                adminComponent = new ComponentName(this, ScreenLockAdminReceiver.class);
            }
            return devicePolicyManager != null
                    && devicePolicyManager.isAdminActive(adminComponent);
        } catch (Throwable ignored) {
            return false;
        }
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
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(22));
        button.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(1));
        return button;
    }

    private TextView textView(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
