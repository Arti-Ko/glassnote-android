package com.sleepycoffee.glassnote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepycoffee.glassnote.data.RecordingLanguage
import com.sleepycoffee.glassnote.data.Settings
import com.sleepycoffee.glassnote.data.StoredNote
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService
import com.sleepycoffee.glassnote.transcription.ModelManager
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed interface Screen {
    data object Library : Screen
    data class Detail(val id: String) : Screen
    data object Settings : Screen
}

private val CardShape = RoundedCornerShape(26.dp)
private val FieldShape = RoundedCornerShape(20.dp)

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }
    AmbientBackground {
        when (val s = screen) {
            Screen.Library -> LibraryScreen({ screen = Screen.Detail(it) }, { screen = Screen.Settings })
            is Screen.Detail -> NoteDetailScreen(s.id) { screen = Screen.Library }
            Screen.Settings -> SettingsScreen { screen = Screen.Library }
        }
    }
}

@Composable
fun LibraryScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val notes by RecordingController.notes.collectAsState()
    val state by RecordingController.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(notes, query) { RecordingController.search(query) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Glassnote", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    GlassIconButton(Icons.Rounded.Settings, "Настройки", onSettings)
                }
            }
            item { SearchField(query) { query = it } }
            item { ModelBanner() }
            if (filtered.isEmpty()) item { EmptyState(query.isBlank()) }
            else items(filtered, key = { it.id }) { NoteCard(it) { onOpen(it.id) } }
        }
        RecordButton(
            recording = state.recording,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 26.dp),
            onClick = { RecordingService.toggle(ctx) }
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().glass(FieldShape).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f))
        TextField(
            value = query, onValueChange = onChange, singleLine = true,
            placeholder = { Text("Поиск по заметкам", color = Color.White.copy(0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Accent, focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NoteCard(n: StoredNote, onClick: () -> Unit) {
    val pending = n.transcript.isBlank() && n.note.segments.isEmpty()
    Column(Modifier.fillMaxWidth().glass(CardShape).clickable(onClick = onClick).padding(18.dp)) {
        Text(n.note.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaPill(prettyDate(n.note.createdAt))
            MetaPill(mmss(n.note.durationSec))
            n.note.language?.takeIf { it.isNotBlank() }?.let { MetaPill(it.uppercase()) }
        }
        if (n.transcript.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(n.transcript, fontSize = 14.sp, color = Color.White.copy(0.72f), lineHeight = 20.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else if (pending) {
            Spacer(Modifier.height(10.dp))
            Text("аудио сохранено · откройте, чтобы расшифровать", fontSize = 13.sp, color = Color.White.copy(0.5f))
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Text(text, fontSize = 12.sp, color = Color.White.copy(0.80f),
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.10f)).padding(horizontal = 9.dp, vertical = 3.dp))
}

@Composable
private fun RecordButton(recording: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.shadow(20.dp, CircleShape, clip = false, spotColor = Accent).clip(CircleShape)
            .background(if (recording) Brush.linearGradient(listOf(Color(0xFFFF5A5F), Color(0xFFE5484D)))
                        else Brush.linearGradient(listOf(Color(0xFF8C92FF), Color(0xFF6E63FF))))
            .clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic, null, tint = Color.White)
        Text(if (recording) "Остановить" else "Запись", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).glass(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, cd, tint = Color.White)
    }
}

@Composable
private fun EmptyState(noNotes: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).glass(CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (noNotes) Icons.Rounded.Mic else Icons.Rounded.SearchOff, null,
                tint = Color.White.copy(0.85f), modifier = Modifier.size(32.dp))
        }
        Text(if (noNotes) "Пока нет заметок" else "Ничего не найдено", color = Color.White, fontWeight = FontWeight.Medium)
        if (noNotes) Text("Нажмите «Запись» или плитку в шторке", color = Color.White.copy(0.55f), fontSize = 13.sp)
    }
}

@Composable
fun NoteDetailScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val notes by RecordingController.notes.collectAsState()
    val tState by RecordingController.state.collectAsState()
    val stored = notes.firstOrNull { it.id == id }
    if (stored == null) { onBack(); return }
    var text by remember(id) { mutableStateOf(stored.transcript) }

    LaunchedEffect(text) {
        if (text != stored.transcript) { delay(800); RecordingController.updateTranscript(text, stored) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
            Spacer(Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.ContentCopy, "Копировать") { copyToClipboard(ctx, markdown(stored, text)) }
            Spacer(Modifier.width(10.dp))
            GlassIconButton(Icons.Rounded.DeleteOutline, "Удалить") { RecordingController.delete(stored); onBack() }
        }
        Spacer(Modifier.height(16.dp))
        Text(stored.note.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaPill(prettyDate(stored.note.createdAt)); MetaPill(mmss(stored.note.durationSec))
        }
        Spacer(Modifier.height(16.dp))
        if (stored.audio.exists()) PlayerBar(stored.audio)
        Spacer(Modifier.height(16.dp))
        if (text.isBlank()) {
            if (tState.transcribing > 0) GlassRow {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                Text("Расшифровка…", color = Color.White)
            } else Row(
                Modifier.fillMaxWidth().glass(FieldShape).clickable { RecordingController.retranscribe(stored) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.GraphicEq, null, tint = Accent)
                Text("Расшифровать", color = Color.White, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(Modifier.fillMaxSize().glass(CardShape).padding(18.dp)) {
                Text("Расшифровка", fontSize = 12.sp, color = Color.White.copy(0.5f))
                Spacer(Modifier.height(8.dp))
                TranscriptEditor(text) { text = it }
            }
        }
    }
}

@Composable
private fun GlassRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().glass(FieldShape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun TranscriptEditor(text: String, onChange: (String) -> Unit) {
    TextField(
        value = text, onValueChange = onChange, modifier = Modifier.fillMaxSize(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Accent, focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(0.9f)
        )
    )
}

@Composable
private fun PlayerBar(file: File) {
    val player = remember { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var durationMs by remember { mutableStateOf(0) }
    LaunchedEffect(file.absolutePath) {
        runCatching {
            player.reset(); player.setDataSource(file.absolutePath); player.prepare()
            durationMs = player.duration
            player.setOnCompletionListener { playing = false; progress = 0f }
            prepared = true
        }
    }
    LaunchedEffect(playing) { while (playing) { progress = if (durationMs > 0) player.currentPosition.toFloat() / durationMs else 0f; delay(200) } }
    DisposableEffect(Unit) { onDispose { runCatching { player.release() } } }
    Row(Modifier.fillMaxWidth().glass(FieldShape).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Accent).clickable {
            if (!prepared) return@clickable
            if (playing) { player.pause(); playing = false } else { player.start(); playing = true }
        }, contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Играть", tint = Color.White)
        }
        Slider(value = progress, onValueChange = { if (durationMs > 0) { player.seekTo((it * durationMs).toInt()); progress = it } },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Accent))
        Text(mmss((durationMs / 1000).toDouble()), color = Color.White.copy(0.75f), fontSize = 12.sp)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf(Settings.language(ctx)) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("ЯЗЫК ЗАПИСИ", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().glass(CardShape).padding(6.dp)) {
            RecordingLanguage.entries.forEach { opt ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .clickable { lang = opt; Settings.setLanguage(ctx, opt) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(opt.label, color = Color.White, modifier = Modifier.weight(1f))
                    Icon(if (lang == opt) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null, tint = if (lang == opt) Accent else Color.White.copy(0.3f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("ПАПКА ЗАМЕТОК", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().glass(CardShape).padding(16.dp)) {
            Text(File(ctx.getExternalFilesDir(null), "Glassnote").absolutePath, color = Color.White.copy(0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ModelBanner() {
    val st by ModelManager.state.collectAsState()
    when (val m = st) {
        is ModelManager.State.Downloading -> GlassRow {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
            Text("Загрузка модели распознавания… ${m.percent}%", color = Color.White, fontSize = 13.sp)
        }
        is ModelManager.State.Failed -> GlassRow {
            Icon(Icons.Rounded.WarningAmber, null, tint = Color(0xFFFFC53D))
            Text("Модель не загружена: ${m.message}", color = Color.White, fontSize = 13.sp)
        }
        else -> {}
    }
}

private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val pretty = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
fun prettyDate(iso: String): String = runCatching { pretty.format(isoParser.parse(iso)!!) }.getOrDefault(iso)
fun mmss(t: Double): String { val s = t.toInt(); return "%d:%02d".format(s / 60, s % 60) }
private fun copyToClipboard(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("note", text))
}
fun markdown(s: StoredNote, text: String) = "# ${s.note.title}\n\n${text}\n"
