package com.sleepycoffee.glassnote.transcription

import com.sleepycoffee.glassnote.data.NoteSegment
import java.io.File

data class TranscriptionResult(val segments: List<NoteSegment>, val language: String?)

interface Transcriber {
    val ready: Boolean
    suspend fun transcribe(audio: File, languageCode: String?): TranscriptionResult
}

/**
 * Заглушка до интеграции whisper.cpp.
 * Записи сохраняются и архивируются уже сейчас; текст появится после подключения
 * нативной библиотеки (см. README — WhisperBridge через JNI + модель ggml).
 */
class PendingTranscriber : Transcriber {
    override val ready = false
    override suspend fun transcribe(audio: File, languageCode: String?) =
        TranscriptionResult(emptyList(), languageCode)
}
