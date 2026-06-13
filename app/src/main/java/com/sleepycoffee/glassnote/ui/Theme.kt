package com.sleepycoffee.glassnote.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sleepycoffee.glassnote.data.ThemeMode

/** Палитра iOS 27 — отдельные значения для светлой и тёмной темы. */
data class Palette(
    val groupedBg: Color, val card: Color, val label: Color, val secondary: Color,
    val tertiary: Color, val separator: Color, val blue: Color, val green: Color,
    val red: Color, val fieldFill: Color, val glassFill: Color, val glassBorder: Color,
    val cardShadow: Color, val dark: Boolean
)

val LightPalette = Palette(
    groupedBg = Color(0xFFF2F2F7), card = Color(0xFFFFFFFF), label = Color(0xFF000000),
    secondary = Color(0xFF3C3C43).copy(alpha = 0.60f), tertiary = Color(0xFF3C3C43).copy(alpha = 0.30f),
    separator = Color(0xFF3C3C43).copy(alpha = 0.20f), blue = Color(0xFF007AFF),
    green = Color(0xFF34C759), red = Color(0xFFFF3B30), fieldFill = Color(0xFF767680).copy(alpha = 0.12f),
    glassFill = Color.White.copy(alpha = 0.80f), glassBorder = Color.White.copy(alpha = 0.90f),
    cardShadow = Color.Black.copy(alpha = 0.06f), dark = false
)

val DarkPalette = Palette(
    groupedBg = Color(0xFF000000), card = Color(0xFF1C1C1E), label = Color(0xFFFFFFFF),
    secondary = Color(0xFFEBEBF5).copy(alpha = 0.60f), tertiary = Color(0xFFEBEBF5).copy(alpha = 0.30f),
    separator = Color(0xFF545458).copy(alpha = 0.65f), blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158), red = Color(0xFFFF453A), fieldFill = Color(0xFF767680).copy(alpha = 0.24f),
    glassFill = Color.White.copy(alpha = 0.14f), glassBorder = Color.White.copy(alpha = 0.22f),
    cardShadow = Color.Black.copy(alpha = 0.50f), dark = true
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Живое состояние выбранной темы (Система/Светлая/Тёмная). */
object ThemeState {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
}

@Composable
fun GlassnoteTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark)
        darkColorScheme(primary = p.blue, background = p.groupedBg, surface = p.card, onBackground = p.label, onSurface = p.label)
    else
        lightColorScheme(primary = p.blue, background = p.groupedBg, surface = p.card, onBackground = p.label, onSurface = p.label)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Плавающая «Liquid Glass» капсула. */
fun Modifier.iosGlass(shape: Shape, p: Palette): Modifier = this
    .shadow(10.dp, shape, clip = false, spotColor = Color.Black.copy(alpha = if (p.dark) 0.45f else 0.18f))
    .clip(shape).background(p.glassFill).border(0.7.dp, p.glassBorder, shape)

/** Inset-карточка списка. */
fun Modifier.insetCard(shape: Shape, p: Palette): Modifier = this
    .shadow(4.dp, shape, clip = false, spotColor = p.cardShadow).clip(shape).background(p.card)
