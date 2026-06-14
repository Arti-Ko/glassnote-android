package com.sleepycoffee.glassnote.update

import com.sleepycoffee.glassnote.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val pageUrl: String, val apkUrl: String?)

/** Проверка обновлений через GitHub Releases (без adb). */
object UpdateChecker {
    private const val REPO = "Arti-Ko/glassnote-android"

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    suspend fun check() = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000; readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != 200) return@withContext
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").removePrefix("v").trim()
            val page = json.optString("html_url")
            var apk: String? = null
            json.optJSONArray("assets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", true)) { apk = a.optString("browser_download_url"); break }
                }
            }
            if (tag.isNotBlank() && isNewer(tag, BuildConfig.VERSION_NAME)) {
                _update.value = UpdateInfo(tag, page, apk)
            }
        }
        Unit
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }; val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
