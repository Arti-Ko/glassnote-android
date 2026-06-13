package com.sleepycoffee.glassnote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepycoffee.glassnote.data.RecordingLanguage
import com.sleepycoffee.glassnote.data.Settings
import com.sleepycoffee.glassnote.data.StoredNote
import com.sleepycoffee.glassnote.data.ThemeMode
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

private val Card = RoundedCornerShape(18.dp)
private val Field = RoundedCornerShape(12.dp)

@Composable
fun AppRoot() {
    val c = LocalPalette.current
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }
    Box(Modifier.fillMaxSize().background(c.groupedBg)) {
        when (val s = screen) {
            Screen.Library -> LibraryScreen({ screen = Screen.Detail(it) }, { screen = Screen.Settings })
            is Screen.Detail -> NoteDetailScreen(s.id) { screen = Screen.Library }
            Screen.Settings -> SettingsScreen { screen = Screen.Library }
        }
    }
}

@Composable
fun LibraryScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    val notes by RecordingController.notes.collectAsState()
    val state by RecordingController.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(notes, query) { RecordingController.search(query) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Glassnote", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = c.label)
                        Text(if (notes.isEmpty()) "Голосовые заметки" else "${notes.size} ${plural(notes.size)}",
                            fontSize = 15.sp, color = c.secondary)
                    }
                    IosGlassButton(Icons.Rounded.Settings, "Настройки", onSettings)
                }
            }
            item { SearchField(query) { query = it } }
            item { ModelBanner() }
            if (filtered.isEmpty()) item { EmptyState(query.isBlank()) }
            else items(filtered, key = { it.id }) { NoteCard(it) { onOpen(it.id) } }
        }
        RecordButton(state.recording, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)) {
            RecordingService.toggle(ctx)
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val c = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(Field).background(c.fieldFill).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = c.secondary, modifier = Modifier.size(20.dp))
        TextField(
            value = query, onValueChange = onChange, singleLine = true,
            placeholder = { Text("Поиск", color = c.secondary, fontSize = 17.sp) },
            textStyle = TextStyle(fontSize = 17.sp, color = c.label),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor = c.blue),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NoteCard(n: StoredNote, onClick: () -> Unit) {
    val c = LocalPalette.current
    val pending = n.transcript.isBlank() && n.note.segments.isEmpty()
    Row(Modifier.fillMaxWidth().insetCard(Card, c).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(n.note.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(meta(n), fontSize = 13.sp, color = c.secondary)
            if (n.transcript.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(n.transcript, fontSize = 15.sp, color = c.secondary, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else if (pending) {
                Spacer(Modifier.height(5.dp))
                Text("Аудио сохранено · откройте, чтобы расшифровать", fontSize = 13.sp, color = c.tertiary)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.tertiary)
    }
}

private fun meta(n: StoredNote): String {
    val parts = mutableListOf(prettyDate(n.note.createdAt), mmss(n.note.durationSec))
    n.note.language?.takeIf { it.isNotBlank() }?.let { parts.add(it.uppercase()) }
    return parts.joinToString("  ·  ")
}

@Composable
private fun RecordButton(recording: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalPalette.current
    val shape = CircleShape
    Row(modifier.shadow(16.dp, shape, clip = false, spotColor = (if (recording) c.red else c.blue).copy(alpha = 0.5f))
        .clip(shape).background(if (recording) c.red else c.blue).clickable(onClick = onClick).padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic, null, tint = Color.White)
        Text(if (recording) "Остановить" else "Запись", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
private fun IosGlassButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
    val c = LocalPalette.current
    Box(Modifier.size(40.dp).iosGlass(CircleShape, c).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, cd, tint = c.blue, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyState(noNotes: Boolean) {
    val c = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(if (noNotes) Icons.Rounded.Mic else Icons.Rounded.SearchOff, null, tint = c.tertiary, modifier = Modifier.size(56.dp))
        Text(if (noNotes) "Нет заметок" else "Ничего не найдено", color = c.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (noNotes) Text("Нажмите «Запись» или плитку в шторке", color = c.secondary, fontSize = 14.sp)
    }
}

@Composable
fun NoteDetailScreen(id: String, onBack: () -> Unit) {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    BackHandler { onBack() }
    val notes by RecordingController.notes.collectAsState()
    val tState by RecordingController.state.collectAsState()
    val stored = notes.firstOrNull { it.id == id }
    if (stored == null) { onBack(); return }
    var text by remember(id) { mutableStateOf(stored.transcript) }

    LaunchedEffect(text) { if (text != stored.transcript) { delay(800); RecordingController.updateTranscript(text, stored) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IosGlassButton(Icons.AutoMirrored.Rounded.ArrowBackIos, "Назад", onBack)
            Spacer(Modifier.weight(1f))
            IosGlassButton(Icons.Rounded.IosShare, "Копировать") { copyToClipboard(ctx, markdown(stored, text)) }
            Spacer(Modifier.width(10.dp))
            IosGlassButton(Icons.Rounded.DeleteOutline, "Удалить") { RecordingController.delete(stored); onBack() }
        }
        Spacer(Modifier.height(16.dp))
        Text(stored.note.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(meta(stored), fontSize = 13.sp, color = c.secondary)
        Spacer(Modifier.height(16.dp))
        if (stored.audio.exists()) PlayerBar(stored.audio)
        Spacer(Modifier.height(14.dp))
        if (text.isBlank()) {
            if (tState.transcribing > 0) Row(Modifier.fillMaxWidth().insetCard(Card, c).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.blue)
                Text("Расшифровка…", color = c.label, fontSize = 16.sp)
            } else Row(Modifier.fillMaxWidth().insetCard(Card, c).clickable { RecordingController.retranscribe(stored) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.GraphicEq, null, tint = c.blue)
                Text("Расшифровать", color = c.blue, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(Modifier.fillMaxSize().insetCard(Card, c).padding(4.dp)) {
                TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(fontSize = 17.sp, color = c.label, lineHeight = 24.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = c.blue))
            }
        }
    }
}

@Composable
private fun PlayerBar(file: File) {
    val c = LocalPalette.current
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
    Row(Modifier.fillMaxWidth().insetCard(Card, c).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.blue).clickable {
            if (!prepared) return@clickable
            if (playing) { player.pause(); playing = false } else { player.start(); playing = true }
        }, contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Играть", tint = Color.White)
        }
        Slider(value = progress, onValueChange = { if (durationMs > 0) { player.seekTo((it * durationMs).toInt()); progress = it } },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = c.blue, inactiveTrackColor = c.tertiary))
        Text(mmss((durationMs / 1000).toDouble()), color = c.secondary, fontSize = 13.sp)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    BackHandler { onBack() }
    var lang by remember { mutableStateOf(Settings.language(ctx)) }
    var theme by remember { mutableStateOf(ThemeState.mode) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IosGlassButton(Icons.AutoMirrored.Rounded.ArrowBackIos, "Назад", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.label)
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader("ОФОРМЛЕНИЕ")
        Column(Modifier.fillMaxWidth().insetCard(Card, c)) {
            ThemeMode.entries.forEachIndexed { i, opt ->
                SettingRow(opt.label, theme == opt) { theme = opt; ThemeState.mode = opt; Settings.setThemeMode(ctx, opt) }
                if (i < ThemeMode.entries.lastIndex) RowDivider(c)
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader("ЯЗЫК ЗАПИСИ")
        Column(Modifier.fillMaxWidth().insetCard(Card, c)) {
            RecordingLanguage.entries.forEachIndexed { i, opt ->
                SettingRow(opt.label, lang == opt) { lang = opt; Settings.setLanguage(ctx, opt) }
                if (i < RecordingLanguage.entries.lastIndex) RowDivider(c)
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader("ПАПКА ЗАМЕТОК")
        Column(Modifier.fillMaxWidth().insetCard(Card, c).padding(16.dp)) {
            Text(File(ctx.getExternalFilesDir(null), "Glassnote").absolutePath, color = c.secondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = c.blue)
    }
}

@Composable private fun RowDivider(c: Palette) = HorizontalDivider(Modifier.padding(start = 16.dp), color = c.separator, thickness = 0.5.dp)

@Composable
private fun SectionHeader(text: String) {
    val c = LocalPalette.current
    Text(text, fontSize = 13.sp, color = c.secondary, modifier = Modifier.padding(start = 16.dp, bottom = 7.dp))
}

@Composable
private fun ModelBanner() {
    val c = LocalPalette.current
    val st by ModelManager.state.collectAsState()
    when (val m = st) {
        is ModelManager.State.Downloading -> Row(Modifier.fillMaxWidth().insetCard(Card, c).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = c.blue)
            Text("Загрузка модели распознавания… ${m.percent}%", color = c.label, fontSize = 14.sp)
        }
        is ModelManager.State.Failed -> Row(Modifier.fillMaxWidth().insetCard(Card, c).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.WarningAmber, null, tint = c.red)
            Text("Модель не загружена: ${m.message}", color = c.label, fontSize = 14.sp)
        }
        else -> {}
    }
}

private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val pretty = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
fun prettyDate(iso: String): String = runCatching { pretty.format(isoParser.parse(iso)!!) }.getOrDefault(iso)
fun mmss(t: Double): String { val s = t.toInt(); return "%d:%02d".format(s / 60, s % 60) }
private fun plural(n: Int): String { val m = n % 10; val mm = n % 100
    return if (m == 1 && mm != 11) "заметка" else if (m in 2..4 && mm !in 12..14) "заметки" else "заметок" }
private fun copyToClipboard(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("note", text))
}
fun markdown(s: StoredNote, text: String) = "# ${s.note.title}\n\n${text}\n"
