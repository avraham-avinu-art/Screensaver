package com.avrahamart.nightscreen;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/**
 * Android Dream host for the shared screensaver renderer.
 *
 * The Activity and DreamService deliberately use the exact same
 * ScreensaverDisplayView. This class owns only Dream lifecycle and system-media
 * session access; it contains no independent clock, date, layout or player UI.
 */
public final class ScreensaverDreamService extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private ScreensaverDisplayView displayView;

    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;
    private MediaController.Callback mediaCallback;
    private MediaSessionManager.OnActiveSessionsChangedListener activeSessionsListener;
    private boolean activeSessionsListenerRegistered;
    private boolean dreamActive;

    private String lastTrackIdentity;
    private boolean introWindowVisible;
    private long introWindowDeadline;
    private long currentMediaDurationMs;
    private boolean lastPlaybackActive;
    private boolean lastControlsIntroOnly;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!dreamActive || displayView == null) return;
            displayView.updateClock();
            updateMediaProgress();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);
        hideSystemUi();

        displayView = new ScreensaverDisplayView(this, prefs,
                new ScreensaverDisplayView.Actions() {
                    @Override public void onOpenSettings() {
                        openSettings();
                    }

                    @Override public void onPlayPause() {
                        dispatchPlayPause();
                    }

                    @Override public void onPrevious() {
                        dispatchPrevious();
                    }

                    @Override public void onNext() {
                        dispatchNext();
                    }

                    @Override public void onSeekProgress(int progress) {
                        seekToProgress(progress);
                    }
                });

        displayView.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                displayView.showSettingsButtonForThreeSeconds();
            }
            return false;
        });

        setContentView(displayView);
        hideSystemUi();
        displayView.applySettings(getWindow(), false, null);
        displayView.updateClock();

        preferenceListener = (sharedPreferences, key) -> {
            if (!dreamActive || displayView == null) return;
            displayView.applySettings(getWindow(), hasUsableMediaSession(mediaController),
                    currentArtwork());
            if ("showPlayer".equals(key) || "showPlayerOnlyAtStart".equals(key)
                    || "playerControlsDisplayMode".equals(key)
                    || "showDetails".equals(key)) {
                refreshMedia(true);
            }
        };
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        hideSystemUi();
        dreamActive = true;
        if (prefs != null && preferenceListener != null) {
            prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        }
        registerActiveSessionsListener();
        handler.removeCallbacks(tick);
        handler.post(tick);
        refreshMedia(true);
    }

    @Override
    public void onDreamingStopped() {
        dreamActive = false;
        handler.removeCallbacks(tick);
        if (prefs != null && preferenceListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
        unregisterActiveSessionsListener();
        detachMediaController();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        dreamActive = false;
        handler.removeCallbacks(tick);
        unregisterActiveSessionsListener();
        detachMediaController();
        if (prefs != null && preferenceListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
        displayView = null;
        super.onDetachedFromWindow();
    }

    private void hideSystemUi() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Throwable ignored) {
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable ignored) {
        }
        finish();
    }

    private boolean getBool(String key, boolean fallback) {
        return prefs != null && prefs.getBoolean(key, fallback);
    }

    private int getInt(String key, int fallback) {
        return prefs == null ? fallback : prefs.getInt(key, fallback);
    }

    private String getString(String key, String fallback) {
        return prefs == null ? fallback : prefs.getString(key, fallback);
    }

    private boolean hasNotificationAccess() {
        ComponentName component = new ComponentName(this, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager = getSystemService(NotificationManager.class);
                return manager != null && manager.isNotificationListenerAccessGranted(component);
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

    private void registerActiveSessionsListener() {
        if (Build.VERSION.SDK_INT < 21 || activeSessionsListenerRegistered
                || !getBool("showPlayer", false)) return;
        try {
            if (mediaSessionManager == null) {
                mediaSessionManager = getSystemService(MediaSessionManager.class);
            }
            if (mediaSessionManager == null || !hasNotificationAccess()) return;
            ComponentName listener = new ComponentName(this, SystemMediaNotificationListener.class);
            activeSessionsListener = controllers -> {
                if (dreamActive) handler.post(() -> refreshMedia(true));
            };
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    activeSessionsListener, listener, handler);
            activeSessionsListenerRegistered = true;
        } catch (Throwable ignored) {
            activeSessionsListener = null;
            activeSessionsListenerRegistered = false;
        }
    }

    private void unregisterActiveSessionsListener() {
        if (mediaSessionManager == null || activeSessionsListener == null
                || !activeSessionsListenerRegistered) {
            activeSessionsListener = null;
            activeSessionsListenerRegistered = false;
            return;
        }
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener);
        } catch (Throwable ignored) {
        }
        activeSessionsListener = null;
        activeSessionsListenerRegistered = false;
    }

    private void refreshMedia(boolean inspectSessions) {
        if (!dreamActive || displayView == null) return;
        if (!getBool("showPlayer", false) || !hasNotificationAccess()) {
            detachMediaController();
            hideMedia();
            return;
        }

        try {
            if (mediaSessionManager == null) {
                mediaSessionManager = getSystemService(MediaSessionManager.class);
            }
            if (mediaSessionManager == null) {
                hideMedia();
                return;
            }

            ComponentName listener = new ComponentName(this, SystemMediaNotificationListener.class);
            MediaController selected = null;
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    MediaSession.Token token = mediaSessionManager.getMediaKeyEventSession();
                    if (token != null) {
                        MediaController candidate = new MediaController(this, token);
                        if (hasUsableMediaSession(candidate)) selected = candidate;
                    }
                } catch (Throwable ignored) {
                }
            }

            if (selected == null) {
                List<MediaController> sessions = mediaSessionManager.getActiveSessions(listener);
                selected = selectBestSession(sessions);
            }

            if (selected == null) {
                detachMediaController();
                hideMedia();
                return;
            }

            if (mediaController == null
                    || !mediaController.getSessionToken().equals(selected.getSessionToken())) {
                attachMediaController(selected);
            }
            renderMedia(selected);
        } catch (SecurityException ignored) {
            detachMediaController();
            hideMedia();
        } catch (Throwable ignored) {
            hideMedia();
        }
    }

    private MediaController selectBestSession(List<MediaController> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;
        for (MediaController item : sessions) {
            PlaybackState state = item.getPlaybackState();
            if (state != null && (state.getState() == PlaybackState.STATE_PLAYING
                    || state.getState() == PlaybackState.STATE_BUFFERING)) return item;
        }
        for (MediaController item : sessions) {
            PlaybackState state = item.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PAUSED
                    && hasMetadataTitle(item.getMetadata())) return item;
        }
        for (MediaController item : sessions) {
            if (hasMetadataTitle(item.getMetadata())) return item;
        }
        return null;
    }

    private boolean hasMetadataTitle(MediaMetadata metadata) {
        if (metadata == null) return false;
        String title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        }
        return title != null && !title.trim().isEmpty();
    }

    private boolean hasUsableMediaSession(MediaController controller) {
        if (controller == null) return false;
        try {
            PlaybackState state = controller.getPlaybackState();
            if (state != null) {
                int value = state.getState();
                if (value == PlaybackState.STATE_PLAYING
                        || value == PlaybackState.STATE_BUFFERING
                        || value == PlaybackState.STATE_PAUSED) return true;
            }
            return hasMetadataTitle(controller.getMetadata());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void attachMediaController(MediaController selected) {
        detachMediaController();
        mediaController = selected;
        mediaCallback = new MediaController.Callback() {
            @Override public void onPlaybackStateChanged(PlaybackState state) {
                handler.post(() -> refreshMedia(false));
            }

            @Override public void onMetadataChanged(MediaMetadata metadata) {
                handler.post(() -> refreshMedia(false));
            }

            @Override public void onSessionDestroyed() {
                handler.post(() -> {
                    detachMediaController();
                    hideMedia();
                });
            }
        };
        try {
            mediaController.registerCallback(mediaCallback, handler);
        } catch (Throwable ignored) {
        }
    }

    private void detachMediaController() {
        if (mediaController != null && mediaCallback != null) {
            try {
                mediaController.unregisterCallback(mediaCallback);
            } catch (Throwable ignored) {
            }
        }
        mediaController = null;
        mediaCallback = null;
        currentMediaDurationMs = 0L;
        lastTrackIdentity = null;
        introWindowVisible = false;
        introWindowDeadline = 0L;
    }

    private void renderMedia(MediaController selected) {
        if (displayView == null || selected == null) return;
        PlaybackState state = selected.getPlaybackState();
        MediaMetadata metadata = selected.getMetadata();
        if (state == null) {
            hideMedia();
            return;
        }

        int playback = state.getState();
        if (playback != PlaybackState.STATE_PLAYING
                && playback != PlaybackState.STATE_BUFFERING
                && playback != PlaybackState.STATE_PAUSED) {
            hideMedia();
            return;
        }

        String title = metadata == null ? null
                : metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = metadata == null ? null
                    : metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        }
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.main_activity_text_027);
        }
        title = removeFileExtension(title);

        String artist = metadata == null ? "" :
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        artist = artist == null ? "" : artist.trim();
        if (artist.equalsIgnoreCase("unknown artist")
                || artist.equalsIgnoreCase("unknown")
                || artist.equalsIgnoreCase("artist unknown")
                || artist.equals(getString(R.string.main_activity_text_028))
                || artist.equals(getString(R.string.main_activity_text_029))
                || artist.equals(getString(R.string.main_activity_text_030))) {
            artist = "";
        }

        long duration = metadata == null ? 0L
                : Math.max(0L, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
        currentMediaDurationMs = duration;
        long position = Math.max(0L, state.getPosition());
        if (duration > 0L) position = Math.min(position, duration);

        String mediaId = metadata == null ? null
                : metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        String identity = mediaId != null && !mediaId.trim().isEmpty()
                ? "id:" + mediaId.trim() : "title:" + title.trim();
        boolean playing = playback == PlaybackState.STATE_PLAYING
                || playback == PlaybackState.STATE_BUFFERING;
        updateIntroWindow(identity, playing, position);

        boolean introOnly = getBool("showPlayerOnlyAtStart", false);
        boolean playerVisible = !introOnly || isIntroWindowOpen();
        displayView.setMediaText(title, artist, getBool("showDetails", true) && !artist.isEmpty());
        displayView.setMediaArtwork(currentArtwork(), true);
        displayView.setPlayerVisible(playerVisible);

        String controlsMode = getString("playerControlsDisplayMode", "on");
        boolean controlsVisible = "on".equals(controlsMode)
                || ("start".equals(controlsMode) && isIntroWindowOpen());
        long actions = state.getActions();
        displayView.setMediaControls(playerVisible && controlsVisible,
                playback == PlaybackState.STATE_PLAYING,
                (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
                (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0L);

        boolean showProgress = playerVisible && controlsVisible && duration > 0L;
        displayView.setProgressRowVisible(showProgress);
        if (showProgress) {
            displayView.updateMediaProgress(position, duration);
        }
    }

    private void updateIntroWindow(String identity, boolean playbackActive, long positionMs) {
        long windowMs = PlayerStartDuration.millis(getInt("playerStartDurationSeconds", 6));
        boolean first = lastTrackIdentity == null;
        boolean changed = !first && !identity.equals(lastTrackIdentity);
        boolean restarted = !first && !changed
                && lastTrackPositionMs > windowMs
                && positionMs < windowMs
                && lastTrackPositionMs - positionMs > 5000L;
        boolean began = playbackActive && !lastPlaybackActive;
        boolean introOnly = getBool("showPlayerOnlyAtStart", false);
        boolean controlsAtStart = "start".equals(getString("playerControlsDisplayMode", "on"));
        boolean introNeeded = introOnly || controlsAtStart;

        if (!introNeeded) {
            introWindowVisible = false;
            introWindowDeadline = 0L;
        } else if (playbackActive && (first || changed || restarted || began)
                && positionMs < windowMs) {
            introWindowVisible = true;
            introWindowDeadline = android.os.SystemClock.elapsedRealtime()
                    + Math.max(1L, windowMs - positionMs);
        }

        lastTrackIdentity = identity;
        lastTrackPositionMs = positionMs;
        lastPlaybackActive = playbackActive;

        if (introWindowVisible && !isIntroWindowOpen()) {
            introWindowVisible = false;
            introWindowDeadline = 0L;
        }
    }

    private long lastTrackPositionMs = -1L;

    private boolean isIntroWindowOpen() {
        return introWindowVisible
                && android.os.SystemClock.elapsedRealtime() < introWindowDeadline;
    }

    private void updateMediaProgress() {
        if (displayView == null || mediaController == null || currentMediaDurationMs <= 0L) return;
        try {
            PlaybackState state = mediaController.getPlaybackState();
            if (state == null || state.getState() != PlaybackState.STATE_PLAYING) return;
            long position = Math.max(0L, state.getPosition());
            long updated = state.getLastPositionUpdateTime();
            if (updated > 0L) {
                long elapsed = Math.max(0L,
                        android.os.SystemClock.elapsedRealtime() - updated);
                position += Math.round(elapsed * Math.max(0f, state.getPlaybackSpeed()));
            }
            displayView.updateMediaProgress(
                    Math.min(currentMediaDurationMs, position), currentMediaDurationMs);
        } catch (Throwable ignored) {
        }
    }

    private Bitmap currentArtwork() {
        if (mediaController == null) return null;
        try {
            MediaMetadata metadata = mediaController.getMetadata();
            Bitmap artwork = metadata == null ? null
                    : metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (artwork == null && metadata != null) {
                artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            }
            if (artwork == null && metadata != null) {
                artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            }
            return artwork != null && !artwork.isRecycled() ? artwork : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void hideMedia() {
        if (displayView == null) return;
        displayView.setPlayerVisible(false);
        displayView.setMediaText("", "", false);
        displayView.setMediaArtwork(null, false);
        displayView.setMediaControls(false, false, false, false);
        displayView.setProgressRowVisible(false);
    }

    private String removeFileExtension(String title) {
        if (title == null) return "";
        String value = title.trim();
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        if (dot > slash + 0 && dot < value.length() - 1) {
            return value.substring(0, dot);
        }
        return value;
    }

    private void seekToProgress(int progress) {
        try {
            if (mediaController != null && currentMediaDurationMs > 0L) {
                mediaController.getTransportControls().seekTo(
                        currentMediaDurationMs * Math.max(0, Math.min(1000, progress)) / 1000L);
            }
        } catch (Throwable ignored) {
        }
    }

    private void dispatchPlayPause() {
        try {
            if (mediaController != null) {
                PlaybackState state = mediaController.getPlaybackState();
                if (state != null) {
                    if (state.getState() == PlaybackState.STATE_PLAYING
                            && (state.getActions() & PlaybackState.ACTION_PAUSE) != 0L) {
                        mediaController.getTransportControls().pause();
                        return;
                    }
                    if (state.getState() != PlaybackState.STATE_PLAYING
                            && (state.getActions() & PlaybackState.ACTION_PLAY) != 0L) {
                        mediaController.getTransportControls().play();
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    private void dispatchPrevious() {
        try {
            if (mediaController != null) {
                mediaController.getTransportControls().skipToPrevious();
                handler.postDelayed(() -> refreshMedia(true), 250L);
                return;
            }
        } catch (Throwable ignored) {
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    private void dispatchNext() {
        try {
            if (mediaController != null) {
                mediaController.getTransportControls().skipToNext();
                handler.postDelayed(() -> refreshMedia(true), 250L);
                return;
            }
        } catch (Throwable ignored) {
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    private void dispatchMediaKey(int keyCode) {
        try {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return;
            long now = System.currentTimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            handler.postDelayed(() -> refreshMedia(true), 300L);
        } catch (Throwable ignored) {
        }
    }
}
