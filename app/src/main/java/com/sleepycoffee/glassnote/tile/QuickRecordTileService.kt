package com.sleepycoffee.glassnote.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService

/** Плитка «Быстрая заметка»: тап — старт записи (плитка становится активной),
 *  повторный тап — стоп + сохранение. Приложение НЕ открывается, экран остаётся
 *  заблокированным. Доступна и из шторки на экране блокировки. */
class QuickRecordTileService : TileService() {

    override fun onStartListening() { updateTile() }

    override fun onClick() {
        super.onClick()
        RecordingController.init(applicationContext)
        RecordingService.toggle(applicationContext)
        // небольшая задержка не нужна: подсветим оптимистично по будущему состоянию
        val willRecord = !runCatching { RecordingController.isRecording }.getOrDefault(false)
        render(willRecord)
    }

    private fun updateTile() {
        render(runCatching { RecordingController.isRecording }.getOrDefault(false))
    }

    private fun render(recording: Boolean) {
        qsTile?.apply {
            state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (recording) "Запись…" else "Быстрая заметка"
            if (Build.VERSION.SDK_INT >= 29) subtitle = if (recording) "Идёт запись · тап = сохранить" else "Тап = записать"
            updateTile()
        }
    }
}
