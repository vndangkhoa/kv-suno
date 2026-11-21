package com.kvsuno.app.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val apkUrl: String?,
    val pageUrl: String
)

object UpdateChecker {
    private const val OWNER = "vndangkhoa"
    private const val REPO = "kv-suno"
    private const val PKG_PAGE = "https://syno.vndns.net/package/kvsuno"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatest(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        fetchFrom("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=1")
            ?: fetchFrom("https://git.khoavo.myds.me/api/v1/repos/$OWNER/$REPO/releases?limit=1")
            ?: fetchFrom("https://git.khoavo.vndns.net/api/v1/repos/$OWNER/$REPO/releases?limit=1")
    }

    private fun fetchFrom(url: String): AppUpdateInfo? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONArray(resp.body?.string() ?: return null)
                if (arr.length() == 0) return null
                val r = arr.getJSONObject(0)
                val assets = r.optJSONArray("assets")
                var apk: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apk = a.optString("browser_download_url").ifEmpty { null }
                            break
                        }
                    }
                }
                AppUpdateInfo(
                    tag = r.optString("tag_name"),
                    name = r.optString("name").ifEmpty { r.optString("tag_name") },
                    notes = r.optString("notes").ifEmpty { r.optString("body") },
                    apkUrl = apk ?: "https://syno.vndns.net/kvsuno-release.apk",
                    pageUrl = r.optString("html_url").ifEmpty { PKG_PAGE }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /** True when [latestTag] (e.g. "v2.0.4") is newer than [current] ("2.0.3"). */
    fun isNewer(latestTag: String, current: String): Boolean {
        fun parts(v: String): List<Int> =
            v.trim().trimStart('v', 'V').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val l = parts(latestTag)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val d = l.getOrElse(i) { 0 } - c.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }
}
