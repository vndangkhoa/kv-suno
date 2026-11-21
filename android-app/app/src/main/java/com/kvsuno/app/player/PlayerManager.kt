package com.kvsuno.app.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kvsuno.app.KVSunoApp
import com.kvsuno.app.data.api.DirectSunoClient
import com.kvsuno.app.data.api.RetrofitClient
import com.kvsuno.app.data.download.OfflineDownloadManager
import com.kvsuno.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

object PlayerManager {

    private const val TAG = "PlayerManager"

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activePlayJob: Job? = null
    private var prefetchJob: Job? = null
    private var enrichJob: Job? = null

    @Volatile
    private var currentPlayUsedDirectCdn: Boolean = false
    private val failoverAttemptedTrackIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    @Volatile
    private var benchTrackId: String = ""
    @Volatile
    private var benchTapElapsedMs: Long = 0L

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentCoverBitmap = MutableStateFlow<Bitmap?>(null)
    val currentCoverBitmap: StateFlow<Bitmap?> = _currentCoverBitmap

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat

    private val _waveformBars = MutableStateFlow<List<Float>>(emptyList())
    val waveformBars: StateFlow<List<Float>> = _waveformBars

    private val _likedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val likedTrackIds: StateFlow<Set<String>> = _likedTrackIds

    private val _recentlyPlayedHistory = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayedHistory: StateFlow<List<Track>> = _recentlyPlayedHistory

    private val _lastLoadLatencyMs = MutableStateFlow<Long?>(null)
    val lastLoadLatencyMs: StateFlow<Long?> = _lastLoadLatencyMs

    private val _lastLoadSource = MutableStateFlow<String?>(null)
    val lastLoadSource: StateFlow<String?> = _lastLoadSource

    val downloadedTracks: StateFlow<List<Track>> get() = OfflineDownloadManager.downloadedTracks
    val downloadedTrackIds: StateFlow<Set<String>> get() = OfflineDownloadManager.downloadedTrackIds
    val downloadProgress: StateFlow<Map<String, Float>> get() = OfflineDownloadManager.downloadProgress
    val totalDownloadBytes: StateFlow<Long> get() = OfflineDownloadManager.totalStorageBytes

    init {
        startProgressTracker()
    }

    @OptIn(UnstableApi::class)
    fun getExoPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val appCtx = context.applicationContext
            OfflineDownloadManager.init(appCtx)
            loadLikedIdsFromPrefs(appCtx)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val dataSourceFactory = AudioCacheManager.buildPlaybackDataSourceFactory(appCtx)

            val mediaSourceFactory = DefaultMediaSourceFactory(appCtx)
                .setDataSourceFactory(dataSourceFactory)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 50_000, 250, 750)
                .build()

            exoPlayer = ExoPlayer.Builder(appCtx)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            _isPlaying.value = playing
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                _duration.value = duration.coerceAtLeast(0L)
                                val track = _currentTrack.value
                                if (track != null && track.id == benchTrackId) {
                                    val readyMs = (SystemClock.elapsedRealtime() - benchTapElapsedMs).coerceAtLeast(0L)
                                    _lastLoadLatencyMs.value = readyMs
                                    _lastLoadSource.value = if (currentPlayUsedDirectCdn) "SUNO_CDN_MP4" else "PROXY_CACHE"
                                }
                            } else if (state == Player.STATE_ENDED) {
                                playNext()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            val track = _currentTrack.value ?: return
                            Log.w(TAG, "Playback error on ${track.id} (directCdn=$currentPlayUsedDirectCdn): ${error.message}")
                            if (!failoverAttemptedTrackIds.add(track.id)) return

                            if (!currentPlayUsedDirectCdn && RetrofitClient.isDirectFailoverEnabled()) {
                                AudioCacheManager.evictTrack(appCtx, track.id)
                                val directUrl = track.videoUrl.takeIf { it.startsWith("http") }
                                    ?: DirectSunoClient.directCdnStreamUrl(track.id)
                                startMediaItem(
                                    track = track,
                                    streamUrl = directUrl,
                                    mimeType = null,
                                    usedDirectCdn = true,
                                    cacheKey = null
                                )
                            } else if (currentPlayUsedDirectCdn) {
                                val proxyUrl = RetrofitClient.getStreamUrl(track.id)
                                startMediaItem(
                                    track = track,
                                    streamUrl = proxyUrl,
                                    mimeType = null,
                                    usedDirectCdn = false,
                                    cacheKey = AudioCacheManager.trackCacheKey(track.id)
                                )
                            }
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    private fun buildServerStreamUrl(track: Track): String {
        val raw = track.url.trim()
        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("/api/") -> "${RetrofitClient.getBaseUrl()}$raw"
            else -> RetrofitClient.getStreamUrl(track.id)
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val appCtx = KVSunoApp.instance
        getExoPlayer(appCtx)
        activePlayJob?.cancel()
        enrichJob?.cancel()
        failoverAttemptedTrackIds.remove(track.id)

        benchTrackId = track.id
        benchTapElapsedMs = SystemClock.elapsedRealtime()
        _lastLoadLatencyMs.value = null
        _lastLoadSource.value = null

        _currentTrack.value = track
        _queue.value = newQueue
        _queueIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val currentHistory = _recentlyPlayedHistory.value.toMutableList()
        currentHistory.removeAll { it.id == track.id }
        currentHistory.add(0, track)
        _recentlyPlayedHistory.value = currentHistory.take(50)

        generateWaveform(track.id)

        // Enrich lyrics & Style DNA tags asynchronously if missing
        if (track.promptLyrics.isBlank() || track.tags.isBlank()) {
            enrichJob = scope.launch {
                val enriched = runCatching { appCtx.musicRepository.getClipInfo(track.id) }.getOrNull()
                if (enriched != null && _currentTrack.value?.id == track.id) {
                    val merged = track.copy(
                        tags = track.tags.ifBlank { enriched.tags },
                        promptLyrics = track.promptLyrics.ifBlank { enriched.promptLyrics },
                        duration = if (track.duration > 0) track.duration else enriched.duration,
                        modelName = track.modelName.ifBlank { enriched.modelName }
                    )
                    _currentTrack.value = merged
                }
            }
        }

        // 1. Offline local file playback (< 30ms, zero network)
        val offlineFile = OfflineDownloadManager.getOfflineFile(track.id)
        if (offlineFile != null) {
            startMediaItem(
                track = track,
                streamUrl = Uri.fromFile(offlineFile).toString(),
                mimeType = null,
                usedDirectCdn = false,
                cacheKey = null
            )
            prefetchUpcoming(newQueue, _queueIndex.value)
            return
        }

        // 2. Primary fast path: SimpleCache-backed Suno proxy stream (auto-failover to cdn1.suno.ai mp4)
        AudioCacheManager.prioritizeTrack(track.id)
        val serverStreamUrl = buildServerStreamUrl(track)
        startMediaItem(
            track = track,
            streamUrl = serverStreamUrl,
            mimeType = null,
            usedDirectCdn = false,
            cacheKey = AudioCacheManager.trackCacheKey(track.id)
        )

        prefetchUpcoming(newQueue, _queueIndex.value)
    }

    private fun prefetchUpcoming(queue: List<Track>, currentIndex: Int) {
        val nextTracks = ((currentIndex + 1)..(currentIndex + 3))
            .mapNotNull { idx -> queue.getOrNull(idx) }
        if (nextTracks.isEmpty()) return

        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.IO) {
            delay(250)
            if (!isActive) return@launch
            AudioCacheManager.preloadTracks(KVSunoApp.instance, nextTracks, maxCount = 3)
        }
    }

    private fun startMediaItem(
        track: Track,
        streamUrl: String,
        mimeType: String?,
        usedDirectCdn: Boolean,
        cacheKey: String?
    ) {
        val player = exoPlayer ?: getExoPlayer(KVSunoApp.instance)
        currentPlayUsedDirectCdn = usedDirectCdn

        val itemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.displayArtistHandle)
                    .setAlbumTitle(track.primaryGenreTag)
                    .setArtworkUri(Uri.parse(track.coverUrl))
                    .setIsPlayable(true)
                    .build()
            )
        if (!mimeType.isNullOrBlank()) {
            itemBuilder.setMimeType(mimeType)
        }
        if (!cacheKey.isNullOrBlank()) {
            itemBuilder.setCustomCacheKey(cacheKey)
        }
        val mediaItem = itemBuilder.build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        _isPlaying.value = true

        try {
            val context = KVSunoApp.instance
            val serviceIntent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scope.launch(Dispatchers.IO) {
            try {
                val context = KVSunoApp.instance
                val request = ImageRequest.Builder(context)
                    .data(track.coverUrl)
                    .allowHardware(false)
                    .build()
                val result = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        if (_currentTrack.value?.id == track.id) {
                            _currentCoverBitmap.value = bitmap
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        if (_isShuffle.value && q.size > 1) {
            val candidates = q.indices.filter { it != _queueIndex.value }
            val pick = candidates.randomOrNull() ?: 0
            playTrack(q[pick], q)
            return
        }
        var nextIdx = _queueIndex.value + 1
        if (nextIdx >= q.size) {
            if (_isRepeat.value) {
                nextIdx = 0
            } else {
                return
            }
        }
        playTrack(q[nextIdx], q)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        val prevIdx = (_queueIndex.value - 1).coerceAtLeast(0)
        playTrack(q[prevIdx], q)
    }

    fun toggleDownload(context: Context, track: Track) {
        OfflineDownloadManager.toggleDownload(context, track)
    }

    fun downloadTracks(context: Context, tracks: List<Track>) {
        OfflineDownloadManager.downloadTracks(context, tracks)
    }

    fun toggleLike(trackId: String) {
        val current = _likedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        _likedTrackIds.value = current
        saveLikedIdsToPrefs(current)
    }

    fun isTrackLiked(trackId: String): Boolean = _likedTrackIds.value.contains(trackId)

    private fun loadLikedIdsFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("kv_suno_likes", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("liked_ids", emptySet()) ?: emptySet()
        _likedTrackIds.value = saved.toSet()
    }

    private fun saveLikedIdsToPrefs(ids: Set<String>) {
        runCatching {
            val prefs = KVSunoApp.instance.getSharedPreferences("kv_suno_likes", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("liked_ids", ids).apply()
        }
    }

    fun clearAllUserData() {
        _likedTrackIds.value = emptySet()
        saveLikedIdsToPrefs(emptySet())
        _recentlyPlayedHistory.value = emptyList()
        _currentTrack.value = null
        _queue.value = emptyList()
        _queueIndex.value = 0
        _isPlaying.value = false
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    private fun generateWaveform(seedStr: String) {
        val seed = seedStr.hashCode().toLong()
        val random = Random(seed)
        val barCount = 70
        val bars = ArrayList<Float>(barCount)
        for (i in 0 until barCount) {
            val norm = (i.toFloat() / barCount)
            val envelope = (sin(norm * Math.PI) * 0.4 + 0.6).toFloat()
            val amp = (random.nextFloat() * 0.7f + 0.3f) * envelope
            bars.add(amp.coerceIn(0.12f, 1.0f))
        }
        _waveformBars.value = bars
    }

    private fun startProgressTracker() {
        scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration.coerceAtLeast(0L)
                    }
                }
                delay(200)
            }
        }
    }
}
