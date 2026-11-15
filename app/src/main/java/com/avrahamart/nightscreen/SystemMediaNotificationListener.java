package com.avrahamart.nightscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Observes media-session metadata and can wake the screensaver for a new track.
 * The trigger is deliberately limited to an enabled user setting, the enabled
 * media player, a real charging state and a currently non-interactive screen.
 */
public class SystemMediaNotificationListener extends NotificationListenerService {
    private static final String TAG = "NightScreenMedia";
    private static final long WAKE_THROTTLE_MS = 1_500L;

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Map<String, MediaController> controllers = new HashMap<>();
    private final Map<String, MediaController.Callback> callbacks = new HashMap<>();
    private final Map<String, String> lastTrackBySession = new HashMap<>();
    private final Map<String, String> pendingTrackBySession = new HashMap<>();
    private MediaSessionManager mediaSessionManager;
    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
    private long lastWakeElapsedRealtime;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        NightScreenApplication.syncChargerReceiver(this);
        registerMediaSessions();
    }

    @Override
    public void onListenerDisconnected() {
        unregisterMediaSessions();
        NightScreenApplication.syncChargerReceiver(this);
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        unregisterMediaSessions();
        super.onDestroy();
    }

    private void registerMediaSessions() {
        if (mediaSessionManager != null) return;
        try {
            mediaSessionManager = (MediaSessionManager)
                    getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) return;
            sessionsListener = this::replaceActiveControllers;
            ComponentName listener = new ComponentName(
                    this, SystemMediaNotificationListener.class);
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionsListener, listener, handler);
            replaceActiveControllers(mediaSessionManager.getActiveSessions(listener));
        } catch (SecurityException error) {
            Log.w(TAG, "Media session access is not available.", error);
            unregisterMediaSessions();
        } catch (Throwable error) {
            Log.w(TAG, "Could not observe active media sessions.", error);
        }
    }

    private void unregisterMediaSessions() {
        if (mediaSessionManager != null && sessionsListener != null) {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
            } catch (Throwable ignored) {
            }
        }
        for (Map.Entry<String, MediaController> entry : controllers.entrySet()) {
            MediaController.Callback callback = callbacks.get(entry.getKey());
            if (callback != null) {
                try {
                    entry.getValue().unregisterCallback(callback);
                } catch (Throwable ignored) {
                }
            }
        }
        controllers.clear();
        callbacks.clear();
        lastTrackBySession.clear();
        pendingTrackBySession.clear();
        sessionsListener = null;
        mediaSessionManager = null;
    }

    private void replaceActiveControllers(List<MediaController> active) {
        Map<String, MediaController> next = new HashMap<>();
        if (active != null) {
            for (MediaController controller : active) {
                if (controller == null) continue;
                next.put(sessionKey(controller), controller);
            }
        }

        for (String oldKey : new ArrayList<>(controllers.keySet())) {
            if (next.containsKey(oldKey)) continue;
            MediaController old = controllers.remove(oldKey);
            MediaController.Callback callback = callbacks.remove(oldKey);
            if (old != null && callback != null) {
                try {
                    old.unregisterCallback(callback);
                } catch (Throwable ignored) {
                }
            }
            lastTrackBySession.remove(oldKey);
            pendingTrackBySession.remove(oldKey);
        }

        for (Map.Entry<String, MediaController> entry : next.entrySet()) {
            final String key = entry.getKey();
            final MediaController controller = entry.getValue();
            if (controllers.containsKey(key)) continue;

            MediaController.Callback callback = new MediaController.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    onTrackMetadataChanged(controller, key, metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    if (state == null || state.getState() != PlaybackState.STATE_PLAYING) return;
                    // Some players publish the next track metadata while buffering.
                    // Remember it and trigger only after the player actually starts.
                    if (pendingTrackBySession.remove(key) != null) {
                        wakeForTrackChange();
                    }
                }

                @Override
                public void onSessionDestroyed() {
                    removeController(key);
                }
            };

            controllers.put(key, controller);
            callbacks.put(key, callback);
            try {
                controller.registerCallback(callback, handler);
                // Establish an initial baseline without treating session discovery
                // or notification-listener reconnection as a track transition.
                String baseline = trackSignature(controller.getMetadata());
                if (baseline != null) lastTrackBySession.put(key, baseline);
            } catch (Throwable error) {
                controllers.remove(key);
                callbacks.remove(key);
                Log.w(TAG, "Could not register a media session callback.", error);
            }
        }
    }

    private void onTrackMetadataChanged(MediaController controller, String key,
                                        MediaMetadata metadata) {
        String signature = trackSignature(metadata);
        if (signature == null) return;
        String previous = lastTrackBySession.put(key, signature);
        if (previous == null || previous.equals(signature)) return;

        PlaybackState state;
        try {
            state = controller.getPlaybackState();
        } catch (Throwable ignored) {
            return;
        }
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
            pendingTrackBySession.remove(key);
            wakeForTrackChange();
        } else {
            pendingTrackBySession.put(key, signature);
        }
    }

    private void removeController(String key) {
        MediaController controller = controllers.remove(key);
        MediaController.Callback callback = callbacks.remove(key);
        if (controller != null && callback != null) {
            try {
                controller.unregisterCallback(callback);
            } catch (Throwable ignored) {
            }
        }
        lastTrackBySession.remove(key);
        pendingTrackBySession.remove(key);
    }

    private String sessionKey(MediaController controller) {
        try {
            return controller.getPackageName() + "|"
                    + controller.getSessionToken().hashCode();
        } catch (Throwable ignored) {
            return controller.getPackageName();
        }
    }

    private String trackSignature(MediaMetadata metadata) {
        if (metadata == null) return null;
        String mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
        String signature = safe(mediaId) + "|" + safe(title) + "|"
                + safe(artist) + "|" + safe(album);
        return "|||".equals(signature) ? null : signature;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void wakeForTrackChange() {
        MonitorSettingsSnapshot settings = MonitorSettingsSnapshot.read(this);
        if (settings == null || !settings.wakeOnTrackChange || !settings.showPlayer) return;
        // Track-change wake is an automatic activation too: apply the same
        // foreground-app exclusions before waking the display or launching.
        if (!AutoStartConditions.canActivate(this, settings)) return;
        if (android.os.Build.VERSION.SDK_INT >= 29 && !android.provider.Settings.canDrawOverlays(this)) {
            return;
        }
        if (!isCharging()) return;

        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (power == null || power.isInteractive()) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastWakeElapsedRealtime < WAKE_THROTTLE_MS) return;
        lastWakeElapsedRealtime = now;

        MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(this, true);
        try {
            PowerManager.WakeLock wakeLock = power.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "NightScreen:TrackChange");
            wakeLock.acquire(5_000L);
        } catch (Throwable error) {
            Log.w(TAG, "Could not acquire the track-change wake lock.", error);
        }

        Intent launch = new Intent(this, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_TRACK_CHANGE, true);
        try {
            startActivity(launch);
        } catch (Throwable error) {
            MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(this, false);
            Log.w(TAG, "Android blocked the track-change screensaver launch.", error);
        }
    }

    private boolean isCharging() {
        try {
            Intent battery = registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return false;
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            return plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
