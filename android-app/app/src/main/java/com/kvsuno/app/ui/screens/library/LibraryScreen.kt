package com.kvsuno.app.ui.screens.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.AuthRepository
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.components.TrackListItem
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldDeep
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoTinted
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.SunoWhite20
import com.kvsuno.app.ui.theme.YouTubeSansFontFamily
import com.kvsuno.app.ui.theme.YtmDisplay02
import com.kvsuno.app.ui.theme.YtmSubtitle01
import com.kvsuno.app.ui.theme.YtmTitle03
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private val libraryFilterChips = listOf(
    "Playlists",
    "Songs",
    "Saved Prompts",
    "Creators",
    "Downloads"
)

private enum class LibrarySortMode(val label: String) {
    RECENT_ACTIVITY("Recent activity"),
    TITLE_AZ("A–Z"),
    ARTIST_AZ("Creator")
}

private data class LocalUserPlaylist(
    val id: String,
    val name: String,
    val coverUrl: String,
    val tracks: List<Track>
)

private const val PLAYLIST_PREFS_NAME = "kv_suno_playlists"
private const val PLAYLIST_PREFS_KEY = "saved_playlists_json"

private fun loadLocalPlaylists(context: Context): List<LocalUserPlaylist> {
    return try {
        val raw = context.getSharedPreferences(PLAYLIST_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PLAYLIST_PREFS_KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tracksArr = obj.optJSONArray("tracks") ?: JSONArray()
                val tracks = buildList {
                    for (j in 0 until tracksArr.length()) {
                        val t = tracksArr.getJSONObject(j)
                        add(
                            Track(
                                id = t.optString("id"),
                                title = t.optString("title"),
                                artist = t.optString("artist"),
                                handle = t.optString("handle"),
                                album = t.optString("album"),
                                duration = t.optInt("duration", 0),
                                coverUrl = t.optString("cover_url"),
                                url = t.optString("url"),
                                tags = t.optString("tags")
                            )
                        )
                    }
                }
                add(
                    LocalUserPlaylist(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        coverUrl = obj.optString("cover_url"),
                        tracks = tracks
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveLocalPlaylists(context: Context, playlists: List<LocalUserPlaylist>) {
    val arr = JSONArray()
    playlists.forEach { pl ->
        val obj = JSONObject().apply {
            put("id", pl.id)
            put("name", pl.name)
            put("cover_url", pl.coverUrl)
            val tArr = JSONArray()
            pl.tracks.forEach { tr ->
                tArr.put(
                    JSONObject().apply {
                        put("id", tr.id)
                        put("title", tr.title)
                        put("artist", tr.artist)
                        put("handle", tr.handle)
                        put("album", tr.album)
                        put("duration", tr.duration)
                        put("cover_url", tr.coverUrl)
                        put("url", tr.url)
                        put("tags", tr.tags)
                    }
                )
            }
            put("tracks", tArr)
        }
        arr.put(obj)
    }
    context.getSharedPreferences(PLAYLIST_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PLAYLIST_PREFS_KEY, arr.toString())
        .apply()
}

@Composable
fun LibraryScreen(
    authRepo: AuthRepository,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier,
    onNavigateSearch: () -> Unit = {},
    onNavigateStudio: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onArtistClick: (name: String, avatarUrl: String, subtitle: String) -> Unit = { _, _, _ -> },
    onPlaylistClick: (id: String, title: String, coverUrl: String, subtitle: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val currentUser by authRepo.currentUser.collectAsState()
    val likedIds by PlayerManager.likedTrackIds.collectAsState()
    val historyTracks by PlayerManager.recentlyPlayedHistory.collectAsState()
    val persistedDownloads by PlayerManager.downloadedTracks.collectAsState()
    val downloadProgress by PlayerManager.downloadProgress.collectAsState()
    val totalDownloadBytes by PlayerManager.totalDownloadBytes.collectAsState()
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val queueTracks by PlayerManager.queue.collectAsState()
    val savedPrompts by musicRepo.savedPrompts.collectAsState()

    var activeFilter by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(LibrarySortMode.RECENT_ACTIVITY) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showInlineFilter by remember { mutableStateOf(false) }
    var inlineFilterQuery by remember { mutableStateOf("") }
    var showLikedCollectionOnly by remember { mutableStateOf(false) }
    var showScopeMenu by remember { mutableStateOf(false) }

    var userPlaylists by remember { mutableStateOf(loadLocalPlaylists(context)) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(PLAYLIST_PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PLAYLIST_PREFS_KEY) {
                userPlaylists = loadLocalPlaylists(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        userPlaylists = loadLocalPlaylists(context)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val cachedChartTracks = remember { musicRepo.getCachedSongsChart("hot", false) }
    val allKnownTracks = remember(historyTracks, queueTracks, persistedDownloads, currentTrack, cachedChartTracks) {
        (persistedDownloads + historyTracks + queueTracks + listOfNotNull(currentTrack) + cachedChartTracks)
            .distinctBy { it.id }
    }

    val likedTracks = remember(allKnownTracks, likedIds) {
        allKnownTracks.filter { likedIds.contains(it.id) }
    }

    fun applySortAndQuery(tracks: List<Track>): List<Track> {
        val filtered = if (inlineFilterQuery.isBlank()) {
            tracks
        } else {
            val q = inlineFilterQuery.trim()
            tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.handle.contains(q, ignoreCase = true) ||
                    it.tags.contains(q, ignoreCase = true)
            }
        }
        return when (sortMode) {
            LibrarySortMode.RECENT_ACTIVITY -> filtered
            LibrarySortMode.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortMode.ARTIST_AZ -> filtered.sortedBy { it.displayArtistHandle.lowercase() }
        }
    }

    val displayedSongs = remember(likedTracks, historyTracks, sortMode, inlineFilterQuery) {
        applySortAndQuery((likedTracks + historyTracks).distinctBy { it.id })
    }
    val displayedDownloads = remember(persistedDownloads, sortMode, inlineFilterQuery) {
        applySortAndQuery(persistedDownloads)
    }
    val creatorGroups = remember(allKnownTracks, inlineFilterQuery) {
        applySortAndQuery(allKnownTracks)
            .filter { it.handle.isNotBlank() || it.artist.isNotBlank() }
            .groupBy { it.handle.ifBlank { it.artist } }
            .entries
            .map { (creatorKey, items) -> creatorKey to items }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack)
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showScopeMenu = true }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = if (activeFilter == "Downloads") "Downloads" else "Library",
                        style = TextStyle(
                            fontFamily = YouTubeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.04).em,
                            color = SunoTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = YtmIcons.ChevronDown18,
                        contentDescription = "Switch library scope",
                        tint = SunoTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showScopeMenu,
                    onDismissRequest = { showScopeMenu = false },
                    modifier = Modifier.background(SunoElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Library", color = SunoTextPrimary) },
                        onClick = {
                            activeFilter = null
                            showLikedCollectionOnly = false
                            showScopeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Saved Prompts", color = SunoEmerald) },
                        onClick = {
                            activeFilter = "Saved Prompts"
                            showLikedCollectionOnly = false
                            showScopeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Downloads", color = SunoTextPrimary) },
                        onClick = {
                            activeFilter = "Downloads"
                            showLikedCollectionOnly = false
                            showScopeMenu = false
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        if (allKnownTracks.isNotEmpty()) {
                            showInlineFilter = !showInlineFilter
                            if (!showInlineFilter) inlineFilterQuery = ""
                        } else {
                            onNavigateSearch()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = YtmIcons.SearchOutline,
                        contentDescription = "Search library",
                        tint = SunoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { showCreatePlaylistDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = YtmIcons.Add24,
                        contentDescription = "New playlist",
                        tint = SunoTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SunoEmerald)
                            .border(1.dp, SunoWhite20, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = currentUser?.name?.firstOrNull()?.uppercaseChar()?.toString()
                        if (initial != null) {
                            Text(
                                text = initial,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SunoPureBlack
                            )
                        } else {
                            Icon(
                                imageVector = YtmIcons.PersonAudio24,
                                contentDescription = "Settings",
                                tint = SunoPureBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showInlineFilter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SunoTinted)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = YtmIcons.SearchOutline,
                    contentDescription = null,
                    tint = SunoTextSubdued,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (inlineFilterQuery.isEmpty()) {
                        Text("Filter your library...", fontSize = 14.sp, color = SunoTextSubdued)
                    }
                    BasicTextField(
                        value = inlineFilterQuery,
                        onValueChange = { inlineFilterQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = SunoTextPrimary),
                        cursorBrush = SolidColor(SunoEmerald),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (inlineFilterQuery.isNotEmpty()) {
                    IconButton(onClick = { inlineFilterQuery = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = YtmIcons.CloseX24,
                            contentDescription = "Clear filter",
                            tint = SunoTextSubdued,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter Chip Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(libraryFilterChips) { chip ->
                val isSelected = activeFilter == chip
                Surface(
                    onClick = {
                        activeFilter = if (isSelected) null else chip
                        showLikedCollectionOnly = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SunoEmerald else SunoWhite10,
                    border = if (isSelected) null else BorderStroke(1.dp, SunoWhite08),
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = chip,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SunoPureBlack else SunoTextPrimary
                        )
                    }
                }
            }
        }

        // 3. Sort Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val values = LibrarySortMode.entries
                    sortMode = values[(sortMode.ordinal + 1) % values.size]
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = YtmIcons.Sort24, contentDescription = "Sort", tint = SunoTextPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = sortMode.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SunoTextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = YtmIcons.ChevronDown18, contentDescription = null, tint = SunoTextSubdued, modifier = Modifier.size(16.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            if (showLikedCollectionOnly) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Liked Songs (${likedTracks.size})", style = YtmDisplay02)
                        TextButton(onClick = { showLikedCollectionOnly = false }) {
                            Text("All library", color = SunoEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (likedTracks.isEmpty()) {
                    item {
                        EmptyLibraryNotice("Tracks you like with the thumbs-up icon will appear here.")
                    }
                } else {
                    items(likedTracks) { track ->
                        TrackListItem(
                            track = track,
                            onTrackClick = { PlayerManager.playTrack(track, likedTracks) },
                            isLiked = true,
                            onLikeClick = { PlayerManager.toggleLike(track.id) },
                            onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                        )
                    }
                }
            } else {
                // Liked Songs & User Playlists
                if (activeFilter == null || activeFilter == "Playlists") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLikedCollectionOnly = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(colors = listOf(SunoEmeraldDeep, SunoEmerald))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = YtmIcons.ThumbUpFilled,
                                    contentDescription = "Liked Songs",
                                    tint = SunoPureBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Liked Songs", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SunoTextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(YtmIcons.Pin24, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (likedTracks.isEmpty()) "Auto playlist" else "Auto playlist • ${likedTracks.size} songs",
                                        fontSize = 13.sp,
                                        color = SunoTextSubdued
                                    )
                                }
                            }
                            if (likedTracks.isNotEmpty()) {
                                IconButton(onClick = { PlayerManager.playTrack(likedTracks.first(), likedTracks) }) {
                                    Icon(YtmIcons.PlayCircle24, contentDescription = "Play Liked", tint = SunoEmerald, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }

                    items(userPlaylists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (playlist.tracks.isNotEmpty()) {
                                        PlayerManager.playTrack(playlist.tracks.first(), playlist.tracks)
                                    } else {
                                        onPlaylistClick(playlist.id, playlist.name, playlist.coverUrl, "Playlist")
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SunoElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playlist.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = playlist.coverUrl,
                                        contentDescription = playlist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(YtmIcons.Mix24, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SunoTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Playlist • ${playlist.tracks.size} songs", fontSize = 13.sp, color = SunoTextSubdued)
                            }
                            IconButton(
                                onClick = {
                                    val updated = userPlaylists.filterNot { it.id == playlist.id }
                                    userPlaylists = updated
                                    saveLocalPlaylists(context, updated)
                                }
                            ) {
                                Icon(YtmIcons.CloseX24, contentDescription = "Remove", tint = SunoTextSubdued, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Saved Prompts Section
                if (activeFilter == null || activeFilter == "Saved Prompts") {
                    if (activeFilter == "Saved Prompts" || savedPrompts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Saved Studio Prompts (${savedPrompts.size})",
                                style = YtmTitle03,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        if (savedPrompts.isEmpty()) {
                            item {
                                EmptyLibraryNotice("Save Style DNA prompts from Prompt Studio to build your reusable preset bank.")
                            }
                        } else {
                            items(savedPrompts) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SunoElevated,
                                    border = BorderStroke(1.dp, SunoWhite08),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = prompt,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = SunoTextPrimary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                onClick = {
                                                    musicRepo.loadFullStyle(prompt)
                                                    onNavigateStudio()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = SunoEmeraldMuted
                                            ) {
                                                Text(
                                                    text = "Open in Studio",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SunoEmerald,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                            Surface(
                                                onClick = {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("Suno Prompt", prompt))
                                                    Toast.makeText(context, "Copied prompt", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = SunoTinted
                                            ) {
                                                Text(
                                                    text = "Copy",
                                                    fontSize = 11.5.sp,
                                                    color = SunoTextPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(onClick = { musicRepo.removeSavedPrompt(prompt) }) {
                                                Text("Delete", color = SunoTextSubdued, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Downloads Section
                if (activeFilter == null || activeFilter == "Downloads") {
                    if (activeFilter == "Downloads" || displayedDownloads.isNotEmpty() || downloadProgress.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = if (downloadProgress.isNotEmpty()) {
                                        "Downloads (${displayedDownloads.size}) • Downloading ${downloadProgress.size}..."
                                    } else {
                                        "Downloads (${displayedDownloads.size})"
                                    },
                                    style = YtmTitle03
                                )
                                if (displayedDownloads.isNotEmpty()) {
                                    val sizeMb = totalDownloadBytes / (1024f * 1024f)
                                    Text(
                                        text = String.format(Locale.getDefault(), "Saved offline in Music/KV Suno • %.1f MB", sizeMb),
                                        style = YtmSubtitle01,
                                        color = SunoTextSubdued
                                    )
                                }
                            }
                        }
                        if (displayedDownloads.isEmpty()) {
                            item {
                                EmptyLibraryNotice("No offline Suno tracks saved yet. Tap Download on any song to save .m4a to Music/KV Suno.")
                            }
                        } else {
                            items(displayedDownloads) { track ->
                                TrackListItem(
                                    track = track,
                                    onTrackClick = { PlayerManager.playTrack(track, displayedDownloads) },
                                    isLiked = likedIds.contains(track.id),
                                    onLikeClick = { PlayerManager.toggleLike(track.id) },
                                    onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                                )
                            }
                        }
                    }
                }

                // Songs Section
                if (activeFilter == "Songs") {
                    if (displayedSongs.isEmpty()) {
                        item { EmptyLibraryNotice("Your played and liked Suno tracks will appear here.") }
                    } else {
                        items(displayedSongs) { track ->
                            TrackListItem(
                                track = track,
                                onTrackClick = { PlayerManager.playTrack(track, displayedSongs) },
                                isLiked = likedIds.contains(track.id),
                                onLikeClick = { PlayerManager.toggleLike(track.id) },
                                onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                            )
                        }
                    }
                }

                // Creators Section
                if (activeFilter == "Creators") {
                    if (creatorGroups.isEmpty()) {
                        item { EmptyLibraryNotice("Creators you listen to will appear here.") }
                    } else {
                        items(creatorGroups) { (creatorKey, items) ->
                            val lead = items.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onArtistClick(
                                            lead.handle.ifBlank { creatorKey },
                                            lead.coverUrl,
                                            "${lead.artist} • ${items.size} tracks"
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = lead.coverUrl,
                                    contentDescription = lead.artist,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SunoElevated)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lead.artist.ifBlank { creatorKey }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SunoTextPrimary)
                                    Text("@${lead.handle.ifBlank { creatorKey }} • ${items.size} tracks", fontSize = 13.sp, color = SunoTextSubdued)
                                }
                            }
                        }
                    }
                }

                // Recent History on Overview
                if (activeFilter == null && historyTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Recently Played",
                            style = YtmTitle03,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(historyTracks.take(15)) { track ->
                        TrackListItem(
                            track = track,
                            onTrackClick = { PlayerManager.playTrack(track, historyTracks) },
                            isLiked = likedIds.contains(track.id),
                            onLikeClick = { PlayerManager.toggleLike(track.id) },
                            onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = SunoBase,
            titleContentColor = SunoTextPrimary,
            title = { Text("New playlist", style = YtmTitle03) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist title", color = SunoTextSubdued) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SunoEmerald,
                        unfocusedBorderColor = SunoWhite20,
                        focusedTextColor = SunoTextPrimary,
                        unfocusedTextColor = SunoTextPrimary,
                        cursorColor = SunoEmerald
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newPlaylistName.trim()
                        if (trimmed.isNotEmpty()) {
                            val updated = listOf(
                                LocalUserPlaylist(
                                    id = UUID.randomUUID().toString(),
                                    name = trimmed,
                                    coverUrl = "",
                                    tracks = emptyList()
                                )
                            ) + userPlaylists
                            userPlaylists = updated
                            saveLocalPlaylists(context, updated)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create", color = SunoEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = SunoTextSubdued)
                }
            }
        )
    }
}

@Composable
private fun EmptyLibraryNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = YtmSubtitle01, color = SunoTextSubdued)
    }
}
