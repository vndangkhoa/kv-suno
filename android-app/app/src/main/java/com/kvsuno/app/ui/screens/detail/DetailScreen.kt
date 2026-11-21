package com.kvsuno.app.ui.screens.detail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.CreatorHit
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.AudioCacheManager
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.components.TrackListItem
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.YtmEntityHeaderTitle
import com.kvsuno.app.ui.theme.YtmSubtitle01
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DetailScreen(
    id: String,
    title: String,
    coverUrl: String,
    subtitle: String,
    type: String,
    musicRepo: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowState = LocalWindowState.current

    var creatorInfo by remember { mutableStateOf<CreatorHit?>(null) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInlineSearch by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val likedIds by PlayerManager.likedTrackIds.collectAsState()
    val downloadedIds by PlayerManager.downloadedTrackIds.collectAsState()

    BackHandler(onBack = onBack)

    val isArtist = type.equals("artist", ignoreCase = true) || type.equals("creator", ignoreCase = true)

    LaunchedEffect(id, title, type) {
        isLoading = true
        if (isArtist) {
            val handle = id.ifBlank { title }.removePrefix("@")
            val res = musicRepo.getCreatorProfile(handle)
            val pair = res.getOrNull()
            creatorInfo = pair?.first
            tracks = pair?.second.orEmpty()
        } else {
            val query = title.ifEmpty { id }
            val res = musicRepo.search(query)
            tracks = res.getOrDefault(emptyList())
        }
        if (tracks.isNotEmpty()) {
            AudioCacheManager.preloadTracks(context, tracks, maxCount = 3)
        }
        isLoading = false
    }

    val resolvedTitle = creatorInfo?.bestName?.ifBlank { title } ?: title
    val resolvedCover = coverUrl.ifBlank { creatorInfo?.bestAvatar.orEmpty() }
        .ifBlank { tracks.firstOrNull()?.coverUrl.orEmpty() }
    val resolvedTags = creatorInfo?.styleFocus?.ifBlank { tracks.firstOrNull()?.tags.orEmpty() }
        ?: tracks.firstOrNull()?.tags.orEmpty()

    val filteredTracks = remember(tracks, filterQuery) {
        if (filterQuery.isBlank()) tracks
        else {
            val q = filterQuery.trim()
            tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.tags.contains(q, ignoreCase = true)
            }
        }
    }

    val typeLabel = when {
        isArtist -> "Suno Creator"
        type.equals("album", ignoreCase = true) -> "Style Collection"
        else -> "Playlist"
    }
    val headerSubtitle = remember(typeLabel, id, subtitle, tracks.size) {
        if (isArtist) {
            "@${id.removePrefix("@")} • ${tracks.size} tracks"
        } else {
            "$typeLabel • ${tracks.size} tracks"
        }
    }
    val allDownloaded = tracks.isNotEmpty() && tracks.all { downloadedIds.contains(it.id) }

    fun shareEntity() {
        val cleanHandle = id.removePrefix("@")
        val shareUrl = if (isArtist) "https://suno.com/@$cleanHandle" else "https://suno.com/song/$cleanHandle"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "Listen to $resolvedTitle on KV Suno:\n$shareUrl")
            setType("text/plain")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share $typeLabel"))
    }

    fun saveToLibraryPlaylist() {
        try {
            val prefs = context.getSharedPreferences("kv_suno_playlists", Context.MODE_PRIVATE)
            val existing = prefs.getString("saved_playlists_json", null)
            val arr = if (existing != null) JSONArray(existing) else JSONArray()
            val tArr = JSONArray()
            tracks.forEach { tr ->
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
            val newObj = JSONObject().apply {
                put("id", id.ifBlank { resolvedTitle })
                put("name", resolvedTitle)
                put("cover_url", resolvedCover)
                put("tracks", tArr)
            }
            val nextArr = JSONArray().apply {
                put(newObj)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    if (item.optString("name") != resolvedTitle) put(item)
                }
            }
            prefs.edit().putString("saved_playlists_json", nextArr.toString()).apply()
            Toast.makeText(context, "Saved '$resolvedTitle' to Library", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Could not save playlist", Toast.LENGTH_SHORT).show()
        }
    }

    val topNavBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(YtmIcons.ArrowLeft24, contentDescription = "Back", tint = SunoTextPrimary, modifier = Modifier.size(24.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        showInlineSearch = !showInlineSearch
                        if (!showInlineSearch) filterQuery = ""
                    }
                ) {
                    Icon(YtmIcons.SearchOutline, contentDescription = "Filter tracks", tint = SunoTextPrimary, modifier = Modifier.size(22.dp))
                }
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(YtmIcons.OverflowVertical24, contentDescription = "More options", tint = SunoTextPrimary, modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(SunoElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play all", color = SunoTextPrimary) },
                            onClick = {
                                showOverflowMenu = false
                                tracks.firstOrNull()?.let { PlayerManager.playTrack(it, tracks) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shuffle play", color = SunoTextPrimary) },
                            onClick = {
                                showOverflowMenu = false
                                val shuffled = tracks.shuffled()
                                shuffled.firstOrNull()?.let { PlayerManager.playTrack(it, shuffled) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save to Library", color = SunoTextPrimary) },
                            onClick = {
                                showOverflowMenu = false
                                saveToLibraryPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = SunoTextPrimary) },
                            onClick = {
                                showOverflowMenu = false
                                shareEntity()
                            }
                        )
                    }
                }
            }
        }
    }

    val inlineSearchRow: @Composable () -> Unit = {
        if (showInlineSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SunoWhite10)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(YtmIcons.SearchOutline, contentDescription = null, tint = SunoTextSubdued, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (filterQuery.isEmpty()) {
                        Text("Search in $resolvedTitle", fontSize = 14.sp, color = SunoTextSubdued)
                    }
                    BasicTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = SunoTextPrimary),
                        cursorBrush = SolidColor(SunoEmerald),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (filterQuery.isNotEmpty()) {
                    IconButton(onClick = { filterQuery = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(YtmIcons.CloseX24, contentDescription = "Clear", tint = SunoTextSubdued, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    val heroHeaderContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = resolvedCover,
                contentDescription = resolvedTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(188.dp)
                    .clip(if (isArtist) CircleShape else RoundedCornerShape(10.dp))
                    .background(SunoElevated)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = resolvedTitle,
                style = YtmEntityHeaderTitle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = headerSubtitle,
                fontSize = 14.sp,
                color = SunoEmerald,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (resolvedTags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = {
                        musicRepo.loadFullStyle(resolvedTags)
                        Toast.makeText(context, "Loaded Style DNA into Prompt Studio", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = SunoEmeraldMuted,
                    border = BorderStroke(1.dp, SunoEmerald.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "+ Load Style DNA: ${resolvedTags.take(48)}",
                        fontSize = 11.5.sp,
                        color = SunoEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            val circleBtnSize = if (windowState.isLandscapeTwoPaneWatch) 40.dp else 44.dp
            val playBtnSize = if (windowState.isLandscapeTwoPaneWatch) 50.dp else 56.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (windowState.isLandscapeTwoPaneWatch) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SunoDetailCircleButton(
                    icon = if (allDownloaded) YtmIcons.Downloaded24 else YtmIcons.Download24,
                    contentDescription = "Download all",
                    buttonSize = circleBtnSize,
                    onClick = { if (tracks.isNotEmpty()) PlayerManager.downloadTracks(context, tracks) }
                )
                SunoDetailCircleButton(
                    icon = YtmIcons.PlaylistAdd24,
                    contentDescription = "Save playlist",
                    buttonSize = circleBtnSize,
                    onClick = { saveToLibraryPlaylist() }
                )
                Box(
                    modifier = Modifier
                        .size(playBtnSize)
                        .clip(CircleShape)
                        .background(SunoEmerald)
                        .clickable {
                            tracks.firstOrNull()?.let { first -> PlayerManager.playTrack(first, tracks) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = YtmIcons.Play36,
                        contentDescription = "Play all",
                        tint = SunoPureBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }
                SunoDetailCircleButton(
                    icon = YtmIcons.Shuffle24,
                    contentDescription = "Shuffle",
                    buttonSize = circleBtnSize,
                    onClick = {
                        val shuffled = tracks.shuffled()
                        shuffled.firstOrNull()?.let { first -> PlayerManager.playTrack(first, shuffled) }
                    }
                )
                SunoDetailCircleButton(
                    icon = YtmIcons.Share24,
                    contentDescription = "Share",
                    buttonSize = circleBtnSize,
                    onClick = { shareEntity() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack)
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                topNavBar()
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SunoEmerald, strokeWidth = 2.5.dp)
                }
            }
        } else if (windowState.isLandscapeTwoPaneWatch) {
            // Wide Tablet / Foldable Landscape: 2-Pane Split (Left Hero Card, Right Tracklist)
            Row(modifier = Modifier.fillMaxSize()) {
                val leftPaneModifier = if (windowState.isBook && windowState.hingeStartDp > 220f) {
                    Modifier
                        .width(windowState.hingeStartDp.dp)
                        .fillMaxHeight()
                        .background(SunoBase)
                } else {
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(SunoBase)
                }
                Box(modifier = leftPaneModifier) {
                    if (resolvedCover.isNotBlank()) {
                        AsyncImage(
                            model = resolvedCover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.32f)
                                .blur(44.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x66000000), SunoPureBlack.copy(alpha = 0.88f), SunoPureBlack)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        topNavBar()
                        inlineSearchRow()
                        heroHeaderContent()
                    }
                }

                if (windowState.isBook && windowState.hingeWidthDp > 0f) {
                    Spacer(modifier = Modifier.width(windowState.hingeWidthDp.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(if (windowState.isBook && windowState.hingeStartDp > 220f) 1f else 0.58f)
                        .fillMaxHeight()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
                ) {
                    if (filteredTracks.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = "No tracks found", style = YtmSubtitle01)
                            }
                        }
                    } else {
                        itemsIndexed(filteredTracks) { index, track ->
                            TrackListItem(
                                track = track,
                                rankNumber = index + 1,
                                onTrackClick = { PlayerManager.playTrack(track, filteredTracks) },
                                isLiked = likedIds.contains(track.id),
                                onLikeClick = { PlayerManager.toggleLike(track.id) },
                                onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (resolvedCover.isNotBlank()) {
                            AsyncImage(
                                model = resolvedCover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.38f)
                                    .blur(40.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x66000000),
                                            Color(0x99000000),
                                            SunoPureBlack.copy(alpha = 0.88f),
                                            SunoPureBlack
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            topNavBar()
                            inlineSearchRow()
                            heroHeaderContent()
                        }
                    }
                }

                if (filteredTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filterQuery.isNotBlank()) "No matching tracks" else "No tracks found",
                                style = YtmSubtitle01
                            )
                        }
                    }
                } else {
                    itemsIndexed(filteredTracks) { index, track ->
                        TrackListItem(
                            track = track,
                            rankNumber = index + 1,
                            onTrackClick = { PlayerManager.playTrack(track, filteredTracks) },
                            isLiked = likedIds.contains(track.id),
                            onLikeClick = { PlayerManager.toggleLike(track.id) },
                            onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SunoDetailCircleButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(SunoWhite10)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SunoTextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
