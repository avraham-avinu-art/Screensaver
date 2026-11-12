package io.clockbeacon.minilock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

final class BeaconScreenEventReceiver extends BroadcastReceiver {
    private final BeaconPulseService engine;

    BeaconScreenEventReceiver(BeaconPulseService engine) {
        this.engine = engine;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            engine.onDisplayAwake();
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            engine.hideClockface();
        }
    }
}
