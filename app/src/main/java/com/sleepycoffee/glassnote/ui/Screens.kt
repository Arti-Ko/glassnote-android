package com.sleepycoffee.glassnote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sleepycoffee.glassnote.data.RecordingLanguage
import com.sleepycoffee.glassnote.data.Settings
import com.sleepycoffee.glassnote.data.StoredNote
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService
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

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Library) }
    when (val s = screen) {
        Screen.Library -> LibraryScreen({ screen = Screen.Detail(it) }, { screen = Screen.Settings })
        is Screen.Detail -> NoteDetailScreen(s.id) { screen = Screen.Library }
        Screen.Settings -> SettingsScreen { screen = Screen.Library }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val notes by RecordingController.notes.collectAsState()
    val state by RecordingController.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(notes, query) { RecordingController.search(query) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Glassnote") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = { IconButton(onSettings) { Icon(Icons.Filled.Settings, "Настройки") } }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { RecordingService.toggle(ctx) },
                    containerColor = if (state.recording) Color(0xFFE5484D) else Indigo,
                    contentColor = Color.White,
                    icon = { Icon(if (state.recording) Icons.Filled.Stop else Icons.Filled.Mic, null) },
                    text = { Text(if (state.recording) "Стоп" else "Запись") }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                OutlinedTextField(
                    query, { query = it },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск по заметкам") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                if (state.transcribing > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Расшифровывается: ${state.transcribing}") },
                        leadingIcon = { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (filtered.isEmpty()) {
                    EmptyState(query.isBlank())
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { NoteCard(it) { onOpen(it.id) } }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(n: StoredNote, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                n.note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(prettyDate(n.note.createdAt), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(mmss(n.note.durationSec), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                n.note.language?.takeIf { it.isNotBlank() }?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (n.note.edited) Icon(Icons.Filled.Edit, null, Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (n.transcript.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    n.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(noNotes: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (noNotes) Icons.Filled.Mic else Icons.Filled.SearchOff,
                null, Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (noNotes) "Пока нет заметок\nНажмите «Запись» или плитку в шторке" else "Ничего не найдено",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val notes by RecordingController.notes.collectAsState()
    val stored = notes.firstOrNull { it.id == id }
    if (stored == null) { onBack(); return }
    var text by remember(id) { mutableStateOf(stored.transcript) }

    LaunchedEffect(text) {
        if (text != stored.transcript) { delay(800); RecordingController.updateTranscript(text, stored) }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stored.note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                    actions = {
                        IconButton(onClick = { copyToClipboard(ctx, markdown(stored, text)) }) {
                            Icon(Icons.Filled.ContentCopy, "Копировать")
                        }
                        IconButton(onClick = { RecordingController.delete(stored); onBack() }) {
                            Icon(Icons.Filled.Delete, "Удалить")
                        }
                    }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
                Text(prettyDate(stored.note.createdAt) + "  ·  " + mmss(stored.note.durationSec),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                if (stored.audio.exists()) PlayerBar(stored.audio)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    text, { text = it },
                    Modifier.fillMaxSize(),
                    label = { Text("Расшифровка") }
                )
            }
        }
    }
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
    LaunchedEffect(playing) {
        while (playing) {
            progress = if (durationMs > 0) player.currentPosition.toFloat() / durationMs else 0f
            delay(200)
        }
    }
    DisposableEffect(Unit) { onDispose { runCatching { player.release() } } }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (!prepared) return@IconButton
                if (playing) { player.pause(); playing = false } else { player.start(); playing = true }
            }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Воспроизвести")
            }
            Slider(
                value = progress,
                onValueChange = { if (durationMs > 0) { player.seekTo((it * durationMs).toInt()); progress = it } },
                modifier = Modifier.weight(1f)
            )
            Text(mmss((durationMs / 1000).toDouble()), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf(Settings.language(ctx)) }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Настройки") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Язык записи", style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Column {
                        RecordingLanguage.entries.forEach { opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { lang = opt; Settings.setLanguage(ctx, opt) }
                                    .padding(horizontal = 8.dp)
                            ) {
                                RadioButton(selected = lang == opt, onClick = { lang = opt; Settings.setLanguage(ctx, opt) })
                                Text(opt.label)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Папка заметок", style = MaterialTheme.typography.titleMedium)
                Text(
                    File(ctx.getExternalFilesDir(null), "Glassnote").absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
private val pretty = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))

fun prettyDate(iso: String): String =
    runCatching { pretty.format(isoParser.parse(iso)!!) }.getOrDefault(iso)

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("note", text))
}

fun markdown(s: StoredNote, text: String) = "# ${s.note.title}\n\n${text}\n"
