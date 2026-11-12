package io.clockbeacon.minilock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * The single-process preference endpoint for the isolated beacon engine.
 * The engine lives in :beaconengine and never reads a cached SharedPreferences
 * instance belonging to a different process.
 */
public final class BeaconControlProvider extends ContentProvider {
    static final String AUTHORITY = "io.clockbeacon.minilock.control";
    static final String METHOD_FETCH = "beacon.fetch-state";
    static final String METHOD_STORE = "beacon.store-state";
    static final String KEY_RUNNING = "beacon_enabled";

    private static final String PREFS_NAME = "beacon_control_state";

    @Override
    public boolean onCreate() {
        return getContext() != null;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) return null;

        SharedPreferences state =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (METHOD_FETCH.equals(method)) {
            Bundle response = new Bundle();
            response.putBoolean(KEY_RUNNING, state.getBoolean(KEY_RUNNING, false));
            return response;
        }

        if (METHOD_STORE.equals(method)) {
            boolean enabled = extras != null && extras.getBoolean(KEY_RUNNING, false);
            boolean saved = state.edit().putBoolean(KEY_RUNNING, enabled).commit();
            Bundle response = new Bundle();
            response.putBoolean("stored", saved);
            response.putBoolean(KEY_RUNNING, enabled);
            return response;
        }

        return super.call(method, arg, extras);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { return 0; }
}
