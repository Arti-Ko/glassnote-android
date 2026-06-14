package com.sleepycoffee.glassnote.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sleepycoffee.glassnote.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val pageUrl: String, val apkUrl: String?)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class Error(val message: String) : UpdateState
}

/** Проверка/скачивание/установка обновлений через GitHub Releases. */
object UpdateChecker {
    private const val REPO = "Arti-Ko/glassnote-android"
    val currentVersion: String get() = BuildConfig.VERSION_NAME

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available: StateFlow<UpdateInfo?> = _available.asStateFlow()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    suspend fun check(manual: Boolean = false) {
        if (manual) _state.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000; readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                if (conn.responseCode != 200) { if (manual) _state.value = UpdateState.Error("HTTP ${conn.responseCode}"); return@runCatching }
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
                if (tag.isNotBlank() && isNewer(tag, currentVersion)) {
                    val info = UpdateInfo(tag, page, apk)
                    _available.value = info
                    _state.value = UpdateState.Available(info)
                } else if (manual) {
                    _state.value = UpdateState.UpToDate
                }
            }.onFailure { if (manual) _state.value = UpdateState.Error(it.message ?: "ошибка сети") }
        }
    }

    /** Скачивает APK и запускает системный установщик (auto-update для sideload). */
    suspend fun install(ctx: Context) {
        val info = _available.value ?: return
        val url = info.apkUrl
        if (url == null) { openPage(ctx, info.pageUrl); return }
        withContext(Dispatchers.IO) {
            runCatching {
                _state.value = UpdateState.Downloading(0)
                val file = File(ctx.cacheDir, "glassnote-update.apk")
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
                }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
                conn.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(1 shl 16); var read: Int; var done = 0L
                        while (input.read(buf).also { read = it } >= 0) {
                            out.write(buf, 0, read); done += read
                            if (total > 0) _state.value = UpdateState.Downloading((done * 100 / total).toInt())
                        }
                    }
                }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                _state.value = UpdateState.Available(info)
            }.onFailure { _state.value = UpdateState.Error(it.message ?: "ошибка загрузки") }
        }
    }

    private fun openPage(ctx: Context, page: String) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
