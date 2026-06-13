package com.sleepycoffee.glassnote.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService

/** Стеклянная плашка записи, всплывающая снизу. Стоп завершает запись,
 *  «свернуть» прячет плашку — запись продолжается в сервисе. */
@Composable
fun RecordingPanelSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val state by RecordingController.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2A2D3A).copy(alpha = 0.92f), Color(0xFF1A1C26).copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                PulsingDot()
                WaveformBars(state.levels, Modifier.weight(1f).height(34.dp))
                Text(
                    mmss(state.elapsed),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Свернуть", tint = Color.White.copy(alpha = 0.7f))
                }
                FilledIconButton(
                    onClick = {
                        RecordingService.stop(ctx)
                        onClose()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE5484D))
                ) { Icon(Icons.Filled.Stop, "Стоп", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s"
    )
    Box(
        Modifier
            .size((10 * scale).dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE5484D))
    )
}

@Composable
fun WaveformBars(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = levels.size.coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val bw = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        levels.forEachIndexed { i, lv ->
            val h = (lv * size.height).coerceAtLeast(3f)
            val x = i * (bw + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(x, y),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2, bw / 2)
            )
        }
    }
}

fun mmss(t: Double): String {
    val s = t.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
