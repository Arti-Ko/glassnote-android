package com.sleepycoffee.glassnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Токены iOS 27 (Liquid Glass, светлая тема). */
object IOS {
    val groupedBg = Color(0xFFF2F2F7)
    val card = Color(0xFFFFFFFF)
    val label = Color(0xFF000000)
    val secondary = Color(0xFF3C3C43).copy(alpha = 0.60f)
    val tertiary = Color(0xFF3C3C43).copy(alpha = 0.30f)
    val separator = Color(0xFF3C3C43).copy(alpha = 0.20f)
    val blue = Color(0xFF007AFF)
    val green = Color(0xFF34C759)
    val red = Color(0xFFFF3B30)
    val fieldFill = Color(0xFF767680).copy(alpha = 0.12f)
}

val Accent = IOS.blue

private val Scheme = lightColorScheme(
    primary = IOS.blue,
    secondary = IOS.blue,
    background = IOS.groupedBg,
    surface = IOS.card,
    onBackground = IOS.label,
    onSurface = IOS.label,
    onSurfaceVariant = IOS.secondary
)

@Composable
fun GlassnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/** Плавающая «Liquid Glass» капсула тулбара — полупрозрачное стекло + грань + мягкая тень. */
fun Modifier.iosGlass(shape: Shape): Modifier = this
    .shadow(10.dp, shape, clip = false, spotColor = Color.Black.copy(alpha = 0.18f), ambientColor = Color.Black.copy(alpha = 0.12f))
    .clip(shape)
    .background(Color.White.copy(alpha = 0.80f))
    .border(0.7.dp, Color.White.copy(alpha = 0.9f), shape)

/** Белая inset-карточка списка iOS. */
fun Modifier.insetCard(shape: Shape): Modifier = this
    .shadow(4.dp, shape, clip = false, spotColor = Color.Black.copy(alpha = 0.06f), ambientColor = Color.Black.copy(alpha = 0.04f))
    .clip(shape)
    .background(IOS.card)
