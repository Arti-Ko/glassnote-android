package com.sleepycoffee.glassnote.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sleepycoffee.glassnote.R
import com.sleepycoffee.glassnote.ui.MainActivity
import kotlinx.coroutines.*

/** Foreground-сервис записи. Уведомление видно на локскрине с кнопкой «Остановить»;
 *  держит процесс живым во время расшифровки (запуск с локскрина без открытия приложения). */
class RecordingService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); RecordingController.init(this); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRec()
            ACTION_STOP -> stopAndFinish()
            ACTION_TOGGLE -> if (RecordingController.isRecording) stopAndFinish() else startRec()
        }
        return START_NOT_STICKY
    }

    private fun startRec() {
        startForeground(NOTIF_ID, recordingNotification())
        QuickControl.hide(this)
        RecordingController.start()
    }

    private fun stopAndFinish() {
        RecordingController.stop()
        startForeground(NOTIF_ID, simpleNotification("Расшифровка…"))
        scope.launch {
            delay(800)
            while (RecordingController.state.value.transcribing > 0) delay(300)
            QuickControl.show(this@RecordingService)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun openApp() = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    private fun recordingNotification(): Notification {
        val stop = PendingIntent.getForegroundService(
            this, 11, Intent(this, RecordingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("Glassnote")
            .setContentText("Идёт запись")
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(openApp())
            .addAction(R.drawable.ic_tile, "⏹ Остановить и сохранить", stop)
            .build()
    }

    private fun simpleNotification(text: String): Notification =
        NotificationCompat.Builder(this, CH)
            .setContentTitle("Glassnote").setContentText(text)
            .setSmallIcon(R.drawable.ic_tile).setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp()).build()

    private fun createChannel() {
        val ch = NotificationChannel(CH, "Запись", NotificationManager.IMPORTANCE_LOW).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
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
