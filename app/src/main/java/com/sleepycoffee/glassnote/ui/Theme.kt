package com.sleepycoffee.glassnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF6470FF)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    secondary = Indigo,
    background = Color(0xFF0E0F16),
    surface = Color(0xFF171A28),
    surfaceVariant = Color(0xFF1E2233)
)
private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Indigo,
    background = Color(0xFFF5F6FF),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun GlassnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

/** Мягкий вертикальный градиент-подложка под весь экран. */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme())
        listOf(Color(0xFF141627), Color(0xFF0A0B12))
    else
        listOf(Color(0xFFF1F3FF), Color(0xFFE6E9FF))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) { content() }
}
