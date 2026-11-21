package com.kvsuno.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kvsuno.app.KVSunoApp
import com.kvsuno.app.data.api.DirectSunoClient
import com.kvsuno.app.data.api.RetrofitClient
import com.kvsuno.app.data.model.ArtistHit
import com.kvsuno.app.data.model.CreatorHit
import com.kvsuno.app.data.model.GeneratedLyricsResult
import com.kvsuno.app.data.model.LyricLine
import com.kvsuno.app.data.model.PromptScoreResult
import com.kvsuno.app.data.model.StyleExplainResult
import com.kvsuno.app.data.model.SunoCrawlerStatusResponse
import com.kvsuno.app.data.model.SyncedLyricsResponse
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.model.UniversalSearchResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class MusicRepository(
    context: Context = KVSunoApp.instance
) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val api get() = RetrofitClient.getApi()

    private val chartMemCache = ConcurrentHashMap<String, List<Track>>()
    private val creatorsMemCache = ConcurrentHashMap<String, List<CreatorHit>>()
    private val clipMemCache = ConcurrentHashMap<String, Track>()

    // Prompt Studio Cart State (shared across Home, Search, FullPlayer & PromptStudio screens)
    private val _studioTags = MutableStateFlow<List<String>>(loadStringList(KEY_STUDIO_TAGS))
    val studioTags: StateFlow<List<String>> = _studioTags

    private val _customPromptText = MutableStateFlow(prefs.getString(KEY_CUSTOM_PROMPT, "") ?: "")
    val customPromptText: StateFlow<String> = _customPromptText

    private val _generatedLyricsText = MutableStateFlow(prefs.getString(KEY_STUDIO_LYRICS, "") ?: "")
    val generatedLyricsText: StateFlow<String> = _generatedLyricsText

    private val _savedPrompts = MutableStateFlow<List<String>>(loadStringList(KEY_SAVED_PROMPTS))
    val savedPrompts: StateFlow<List<String>> = _savedPrompts

    fun addStudioTag(rawTag: String) {
        val clean = rawTag.trim()
        if (clean.isEmpty()) return
        val current = _studioTags.value.toMutableList()
        if (current.none { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            _studioTags.value = current
            saveStringList(KEY_STUDIO_TAGS, current)
        }
        syncCustomPromptFromTags(current)
    }

    fun removeStudioTag(tag: String) {
        val updated = _studioTags.value.filterNot { it.equals(tag, ignoreCase = true) }
        _studioTags.value = updated
        saveStringList(KEY_STUDIO_TAGS, updated)
        syncCustomPromptFromTags(updated)
    }

    fun setCustomPromptText(text: String) {
        _customPromptText.value = text
        prefs.edit().putString(KEY_CUSTOM_PROMPT, text).apply()
    }

    fun setGeneratedLyricsText(text: String) {
        _generatedLyricsText.value = text
        prefs.edit().putString(KEY_STUDIO_LYRICS, text).apply()
    }

    fun loadFullStyle(styleString: String) {
        val parsed = styleString.split(",", ";", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        if (parsed.isEmpty()) return
        _studioTags.value = parsed
        saveStringList(KEY_STUDIO_TAGS, parsed)
        setCustomPromptText(parsed.joinToString(", "))
    }

    fun clearStudio() {
        _studioTags.value = emptyList()
        saveStringList(KEY_STUDIO_TAGS, emptyList())
        setCustomPromptText("")
    }

    fun saveCurrentPrompt(): Boolean {
        val prompt = _customPromptText.value.trim().ifBlank { _studioTags.value.joinToString(", ") }.trim()
        if (prompt.isBlank()) return false
        val updated = (listOf(prompt) + _savedPrompts.value.filterNot { it.equals(prompt, ignoreCase = true) }).take(30)
        _savedPrompts.value = updated
        saveStringList(KEY_SAVED_PROMPTS, updated)
        return true
    }

    fun removeSavedPrompt(prompt: String) {
        val updated = _savedPrompts.value.filterNot { it == prompt }
        _savedPrompts.value = updated
        saveStringList(KEY_SAVED_PROMPTS, updated)
    }

    private fun syncCustomPromptFromTags(tags: List<String>) {
        setCustomPromptText(tags.joinToString(", "))
    }

    fun getCachedSongsChart(sortBy: String = "hot", vnOnly: Boolean = false): List<Track> {
        val key = "chart_${sortBy.lowercase()}_vn_$vnOnly"
        chartMemCache[key]?.let { if (it.isNotEmpty()) return it }
        val loaded = readTracksFromPrefs(key)
        if (loaded.isNotEmpty()) {
            chartMemCache[key] = loaded
            return loaded
        }
        return if (vnOnly) DirectSunoClient.getFallbackSeedTracks().filter { it.isVietnamese }
        else DirectSunoClient.getFallbackSeedTracks()
    }

    fun getCachedCreatorsRanking(sortBy: String = "hot"): List<CreatorHit> {
        val key = "creators_${sortBy.lowercase()}"
        creatorsMemCache[key]?.let { if (it.isNotEmpty()) return it }
        val loaded = readCreatorsFromPrefs(key)
        if (loaded.isNotEmpty()) {
            creatorsMemCache[key] = loaded
            return loaded
        }
        return DirectSunoClient.getFallbackSeedCreators()
    }

    suspend fun getSongsChart(
        sortBy: String = "hot",
        creator: String = "all",
        vnOnly: Boolean = false,
        limit: Int = 80
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        val key = "chart_${sortBy.lowercase()}_vn_$vnOnly"
        try {
            val response = api.getSongsChart(sort = sortBy, creator = creator, limit = limit, vnOnly = vnOnly)
            if (response.isSuccessful && response.body() != null) {
                val tracks = response.body()!!.songs.map { it.toTrack() }
                if (tracks.isNotEmpty()) {
                    if (creator == "all") {
                        chartMemCache[key] = tracks
                        writeTracksToPrefs(key, tracks)
                    }
                    tracks.forEach { clipMemCache[it.id] = it }
                    return@withContext Result.success(tracks)
                }
            }
            Result.success(getCachedSongsChart(sortBy, vnOnly))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (RetrofitClient.isDirectFailoverEnabled() && creator != "all") {
                val (_, directTracks) = DirectSunoClient.fetchProfileClips(creator, 1)
                if (directTracks.isNotEmpty()) return@withContext Result.success(directTracks)
            }
            Result.success(getCachedSongsChart(sortBy, vnOnly))
        }
    }

    suspend fun getCreatorsRanking(sortBy: String = "hot"): Result<List<CreatorHit>> = withContext(Dispatchers.IO) {
        val key = "creators_${sortBy.lowercase()}"
        try {
            val response = api.getCreatorsRanking(sort = sortBy)
            if (response.isSuccessful && response.body() != null) {
                val creators = response.body()!!.creators.map { it.toCreatorHit() }
                if (creators.isNotEmpty()) {
                    creatorsMemCache[key] = creators
                    writeCreatorsToPrefs(key, creators)
                    return@withContext Result.success(creators)
                }
            }
            Result.success(getCachedCreatorsRanking(sortBy))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.success(getCachedCreatorsRanking(sortBy))
        }
    }

    suspend fun getCreatorProfile(handle: String, page: Int = 1): Result<Pair<CreatorHit?, List<Track>>> = withContext(Dispatchers.IO) {
        val cleanHandle = handle.removePrefix("@").trim()
        try {
            val response = api.getProfile(handle = cleanHandle, page = page)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val resolvedHandle = body.handle.ifBlank { cleanHandle }
                val resolvedName = body.displayName.ifBlank { resolvedHandle }
                val tracks = body.clips.map { it.toTrack(resolvedHandle, resolvedName) }
                tracks.forEach { clipMemCache[it.id] = it }
                val creator = CreatorHit(
                    username = resolvedHandle,
                    handle = resolvedHandle,
                    displayName = resolvedName,
                    avatarUrl = body.avatarImageUrl,
                    avatarImageUrl = body.avatarImageUrl,
                    profileDescription = body.profileDescription,
                    tracksCount = maxOf(body.numTotalClips, tracks.size),
                    totalPlays = tracks.sumOf { it.viewCount ?: 0L },
                    totalUpvotes = tracks.sumOf { it.likeCount ?: 0L },
                    verified = body.isVerified,
                    topSongs = tracks.take(3)
                )
                if (tracks.isNotEmpty()) {
                    return@withContext Result.success(creator to tracks)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        if (RetrofitClient.isDirectFailoverEnabled()) {
            val (directCreator, directTracks) = DirectSunoClient.fetchProfileClips(cleanHandle, page)
            if (directTracks.isNotEmpty()) {
                directTracks.forEach { clipMemCache[it.id] = it }
                return@withContext Result.success(directCreator to directTracks)
            }
        }

        // Local fallback from cached charts & creators
        val localSongs = (getCachedSongsChart("hot", false) + getCachedCreatorsRanking("hot").flatMap { it.topSongs })
            .distinctBy { it.id }
            .filter { it.handle.equals(cleanHandle, ignoreCase = true) || it.artist.equals(cleanHandle, ignoreCase = true) }
        val localCreator = getCachedCreatorsRanking("hot").firstOrNull {
            it.cleanHandle.equals(cleanHandle, ignoreCase = true)
        }
        Result.success(localCreator to localSongs)
    }

    suspend fun getClipInfo(songId: String): Track? = withContext(Dispatchers.IO) {
        val cleanId = songId.trim()
        if (cleanId.isEmpty()) return@withContext null
        clipMemCache[cleanId]?.takeIf { it.promptLyrics.isNotBlank() }?.let { return@withContext it }

        try {
            val response = api.getClipInfo(cleanId)
            if (response.isSuccessful && response.body() != null) {
                val track = response.body()!!.toTrack()
                if (track.id.isNotBlank()) {
                    clipMemCache[track.id] = track
                    return@withContext track
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        if (RetrofitClient.isDirectFailoverEnabled()) {
            val direct = DirectSunoClient.fetchClipInfo(cleanId)
            if (direct != null) {
                clipMemCache[direct.id] = direct
                return@withContext direct
            }
        }

        clipMemCache[cleanId] ?: DirectSunoClient.getFallbackSeedTracks().firstOrNull { it.id == cleanId }
    }

    suspend fun getRecommendations(trackId: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSimilarClips(trackId)
            if (response.isSuccessful && response.body() != null) {
                val clips = response.body()!!.similarClips.map { it.toTrack() }
                if (clips.isNotEmpty()) {
                    clips.forEach { clipMemCache[it.id] = it }
                    return@withContext Result.success(clips)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        val seed = clipMemCache[trackId]
        val pool = getCachedSongsChart("hot", false)
            .filter { it.id != trackId }
        val sorted = if (seed != null && seed.styleTagList.isNotEmpty()) {
            val seedWords = seed.tags.lowercase().split(",", " ").filter { it.length >= 3 }.toSet()
            pool.sortedByDescending { cand ->
                cand.tags.lowercase().split(",", " ").count { it in seedWords }
            }
        } else {
            pool
        }
        Result.success(sorted.take(15))
    }

    suspend fun getLyrics(trackId: String, title: String, artist: String): Result<SyncedLyricsResponse> = withContext(Dispatchers.IO) {
        val cachedTrack = clipMemCache[trackId]
        val existingPrompt = cachedTrack?.promptLyrics.orEmpty()
        val rawPrompt = if (existingPrompt.isNotBlank()) {
            existingPrompt
        } else {
            getClipInfo(trackId)?.promptLyrics.orEmpty()
        }
        if (rawPrompt.isNotBlank()) {
            val parsed = parseSunoLyrics(rawPrompt)
            if (parsed.isNotEmpty()) {
                return@withContext Result.success(
                    SyncedLyricsResponse(
                        synced = parsed.any { it.time > 0.0 },
                        lyrics = parsed,
                        source = "Suno Prompt"
                    )
                )
            }
        }
        Result.success(SyncedLyricsResponse(synced = false, lyrics = emptyList(), source = "Suno Prompt"))
    }

    suspend fun search(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val res = universalSearch(query)
        res.map { it.songs }
    }

    suspend fun universalSearch(query: String): Result<UniversalSearchResponse> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext Result.success(UniversalSearchResponse())

        // Ensure chart & creators are warmed if empty
        if (chartMemCache.isEmpty()) {
            runCatching { getSongsChart("hot", "all", false, 80) }
        }
        if (creatorsMemCache.isEmpty()) {
            runCatching { getCreatorsRanking("hot") }
        }

        val allCreators = getCachedCreatorsRanking("hot")
        val allTracks = (getCachedSongsChart("hot", false) +
            allCreators.flatMap { it.topSongs } +
            clipMemCache.values).distinctBy { it.id }

        val qNoAt = q.removePrefix("@")
        val matchedCreators = allCreators.filter { c ->
            c.displayName.lowercase().contains(qNoAt) ||
                c.cleanHandle.lowercase().contains(qNoAt) ||
                c.styleFocus.lowercase().contains(qNoAt)
        }.toMutableList()

        // If query explicitly looks like a Suno @handle and isn't in local results, fetch their live profile
        var handleTracks = emptyList<Track>()
        if (q.startsWith("@") || (matchedCreators.isEmpty() && !q.contains(" ") && q.length >= 3)) {
            val (fetchedCreator, fetchedSongs) = getCreatorProfile(qNoAt).getOrDefault(null to emptyList())
            if (fetchedCreator != null && matchedCreators.none { it.cleanHandle.equals(fetchedCreator.cleanHandle, ignoreCase = true) }) {
                matchedCreators.add(0, fetchedCreator)
            }
            handleTracks = fetchedSongs
        }

        val tokens = qNoAt.split(Regex("\\s+")).filter { it.isNotBlank() }
        val matchedSongs = (handleTracks + allTracks.filter { t ->
            val haystack = "${t.title} ${t.artist} ${t.handle} ${t.tags} ${t.promptLyrics}".lowercase()
            haystack.contains(qNoAt) || (tokens.size > 1 && tokens.all { haystack.contains(it) })
        }).distinctBy { it.id }

        val artistHits = matchedCreators.map { c ->
            ArtistHit(
                id = c.cleanHandle,
                name = c.bestName,
                handle = c.cleanHandle,
                avatar = c.bestAvatar,
                photo = c.bestAvatar,
                subscribers = "@${c.cleanHandle} • ${c.tracksCount} tracks"
            )
        }

        Result.success(
            UniversalSearchResponse(
                songs = matchedSongs,
                artists = artistHits,
                creators = matchedCreators
            )
        )
    }

    suspend fun getCrawlerStatus(): SunoCrawlerStatusResponse? = withContext(Dispatchers.IO) {
        try {
            val resp = api.getCrawlerStatus()
            if (resp.isSuccessful) resp.body() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun optimizePrompt(prompt: String, language: String = "vi"): PromptScoreResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.optimizePrompt(mapOf("prompt" to prompt, "language" to language))
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext resp.body()!!
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        DirectSunoClient.scoreAndOptimizePrompt(prompt, language)
    }

    suspend fun generateLyrics(
        topic: String,
        genre: String,
        mood: String,
        vocalType: String,
        structure: String,
        language: String = "vi"
    ): GeneratedLyricsResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.generateLyrics(
                mapOf(
                    "topic" to topic,
                    "genre" to genre,
                    "mood" to mood,
                    "vocalType" to vocalType,
                    "structure" to structure,
                    "language" to language
                )
            )
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext resp.body()!!
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        DirectSunoClient.generateLyrics(topic, genre, mood, vocalType, structure, language)
    }

    suspend fun explainStyle(title: String, style: String, language: String = "vi"): StyleExplainResult = withContext(Dispatchers.IO) {
        try {
            val resp = api.explainStyle(mapOf("title" to title, "style" to style, "language" to language))
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext resp.body()!!
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        DirectSunoClient.explainStyle(title, style, language)
    }

    private fun loadStringList(key: String): List<String> {
        return runCatching {
            val raw = prefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(raw)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun saveStringList(key: String, list: List<String>) {
        val arr = JSONArray().apply { list.forEach { put(it) } }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun writeTracksToPrefs(key: String, tracks: List<Track>) {
        runCatching {
            val arr = JSONArray()
            tracks.take(60).forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artist", t.artist)
                        put("handle", t.handle)
                        put("album", t.album)
                        put("duration", t.duration)
                        put("cover_url", t.coverUrl)
                        put("url", t.url)
                        put("video_url", t.videoUrl)
                        put("view_count", t.viewCount ?: 0L)
                        put("like_count", t.likeCount ?: 0L)
                        put("tags", t.tags)
                        put("prompt_lyrics", t.promptLyrics)
                        put("is_vietnamese", t.isVietnamese)
                        put("hot_score", t.hotScore)
                        put("rank", t.rank ?: 0)
                        put("model_name", t.modelName)
                    }
                )
            }
            prefs.edit().putString(key, arr.toString()).apply()
        }
    }

    private fun readTracksFromPrefs(key: String): List<Track> {
        return runCatching {
            val raw = prefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = ArrayList<Track>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isBlank()) continue
                list.add(
                    Track(
                        id = id,
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        handle = obj.optString("handle"),
                        album = obj.optString("album"),
                        duration = obj.optInt("duration", 0),
                        coverUrl = obj.optString("cover_url"),
                        url = obj.optString("url"),
                        videoUrl = obj.optString("video_url"),
                        viewCount = obj.optLong("view_count", 0L),
                        likeCount = obj.optLong("like_count", 0L),
                        tags = obj.optString("tags"),
                        promptLyrics = obj.optString("prompt_lyrics"),
                        isVietnamese = obj.optBoolean("is_vietnamese", false),
                        hotScore = obj.optLong("hot_score", 0L),
                        rank = obj.optInt("rank", 0).takeIf { it > 0 },
                        modelName = obj.optString("model_name")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun writeCreatorsToPrefs(key: String, creators: List<CreatorHit>) {
        runCatching {
            val arr = JSONArray()
            creators.take(50).forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("username", c.username)
                        put("handle", c.handle)
                        put("display_name", c.displayName)
                        put("avatar_url", c.avatarUrl)
                        put("profile_description", c.profileDescription)
                        put("style_focus", c.styleFocus)
                        put("tracks_count", c.tracksCount)
                        put("total_plays", c.totalPlays)
                        put("total_upvotes", c.totalUpvotes)
                        put("hot_score", c.hotScore)
                        put("rank", c.rank)
                        put("verified", c.verified)
                    }
                )
            }
            prefs.edit().putString(key, arr.toString()).apply()
        }
    }

    private fun readCreatorsFromPrefs(key: String): List<CreatorHit> {
        return runCatching {
            val raw = prefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = ArrayList<CreatorHit>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val handle = obj.optString("handle").ifBlank { obj.optString("username") }
                if (handle.isBlank()) continue
                val focus = obj.optString("style_focus")
                list.add(
                    CreatorHit(
                        username = obj.optString("username").ifBlank { handle },
                        handle = handle,
                        displayName = obj.optString("display_name").ifBlank { handle },
                        avatarUrl = obj.optString("avatar_url"),
                        avatarImageUrl = obj.optString("avatar_url"),
                        profileDescription = obj.optString("profile_description"),
                        styleFocus = focus,
                        styleTags = focus.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        tracksCount = obj.optInt("tracks_count", 0),
                        totalPlays = obj.optLong("total_plays", 0L),
                        totalUpvotes = obj.optLong("total_upvotes", 0L),
                        hotScore = obj.optLong("hot_score", 0L),
                        rank = obj.optInt("rank", i + 1),
                        verified = obj.optBoolean("verified", false)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "kv_suno_api_cache"
        private const val KEY_STUDIO_TAGS = "studio_tags_json"
        private const val KEY_CUSTOM_PROMPT = "studio_custom_prompt"
        private const val KEY_STUDIO_LYRICS = "studio_generated_lyrics"
        private const val KEY_SAVED_PROMPTS = "studio_saved_prompts_json"

        private val lrcLineRegex = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)$""")
        private val sectionHeaderRegex = Regex("""^\[[A-Za-z0-9\s\-_/:().]+]$""")

        fun parseSunoLyrics(rawPrompt: String): List<LyricLine> {
            val result = mutableListOf<LyricLine>()
            rawPrompt.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                val lrcMatch = lrcLineRegex.find(trimmed)
                if (lrcMatch != null) {
                    val min = lrcMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val sec = lrcMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                    val fracRaw = lrcMatch.groupValues[3]
                    val fracSec = if (fracRaw.isNotEmpty()) {
                        fracRaw.padEnd(3, '0').take(3).toDoubleOrNull()?.div(1000.0) ?: 0.0
                    } else 0.0
                    val text = lrcMatch.groupValues[4].trim()
                    if (text.isNotEmpty()) {
                        result.add(LyricLine(time = min * 60.0 + sec + fracSec, text = text, isSectionHeader = false))
                    }
                } else {
                    val isHeader = sectionHeaderRegex.matches(trimmed) || trimmed.startsWith("[") && trimmed.contains("]") && trimmed.length <= 42
                    result.add(LyricLine(time = 0.0, text = trimmed, isSectionHeader = isHeader))
                }
            }
            return result
        }
    }
}
