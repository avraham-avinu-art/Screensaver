package com.avrahamart.nightscreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Build;
import android.util.Log;

/**
 * Switches the current app task's visibility in Android Recents according to
 * which top-level screen the user is viewing.
 */
final class RecentTaskVisibility {
    private static final String TAG = "NightScreenRecents";

    private RecentTaskVisibility() {
    }

    static void setExcluded(Activity activity, boolean excluded) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        try {
            ActivityManager activityManager = (ActivityManager)
                    activity.getSystemService(Activity.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return;
            }

            int currentTaskId = activity.getTaskId();
            for (ActivityManager.AppTask appTask : activityManager.getAppTasks()) {
                ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                if (taskInfo != null && taskInfo.id == currentTaskId) {
                    appTask.setExcludeFromRecents(excluded);
                    return;
                }
            }

            Log.w(TAG, "Current app task was not found; taskId=" + currentTaskId
                    + ", excluded=" + excluded);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to update Recents visibility; excluded=" + excluded, error);
        }
    }
}
