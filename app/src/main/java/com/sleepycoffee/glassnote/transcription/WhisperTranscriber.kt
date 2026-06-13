package com.sleepycoffee.glassnote.transcription

import com.sleepycoffee.glassnote.data.NoteSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Расшифровка через whisper.cpp. Контекст модели инициализируется лениво. */
class WhisperTranscriber(private val modelProvider: () -> File?) : Transcriber {
    @Volatile private var ctxPtr = 0L
    @Volatile private var loadedPath: String? = null

    override val ready get() = modelProvider()?.exists() == true

    override suspend fun transcribe(audio: File, languageCode: String?): TranscriptionResult =
        withContext(Dispatchers.Default) {
            val model = modelProvider()?.takeIf { it.exists() }
                ?: return@withContext TranscriptionResult(emptyList(), null)

            if (ctxPtr == 0L || loadedPath != model.absolutePath) {
                if (ctxPtr != 0L) WhisperBridge.nativeFree(ctxPtr)
                ctxPtr = WhisperBridge.nativeInit(model.absolutePath)
                loadedPath = model.absolutePath
            }
            if (ctxPtr == 0L) return@withContext TranscriptionResult(emptyList(), null)

            val samples = AudioDecoder.decodeToMonoFloat16k(audio)
            if (samples.isEmpty()) return@withContext TranscriptionResult(emptyList(), languageCode)

            val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            val raw = WhisperBridge.nativeTranscribe(ctxPtr, samples, languageCode, threads)
                ?: return@withContext TranscriptionResult(emptyList(), null)

            val detected = raw.getOrNull(0)?.ifBlank { null }
            val segs = raw.drop(1).mapNotNull { line ->
                val p = line.split("|", limit = 3)
                if (p.size < 3) null
                else NoteSegment(
                    start = (p[0].toLongOrNull() ?: 0) / 100.0,
                    end = (p[1].toLongOrNull() ?: 0) / 100.0,
                    text = p[2].trim()
                )
            }.filter { it.text.isNotBlank() }
            TranscriptionResult(segs, detected)
        }
}
