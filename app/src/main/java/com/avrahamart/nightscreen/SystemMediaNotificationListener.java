package com.avrahamart.nightscreen;

import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** System-bound listener for media notifications and monitor-notification diagnostics. */
public class SystemMediaNotificationListener extends NotificationListenerService {
    private static final int MONITOR_NOTIFICATION_ID = 6721;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        MonitorDiagnostics.record(this, "notification listener connected; processPid="
                + android.os.Process.myPid());
        MonitorDiagnostics.recordNotificationState(this, "notification listener connected");
    }

    @Override
    public void onListenerDisconnected() {
        MonitorDiagnostics.record(this, "notification listener disconnected; processPid="
                + android.os.Process.myPid());
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        recordMonitorNotification("POSTED", sbn, -1);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        recordMonitorNotification("REMOVED", sbn, -1);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        recordMonitorNotification("POSTED", sbn, -1);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn,
                                      RankingMap rankingMap, int reason) {
        super.onNotificationRemoved(sbn, rankingMap, reason);
        recordMonitorNotification("REMOVED", sbn, reason);
    }

    private void recordMonitorNotification(String event,
                                           StatusBarNotification sbn, int reason) {
        if (sbn == null || !getPackageName().equals(sbn.getPackageName())
                || sbn.getId() != MONITOR_NOTIFICATION_ID) {
            return;
        }
        String details = "event=" + event
                + ", key=" + sbn.getKey()
                + ", tag=" + sbn.getTag()
                + ", ongoing=" + sbn.isOngoing()
                + ", flags=" + sbn.getNotification().flags
                + ", reason=" + reason
                + ", postTime=" + sbn.getPostTime()
                + ", listenerPid=" + android.os.Process.myPid();
        MonitorDiagnostics.record(this, "monitor notification listener event; " + details);
        if ("REMOVED".equals(event)) {
            MonitorDiagnostics.recordNotificationState(this,
                    "notification listener observed removal; reason=" + reason);
        }
    }
}
