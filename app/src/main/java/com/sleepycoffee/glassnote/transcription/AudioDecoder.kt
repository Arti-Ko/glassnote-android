package com.sleepycoffee.glassnote.transcription

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Декодирует m4a (AAC) в mono float PCM 16 кГц — формат, который ждёт whisper. */
object AudioDecoder {
    private const val TARGET_RATE = 16_000

    fun decodeToMonoFloat16k(file: File): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                track = i; format = f; break
            }
        }
        if (track < 0 || format == null) { extractor.release(); return FloatArray(0) }
        extractor.selectTrack(track)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcm = ArrayList<Short>(srcRate * channels)
        val info = MediaCodec.BufferInfo()
        var sawInputEnd = false
        var sawOutputEnd = false

        while (!sawOutputEnd) {
            if (!sawInputEnd) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val inBuf = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(inBuf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEnd = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                val outBuf = codec.getOutputBuffer(outIndex)!!
                val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val arr = ShortArray(info.size / 2)
                shorts.get(arr)
                for (s in arr) pcm.add(s)
                codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEnd = true
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // формат вывода стабилизировался — продолжаем
            }
        }
        codec.stop(); codec.release(); extractor.release()

        // downmix в моно
        val frames = pcm.size / channels
        val mono = FloatArray(frames)
        var j = 0
        for (i in 0 until frames) {
            var acc = 0
            for (c in 0 until channels) acc += pcm[j++].toInt()
            mono[i] = (acc.toFloat() / channels) / 32768f
        }
        return resample(mono, srcRate, TARGET_RATE)
    }

    /** Усредняющий ресэмплинг (box-filter) — без анти-алиасинга whisper не слышит речь. */
    private fun resample(input: FloatArray, from: Int, to: Int): FloatArray {
        if (from == to || input.isEmpty()) return input
        val outLen = (input.size.toLong() * to / from).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        val step = from.toDouble() / to
        for (i in 0 until outLen) {
            val s = (i * step).toInt()
            val e = ((i + 1) * step).toInt().coerceAtMost(input.size).coerceAtLeast(s + 1)
            var acc = 0f
            var k = s
            while (k < e) { acc += input[k]; k++ }
            out[i] = acc / (e - s)
        }
        return out
    }
}
