package com.kvsuno.app.data.api

import com.kvsuno.app.data.model.CreatorHit
import com.kvsuno.app.data.model.GeneratedLyricsResult
import com.kvsuno.app.data.model.PromptScoreResult
import com.kvsuno.app.data.model.StyleExplainResult
import com.kvsuno.app.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SoundWikiTagItem(
    val id: String,
    val name: String,
    val category: String,
    val subcategory: String,
    val promptSnippet: String
)

/**
 * Direct Suno Studio API fallback, Sound Wiki tag catalog, verified community seeds,
 * and autonomous local AI Prompt Scorer / Lyrics Generator / Style Explainer engines.
 */
object DirectSunoClient {

    private val directHttp by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun directCdnStreamUrl(trackId: String): String {
        val cleanId = trackId.trim()
        return "https://cdn1.suno.ai/$cleanId.mp4"
    }

    suspend fun fetchProfileClips(handle: String, page: Int = 1): Pair<CreatorHit?, List<Track>> = withContext(Dispatchers.IO) {
        val cleanHandle = handle.removePrefix("@").trim().ifBlank { "futurescall" }
        try {
            val url = "https://studio-api.prod.suno.com/api/profiles/${URLEncoder.encode(cleanHandle, "UTF-8")}?playlists_sort_by=upvote_count&clips_sort_by=play_count&page=$page"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            directHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null to emptyList()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null to emptyList()
                val json = JSONObject(body)
                val displayName = json.optString("display_name").ifBlank { cleanHandle }
                val avatar = json.optString("avatar_image_url")
                val desc = json.optString("profile_description")
                val totalClips = json.optInt("num_total_clips", 0)
                val clipsArr = json.optJSONArray("clips")
                val tracks = buildList {
                    if (clipsArr != null) {
                        for (i in 0 until clipsArr.length()) {
                            val c = clipsArr.optJSONObject(i) ?: continue
                            val id = c.optString("id").trim()
                            if (id.isBlank()) continue
                            val meta = c.optJSONObject("metadata")
                            val tags = meta?.optString("tags").orEmpty().ifBlank { c.optString("tags") }
                            val prompt = meta?.optString("prompt").orEmpty().ifBlank { c.optString("prompt") }
                            val dur = (meta?.optDouble("duration", 180.0) ?: c.optDouble("duration", 180.0)).toInt()
                            val plays = c.optLong("play_count", 0L)
                            val upvotes = c.optLong("upvote_count", 0L)
                            val cover = c.optString("image_large_url").ifBlank { c.optString("image_url") }
                                .ifBlank { "https://cdn1.suno.ai/image_$id.png" }
                            val clipTitle = c.optString("title").ifBlank { "Untitled Track" }.trim()
                            add(
                                Track(
                                    id = id,
                                    title = clipTitle,
                                    artist = c.optString("display_name").ifBlank { displayName },
                                    handle = cleanHandle,
                                    album = "@$cleanHandle",
                                    duration = dur,
                                    coverUrl = cover,
                                    url = "/api/suno-audio-proxy?input=$id",
                                    videoUrl = directCdnStreamUrl(id),
                                    viewCount = plays,
                                    likeCount = upvotes,
                                    tags = tags,
                                    promptLyrics = prompt,
                                    isVietnamese = isVietnameseText("$clipTitle $tags $prompt"),
                                    hotScore = plays + upvotes * 25L,
                                    modelName = c.optString("major_model_version").ifBlank { c.optString("model_name") }
                                )
                            )
                        }
                    }
                }.sortedByDescending { it.viewCount ?: 0L }

                val totalPlays = tracks.sumOf { it.viewCount ?: 0L }
                val creator = CreatorHit(
                    username = cleanHandle,
                    handle = cleanHandle,
                    displayName = displayName,
                    avatarUrl = avatar,
                    avatarImageUrl = avatar,
                    profileDescription = desc,
                    styleFocus = tracks.firstOrNull()?.tags.orEmpty(),
                    styleTags = tracks.firstOrNull()?.styleTagList?.take(3).orEmpty(),
                    tracksCount = maxOf(totalClips, tracks.size),
                    totalPlays = totalPlays,
                    totalUpvotes = tracks.sumOf { it.likeCount ?: 0L },
                    hotScore = totalPlays,
                    verified = json.optBoolean("is_verified", false),
                    topSongs = tracks.take(3)
                )
                creator to tracks
            }
        } catch (_: Exception) {
            null to emptyList()
        }
    }

    suspend fun fetchClipInfo(songId: String): Track? = withContext(Dispatchers.IO) {
        val cleanId = songId.trim()
        if (cleanId.isBlank()) return@withContext null
        try {
            val url = "https://studio-api.prod.suno.com/api/clip/${URLEncoder.encode(cleanId, "UTF-8")}"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            directHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val c = JSONObject(body)
                val id = c.optString("id").ifBlank { cleanId }
                val meta = c.optJSONObject("metadata")
                val handle = c.optString("handle").removePrefix("@")
                val displayName = c.optString("display_name").ifBlank { handle.ifBlank { "Suno Creator" } }
                val tags = meta?.optString("tags").orEmpty().ifBlank { c.optString("tags") }
                val prompt = meta?.optString("prompt").orEmpty().ifBlank { c.optString("prompt") }
                val dur = (meta?.optDouble("duration", 180.0) ?: 180.0).toInt()
                val title = c.optString("title").ifBlank { "Suno Track ${id.take(8)}" }.trim()
                val cover = c.optString("image_large_url").ifBlank { c.optString("image_url") }
                    .ifBlank { "https://cdn1.suno.ai/image_$id.png" }
                Track(
                    id = id,
                    title = title,
                    artist = displayName,
                    handle = handle,
                    album = if (handle.isNotBlank()) "@$handle" else "Suno Studio",
                    duration = dur,
                    coverUrl = cover,
                    url = "/api/suno-audio-proxy?input=$id",
                    videoUrl = directCdnStreamUrl(id),
                    viewCount = c.optLong("play_count", 0L),
                    likeCount = c.optLong("upvote_count", 0L),
                    tags = tags,
                    promptLyrics = prompt,
                    isVietnamese = isVietnameseText("$title $tags $prompt"),
                    modelName = c.optString("major_model_version").ifBlank { c.optString("model_name") }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private val vnRegex = Regex("[ạảãầấậẩẫăằắặẳẵẹẻẽềếệểễịỉĩọỏõồốộổỗơờớợởỡụủũưừứựửữỳỵỷỹđ]", RegexOption.IGNORE_CASE)
    private fun isVietnameseText(text: String): Boolean {
        val lower = text.lowercase()
        return vnRegex.containsMatchIn(lower) ||
            lower.contains("v-pop") || lower.contains("vietnam") || lower.contains("bolero") || lower.contains("vinahouse")
    }

    /**
     * Autonomous local 5-Pillar Suno Prompt Scorer & Optimizer (mirrors server/services/promptScorer.js).
     */
    fun scoreAndOptimizePrompt(promptText: String, language: String = "vi"): PromptScoreResult {
        val p = promptText.trim().lowercase()
        val isVi = language == "vi"

        var score = 40
        val strengths = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val suggestedTags = mutableListOf<String>()

        var detectedGenre = "General / Pop Fusion"
        val genres = listOf(
            "Vietnamese Folk Trap / V-Pop" to listOf("vietnam", "folk", "trap", "v-pop", "dân gian", "bolero", "vinahouse"),
            "Cinematic Epic / Orchestral" to listOf("cinematic", "epic", "orchestra", "trailer", "soundtrack", "symphonic"),
            "Electronic Dance Music / EDM" to listOf("edm", "future bass", "synthwave", "house", "trance", "techno", "dubstep"),
            "Hip-Hop / Urban Rap" to listOf("hip hop", "rap", "boom bap", "drill", "lo-fi", "lofi"),
            "Rock / Metal / Alternative" to listOf("rock", "metal", "grunge", "punk", "indie rock"),
            "Acoustic Ballad / R&B" to listOf("ballad", "acoustic", "r&b", "soul", "piano ballad")
        )
        for ((name, keywords) in genres) {
            if (keywords.any { p.contains(it) }) {
                detectedGenre = name
                score += 15
                strengths.add(if (isVi) "Thể loại rõ ràng: $name" else "Well-defined genre: $name")
                break
            }
        }

        var detectedMood = "Emotional & Catchy"
        val moods = listOf(
            "Eerie, Dark, Haunting" to listOf("dark", "eerie", "haunting", "u ám", "ma mị", "creepy"),
            "Euphoric, High Energy, Uplifting" to listOf("energetic", "euphoric", "uplifting", "sôi động", "vui tươi", "bùng nổ"),
            "Melancholic, Nostalgic, Sad" to listOf("sad", "melancholy", "nostalgic", "buồn", "sầu", "hoài niệm"),
            "Chill, Atmospheric, Ambient" to listOf("chill", "ambient", "atmospheric", "thư giãn", "êm dịu")
        )
        for ((name, keywords) in moods) {
            if (keywords.any { p.contains(it) }) {
                detectedMood = name
                score += 12
                strengths.add(if (isVi) "Tâm trạng xác định: $name" else "Expressive mood: $name")
                break
            }
        }

        var detectedInst = "Standard rhythm section"
        val instruments = listOf(
            "808 Sub Bass, Dan Tranh, Bamboo Flute" to listOf("808", "tranh", "sáo", "flute", "bass"),
            "Electric Guitar, Distorted Riffs, Drums" to listOf("guitar", "distort", "drum", "drums"),
            "Acoustic Piano, Warm Strings, Cello" to listOf("piano", "strings", "cello", "violin"),
            "Synth Plucks, Rolling Arpeggiator, Sub Bass" to listOf("synth", "arp", "sub bass", "lead synth")
        )
        for ((name, keywords) in instruments) {
            if (keywords.any { p.contains(it) }) {
                detectedInst = name
                score += 15
                strengths.add(if (isVi) "Nhạc cụ phong phú: $name" else "Specific instrumentation: $name")
                break
            }
        }

        var detectedVocals = "Clear expressive vocal delivery"
        val vocals = listOf(
            "Fast aggressive male rap flow" to listOf("rap", "male", "aggressive", "flow"),
            "Female airy emotive lead vocals" to listOf("female", "airy", "emotive", "nữ"),
            "Layered choral harmonies & whispers" to listOf("whisper", "harmony", "choir", "vocal chop"),
            "Instrumental (No vocals)" to listOf("instrumental", "không lời")
        )
        for ((name, keywords) in vocals) {
            if (keywords.any { p.contains(it) }) {
                detectedVocals = name
                score += 10
                strengths.add(if (isVi) "Phong cách giọng hát cụ thể" else "Clear vocal guidance")
                break
            }
        }

        val mixingTags = listOf("reverb", "delay", "saturated", "mastered", "spatial", "hi-fi", "cinematic riser", "beat drop", "stereo")
        val hasMixing = mixingTags.filter { p.contains(it) }
        val detectedMixing = if (hasMixing.isNotEmpty()) {
            score += 8
            strengths.add(if (isVi) "Hiệu ứng mixing chuyên nghiệp" else "Studio mixing descriptors")
            "${hasMixing.joinToString(", ")} production mix"
        } else {
            warnings.add(if (isVi) "Chưa có thông số mixing (reverb, bass drop, saturation)" else "Missing mixing/production tags")
            suggestedTags.addAll(listOf("spatial reverb", "hard 808 drop", "wide stereo mix"))
            "Clean modern studio mix, spatial reverb"
        }

        if (p.length < 20) {
            warnings.add(if (isVi) "Prompt quá ngắn, Suno dễ tạo nhạc chung chung" else "Prompt is too short, may result in generic music")
            score = maxOf(score - 15, 35)
        } else if (p.length > 300) {
            warnings.add(if (isVi) "Prompt quá dài có thể bị Suno cắt bớt các tag cuối" else "Prompt is lengthy; Suno may truncate trailing tags")
        }

        val finalScore = score.coerceIn(45, 98)
        val grade = when {
            finalScore >= 90 -> "A+"
            finalScore >= 80 -> "A"
            finalScore >= 65 -> "B"
            else -> "C"
        }

        val tagList = listOf(
            if (detectedGenre != "General / Pop Fusion") detectedGenre.lowercase().replace(Regex("\\s*/\\s*"), ", ") else "modern fusion",
            detectedMood.lowercase(),
            detectedInst.lowercase(),
            detectedVocals.lowercase(),
            "professional studio mastering, crisp audio mix"
        )
        val optimized = buildString {
            if (promptText.isNotBlank()) {
                append(promptText.trim().trimEnd(','))
                append(", ")
            }
            append(tagList.joinToString(", "))
        }

        return PromptScoreResult(
            score = finalScore,
            producerGrade = grade,
            strengths = strengths.ifEmpty { listOf(if (isVi) "Ý tưởng cơ bản khả thi" else "Baseline musical concept") },
            warnings = warnings.ifEmpty { listOf(if (isVi) "Prompt đạt chuẩn tốt" else "Prompt follows production best practices") },
            suggestedTags = suggestedTags.ifEmpty { listOf("driving tempo", "crisp drums", "wide reverb", "dynamic drop") },
            pillars = mapOf(
                "genre" to detectedGenre,
                "mood" to detectedMood,
                "instruments" to detectedInst,
                "vocals" to detectedVocals,
                "mixing" to detectedMixing
            ),
            optimizedPrompt = optimized
        )
    }

    /**
     * Autonomous local Suno AI Lyrics Generator (mirrors server/services/lyricsEngine.js).
     */
    fun generateLyrics(
        topic: String = "Tình yêu mùa thu",
        genre: String = "Pop Ballad",
        mood: String = "Emotional and Catchy",
        vocalType: String = "Female Emotional",
        structure: String = "Standard Pop",
        language: String = "vi"
    ): GeneratedLyricsResult {
        val isVi = language == "vi"
        val cleanTopic = topic.ifBlank { if (isVi) "Đêm Mưa Sài Gòn" else "Midnight Echoes" }
        val title = if (isVi) "Bản Tình Ca ${cleanTopic.take(18)}" else "Echoes of ${cleanTopic.take(18)}"

        val lyrics = if (isVi) {
            when {
                structure.contains("Rap", ignoreCase = true) || genre.contains("Rap", ignoreCase = true) || genre.contains("Trap", ignoreCase = true) -> """
[INTRO]
(Tiếng 808 gắt, tiếng đàn tranh rải nhịp)
$cleanTopic... lắng nghe nhịp tim đêm nay...

[VERSE 1 - RAP]
(Flow nhanh, dứt khoát từng câu)
Đêm buông xuống thành phố lên đèn hoa lệ
Ta mang theo giấc mộng $cleanTopic giữa bộn bề
Thành công không phải món quà dễ đến
Ai cũng từng nếm trải cay đắng bên thềm.

[HOOK]
(Drop beat căng - Sub bass rung chuyển)
Bước tới, không bao giờ nhìn lại phía sau
Đốt cháy hết đam mê vượt qua mọi nỗi đau
Biến ước mơ thành hiện thực giữa khung trời lồng lộng!

[VERSE 2 - RAP]
(Flow đảo phách, gieo vần liên tục)
Từ con số không đi lên bằng đôi bàn tay
Mồ hôi rơi từng ngày không hề lung lay
Bản lĩnh của chiến binh không chịu khuất phục gió sương!

[OUTRO]
(Bass dịu dần, tiếng đàn tranh ngân xa)
(Fade out)
                """.trim()

                else -> """
[INTRO]
(Tiếng piano chậm rãi, không gian mộc mạc)
$cleanTopic... lá rơi bên thềm gió lay êm đềm...

[VERSE 1]
Bước qua con phố quen ngày xưa đón đưa
Cơn mưa chiều nay rơi như ngày đầu mới hứa
Ánh mắt ngày ấy ấm nồng trao thiết tha
Giờ chỉ còn nỗi nhớ theo bước chân xa.

[PRE-CHORUS]
(Nhịp điệu dồn dập, violin hòa quyện)
Thời gian trôi không thể quay lại
Chỉ còn lại những vết thương khôn nguôi
Dù biết rằng người đã rời xa mãi mãi
Vẫn mơ về một sớm mai có em.

[CHORUS]
(Điệp khúc cao trào - Trống đánh mạnh, hòa âm ngân vang)
Hãy giữ lấy những ký ức ngọt ngào
Dù con đường đôi ta giờ chia hai ngả
Tình yêu đó dẫu muôn vàn trắc trở
Vẫn là khúc ca đẹp nhất cuộc đời anh!

[BRIDGE]
(Nhạc lắng xuống, guitar acoustic độc tấu)
Nếu có một ngày tình cờ gặp lại
Liệu ta có mỉm cười chào nhau như bạn cũ?

[OUTRO]
(Tiếng chuông ngân xa, giai điệu dần tan vào hư không)
(Fade out)
                """.trim()
            }
        } else {
            """
[INTRO]
(Soft acoustic guitar picking, ambient reverb)
$cleanTopic... distant whispers in the night...

[VERSE 1]
Walking down the boulevard where we used to run
Underneath the golden haze of the setting sun
I can still hear the promises we made back then
Before the winter cold settled in.

[PRE-CHORUS]
(Building up, strings rising in the background)
Time keeps moving forward, never looking back
Even if the distance keeps us apart
You'll always have a haven in my heart.

[CHORUS]
(Powerful climax - Full drums and soaring lead vocals)
Hold on to the echoes of $cleanTopic
Through every storm and across the deepest sea
This melody will never drift away!

[BRIDGE]
(Strip down to warm piano solo)
If our paths should cross again someday
Just a gratitude for the love we shared...

[OUTRO]
(Strings fade into gentle tape hiss)
(Fade out)
            """.trim()
        }

        val suggestedStyle = "${genre.lowercase()}, ${mood.lowercase()}, ${vocalType.lowercase()}, expressive dynamic range, professional studio mastering, 92 bpm"
        val notes = if (isVi) {
            "Phân bổ năng lượng tăng dần từ [VERSE 1] lên [PRE-CHORUS] và bung nổ ở [CHORUS] với trống dày và hòa âm vocal."
        } else {
            "Build emotional intensity gradually into the [CHORUS] and add vocal harmonies in the [BRIDGE]."
        }
        return GeneratedLyricsResult(
            title = title,
            lyrics = lyrics,
            suggestedStylePrompt = suggestedStyle,
            musicalNotes = notes
        )
    }

    fun explainStyle(title: String, style: String, language: String = "vi"): StyleExplainResult {
        val isVi = language == "vi"
        val s = style.lowercase()
        val detected = buildList {
            if (s.contains("trap") || s.contains("rap") || s.contains("hip hop")) add("Trap / Urban Hip-Hop")
            if (s.contains("folk") || s.contains("tranh") || s.contains("flute")) add("Vietnamese Folk Fusion")
            if (s.contains("edm") || s.contains("house") || s.contains("synth")) add("Electronic / EDM")
            if (s.contains("ballad") || s.contains("pop") || s.contains("acoustic")) add("Pop / Ballad")
            if (s.contains("cinematic") || s.contains("orchestral")) add("Cinematic Soundtrack")
        }.joinToString(" & ").ifBlank { "Modern Suno AI Fusion" }

        val text = if (isVi) {
            "Phong cách \"$title\": $detected. Kết hợp nhịp điệu hiện đại cùng không gian reverb rộng và dải trầm rõ nét. Gợi ý dùng thẻ [Verse] chuyển [Drop] trước [Chorus] để đạt cao trào."
        } else {
            "Style Analysis for \"$title\": $detected. Blends contemporary rhythm arrangement with wide spatial reverb and punchy sub-lows. Use [Verse] -> [Drop] -> [Chorus] metatags."
        }
        return StyleExplainResult(text)
    }

    fun getFallbackSeedTracks(): List<Track> = listOf(
        Track(
            id = "a15a3e62-b002-4e5b-a571-0d86adc503f2",
            title = "Duyên Lỡ .. Lỡ Duyên",
            artist = "TienNg",
            handle = "futurescall",
            album = "@futurescall",
            duration = 181,
            coverUrl = "https://cdn2.suno.ai/746f5806-0e35-4ee5-94fd-e64d2f9a333b.jpeg",
            url = "/api/suno-audio-proxy?input=a15a3e62-b002-4e5b-a571-0d86adc503f2",
            videoUrl = directCdnStreamUrl("a15a3e62-b002-4e5b-a571-0d86adc503f2"),
            viewCount = 12253,
            likeCount = 285,
            tags = "Vietnamese folk trap, fast driving tempo, aggressive male rap, hard 808 sub bass, bamboo flute, dan tranh zither solo",
            promptLyrics = "[INTRO]\n(Nhịp trống Trap dồn dập, tiếng đàn tranh rải mộc mạc)\nÂm dương... dương âm.\n\n[VERSE 1]\nĐêm mờ sương, người thương mờ mắt,\nTình dắt lối, tối lối ta đi.\n\n[CHORUS]\nDuyên âm không thành, đừng thành âm duyên.\nTình thực tan tành, đừng tành thực tình.",
            isVietnamese = true,
            rank = 1,
            modelName = "v6"
        ),
        Track(
            id = "0242a69a-69c1-4fa1-b918-102eb1044e9b",
            title = "Mưa Rơi Phố Cổ",
            artist = "♡ Muzik VN ♡",
            handle = "muzikvn",
            album = "@muzikvn",
            duration = 165,
            coverUrl = "https://cdn2.suno.ai/c6bfd5c2-ed4a-4db5-a009-c800ac55aedc.jpeg",
            url = "/api/suno-audio-proxy?input=0242a69a-69c1-4fa1-b918-102eb1044e9b",
            videoUrl = directCdnStreamUrl("0242a69a-69c1-b918-102eb1044e9b"),
            viewCount = 48500,
            likeCount = 910,
            tags = "V-Pop, Emotional Ballad, Warm Piano, Lush Strings, Female Airy Vocals",
            promptLyrics = "[INTRO]\n(Piano dịu nhẹ trong đêm mưa)\n\n[VERSE 1]\nCon phố nhỏ lặng thầm dưới cơn mưa đêm...",
            isVietnamese = true,
            rank = 2,
            modelName = "v5"
        ),
        Track(
            id = "5f1e3166-50b1-4b81-9def-465bf04e61e6",
            title = "Sapa Sương Mờ Lo-Fi",
            artist = "AI Music Creator",
            handle = "aimusiccreater",
            album = "@aimusiccreater",
            duration = 182,
            coverUrl = "https://cdn2.suno.ai/image_5f1e3166-50b1-4b81-9def-465bf04e61e6.jpeg",
            url = "/api/suno-audio-proxy?input=5f1e3166-50b1-4b81-9def-465bf04e61e6",
            videoUrl = directCdnStreamUrl("5f1e3166-50b1-4b81-9def-465bf04e61e6"),
            viewCount = 34200,
            likeCount = 620,
            tags = "Vietnam Sapa Lo-Fi, Chill Beats, Bamboo Flute, Vinyl Crackle, Rhodes Piano",
            promptLyrics = "[INTRO]\n(Tiếng sáo trúc ngân vang giữa đồi sương Sapa)\n[Instrumental Lo-Fi Groove]",
            isVietnamese = true,
            rank = 3,
            modelName = "v5"
        ),
        Track(
            id = "4d8e56a4-5aa2-4740-8d7f-464bbc59c4cb",
            title = "Sài Gòn Đêm Đèn Vàng",
            artist = "Sài Gòn....( Kymngọc )",
            handle = "saigonmusic",
            album = "@saigonmusic",
            duration = 175,
            coverUrl = "https://cdn2.suno.ai/image_video_upload_bf486926-9865-4e2d-9719-8ce07347d2b9_snapshot_0s.jpeg",
            url = "/api/suno-audio-proxy?input=4d8e56a4-5aa2-4740-8d7f-464bbc59c4cb",
            videoUrl = directCdnStreamUrl("4d8e56a4-5aa2-4740-8d7f-464bbc59c4cb"),
            viewCount = 16878,
            likeCount = 340,
            tags = "Nhạc Sài Gòn, Bolero, Trữ Tình, Acoustic Guitar, Warm Saxophone",
            promptLyrics = "[INTRO]\n(Guitar Bolero chậm rãi)\n\n[VERSE 1]\nĐèn vàng hắt hiu trên con hẻm nhỏ Sài Gòn...",
            isVietnamese = true,
            rank = 4,
            modelName = "v5"
        ),
        Track(
            id = "bbe0adf5-f3a3-43ba-ae75-f3587ba6328b",
            title = "Neon Tokyo Synthwave",
            artist = "PylonStudio",
            handle = "daoxudeo",
            album = "@daoxudeo",
            duration = 210,
            coverUrl = "https://cdn2.suno.ai/image_bbe0adf5-f3a3-43ba-ae75-f3587ba6328b.jpeg",
            url = "/api/suno-audio-proxy?input=bbe0adf5-f3a3-43ba-ae75-f3587ba6328b",
            videoUrl = directCdnStreamUrl("bbe0adf5-f3a3-43ba-ae75-f3587ba6328b"),
            viewCount = 35621,
            likeCount = 510,
            tags = "Synthwave, V-Pop Indie, Analog Arp, Gated Snare, Spatial Reverb",
            promptLyrics = "[INTRO]\n(Analog synth arpeggio rising)\n\n[CHORUS]\nRunning through the neon rain...",
            isVietnamese = false,
            rank = 5,
            modelName = "v5"
        ),
        Track(
            id = "10cfc44e-736b-48fe-a2a7-a5b8e925f0ba",
            title = "Khúc Ca Quê Hương",
            artist = "Pham Phu Ngoc",
            handle = "phamphungoc",
            album = "@phamphungoc",
            duration = 213,
            coverUrl = "https://cdn2.suno.ai/10cfc44e-736b-48fe-a2a7-a5b8e925f0ba_348c378e.jpeg",
            url = "/api/suno-audio-proxy?input=10cfc44e-736b-48fe-a2a7-a5b8e925f0ba",
            videoUrl = directCdnStreamUrl("10cfc44e-736b-48fe-a2a7-a5b8e925f0ba"),
            viewCount = 20730,
            likeCount = 415,
            tags = "Nhạc Quê Hương, Trữ Tình, Đàn Bầu, Sáo Trúc, Male Warm Vocal",
            promptLyrics = "[INTRO]\n(Tiếng đàn bầu ngân nga)\n\n[VERSE 1]\nDòng sông quê hương êm đềm chảy mãi...",
            isVietnamese = true,
            rank = 6,
            modelName = "v5"
        )
    )

    fun getFallbackSeedCreators(): List<CreatorHit> {
        val seeds = getFallbackSeedTracks()
        return listOf(
            CreatorHit("ghorppo", "ghorppo", "Ghorppo", seeds[4].coverUrl, "", "Suno AI Creator", "heartland folk rock, indie acoustic", listOf("Folk Rock", "Acoustic"), 611, 550129, 4120, 590000, 1, true, listOf(seeds[4])),
            CreatorHit("muzikvn", "muzikvn", "♡ Muzik VN ♡", seeds[1].coverUrl, "", "V-Pop & Remix Hitmaker", "V-Pop, Remix, Cover, Nhạc Trẻ", listOf("V-Pop", "Remix", "Nhạc Trẻ"), 638, 375815, 3290, 415000, 2, true, listOf(seeds[1])),
            CreatorHit("aimusiccreater", "aimusiccreater", "AI Music Creator", seeds[2].coverUrl, "", "Vietnam Sapa Lo-Fi & Chill", "Vietnam Sapa Lo-Fi, Chill, Bamboo Flute", listOf("Lo-Fi", "Chill", "Sapa"), 251, 274231, 2140, 300000, 3, true, listOf(seeds[2])),
            CreatorHit("daoxudeo", "daoxudeo", "PylonStudio", seeds[4].coverUrl, "", "V-Pop, Indie, Experimental", "V-Pop, Indie, Synthwave", listOf("V-Pop", "Indie", "Synthwave"), 360, 35621, 580, 42000, 4, true, listOf(seeds[4])),
            CreatorHit("phamphungoc", "phamphungoc", "Pham Phu Ngoc", seeds[5].coverUrl, "", "Nhạc Quê Hương & Trữ Tình", "Nhạc Quê Hương, Trữ Tình, Kỷ Niệm", listOf("Quê Hương", "Trữ Tình"), 2008, 20730, 415, 35000, 5, true, listOf(seeds[5])),
            CreatorHit("saigonmusic", "saigonmusic", "Sài Gòn....( Kymngọc )", seeds[3].coverUrl, "", "Nhạc Sài Gòn & Bolero", "Nhạc Sài Gòn, Bolero, Trữ Tình", listOf("Bolero", "Sài Gòn"), 1731, 16878, 340, 29000, 6, true, listOf(seeds[3])),
            CreatorHit("futurescall", "futurescall", "TienNg", seeds[0].coverUrl, "", "Vietnamese Folk Trap & Bolero", "Nhạc Việt, Folk Trap, Bolero, V-Pop", listOf("Folk Trap", "Bolero", "V-Pop"), 165, 12253, 285, 22000, 7, true, listOf(seeds[0])),
            CreatorHit("neomusiccafe", "neomusiccafe", "RaonBlue / LuneBlue", seeds[1].coverUrl, "", "Vietnam Pop & Asian Ballad", "Vietnam Pop, Asian Ballad", listOf("Vietnam Pop", "Ballad"), 284, 15955, 220, 21000, 8, true, listOf(seeds[1]))
        )
    }

    val soundWikiCatalog: List<SoundWikiTagItem> = listOf(
        // Vietnamese DNA
        SoundWikiTagItem("vn-vpop", "V-Pop Modern", "Vietnamese DNA", "V-Pop & Nhạc Trẻ", "modern v-pop, emotional vietnamese vocal, lush synth pop, crisp snare, catchy chorus"),
        SoundWikiTagItem("vn-folktrap", "Vietnamese Folk Trap", "Vietnamese DNA", "Folk Fusion", "vietnamese folk trap, hard 808 sub bass, dan tranh zither, bamboo flute, aggressive rap flow"),
        SoundWikiTagItem("vn-bolero", "Bolero Trữ Tình", "Vietnamese DNA", "Trữ Tình", "vietnamese bolero, slow romantic rhythm, nylon acoustic guitar, warm saxophone, nostalgic vocal"),
        SoundWikiTagItem("vn-vinahouse", "Vinahouse 138 BPM", "Vietnamese DNA", "Club & Remix", "vinahouse, vietnamese dance music, bouncy energetic beat, 138 bpm, catchy synth lead, heavy bass"),
        SoundWikiTagItem("vn-dantranh", "Đàn Tranh Zither", "Vietnamese DNA", "Traditional", "dan tranh zither solo, plucked traditional vietnamese strings, delicate pentatonic melody"),
        SoundWikiTagItem("vn-saotruc", "Sáo Trúc Bamboo Flute", "Vietnamese DNA", "Traditional", "sao truc bamboo flute, airy breathy woodwind melody, atmospheric vietnamese mountain echo"),
        SoundWikiTagItem("vn-danbau", "Đàn Bầu Monochord", "Vietnamese DNA", "Traditional", "dan bau vietnamese monochord, bending harmonics, deeply melancholic traditional resonance"),
        SoundWikiTagItem("vn-lofi", "Sapa Lo-Fi Chill", "Vietnamese DNA", "Chill & Study", "vietnam sapa lo-fi chill, warm rhodes piano, vinyl crackle, soft rain ambience, bamboo flute"),

        // Genres
        SoundWikiTagItem("g-lofi", "Lo-Fi Hip Hop", "Genres", "Chill & Study", "lo-fi hip hop, dusty drum breaks, vinyl crackle, melancholic jazz piano chords"),
        SoundWikiTagItem("g-synthwave", "80s Synthwave", "Genres", "Electronic", "synthwave, driving 80s arpeggio, gated snare drum, retro-futuristic analog synths"),
        SoundWikiTagItem("g-cyberpunk", "Dark Cyberpunk", "Genres", "Electronic", "dark cyberpunk, aggressive mid-tempo electro, distorted basslines, industrial percussion"),
        SoundWikiTagItem("g-chillout", "Deep Chillout", "Genres", "Ambient", "hypnotic deep chillout, minimal crystalline bell motif, warm tape pad, 90 bpm"),
        SoundWikiTagItem("g-ballad", "Acoustic Ballad", "Genres", "Pop & Soul", "intimate acoustic ballad, fingerpicked guitar, warm cello strings, emotional vocal climax"),
        SoundWikiTagItem("g-rnb", "Modern R&B Soul", "Genres", "Pop & Soul", "modern r&b, silky electric piano, deep sub bass groove, layered falsetto harmonies"),
        SoundWikiTagItem("g-drill", "Melodic Drill", "Genres", "Hip-Hop", "melodic drill, sliding 808 bass, crisp syncopated hi-hats, haunting piano loop"),
        SoundWikiTagItem("g-cinematic", "Cinematic Epic", "Genres", "Orchestral", "cinematic orchestral soundtrack, soaring brass, sweeping strings, taiko percussion"),
        SoundWikiTagItem("g-indie", "Dream Indie Pop", "Genres", "Indie", "dreamy indie pop, shimmering chorus guitars, reverb-drenched snare, nostalgic synth pad"),
        SoundWikiTagItem("g-house", "Deep House", "Genres", "Electronic", "deep house groove, 124 bpm four-on-the-floor kick, warm bassline, soulful vocal chops"),

        // Instruments
        SoundWikiTagItem("i-rhodes", "Fender Rhodes Piano", "Instruments", "Keys", "warm fender rhodes electric piano, bell-like tone, jazzy chords, gentle tremolo"),
        SoundWikiTagItem("i-808", "Roland TR-808 Kit", "Instruments", "Drums", "classic tr-808 drum kit, booming sub kick, snappy synthetic snare, crisp hi-hat rolls"),
        SoundWikiTagItem("i-juno", "Juno-106 Analog Pad", "Instruments", "Synths", "lush juno-106 analog synth pad, rich chorus effect, warm sustained chords"),
        SoundWikiTagItem("i-cello", "Solo Cello & Strings", "Instruments", "Orchestral", "close-mic solo cello, warm sweeping string section, expressive vibrato"),
        SoundWikiTagItem("i-guitar", "Fingerstyle Guitar", "Instruments", "Acoustic", "intimate fingerstyle acoustic guitar, warm woody resonance, soft fret noise"),
        SoundWikiTagItem("i-sax", "Late-Night Saxophone", "Instruments", "Brass", "smoky late-night tenor saxophone solo, velvet reverb, expressive bends"),
        SoundWikiTagItem("i-piano", "Felt Grand Piano", "Instruments", "Keys", "intimate felt grand piano, soft hammer warmth, subtle room ambience"),
        SoundWikiTagItem("i-sub", "Deep Sine Sub-Bass", "Instruments", "Bass", "deep chest-thumping sine sub-bass, clean low-end punch"),

        // Vocals
        SoundWikiTagItem("v-female-airy", "Female Airy Vocals", "Vocals", "Lead", "female airy emotive lead vocals, intimate breathy whisper, soaring chorus"),
        SoundWikiTagItem("v-male-warm", "Male Warm Baritone", "Vocals", "Lead", "warm male baritone vocal, soulful vibrato, close-mic clarity"),
        SoundWikiTagItem("v-rap-fast", "Rapid Male Rap Flow", "Vocals", "Rap", "aggressive male rap with rapid flow, crisp diction, rhythmic ad-libs"),
        SoundWikiTagItem("v-duet", "Male & Female Duet", "Vocals", "Harmony", "emotional male and female vocal duet, call and response harmonies"),
        SoundWikiTagItem("v-choir", "Gospel / Ethereal Choir", "Vocals", "Backing", "layered ethereal choir harmonies, wide stereo cathedral vocal pads"),
        SoundWikiTagItem("v-inst", "Instrumental Only", "Vocals", "No Vocals", "instrumental, zero vocals, focus on lead melody and dynamics"),

        // SFX & Mastering
        SoundWikiTagItem("s-halftime", "Halftime Beat Switch", "SFX & Mastering", "Transitions", "sudden halftime beat switch, sluggish deep sub-bass drop, snappy snare"),
        SoundWikiTagItem("s-binaural", "3D Binaural Spatial", "SFX & Mastering", "Spatial Audio", "wide binaural 3d stereo field, immersive spatial reverb, crystalline highs"),
        SoundWikiTagItem("s-vinyl", "Warm Vinyl Crackle", "SFX & Mastering", "Lo-Fi Texture", "warm analog tape saturation, gentle vinyl crackle, nostalgic warmth"),
        SoundWikiTagItem("s-riser", "Cinematic Drop Riser", "SFX & Mastering", "Transitions", "cinematic tension riser into a heavy beat drop, explosive sub impact"),
        SoundWikiTagItem("s-master", "Hi-Fi Studio Master", "SFX & Mastering", "Mastering", "professional studio mastering, crisp audio mix, wide dynamic range, 24-bit clarity"),

        // Metatags
        SoundWikiTagItem("m-intro", "[Intro]", "Metatags", "Structure", "[Intro]"),
        SoundWikiTagItem("m-verse1", "[Verse 1]", "Metatags", "Structure", "[Verse 1]"),
        SoundWikiTagItem("m-pre", "[Pre-Chorus]", "Metatags", "Structure", "[Pre-Chorus]"),
        SoundWikiTagItem("m-chorus", "[Chorus]", "Metatags", "Structure", "[Chorus]"),
        SoundWikiTagItem("m-drop", "[Drop]", "Metatags", "Energy", "[Drop]"),
        SoundWikiTagItem("m-bridge", "[Bridge]", "Metatags", "Structure", "[Bridge]"),
        SoundWikiTagItem("m-solo", "[Instrumental Solo]", "Metatags", "Instrumental", "[Instrumental Solo]"),
        SoundWikiTagItem("m-outro", "[Outro]", "Metatags", "Structure", "[Outro]")
    )
}
