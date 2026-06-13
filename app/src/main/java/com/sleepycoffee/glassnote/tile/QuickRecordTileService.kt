package com.sleepycoffee.glassnote.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.ui.RecordActivity

/** Плитка быстрых настроек: открывает стеклянную плашку записи (в т.ч. с локскрина). */
class QuickRecordTileService : TileService() {

    override fun onStartListening() { updateTile() }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, RecordActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pi)
        } else if (isLocked) {
            unlockAndRun { @Suppress("DEPRECATION") startActivityAndCollapse(intent) }
        } else {
            @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (runCatching { RecordingController.isRecording }.getOrDefault(false))
                Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
