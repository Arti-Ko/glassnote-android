package com.sleepycoffee.glassnote.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import kotlin.math.log10

/** Запись в m4a (AAC mono 48kHz) — тот же формат, что и на macOS. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var outputFile: File? = null; private set
    private var startedAt = 0L

    fun start(output: File) {
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(48_000)
        r.setAudioChannels(1)
        r.setAudioEncodingBitRate(96_000)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        outputFile = output
        startedAt = SystemClock.elapsedRealtime()
    }

    fun elapsedSec(): Double = (SystemClock.elapsedRealtime() - startedAt) / 1000.0

    /** Нормализованный уровень микрофона 0..1 для волны. */
    fun level(): Float {
        val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        if (amp <= 0) return 0f
        val db = 20.0 * log10(amp.toDouble())
        return ((db - 30) / 60).coerceIn(0.0, 1.0).toFloat()
    }

    fun stop(): Double {
        val dur = elapsedSec()
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        return dur
    }
}
