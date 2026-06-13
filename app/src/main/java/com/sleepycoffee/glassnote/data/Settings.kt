package com.sleepycoffee.glassnote.data

import android.content.Context

/** Язык распознавания: auto/ru/en. По умолчанию автоопределение (как на macOS). */
enum class RecordingLanguage(val code: String, val label: String) {
    AUTO("auto", "Автоопределение"),
    RU("ru", "Русский"),
    EN("en", "English");

    val whisperCode: String? get() = if (this == AUTO) null else code

    companion object {
        fun from(code: String?): RecordingLanguage = entries.firstOrNull { it.code == code } ?: AUTO
    }
}

object Settings {
    private const val PREF = "glassnote"
    private const val KEY_LANG = "recordingLanguage"

    fun language(ctx: Context): RecordingLanguage =
        RecordingLanguage.from(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LANG, "auto"))

    fun setLanguage(ctx: Context, lang: RecordingLanguage) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_LANG, lang.code).apply()
}
