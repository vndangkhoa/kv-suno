package com.kvsuno.app.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"
    private const val PREFS_NAME = "kv_suno_network_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_DIRECT_FAILOVER = "direct_suno_failover"
    private const val KEY_PROXY_URL = "proxy_url"
    const val DEFAULT_BASE_URL = "https://suno.khoavo.myds.me"

    private var prefs: SharedPreferences? = null
    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var directFailoverEnabled: Boolean = true
    private var proxyUrl: String = ""

    private var apiInstance: KVSunoApi? = null
    private var mediaClientInstance: OkHttpClient? = null
    private var downloadClientInstance: OkHttpClient? = null

    val sharedConnectionPool: ConnectionPool = ConnectionPool(8, 5, TimeUnit.MINUTES)

    private val cloudflareFallbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (systemErr: Exception) {
                val doh = lookupCloudflareDoH(hostname)
                if (doh.isNotEmpty()) doh else throw systemErr
            }
        }
    }

    private fun lookupCloudflareDoH(hostname: String): List<InetAddress> {
        return try {
            val encoded = URLEncoder.encode(hostname, "UTF-8")
            val conn = (URL("https://1.1.1.1/dns-query?name=$encoded&type=A").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/dns-json")
                connectTimeout = 2500
                readTimeout = 2500
            }
            conn.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val answers = json.optJSONArray("Answer") ?: return emptyList()
                val result = ArrayList<InetAddress>()
                for (i in 0 until answers.length()) {
                    val item = answers.optJSONObject(i) ?: continue
                    if (item.optInt("type") == 1) {
                        val ip = item.optString("data").trim()
                        if (ip.isNotEmpty()) {
                            runCatching { InetAddress.getByName(ip) }.getOrNull()?.let { result.add(it) }
                        }
                    }
                }
                result
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        val savedUrl = p.getString(KEY_BASE_URL, null)?.trim().orEmpty()
        if (savedUrl.isNotBlank()) {
            currentBaseUrl = normalizeServerUrl(savedUrl)
        }
        directFailoverEnabled = p.getBoolean(KEY_DIRECT_FAILOVER, true)
        proxyUrl = p.getString(KEY_PROXY_URL, "")?.trim().orEmpty()
        apiInstance = null
        mediaClientInstance = null
        downloadClientInstance = null
    }

    private fun normalizeServerUrl(raw: String): String {
        var formatted = raw.trim()
        if (formatted.isBlank()) return DEFAULT_BASE_URL
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            val isLan = formatted.startsWith("192.168.") ||
                formatted.startsWith("10.") ||
                formatted.startsWith("172.") ||
                formatted.startsWith("127.0.0.1") ||
                formatted.startsWith("localhost") ||
                formatted.contains(":")
            formatted = if (isLan) "http://$formatted" else "https://$formatted"
        }
        return formatted.trimEnd('/')
    }

    private fun normalizeProxyUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.contains("://")) trimmed.trimEnd('/') else "http://$trimmed".trimEnd('/')
    }

    fun getBaseUrl(): String = currentBaseUrl

    @Synchronized
    fun setBaseUrl(newUrl: String) {
        val formatted = normalizeServerUrl(newUrl)
        prefs?.edit()?.putString(KEY_BASE_URL, formatted)?.apply()
        if (currentBaseUrl != formatted || apiInstance == null) {
            currentBaseUrl = formatted
            apiInstance = createApi(formatted)
            mediaClientInstance = null
            downloadClientInstance = null
        }
    }

    fun isDirectFailoverEnabled(): Boolean = directFailoverEnabled

    @Synchronized
    fun setDirectFailoverEnabled(enabled: Boolean) {
        directFailoverEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_DIRECT_FAILOVER, enabled)?.apply()
    }

    fun getProxyUrl(): String = proxyUrl

    @Synchronized
    fun setProxyUrl(rawProxy: String) {
        val normalized = normalizeProxyUrl(rawProxy)
        proxyUrl = normalized
        prefs?.edit()?.putString(KEY_PROXY_URL, normalized)?.apply()
        mediaClientInstance = null
        downloadClientInstance = null
        apiInstance = null
    }

    fun parseProxyOrNull(raw: String = proxyUrl): Proxy? {
        if (raw.isBlank()) return null
        return try {
            val normalized = normalizeProxyUrl(raw)
            val uri = URI(normalized)
            val host = uri.host ?: return null
            val scheme = uri.scheme?.lowercase().orEmpty()
            val defaultPort = if (scheme.startsWith("socks")) 1080 else 29083
            val port = if (uri.port > 0) uri.port else defaultPort
            val type = if (scheme.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(type, InetSocketAddress.createUnresolved(host, port))
        } catch (_: Exception) {
            null
        }
    }

    fun prewarmConnection() {
        try {
            val req = Request.Builder()
                .url("$currentBaseUrl/api/suno-crawler-status")
                .build()
            getMediaOkHttpClient().newCall(req).execute().use { resp ->
                Log.d(TAG, "Prewarmed HTTP/2 socket to $currentBaseUrl (HTTP ${resp.code})")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Prewarm skipped: ${e.message}")
        }
    }

    val mediaCallFactory: Call.Factory = Call.Factory { request ->
        getMediaOkHttpClient().newCall(request)
    }

    private fun buildMediaClient(readTimeoutSec: Long, writeTimeoutSec: Long): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .dns(cloudflareFallbackDns)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request()
                val updated = req.newBuilder()
                    .header("User-Agent", "KVSuno-Android/2.0.3 (Linux; Android 14)")
                    .build()
                chain.proceed(updated)
            }

        parseProxyOrNull()?.let { proxy ->
            val serverHost = runCatching { URI(currentBaseUrl).host?.lowercase() }.getOrNull().orEmpty()
            builder.proxySelector(object : java.net.ProxySelector() {
                override fun select(uri: URI?): List<Proxy> {
                    val h = uri?.host?.lowercase().orEmpty()
                    val isDirectLocal = h.isBlank() ||
                        h == "localhost" ||
                        h == "127.0.0.1" ||
                        h == "::1" ||
                        h == serverHost ||
                        h.endsWith(".local") ||
                        h.startsWith("192.168.") ||
                        h.startsWith("10.")
                    return if (isDirectLocal) listOf(Proxy.NO_PROXY) else listOf(proxy)
                }

                override fun connectFailed(uri: URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {}
            })
        }

        return builder.build()
    }

    @Synchronized
    fun getMediaOkHttpClient(): OkHttpClient {
        mediaClientInstance?.let { return it }
        return buildMediaClient(readTimeoutSec = 35, writeTimeoutSec = 30)
            .also { mediaClientInstance = it }
    }

    @Synchronized
    fun getDownloadOkHttpClient(): OkHttpClient {
        downloadClientInstance?.let { return it }
        return buildMediaClient(readTimeoutSec = 120, writeTimeoutSec = 60)
            .also { downloadClientInstance = it }
    }

    @Synchronized
    fun getApi(): KVSunoApi {
        if (apiInstance == null) {
            apiInstance = createApi(currentBaseUrl)
        }
        return apiInstance!!
    }

    private fun createApi(baseUrl: String): KVSunoApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .dns(cloudflareFallbackDns)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(KVSunoApi::class.java)
    }

    fun getStreamUrl(trackId: String): String {
        return "$currentBaseUrl/api/suno-audio-proxy?input=$trackId"
    }

    fun getDownloadUrl(trackId: String, title: String): String {
        val encodedTitle = URLEncoder.encode(title.ifBlank { "track" }, "UTF-8")
        return "$currentBaseUrl/api/download-suno-m4a?id=$trackId&title=$encodedTitle"
    }
}
