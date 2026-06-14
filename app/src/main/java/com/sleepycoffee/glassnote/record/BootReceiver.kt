package com.sleepycoffee.glassnote.record

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** После перезагрузки возвращает кнопку быстрой записи в шторку/локскрин. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            QuickControl.show(ctx)
        }
    }
}
