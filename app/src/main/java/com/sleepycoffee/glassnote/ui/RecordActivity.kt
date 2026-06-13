package com.sleepycoffee.glassnote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService

/** Прозрачная активность со стеклянной плашкой; показывается поверх локскрина. */
class RecordActivity : ComponentActivity() {

    private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        RecordingController.init(this)

        setContent { GlassnoteTheme { RecordingPanelSheet(onClose = { finish() }) } }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) beginRecording()
        else askMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun beginRecording() {
        if (!RecordingController.isRecording) RecordingService.start(this)
    }
}
