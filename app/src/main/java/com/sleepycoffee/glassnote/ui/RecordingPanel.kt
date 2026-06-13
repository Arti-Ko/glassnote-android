package com.sleepycoffee.glassnote.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService

/** iOS-шит записи снизу: светлый материал, грабер, волна, таймер, стоп. */
@Composable
fun RecordingPanelSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val state by RecordingController.state.collectAsState()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(IOS.card).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 38.dp, height = 5.dp).clip(CircleShape).background(IOS.tertiary))
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PulsingDot()
                WaveformBars(state.levels, Modifier.weight(1f).height(38.dp))
                Text(mmss(state.elapsed), color = IOS.label, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                Box(Modifier.size(40.dp).clip(CircleShape).background(IOS.fieldFill).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть", tint = IOS.secondary)
                }
                Box(Modifier.size(52.dp).clip(CircleShape).background(IOS.red)
                    .clickable { RecordingService.stop(ctx); onClose() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Stop, "Стоп", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val t = rememberInfiniteTransition(label = "p")
    val scale by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")
    Box(Modifier.size((11 * scale).dp).clip(CircleShape).background(IOS.red))
}

@Composable
fun WaveformBars(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = levels.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val bw = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        levels.forEachIndexed { i, lv ->
            val h = (lv * size.height).coerceAtLeast(3f)
            drawRoundRect(color = IOS.blue.copy(alpha = 0.9f),
                topLeft = Offset(i * (bw + gap), (size.height - h) / 2f),
                size = Size(bw, h), cornerRadius = CornerRadius(bw / 2, bw / 2))
        }
    }
}
