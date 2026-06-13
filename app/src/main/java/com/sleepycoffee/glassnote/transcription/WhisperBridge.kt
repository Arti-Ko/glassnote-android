package com.sleepycoffee.glassnote.transcription

/** JNI-мост к whisper.cpp. */
object WhisperBridge {
    init { System.loadLibrary("glassnote") }

    /** @return указатель на whisper_context или 0 при ошибке. */
    external fun nativeInit(modelPath: String): Long

    /** @return массив строк: [0]=код языка, [1..]="t0|t1|текст" (t в единицах 10мс). */
    external fun nativeTranscribe(ctx: Long, samples: FloatArray, lang: String?, threads: Int): Array<String>?

    external fun nativeFree(ctx: Long)
}
