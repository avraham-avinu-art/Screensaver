package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.AlertDialog;
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
    private static final int CONTROL_SURFACE = Color.rgb(27, 47, 70);
    private static final int ON_SURFACE = Color.rgb(239, 246, 255);
    private static final int MUTED = Color.rgb(156, 178, 203);
    private static final int CARD_OUTLINE = Color.rgb(42, 67, 95);
    private static final int ACCENT = Color.rgb(70, 200, 244);

    private TextView notificationStatus;
    private TextView usageStatus;
    private TextView overlayStatus;
    private TextView adminStatus;
    private Button notificationButton;
    private Button usageButton;
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
        root.setPadding(dp(10), dp(2), dp(10), 0);
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int topInset = insets.getSystemWindowInsetTop();
                int bottomInset = insets.getSystemWindowInsetBottom();
                view.setPadding(dp(10), dp(2) + topInset, dp(10), bottomInset);
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

        TextView title = textView(getString(R.string.permissions_activity_text_001), 23, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setIncludeFontPadding(false);
        title.setLetterSpacing(-0.015f);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setTextDirection(View.TEXT_DIRECTION_RTL);
        titleBlock.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = textView(getString(R.string.permissions_activity_text_002), 16, MUTED);
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
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setClipToPadding(false);

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        cards.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        cards.setTextDirection(View.TEXT_DIRECTION_RTL);

        addNotificationPermissionCard(cards);
        addUsageAccessPermissionCard(cards);
        addOverlayPermissionCard(cards);
        addDeviceAdminPermissionCard(cards);

        scroll.addView(cards, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String disabledFeatures =
                PermissionDependencySync.disableFeaturesWithoutPermissions(this);
        if (!disabledFeatures.isEmpty()) {
            AppToast.makeText(this, disabledFeatures, Toast.LENGTH_LONG).show();
        }
        updatePermissionStatuses();
    }

    private void addNotificationPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, getString(R.string.permissions_activity_text_003),
                getString(R.string.permissions_activity_text_004));
        notificationStatus = addStatus(card);
        notificationButton = addPermissionButton(card,
                getString(R.string.permissions_activity_text_005), v -> {
                    if (hasNotificationAccess()) {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_006),
                                Toast.LENGTH_LONG).show();
                    } else {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_007),
                                Toast.LENGTH_LONG).show();
                    }
                    openNotificationAccessSettings();
                });
    }

    private void addUsageAccessPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, getString(R.string.permissions_activity_text_008),
                getString(R.string.permissions_activity_text_009));
        usageStatus = addStatus(card);
        usageButton = addPermissionButton(card,
                getString(R.string.permissions_activity_text_010), v -> {
                    if (hasUsageAccess()) {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_011),
                                Toast.LENGTH_LONG).show();
                    } else {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_012),
                                Toast.LENGTH_LONG).show();
                    }
                    openUsageAccessSettings();
                });
    }

    private void addOverlayPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, getString(R.string.permissions_activity_text_013),
                getString(R.string.permissions_activity_text_014));
        overlayStatus = addStatus(card);
        overlayButton = addPermissionButton(card,
                getString(R.string.permissions_activity_text_015), v -> {
                    if (Settings.canDrawOverlays(this)) {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_016),
                                Toast.LENGTH_LONG).show();
                    } else {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_017),
                                Toast.LENGTH_LONG).show();
                    }
                    openOverlayPermissionSettings();
                });
    }

    private void addDeviceAdminPermissionCard(LinearLayout parent) {
        LinearLayout card = makeCard(parent, getString(R.string.permissions_activity_text_018),
                getString(R.string.permissions_activity_text_019));
        adminStatus = addStatus(card);
        adminButton = addPermissionButton(card,
                getString(R.string.permissions_activity_text_020), v -> {
                    if (isAdminActive()) {
                        confirmDeviceAdminRemoval();
                    } else {
                        AppToast.makeText(this,
                                getString(R.string.permissions_activity_text_021),
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
                ? getString(R.string.permissions_activity_text_022)
                : getString(R.string.permissions_activity_text_023));
        notificationStatus.setTextColor(notificationGranted ? ACCENT : MUTED);
        notificationButton.setText(notificationGranted
                ? getString(R.string.permissions_activity_text_024)
                : getString(R.string.permissions_activity_text_005));
        notificationButton.setEnabled(true);

        boolean usageGranted = hasUsageAccess();
        usageStatus.setText(usageGranted
                ? getString(R.string.permissions_activity_text_025)
                : getString(R.string.permissions_activity_text_023));
        usageStatus.setTextColor(usageGranted ? ACCENT : MUTED);
        usageButton.setText(usageGranted
                ? getString(R.string.permissions_activity_text_026)
                : getString(R.string.permissions_activity_text_010));
        usageButton.setEnabled(true);

        boolean overlayGranted = Settings.canDrawOverlays(this);
        overlayStatus.setText(overlayGranted
                ? getString(R.string.permissions_activity_text_027)
                : getString(R.string.permissions_activity_text_023));
        overlayStatus.setTextColor(overlayGranted ? ACCENT : MUTED);
        overlayButton.setText(overlayGranted
                ? getString(R.string.permissions_activity_text_028)
                : getString(R.string.permissions_activity_text_015));
        overlayButton.setEnabled(true);

        boolean adminGranted = isAdminActive();
        adminStatus.setText(adminGranted
                ? getString(R.string.permissions_activity_text_029)
                : getString(R.string.permissions_activity_text_030));
        adminStatus.setTextColor(adminGranted ? ACCENT : MUTED);
        adminButton.setText(adminGranted
                ? getString(R.string.permissions_activity_text_031)
                : getString(R.string.permissions_activity_text_020));
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
            AppToast.makeText(this, getString(R.string.permissions_activity_text_032),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            AppToast.makeText(this, getString(R.string.permissions_activity_text_032),
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager manager =
                    (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            return manager != null && manager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean openUsageAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return true;
        } catch (ActivityNotFoundException ignored) {
            AppToast.makeText(this, getString(R.string.permissions_activity_text_033),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            AppToast.makeText(this, getString(R.string.permissions_activity_text_033),
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
            AppToast.makeText(this,
                    getString(R.string.permissions_activity_text_034),
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
                    getString(R.string.permissions_activity_text_019));
            startActivity(intent);
        } catch (Throwable ex) {
            AppToast.makeText(this, getString(R.string.permissions_activity_text_035),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeviceAdminRemoval() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setTextDirection(View.TEXT_DIRECTION_RTL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(16));
        panel.setBackground(roundOutline(CARD_SURFACE, dp(22), CARD_OUTLINE));

        TextView title = textView(getString(R.string.permissions_activity_text_036), 19, ON_SURFACE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        title.setIncludeFontPadding(false);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView message = textView(
                getString(R.string.permissions_activity_text_037),
                15, MUTED);
        message.setGravity(Gravity.RIGHT | Gravity.TOP);
        message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        message.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(12);
        panel.addView(message, messageParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(-1, dp(44));
        actionsParams.topMargin = dp(18);

        Button cancel = makeButton(getString(R.string.permissions_activity_text_038));
        cancel.setBackground(roundOutline(CONTROL_SURFACE, dp(20), CARD_OUTLINE));
        cancel.setTextColor(ON_SURFACE);
        LinearLayout.LayoutParams cancelParams =
                new LinearLayout.LayoutParams(0, -1, 1f);
        cancelParams.rightMargin = dp(6);
        actions.addView(cancel, cancelParams);
        cancel.setOnClickListener(v -> dialog.dismiss());

        Button confirm = makeButton(getString(R.string.permissions_activity_text_039));
        LinearLayout.LayoutParams confirmParams =
                new LinearLayout.LayoutParams(0, -1, 1f);
        confirmParams.leftMargin = dp(6);
        actions.addView(confirm, confirmParams);
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            removeDeviceAdmin();
        });

        panel.addView(actions, actionsParams);
        dialog.setView(panel);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(dp(420), metrics.widthPixels - dp(36));
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.62f;
            window.setAttributes(params);
        }
    }

    private void removeDeviceAdmin() {
        if (!isAdminActive()) {
            updatePermissionStatuses();
            return;
        }

        try {
            if (devicePolicyManager == null) {
                devicePolicyManager = (DevicePolicyManager)
                        getSystemService(Context.DEVICE_POLICY_SERVICE);
            }
            if (adminComponent == null) {
                adminComponent = new ComponentName(this, ScreenLockAdminReceiver.class);
            }
            if (devicePolicyManager == null) {
                throw new IllegalStateException("Device policy service is unavailable");
            }

            // The app owns this admin component, so Android allows it to request
            // removal directly. The system completes removal asynchronously.
            devicePolicyManager.removeActiveAdmin(adminComponent);

            adminButton.setEnabled(false);
            adminButton.setText(getString(R.string.permissions_activity_text_040));
            adminStatus.setText(getString(R.string.permissions_activity_text_041));
            adminStatus.setTextColor(MUTED);
            adminButton.postDelayed(() -> verifyDeviceAdminRemoval(0), 250);
        } catch (SecurityException ex) {
            updatePermissionStatuses();
            AppToast.makeText(this,
                    getString(R.string.permissions_activity_text_042),
                    Toast.LENGTH_LONG).show();
            openSecuritySettingsFallback();
        } catch (Throwable ex) {
            updatePermissionStatuses();
            AppToast.makeText(this,
                    getString(R.string.permissions_activity_text_043),
                    Toast.LENGTH_LONG).show();
            openSecuritySettingsFallback();
        }
    }

    private void verifyDeviceAdminRemoval(int attempt) {
        if (isFinishing() || isDestroyed()) return;

        if (!isAdminActive()) {
            updatePermissionStatuses();
            AppToast.makeText(this, getString(R.string.permissions_activity_text_044),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // removeActiveAdmin() is explicitly asynchronous; keep checking the system
        // state instead of assuming that the permission disappears immediately.
        if (attempt < 20) {
            adminButton.postDelayed(
                    () -> verifyDeviceAdminRemoval(attempt + 1), 250);
            return;
        }

        updatePermissionStatuses();
        AppToast.makeText(this,
                getString(R.string.permissions_activity_text_045),
                Toast.LENGTH_LONG).show();
        openSecuritySettingsFallback();
    }

    private void openSecuritySettingsFallback() {
        try {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignoredAgain) {
                AppToast.makeText(this,
                        getString(R.string.permissions_activity_text_046),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Throwable ignored) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignoredAgain) {
                AppToast.makeText(this,
                        getString(R.string.permissions_activity_text_046),
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