package com.sleepycoffee.glassnote.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService

/** Стеклянная плашка записи снизу. */
@Composable
fun RecordingPanelSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val state by RecordingController.state.collectAsState()
    val shape = RoundedCornerShape(32.dp)

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp)
                .shadow(28.dp, shape, clip = false, spotColor = Color(0xFF6E63FF))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF23252F).copy(0.94f), Color(0xFF15161E).copy(0.94f))))
                .border(1.dp, Color.White.copy(0.20f), shape)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PulsingDot()
            WaveformBars(state.levels, Modifier.weight(1f).height(36.dp))
            Text(mmss(state.elapsed), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 17.sp)
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.12f)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть", tint = Color.White.copy(0.8f))
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE5484D))
                .clickable { RecordingService.stop(ctx); onClose() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Stop, "Стоп", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val t = rememberInfiniteTransition(label = "p")
    val scale by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")
    Box(Modifier.size((11 * scale).dp).clip(CircleShape).background(Color(0xFFE5484D)))
}

@Composable
fun WaveformBars(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = levels.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val bw = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        levels.forEachIndexed { i, lv ->
            val h = (lv * size.height).coerceAtLeast(3f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(i * (bw + gap), (size.height - h) / 2f),
                size = Size(bw, h), cornerRadius = CornerRadius(bw / 2, bw / 2)
            )
        }
    }
}
