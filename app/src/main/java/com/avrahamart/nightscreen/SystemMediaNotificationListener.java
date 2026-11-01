package com.avrahamart.nightscreen;

import android.service.notification.NotificationListenerService;

/** Lightweight system-bound process for media and charger event access. */
public class SystemMediaNotificationListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        NightScreenApplication.syncChargerReceiver(this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        NightScreenApplication.syncChargerReceiver(this);
    }
}
