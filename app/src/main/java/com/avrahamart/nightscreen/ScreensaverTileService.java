package com.avrahamart.nightscreen;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class ScreensaverTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        Runnable action = () -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (MainActivity.isMainActivitySessionAlive()) {
                intent.setAction(MainActivity.ACTION_FINISH_FROM_TILE);
            }
            try {
                startActivityAndCollapse(intent);
            } catch (Throwable ignored) {
                try {
                    startActivity(intent);
                } catch (Throwable ignoredAgain) {
                }
            }
        };

        if (isLocked()) unlockAndRun(action);
        else action.run();
        updateTileState();
    }

    @Override
    public void onStopListening() {
        updateTileState();
        super.onStopListening();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel("שומר מסך");
        tile.setContentDescription("הפעלה או סיום של שומר המסך");
        tile.setState(MainActivity.isMainActivityResumed()
                ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
