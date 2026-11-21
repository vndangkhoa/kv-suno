package com.kvsuno.app.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.kvsuno.app.data.api.RetrofitClient
import com.kvsuno.app.data.download.OfflineDownloadManager
import com.kvsuno.app.data.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages a 250 MiB Media3 SimpleCache LRU disk cache for Suno AAC audio streams
 * plus a bounded 256 KiB head preloader for instant tap-to-audio start latency.
 */
@OptIn(UnstableApi::class)
object AudioCacheManager {

    private const val TAG = "AudioCache"
    private const val CACHE_DIR_NAME = "exo_suno_cache"
    private const val MAX_CACHE_BYTES = 250L * 1024L * 1024L
    const val PRELOAD_BYTES = 262_144L
    private const val CACHE_KEY_PREFIX = "kv_suno_"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preloadSemaphore = Semaphore(2)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    @Volatile
    private var simpleCache: SimpleCache? = null
    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    fun trackCacheKey(trackId: String): String = "$CACHE_KEY_PREFIX${trackId.trim()}"

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        simpleCache?.let { return it }
        val appContext = context.applicationContext
        val dir = File(appContext.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val db = databaseProvider ?: StandaloneDatabaseProvider(appContext).also {
            databaseProvider = it
        }
        return SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            db
        ).also { simpleCache = it }
    }

    fun isHeadCached(context: Context, trackId: String, minBytes: Long = 64L * 1024L): Boolean {
        val cleanId = trackId.trim()
        if (cleanId.isEmpty()) return false
        return try {
            getCache(context).getCachedBytes(trackCacheKey(cleanId), 0L, PRELOAD_BYTES) >= minBytes
        } catch (_: Exception) {
            false
        }
    }

    fun evictTrack(context: Context, trackId: String) {
        val cleanId = trackId.trim()
        if (cleanId.isEmpty()) return
        try {
            getCache(context).removeResource(trackCacheKey(cleanId))
        } catch (_: Exception) {
        }
    }

    fun clearCache(context: Context) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        try {
            val cache = getCache(context)
            cache.keys.toList().forEach { key ->
                runCatching { cache.removeResource(key) }
            }
        } catch (_: Exception) {
        }
    }

    fun prioritizeTrack(trackId: String) {
        val keepId = trackId.trim()
        val iterator = activeJobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key != keepId) {
                entry.value.cancel()
                iterator.remove()
            }
        }
    }

    fun buildPlaybackDataSourceFactory(context: Context): DataSource.Factory {
        val appContext = context.applicationContext
        val cache = getCache(appContext)
        val okHttpFactory = OkHttpDataSource.Factory(RetrofitClient.mediaCallFactory)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okHttpFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val localDataSourceFactory = DefaultDataSource.Factory(appContext, okHttpFactory)

        return DataSource.Factory {
            RoutingDataSource(
                cacheDataSource = cacheDataSourceFactory.createDataSource(),
                directHttpDataSource = okHttpFactory.createDataSource(),
                localDataSource = localDataSourceFactory.createDataSource()
            )
        }
    }

    fun preloadTracks(context: Context, tracks: List<Track>, maxCount: Int = 4) {
        if (tracks.isEmpty() || maxCount <= 0) return
        val appContext = context.applicationContext

        val candidates = tracks
            .asSequence()
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .take(maxCount)
            .toList()

        for (track in candidates) {
            val cleanId = track.id.trim()
            if (OfflineDownloadManager.isDownloaded(cleanId) || isHeadCached(appContext, cleanId)) {
                continue
            }
            if (activeJobs.containsKey(cleanId)) continue

            val job = scope.launch {
                preloadSemaphore.withPermit {
                    if (OfflineDownloadManager.isDownloaded(cleanId) || isHeadCached(appContext, cleanId)) {
                        return@withPermit
                    }
                    preloadSingleHead(appContext, track)
                }
            }
            val existing = activeJobs.putIfAbsent(cleanId, job)
            if (existing != null) {
                job.cancel()
            } else {
                job.invokeOnCompletion { activeJobs.remove(cleanId, job) }
            }
        }
    }

    private suspend fun preloadSingleHead(context: Context, track: Track) {
        val cleanId = track.id.trim()
        val url = RetrofitClient.getStreamUrl(cleanId)

        val cache = getCache(context)
        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(RetrofitClient.mediaCallFactory))
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .createDataSource()

        val dataSpec = DataSpec.Builder()
            .setUri(Uri.parse(url))
            .setPosition(0L)
            .setLength(PRELOAD_BYTES)
            .setKey(trackCacheKey(cleanId))
            .build()

        val writer = CacheWriter(
            cacheDataSource,
            dataSpec,
            ByteArray(32 * 1024),
            null
        )

        val currentJob = kotlin.coroutines.coroutineContext[Job]
        val cancelHook = currentJob?.invokeOnCompletion { cause ->
            if (cause != null) {
                writer.cancel()
            }
        }

        val startMs = SystemClock.elapsedRealtime()
        try {
            writer.cache()
            val elapsed = SystemClock.elapsedRealtime() - startMs
            Log.d(TAG, "Preloaded Suno head track=$cleanId in ${elapsed}ms")
        } catch (ce: CancellationException) {
            writer.cancel()
            throw ce
        } catch (e: Exception) {
            Log.d(TAG, "Preload skipped track=$cleanId: ${e.message}")
        } finally {
            cancelHook?.dispose()
        }
    }

    private class RoutingDataSource(
        private val cacheDataSource: CacheDataSource,
        private val directHttpDataSource: DataSource,
        private val localDataSource: DataSource
    ) : DataSource {

        private var active: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            cacheDataSource.addTransferListener(transferListener)
            directHttpDataSource.addTransferListener(transferListener)
            localDataSource.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val scheme = dataSpec.uri.scheme?.lowercase().orEmpty()
            val isHttp = scheme == "http" || scheme == "https"
            val useCache = isHttp && dataSpec.key?.startsWith(CACHE_KEY_PREFIX) == true
            val selected = when {
                useCache -> cacheDataSource
                isHttp -> directHttpDataSource
                else -> localDataSource
            }
            active = selected
            return selected.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return active?.read(buffer, offset, length) ?: -1
        }

        override fun getUri(): Uri? = active?.uri

        override fun getResponseHeaders(): Map<String, List<String>> {
            return active?.responseHeaders ?: emptyMap()
        }

        override fun close() {
            try {
                active?.close()
            } finally {
                active = null
            }
        }
    }
}
