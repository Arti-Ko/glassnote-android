package com.sleepycoffee.glassnote.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sleepycoffee.glassnote.R

/** Постоянное уведомление-кнопка «Быстрая заметка», видимое на экране блокировки.
 *  Тап по действию запускает запись через сервис, не открывая приложение. */
object QuickControl {
    const val CH = "glassnote_quick"
    const val ID = 2

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannel(CH, "Быстрая заметка", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun show(ctx: Context) {
        ensureChannel(ctx)
        val toggle = PendingIntent.getForegroundService(
            ctx, 10,
            Intent(ctx, RecordingService::class.java).setAction(RecordingService.ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("Glassnote")
            .setContentText("Быстрая голосовая заметка")
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_tile, "🎙 Запись", toggle)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(ID, n)
    }

    fun hide(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}
