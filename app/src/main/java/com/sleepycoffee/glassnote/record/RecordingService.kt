package com.sleepycoffee.glassnote.record

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sleepycoffee.glassnote.R
import com.sleepycoffee.glassnote.ui.MainActivity
import kotlinx.coroutines.*

/** Foreground-сервис записи: переживает блокировку и продолжает держать процесс
 *  живым во время расшифровки (важно для запуска с плитки без открытия приложения). */
class RecordingService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); RecordingController.init(this); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> { startForeground(NOTIF_ID, notification("Идёт запись…")); RecordingController.start() }
            ACTION_STOP -> stopAndFinish()
            ACTION_TOGGLE ->
                if (RecordingController.isRecording) stopAndFinish()
                else { startForeground(NOTIF_ID, notification("Идёт запись…")); RecordingController.start() }
        }
        return START_NOT_STICKY
    }

    /** Останавливаем запись, но держим foreground пока идёт расшифровка. */
    private fun stopAndFinish() {
        RecordingController.stop()
        startForeground(NOTIF_ID, notification("Расшифровка…"))
        scope.launch {
            delay(800)
            while (RecordingController.state.value.transcribing > 0) delay(300)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("Glassnote").setContentText(text)
            .setSmallIcon(R.drawable.ic_tile).setOngoing(true).setContentIntent(open).build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CH, "Запись", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_TOGGLE = "toggle"
        private const val CH = "glassnote_rec"
        private const val NOTIF_ID = 1
        fun start(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, RecordingService::class.java).setAction(ACTION_START))
        fun stop(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, RecordingService::class.java).setAction(ACTION_STOP))
        fun toggle(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, RecordingService::class.java).setAction(ACTION_TOGGLE))
    }
}
