package com.kvsuno.app.data.api

import com.kvsuno.app.data.model.GeneratedLyricsResult
import com.kvsuno.app.data.model.PromptScoreResult
import com.kvsuno.app.data.model.StyleExplainResult
import com.kvsuno.app.data.model.SunoClipDto
import com.kvsuno.app.data.model.SunoCrawlerStatusResponse
import com.kvsuno.app.data.model.SunoCreatorsRankingResponse
import com.kvsuno.app.data.model.SunoProfileResponse
import com.kvsuno.app.data.model.SunoSimilarResponse
import com.kvsuno.app.data.model.SunoSongsChartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface KVSunoApi {

    @GET("/api/suno-songs-chart")
    suspend fun getSongsChart(
        @Query("sort") sort: String = "hot",
        @Query("creator") creator: String = "all",
        @Query("limit") limit: Int = 80,
        @Query("vnOnly") vnOnly: Boolean = false
    ): Response<SunoSongsChartResponse>

    @GET("/api/suno-creators-ranking")
    suspend fun getCreatorsRanking(
        @Query("sort") sort: String = "hot"
    ): Response<SunoCreatorsRankingResponse>

    @GET("/api/suno-profile")
    suspend fun getProfile(
        @Query("handle") handle: String,
        @Query("page") page: Int = 1
    ): Response<SunoProfileResponse>

    @GET("/api/suno-showcase")
    suspend fun getShowcase(
        @Query("handle") handle: String = "futurescall"
    ): Response<SunoProfileResponse>

    @GET("/api/suno-clip-info")
    suspend fun getClipInfo(
        @Query("songId") songId: String
    ): Response<SunoClipDto>

    @GET("/api/suno-similar")
    suspend fun getSimilarClips(
        @Query("id") songId: String
    ): Response<SunoSimilarResponse>

    @GET("/api/suno-crawler-status")
    suspend fun getCrawlerStatus(): Response<SunoCrawlerStatusResponse>

    @POST("/api/optimize-prompt")
    suspend fun optimizePrompt(
        @Body payload: Map<String, String>
    ): Response<PromptScoreResult>

    @POST("/api/generate-lyrics")
    suspend fun generateLyrics(
        @Body payload: Map<String, String>
    ): Response<GeneratedLyricsResult>

    @POST("/api/explain-style")
    suspend fun explainStyle(
        @Body payload: Map<String, String>
    ): Response<StyleExplainResult>
}
