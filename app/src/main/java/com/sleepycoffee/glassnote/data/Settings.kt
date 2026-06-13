package com.sleepycoffee.glassnote.data

import android.content.Context

/** Язык распознавания: auto/ru/en. */
enum class RecordingLanguage(val code: String, val label: String) {
    AUTO("auto", "Автоопределение"),
    RU("ru", "Русский"),
    EN("en", "English");

    val whisperCode: String? get() = if (this == AUTO) null else code

    companion object {
        fun from(code: String?): RecordingLanguage = entries.firstOrNull { it.code == code } ?: AUTO
    }
}

/** Оформление приложения. */
enum class ThemeMode(val code: String, val label: String) {
    SYSTEM("system", "Система"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Тёмная");

    companion object {
        fun from(code: String?): ThemeMode = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

object Settings {
    private const val PREF = "glassnote"
    private const val KEY_LANG = "recordingLanguage"
    private const val KEY_THEME = "themeMode"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun language(ctx: Context): RecordingLanguage = RecordingLanguage.from(prefs(ctx).getString(KEY_LANG, "auto"))
    fun setLanguage(ctx: Context, lang: RecordingLanguage) = prefs(ctx).edit().putString(KEY_LANG, lang.code).apply()

    fun themeMode(ctx: Context): ThemeMode = ThemeMode.from(prefs(ctx).getString(KEY_THEME, "system"))
    fun setThemeMode(ctx: Context, mode: ThemeMode) = prefs(ctx).edit().putString(KEY_THEME, mode.code).apply()
}
