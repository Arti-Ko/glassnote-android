#include <jni.h>
#include <string>
#include "whisper.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_sleepycoffee_glassnote_transcription_WhisperBridge_nativeInit(
        JNIEnv* env, jobject, jstring path_) {
    const char* path = env->GetStringUTFChars(path_, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context* ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(path_, path);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jobjectArray JNICALL
Java_com_sleepycoffee_glassnote_transcription_WhisperBridge_nativeTranscribe(
        JNIEnv* env, jobject, jlong ptr, jfloatArray samples_, jstring lang_, jint threads) {
    auto* ctx = reinterpret_cast<whisper_context*>(ptr);
    if (ctx == nullptr) return nullptr;

    jsize n = env->GetArrayLength(samples_);
    jfloat* samples = env->GetFloatArrayElements(samples_, nullptr);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);
    params.beam_search.beam_size = 5;
    params.greedy.best_of = 5;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_special = false;
    params.print_timestamps = false;
    params.translate = false;
    params.n_threads = threads;

    std::string langStr;
    if (lang_ != nullptr) {
        const char* l = env->GetStringUTFChars(lang_, nullptr);
        langStr = l;
        env->ReleaseStringUTFChars(lang_, l);
    }
    // "auto" = автоопределение + расшифровка. detect_language=true НЕ ставить —
    // это режим "только определение языка" без транскрипции (даёт 0 сегментов).
    params.language = langStr.empty() ? "auto" : langStr.c_str();
    params.detect_language = false;

    int rc = whisper_full(ctx, params, samples, n);
    env->ReleaseFloatArrayElements(samples_, samples, JNI_ABORT);
    if (rc != 0) return nullptr;

    int nseg = whisper_full_n_segments(ctx);
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray out = env->NewObjectArray(nseg + 1, strClass, nullptr);

    int langId = whisper_full_lang_id(ctx);
    const char* detected = whisper_lang_str(langId);
    env->SetObjectArrayElement(out, 0, env->NewStringUTF(detected ? detected : ""));

    for (int i = 0; i < nseg; i++) {
        int64_t t0 = whisper_full_get_segment_t0(ctx, i);
        int64_t t1 = whisper_full_get_segment_t1(ctx, i);
        const char* txt = whisper_full_get_segment_text(ctx, i);
        std::string s = std::to_string(t0) + "|" + std::to_string(t1) + "|" + (txt ? txt : "");
        env->SetObjectArrayElement(out, i + 1, env->NewStringUTF(s.c_str()));
    }
    return out;
}

JNIEXPORT void JNICALL
Java_com_sleepycoffee_glassnote_transcription_WhisperBridge_nativeFree(
        JNIEnv*, jobject, jlong ptr) {
    auto* ctx = reinterpret_cast<whisper_context*>(ptr);
    if (ctx != nullptr) whisper_free(ctx);
}

} // extern "C"
