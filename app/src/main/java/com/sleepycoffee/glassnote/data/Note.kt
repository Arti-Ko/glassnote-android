package com.sleepycoffee.glassnote.data

import kotlinx.serialization.Serializable

@Serializable
data class NoteSegment(
    val start: Double,
    val end: Double,
    val text: String,
    val speaker: String? = null   // заполняется в v2 (диаризация)
)

@Serializable
data class Note(
    val id: String,
    val createdAt: String,        // ISO8601 — совпадает с macOS-версией
    val durationSec: Double,
    val language: String? = null,
    val model: String,
    val title: String,
    val edited: Boolean = false,
    val segments: List<NoteSegment> = emptyList()
) {
    companion object {
        fun makeTitle(text: String, maxWords: Int = 8): String {
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) return "Без названия"
            val t = words.take(maxWords).joinToString(" ")
            return if (words.size > maxWords) "$t…" else t
        }
    }
}
