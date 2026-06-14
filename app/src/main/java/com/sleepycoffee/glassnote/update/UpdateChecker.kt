package com.sleepycoffee.glassnote.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
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

/** Проверка через GitHub Releases; установка — через системный DownloadManager
 *  (надёжно тянет редиректы GitHub) + системный установщик APK. */
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
                    setRequestProperty("User-Agent", "Glassnote-Android")
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
            }.onFailure { android.util.Log.w("GlassnoteUpd", "check failed", it); if (manual) _state.value = UpdateState.Error(it.message ?: "ошибка сети") }
        }
    }

    fun install(ctx: Context) {
        val info = _available.value ?: return
        val url = info.apkUrl
        if (url == null) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }

        _state.value = UpdateState.Downloading(0)
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val target = File(ctx.getExternalFilesDir(null), "glassnote-update.apk")
        target.delete()
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("Glassnote ${info.version}")
            .setDescription("Загрузка обновления")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(target))
        val id = dm.enqueue(req)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
                runCatching { c.unregisterReceiver(this) }
                var ok = false
                dm.query(DownloadManager.Query().setFilterById(id)).use { cur ->
                    if (cur.moveToFirst()) {
                        ok = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
                    }
                }
                // целостность: файл существует и начинается с ZIP-сигнатуры PK
                val valid = ok && target.exists() && target.length() > 1_000_000 && runCatching {
                    target.inputStream().use { it.read() == 0x50 && it.read() == 0x4B }
                }.getOrDefault(false)
                if (!valid) { _state.value = UpdateState.Error("Загрузка не удалась"); return }
                val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", target)
                c.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                _state.value = UpdateState.Available(info)
            }
        }
        ContextCompat.registerReceiver(ctx, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
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
