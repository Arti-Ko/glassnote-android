package com.sleepycoffee.glassnote.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF5763F7)

private val DarkColors = darkColorScheme(primary = Indigo, secondary = Indigo)
private val LightColors = lightColorScheme(primary = Indigo, secondary = Indigo)

@Composable
fun GlassnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
