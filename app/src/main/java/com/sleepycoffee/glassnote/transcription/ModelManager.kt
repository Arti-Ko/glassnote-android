package com.sleepycoffee.glassnote.transcription

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Загружает ggml-модель whisper при первом запуске. small-q5_1 — баланс ru-качества и размера (~182МБ). */
object ModelManager {
    private const val MODEL_NAME = "ggml-small-q5_1.bin"
    private const val URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin"

    sealed interface State {
        data object Idle : State
        data class Downloading(val percent: Int) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun modelFile(ctx: Context): File =
        File(ctx.filesDir, "models/$MODEL_NAME")

    fun isReady(ctx: Context): Boolean = modelFile(ctx).exists()

    suspend fun ensure(ctx: Context): File? = withContext(Dispatchers.IO) {
        val target = modelFile(ctx)
        if (target.exists() && target.length() > 0) { _state.value = State.Ready; return@withContext target }
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "$MODEL_NAME.part")
        try {
            _state.value = State.Downloading(0)
            val conn = (URL(URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000; readTimeout = 30_000; instanceFollowRedirects = true
            }
            conn.inputStream.use { input ->
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
                tmp.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    var read: Int; var done = 0L
                    while (input.read(buf).also { read = it } >= 0) {
                        out.write(buf, 0, read); done += read
                        if (total > 0) _state.value = State.Downloading((done * 100 / total).toInt())
                    }
                }
            }
            if (tmp.renameTo(target)) { _state.value = State.Ready; target }
            else { _state.value = State.Failed("Не удалось сохранить модель"); null }
        } catch (e: Exception) {
            tmp.delete()
            _state.value = State.Failed(e.message ?: "Ошибка загрузки модели")
            null
        }
    }
}
