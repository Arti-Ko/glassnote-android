package com.sleepycoffee.glassnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Accent = Color(0xFF7C84FF)

private val Scheme = darkColorScheme(
    primary = Accent,
    secondary = Accent,
    background = Color(0xFF07070C),
    surface = Color(0xFF101018),
    onBackground = Color(0xFFF2F3FF),
    onSurface = Color(0xFFF2F3FF),
    onSurfaceVariant = Color(0xCCFFFFFF)
)

@Composable
fun GlassnoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/**
 * Атмосферный фон в духе macOS Sonoma: тёмная база + крупные размытые
 * цветные «капли» света, сквозь которые просвечивает матовое стекло панелей.
 */
@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF07070C))) {
        Box(
            Modifier.size(460.dp).offset((-120).dp, (-110).dp).blur(130.dp)
                .background(Color(0xFF5B62F5).copy(alpha = 0.55f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.TopEnd).size(380.dp).offset(110.dp, (-70).dp).blur(130.dp)
                .background(Color(0xFFB14DFF).copy(alpha = 0.45f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomStart).size(420.dp).offset((-90).dp, 120.dp).blur(150.dp)
                .background(Color(0xFF1FC8C0).copy(alpha = 0.30f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).size(340.dp).offset(90.dp, 90.dp).blur(140.dp)
                .background(Color(0xFFFF6CA8).copy(alpha = 0.26f), CircleShape)
        )
        content()
    }
}

/** Матовое стекло: лёгкая вертикальная подсветка + тонкая светлая грань + мягкая тень. */
fun Modifier.glass(
    shape: Shape,
    highlight: Float = 0.14f,
    base: Float = 0.05f,
    borderAlpha: Float = 0.22f,
): Modifier = this
    .shadow(elevation = 14.dp, shape = shape, clip = false)
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = highlight), Color.White.copy(alpha = base))
        )
    )
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
