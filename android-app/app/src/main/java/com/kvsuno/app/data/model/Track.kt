package com.kvsuno.app.data.model

import com.google.gson.annotations.SerializedName

data class Track(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val handle: String = "",
    val album: String = "",
    val duration: Int = 0,
    @SerializedName("cover_url") val coverUrl: String = "",
    val url: String = "",
    @SerializedName("video_url") val videoUrl: String = "",
    @SerializedName("view_count") val viewCount: Long? = null,
    @SerializedName("like_count") val likeCount: Long? = null,
    val tags: String = "",
    @SerializedName("prompt_lyrics") val promptLyrics: String = "",
    @SerializedName("is_vietnamese") val isVietnamese: Boolean = false,
    @SerializedName("hot_score") val hotScore: Long = 0L,
    val rank: Int? = null,
    @SerializedName("model_name") val modelName: String = "",
    val bitrate: Int? = 192,
    val codec: String? = "AAC"
) {
    val styleTagList: List<String>
        get() = tags.split(",", ";", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    val primaryGenreTag: String
        get() = styleTagList.firstOrNull() ?: if (isVietnamese) "Nhạc Việt AI" else "Suno AI"

    val displayArtistHandle: String
        get() = when {
            handle.isNotBlank() && artist.isNotBlank() && !artist.equals(handle, ignoreCase = true) ->
                "$artist • @$handle"
            handle.isNotBlank() -> "@${handle.removePrefix("@")}"
            artist.isNotBlank() -> artist
            else -> "Suno Creator"
        }
}

data class CreatorHit(
    val username: String = "",
    val handle: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = "",
    @SerializedName("avatar_image_url") val avatarImageUrl: String = "",
    @SerializedName("profile_description") val profileDescription: String = "",
    @SerializedName("style_focus") val styleFocus: String = "",
    @SerializedName("style_tags") val styleTags: List<String> = emptyList(),
    @SerializedName("tracks_count") val tracksCount: Int = 0,
    @SerializedName("total_plays") val totalPlays: Long = 0L,
    @SerializedName("total_upvotes") val totalUpvotes: Long = 0L,
    @SerializedName("hot_score") val hotScore: Long = 0L,
    val rank: Int = 0,
    val verified: Boolean = false,
    @SerializedName("top_songs") val topSongs: List<Track> = emptyList()
) {
    val cleanHandle: String get() = handle.ifBlank { username }.removePrefix("@")
    val bestName: String get() = displayName.ifBlank { cleanHandle }
    val bestAvatar: String get() = avatarUrl.ifBlank { avatarImageUrl }
}

data class PromptScoreResult(
    val score: Int = 75,
    val producerGrade: String = "B",
    val strengths: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestedTags: List<String> = emptyList(),
    val pillars: Map<String, String> = emptyMap(),
    val optimizedPrompt: String = ""
)

data class GeneratedLyricsResult(
    val title: String = "",
    val lyrics: String = "",
    val suggestedStylePrompt: String = "",
    val musicalNotes: String = ""
)

data class StyleExplainResult(
    val explanation: String = ""
)

// Suno API Raw DTOs for Retrofit deserialization
data class SunoMetadataDto(
    val tags: String? = null,
    val prompt: String? = null,
    val duration: Double? = null,
    val type: String? = null
)

data class SunoMediaUrlDto(
    val url: String? = null,
    @SerializedName("content_type") val contentType: String? = null
)

data class SunoClipDto(
    val id: String? = null,
    @SerializedName("clip_id") val clipId: String? = null,
    val title: String? = null,
    val handle: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("avatar_image_url") val avatarImageUrl: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_large_url") val imageLargeUrl: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("play_count") val playCount: Long? = null,
    @SerializedName("upvote_count") val upvoteCount: Long? = null,
    val duration: Double? = null,
    val tags: String? = null,
    val prompt: String? = null,
    @SerializedName("style_focus") val styleFocus: String? = null,
    @SerializedName("is_vietnamese") val isVietnamese: Boolean? = null,
    @SerializedName("hot_score") val hotScore: Long? = null,
    val rank: Int? = null,
    @SerializedName("model_name") val modelName: String? = null,
    @SerializedName("major_model_version") val majorModelVersion: String? = null,
    val metadata: SunoMetadataDto? = null,
    @SerializedName("media_urls") val mediaUrls: List<SunoMediaUrlDto>? = null
) {
    fun toTrack(fallbackHandle: String = "", fallbackDisplayName: String = ""): Track {
        val cleanId = (id ?: clipId).orEmpty().trim()
        val resolvedHandle = (handle ?: fallbackHandle).removePrefix("@").trim()
        val resolvedArtist = (displayName ?: fallbackDisplayName).ifBlank { resolvedHandle.ifBlank { "Suno Creator" } }
        val resolvedTags = metadata?.tags?.takeIf { it.isNotBlank() }
            ?: tags?.takeIf { it.isNotBlank() }
            ?: styleFocus.orEmpty()
        val resolvedPrompt = metadata?.prompt?.takeIf { it.isNotBlank() } ?: prompt.orEmpty()
        val resolvedDuration = (metadata?.duration ?: duration ?: 180.0).toInt().coerceAtLeast(0)
        val resolvedCover = imageLargeUrl?.takeIf { it.isNotBlank() }
            ?: imageUrl?.takeIf { it.isNotBlank() }
            ?: if (cleanId.isNotBlank()) "https://cdn1.suno.ai/image_$cleanId.png" else ""
        val resolvedVideo = videoUrl?.takeIf { it.isNotBlank() }
            ?: if (cleanId.isNotBlank()) "https://cdn1.suno.ai/$cleanId.mp4" else ""

        return Track(
            id = cleanId,
            title = title?.trim()?.ifBlank { "Untitled Track" } ?: "Untitled Track",
            artist = resolvedArtist,
            handle = resolvedHandle,
            album = if (resolvedHandle.isNotBlank()) "@$resolvedHandle" else "Suno Studio",
            duration = resolvedDuration,
            coverUrl = resolvedCover,
            url = audioUrl.orEmpty(),
            videoUrl = resolvedVideo,
            viewCount = playCount ?: 0L,
            likeCount = upvoteCount ?: 0L,
            tags = resolvedTags,
            promptLyrics = resolvedPrompt,
            isVietnamese = isVietnamese == true,
            hotScore = hotScore ?: ((playCount ?: 0L) + (upvoteCount ?: 0L) * 25L),
            rank = rank,
            modelName = majorModelVersion?.ifBlank { modelName.orEmpty() } ?: modelName.orEmpty(),
            bitrate = 192,
            codec = "AAC"
        )
    }
}

data class SunoSongsChartResponse(
    @SerializedName("total_songs") val totalSongs: Int = 0,
    @SerializedName("total_plays") val totalPlays: Long = 0L,
    @SerializedName("vietnamese_hot_hits_count") val vietnameseHotHitsCount: Int = 0,
    @SerializedName("creators_count") val creatorsCount: Int = 0,
    @SerializedName("sort_by") val sortBy: String = "hot",
    val songs: List<SunoClipDto> = emptyList()
)

data class SunoCreatorDto(
    val username: String = "",
    val handle: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = "",
    @SerializedName("avatar_image_url") val avatarImageUrl: String = "",
    @SerializedName("profile_description") val profileDescription: String = "",
    @SerializedName("style_focus") val styleFocus: String = "",
    @SerializedName("style_tags") val styleTags: List<String> = emptyList(),
    @SerializedName("tracks_count") val tracksCount: Int = 0,
    @SerializedName("total_plays") val totalPlays: Long = 0L,
    @SerializedName("total_upvotes") val totalUpvotes: Long = 0L,
    @SerializedName("hot_score") val hotScore: Long = 0L,
    val rank: Int = 0,
    val verified: Boolean = false,
    @SerializedName("top_songs") val topSongs: List<SunoClipDto> = emptyList()
) {
    fun toCreatorHit(): CreatorHit {
        val cleanHandle = handle.ifBlank { username }.removePrefix("@")
        val bestName = displayName.ifBlank { cleanHandle }
        return CreatorHit(
            username = username.ifBlank { cleanHandle },
            handle = cleanHandle,
            displayName = bestName,
            avatarUrl = avatarUrl.ifBlank { avatarImageUrl },
            avatarImageUrl = avatarImageUrl.ifBlank { avatarUrl },
            profileDescription = profileDescription,
            styleFocus = styleFocus,
            styleTags = if (styleTags.isNotEmpty()) styleTags else styleFocus.split(",").map { it.trim() }.filter { it.isNotBlank() },
            tracksCount = tracksCount,
            totalPlays = totalPlays,
            totalUpvotes = totalUpvotes,
            hotScore = hotScore,
            rank = rank,
            verified = verified,
            topSongs = topSongs.map { it.toTrack(cleanHandle, bestName) }
        )
    }
}

data class SunoCreatorsRankingResponse(
    @SerializedName("sort_by") val sortBy: String = "hot",
    @SerializedName("total_creators") val totalCreators: Int = 0,
    @SerializedName("total_tracks") val totalTracks: Int = 0,
    @SerializedName("total_plays") val totalPlays: Long = 0L,
    val creators: List<SunoCreatorDto> = emptyList()
)

data class SunoProfileResponse(
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("display_name") val displayName: String = "",
    val handle: String = "",
    @SerializedName("profile_description") val profileDescription: String = "",
    @SerializedName("num_total_clips") val numTotalClips: Int = 0,
    @SerializedName("avatar_image_url") val avatarImageUrl: String = "",
    @SerializedName("is_verified") val isVerified: Boolean = false,
    val clips: List<SunoClipDto> = emptyList()
)

data class SunoSimilarResponse(
    @SerializedName("seed_id") val seedId: String = "",
    @SerializedName("seed_title") val seedTitle: String = "",
    @SerializedName("similar_clips") val similarClips: List<SunoClipDto> = emptyList()
)

data class SunoCrawlerStatusResponse(
    val isCrawling: Boolean = false,
    val lastUpdatedAt: String? = null,
    val totalCreators: Int = 0,
    val liveSyncedCreators: Int = 0,
    val totalIndexedSongs: Int = 0,
    val vietnameseHotHitsCount: Int = 0,
    val crawlCount: Int = 0
)

// Shared UI compatibility models matching KV Music layout
data class AlbumHit(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    @SerializedName("cover_url") val coverUrl: String = "",
    val year: String = ""
)

data class PlaylistHit(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    @SerializedName("cover_url") val coverUrl: String = "",
    @SerializedName("track_count") val trackCount: Int = 0
)

data class ArtistHit(
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    @SerializedName("avatar") val avatar: String = "",
    @SerializedName("photo") val photo: String = "",
    @SerializedName("subscribers") val subscribers: String = "",
    @SerializedName("followers") val followers: String = ""
) {
    val displayPhoto: String get() = photo.ifEmpty { avatar }
    val displayFollowers: String get() = followers.ifEmpty { subscribers.ifEmpty { "Suno Creator" } }
}

data class UniversalSearchResponse(
    val songs: List<Track> = emptyList(),
    val albums: List<AlbumHit> = emptyList(),
    val playlists: List<PlaylistHit> = emptyList(),
    val artists: List<ArtistHit> = emptyList(),
    val creators: List<CreatorHit> = emptyList()
)

data class LyricLine(
    val time: Double = 0.0,
    val text: String = "",
    val isSectionHeader: Boolean = false
)

data class SyncedLyricsResponse(
    val synced: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val source: String = "Suno Prompt"
)

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    @SerializedName("avatar_color") val avatarColor: String = "#1ED760",
    @SerializedName("created_at") val createdAt: Long = 0L,
    val liked: List<Track> = emptyList(),
    val history: List<Track> = emptyList()
)

data class AuthResponse(
    val user: User? = null,
    val token: String? = null,
    val error: String? = null
)

data class PairResponse(
    @SerializedName("pair_code") val pairCode: String? = null,
    val error: String? = null,
    val user: User? = null,
    val token: String? = null
)
