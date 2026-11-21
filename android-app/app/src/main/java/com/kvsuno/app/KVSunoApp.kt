package com.kvsuno.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kvsuno.app.data.api.RetrofitClient
import com.kvsuno.app.data.download.OfflineDownloadManager
import com.kvsuno.app.data.repository.AuthRepository
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.AudioCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class KVSunoApp : Application(), ImageLoaderFactory {

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    private val appIoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this
        RetrofitClient.init(this)
        OfflineDownloadManager.init(this)
        musicRepository = MusicRepository(this)
        authRepository = AuthRepository(this)

        appIoScope.launch {
            runCatching { AudioCacheManager.getCache(this@KVSunoApp) }
            RetrofitClient.prewarmConnection()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectionPool(RetrofitClient.sharedConnectionPool)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: KVSunoApp
            private set
    }
}
