package com.avrahamart.nightscreen;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ScreensaverTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        Runnable launch = () -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivityAndCollapse(intent);
            } catch (Throwable ignored) {
                startActivity(intent);
            }
        };

        if (isLocked()) {
            unlockAndRun(launch);
        } else {
            launch.run();
        }
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onStopListening() {
        updateTileState();
        super.onStopListening();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(MainActivity.isMainActivityResumed()
                ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
