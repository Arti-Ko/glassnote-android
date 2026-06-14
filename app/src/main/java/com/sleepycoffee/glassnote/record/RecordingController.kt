package com.sleepycoffee.glassnote.record

import android.content.Context
import com.sleepycoffee.glassnote.audio.AudioRecorder
import com.sleepycoffee.glassnote.data.*
import com.sleepycoffee.glassnote.transcription.ModelManager
import com.sleepycoffee.glassnote.transcription.Transcriber
import com.sleepycoffee.glassnote.transcription.WhisperTranscriber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Date
import java.util.UUID

/** Единый оркестратор записи/расшифровки/хранилища. */
object RecordingController {
    private lateinit var appContext: Context
    lateinit var store: NoteStore; private set
    private lateinit var recorder: AudioRecorder
    private lateinit var transcriber: Transcriber

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private const val BARS = 30

    data class UiState(
        val recording: Boolean = false,
        val transcribing: Int = 0,
        val elapsed: Double = 0.0,
        val levels: List<Float> = List(BARS) { 0f }
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val _notes = MutableStateFlow<List<StoredNote>>(emptyList())
    val notes: StateFlow<List<StoredNote>> = _notes.asStateFlow()

    private var levelJob: Job? = null
    private var pendingFolder: File? = null
    private var startedAt = Date()

    fun init(context: Context) {
        if (::store.isInitialized) return
        appContext = context.applicationContext
        store = NoteStore(File(appContext.getExternalFilesDir(null), "Glassnote"))
        recorder = AudioRecorder(appContext)
        transcriber = WhisperTranscriber { ModelManager.modelFile(appContext).takeIf { it.exists() } }
        _notes.value = store.loadAll()
        QuickControl.show(appContext)
        // Тянем модель в фоне при первом старте.
        scope.launch { ModelManager.ensure(appContext) }
        scope.launch { com.sleepycoffee.glassnote.update.UpdateChecker.check() }
    }

    val isRecording get() = _state.value.recording

    fun start() {
        if (isRecording) return
        startedAt = Date()
        pendingFolder = store.createNoteFolder(startedAt)
        val tmp = File(appContext.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
        if (runCatching { recorder.start(tmp) }.isFailure) return
        _state.update { it.copy(recording = true, elapsed = 0.0, levels = List(BARS) { 0f }) }
        levelJob = scope.launch {
            while (isActive) {
                val lvl = recorder.level()
                _state.update { s -> s.copy(elapsed = recorder.elapsedSec(), levels = s.levels.drop(1) + lvl) }
                delay(60)
            }
        }
    }

    fun stop() {
        if (!isRecording) return
        levelJob?.cancel()
        val dur = recorder.stop()
        val tmp = recorder.outputFile
        val folder = pendingFolder
        _state.update { it.copy(recording = false, levels = List(BARS) { 0f }) }
        if (tmp == null || folder == null) return
        scope.launch { finish(tmp, folder, dur, startedAt) }
    }

    private suspend fun finish(tmp: File, folder: File, dur: Double, date: Date) {
        _state.update { it.copy(transcribing = it.transcribing + 1) }
        try {
            val audio = File(folder, "audio.m4a")
            tmp.copyTo(audio, overwrite = true); tmp.delete()
            var note = Note(
                id = UUID.randomUUID().toString(),
                createdAt = store.isoNow(date),
                durationSec = dur,
                model = "",
                title = "Расшифровывается…"
            )
            refresh(store.save(note, "", folder))

            ModelManager.ensure(appContext)  // гарантируем, что модель скачана
            val lang = Settings.language(appContext).whisperCode
            val result = transcriber.transcribe(audio, lang)
            val text = result.segments.joinToString(" ") { it.text }.trim()
            note = note.copy(
                segments = result.segments,
                language = result.language,
                model = "whisper small-q5_1",
                title = if (text.isEmpty()) "Заметка от ${store.isoNow(date)}" else Note.makeTitle(text)
            )
            refresh(store.save(note, text, folder))
        } catch (e: Exception) {
                android.util.Log.e("GlassnoteWhisper", "pipeline failed", e)
            // аудио сохранено; расшифровку можно повторить позже
        } finally {
            _state.update { it.copy(transcribing = it.transcribing - 1) }
        }
    }

    private fun refresh(s: StoredNote) {
        _notes.update { list -> (list.filterNot { it.id == s.id } + s).sortedByDescending { it.note.createdAt } }
    }

    fun updateTranscript(text: String, stored: StoredNote) = refresh(store.updateTranscript(text, stored))

    fun delete(stored: StoredNote) {
        store.delete(stored)
        _notes.update { l -> l.filterNot { it.id == stored.id } }
    }

    fun retranscribe(stored: StoredNote) {
        scope.launch {
            _state.update { it.copy(transcribing = it.transcribing + 1) }
            try {
                ModelManager.ensure(appContext)
                val lang = Settings.language(appContext).whisperCode
                val result = transcriber.transcribe(stored.audio, lang)
                val text = result.segments.joinToString(" ") { it.text }.trim()
                val n = stored.note.copy(
                    segments = result.segments,
                    language = result.language,
                    model = "whisper small-q5_1",
                    title = if (text.isEmpty()) stored.note.title else Note.makeTitle(text)
                )
                refresh(store.save(n, text, stored.folder))
            } catch (e: Exception) {
                android.util.Log.e("GlassnoteWhisper", "pipeline failed", e)
            } finally {
                _state.update { it.copy(transcribing = it.transcribing - 1) }
            }
        }
    }

    fun search(query: String): List<StoredNote> {
        val q = query.trim()
        if (q.isEmpty()) return _notes.value
        return _notes.value.filter { it.transcript.contains(q, true) || it.note.title.contains(q, true) }
    }
}
