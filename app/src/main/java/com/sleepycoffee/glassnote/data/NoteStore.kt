package com.sleepycoffee.glassnote.data

import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Заметка на диске: папка yyyy-MM-dd_HH-mm-ss/ с audio.m4a, note.json, transcript.md. */
data class StoredNote(val note: Note, val folder: File, val transcript: String) {
    val id get() = note.id
    val audio get() = File(folder, "audio.m4a")
}

/** Файлы — источник истины. Формат идентичен macOS-версии (sync-ready). */
class NoteStore(private val root: File) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val folderFmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    init { root.mkdirs() }

    fun isoNow(date: Date = Date()): String = iso.format(date)

    fun loadAll(): List<StoredNote> =
        root.listFiles { f -> f.isDirectory }
            ?.mapNotNull { load(it) }
            ?.sortedByDescending { it.note.createdAt } ?: emptyList()

    fun load(folder: File): StoredNote? {
        val noteFile = File(folder, "note.json")
        if (!noteFile.exists()) return null
        val note = runCatching { json.decodeFromString<Note>(noteFile.readText()) }.getOrNull() ?: return null
        val tFile = File(folder, "transcript.md")
        val transcript = if (tFile.exists()) tFile.readText()
                         else note.segments.joinToString(" ") { it.text }
        return StoredNote(note, folder, transcript)
    }

    /** Уникальная папка; при коллизии секунды добавляет -2, -3… */
    fun createNoteFolder(date: Date = Date()): File {
        val base = folderFmt.format(date)
        var attempt = 0
        while (true) {
            val name = if (attempt == 0) base else "$base-${attempt + 1}"
            val f = File(root, name)
            if (!f.exists()) { f.mkdirs(); return f }
            attempt++
        }
    }

    fun save(note: Note, transcript: String, folder: File): StoredNote {
        File(folder, "note.json").writeText(json.encodeToString(Note.serializer(), note))
        File(folder, "transcript.md").writeText(transcript)
        return StoredNote(note, folder, transcript)
    }

    /** Правка: transcript.md перезаписывается, edited=true; сегменты не трогаем. */
    fun updateTranscript(text: String, stored: StoredNote): StoredNote {
        val n = stored.note.copy(edited = true, title = Note.makeTitle(text))
        return save(n, text, stored.folder)
    }

    fun delete(stored: StoredNote) { stored.folder.deleteRecursively() }
}
