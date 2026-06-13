package com.sleepycoffee.glassnote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sleepycoffee.glassnote.data.RecordingLanguage
import com.sleepycoffee.glassnote.data.Settings
import com.sleepycoffee.glassnote.data.StoredNote
import com.sleepycoffee.glassnote.record.RecordingController
import com.sleepycoffee.glassnote.record.RecordingService
import kotlinx.coroutines.delay
import java.io.File

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glassnote") },
                actions = { IconButton(onSettings) { Icon(Icons.Filled.Settings, "Настройки") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { RecordingService.toggle(ctx) },
                icon = { Icon(if (state.recording) Icons.Filled.Stop else Icons.Filled.Mic, null) },
                text = { Text(if (state.recording) "Стоп" else "Запись") }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                query, { query = it },
                Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Поиск по заметкам") }, singleLine = true
            )
            if (state.transcribing > 0) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(if (query.isBlank()) "Пока нет заметок" else "Ничего не найдено")
                }
            } else LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { n ->
                    ListItem(
                        headlineContent = { Text(n.note.title, maxLines = 1) },
                        supportingContent = { Text("${n.note.createdAt}  ·  ${mmss(n.note.durationSec)}") },
                        modifier = Modifier.clickable { onOpen(n.id) }
                    )
                    HorizontalDivider()
                }
            }
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stored.note.title, maxLines = 1) },
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
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf(Settings.language(ctx)) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Настройки") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Язык записи", style = MaterialTheme.typography.titleMedium)
            RecordingLanguage.entries.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { lang = opt; Settings.setLanguage(ctx, opt) }
                ) {
                    RadioButton(selected = lang == opt, onClick = { lang = opt; Settings.setLanguage(ctx, opt) })
                    Text(opt.label)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Папка заметок", style = MaterialTheme.typography.titleMedium)
            Text(
                File(ctx.getExternalFilesDir(null), "Glassnote").absolutePath,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("note", text))
}

fun markdown(s: StoredNote, text: String) = "# ${s.note.title}\n\n${text}\n"
