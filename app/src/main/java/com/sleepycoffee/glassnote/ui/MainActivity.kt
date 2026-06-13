package com.sleepycoffee.glassnote.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.sleepycoffee.glassnote.record.RecordingController

class MainActivity : ComponentActivity() {
    private val askPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecordingController.init(this)

        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        askPerms.launch(perms.toTypedArray())

        setContent { GlassnoteTheme { AppRoot() } }
    }
}
