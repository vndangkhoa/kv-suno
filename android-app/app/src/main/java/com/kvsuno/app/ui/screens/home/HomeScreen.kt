package com.kvsuno.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.CreatorHit
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.AudioCacheManager
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.adaptive.WidthClass
import com.kvsuno.app.ui.components.KvSunoBrandMark
import com.kvsuno.app.ui.components.TrackCardSquare
import com.kvsuno.app.ui.components.TrackListItem
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.components.formatCompactNumber
import com.kvsuno.app.ui.theme.ChartBronze
import com.kvsuno.app.ui.theme.ChartGold
import com.kvsuno.app.ui.theme.ChartSilver
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
import kotlinx.coroutines.launch

private val sunoHomeChips = listOf(
    "🔥 Hot Hits",
    "🇻🇳 Nhạc Việt",
    "V-Pop",
    "Ballad",
    "Folk Trap",
    "Bolero",
    "Lo-Fi",
    "Vinahouse / EDM",
    "Cinematic"
)

private val creatorSortOptions = listOf(
    "hot" to "Hot Score",
    "plays" to "Most Played",
    "tracks" to "Most Tracks"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    musicRepo: MusicRepository,
    onNavigateSettings: () -> Unit = {},
    onNavigateStudio: () -> Unit = {},
    onArtistClick: (name: String, avatarUrl: String, subtitle: String) -> Unit = { _, _, _ -> },
    onNavigateSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowState = LocalWindowState.current
    val studioTags by musicRepo.studioTags.collectAsState()

    var chartTracks by remember { mutableStateOf(musicRepo.getCachedSongsChart("hot", false)) }
    var vnChartTracks by remember { mutableStateOf(musicRepo.getCachedSongsChart("hot", true)) }
    var topCreators by remember { mutableStateOf(musicRepo.getCachedCreatorsRanking("hot")) }

    var selectedChip by remember { mutableStateOf<String?>("🔥 Hot Hits") }
    val chipTrackCache = remember { mutableStateMapOf<String, List<Track>>() }
    var isFilterLoading by remember { mutableStateOf(false) }

    var selectedCreatorSort by remember { mutableStateOf("hot") }
    var isCreatorsLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(chartTracks.isEmpty()) }
    var showCastDialog by remember { mutableStateOf(false) }

    val likedIds by PlayerManager.likedTrackIds.collectAsState()
    val listState = rememberLazyListState()
    val quickPicksCarouselState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun loadCreators(sort: String) {
        coroutineScope.launch {
            isCreatorsLoading = true
            val res = musicRepo.getCreatorsRanking(sort)
            if (res.isSuccess) {
                topCreators = res.getOrDefault(emptyList())
            }
            isCreatorsLoading = false
        }
    }

    fun selectChip(chip: String) {
        if (selectedChip == chip) {
            selectedChip = null
            return
        }
        selectedChip = chip
        if (chip == "🔥 Hot Hits" || chip == "🇻🇳 Nhạc Việt" || chipTrackCache.containsKey(chip)) {
            return
        }
        coroutineScope.launch {
            isFilterLoading = true
            val res = musicRepo.search(chip)
            if (res.isSuccess) {
                chipTrackCache[chip] = res.getOrDefault(emptyList())
            }
            isFilterLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (chartTracks.isEmpty()) isLoading = true
        val chartJob = launch {
            val res = musicRepo.getSongsChart(sortBy = "hot", vnOnly = false, limit = 80)
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                if (list.isNotEmpty()) chartTracks = list
            }
            isLoading = false
        }
        val vnJob = launch {
            val res = musicRepo.getSongsChart(sortBy = "hot", vnOnly = true, limit = 60)
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                if (list.isNotEmpty()) vnChartTracks = list
            }
        }
        val creatorsJob = launch {
            val res = musicRepo.getCreatorsRanking("hot")
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                if (list.isNotEmpty()) topCreators = list
            }
        }
        chartJob.join()
        vnJob.join()
        creatorsJob.join()
        isLoading = false
    }

    val activeFilteredTracks = remember(selectedChip, chipTrackCache.toMap(), chartTracks, vnChartTracks) {
        when (selectedChip) {
            null, "🔥 Hot Hits" -> chartTracks
            "🇻🇳 Nhạc Việt" -> vnChartTracks.ifEmpty { chartTracks.filter { it.isVietnamese } }.ifEmpty { chartTracks }
            else -> {
                val cached = chipTrackCache[selectedChip].orEmpty()
                if (cached.isNotEmpty()) {
                    cached
                } else {
                    val q = selectedChip!!.lowercase()
                    chartTracks.filter {
                        it.tags.lowercase().contains(q) || it.title.lowercase().contains(q)
                    }.ifEmpty { chartTracks }
                }
            }
        }
    }

    val quickPicks = remember(activeFilteredTracks) { activeFilteredTracks.take(16) }
    val featuredMixes = remember(vnChartTracks, activeFilteredTracks) {
        (vnChartTracks + activeFilteredTracks).distinctBy { it.id }.take(14)
    }
    val topChartList = remember(activeFilteredTracks) { activeFilteredTracks.take(30) }

    LaunchedEffect(quickPicks) {
        if (quickPicks.isNotEmpty()) {
            AudioCacheManager.preloadTracks(context, quickPicks, maxCount = 4)
        }
    }

    val ambientFadeAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 0f
            else (1f - (listState.firstVisibleItemScrollOffset / 420f)).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack)
    ) {
        // Ambient emerald radial/vertical wash fading smoothly into SunoPureBlack
        if (ambientFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SunoEmerald.copy(alpha = 0.24f * ambientFadeAlpha),
                                SunoEmeraldDeep.copy(alpha = 0.18f * ambientFadeAlpha),
                                Color.Transparent
                            ),
                            center = Offset(180f, 80f),
                            radius = 780f
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SunoPureBlack.copy(alpha = 0.55f),
                                    SunoPureBlack
                                )
                            )
                        )
                )
            }
        }

        if (isLoading && chartTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KvSunoHomeToolbar(
                    studioTagCount = studioTags.size,
                    onStudioClick = onNavigateStudio,
                    onCastClick = { showCastDialog = true },
                    onSearchClick = onNavigateSearch,
                    onProfileClick = onNavigateSettings
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SunoEmerald,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // 1. KV Suno Top Toolbar
                item {
                    Box(modifier = Modifier.statusBarsPadding()) {
                        KvSunoHomeToolbar(
                            studioTagCount = studioTags.size,
                            onStudioClick = onNavigateStudio,
                            onCastClick = { showCastDialog = true },
                            onSearchClick = onNavigateSearch,
                            onProfileClick = onNavigateSettings
                        )
                    }
                }

                // 2. Genre / Mood Filter Chip Bar
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sunoHomeChips) { chip ->
                            SunoFilterChip(
                                label = chip,
                                isSelected = selectedChip == chip,
                                onClick = { selectChip(chip) }
                            )
                        }
                    }

                    if (isFilterLoading) {
                        LinearProgressIndicator(
                            color = SunoEmerald,
                            trackColor = SunoWhite10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(2.dp)
                        )
                    }
                }

                // 3. Shelf 1 — Hot & Most Played Quick Picks (4-row snap carousel)
                if (quickPicks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedChip?.uppercase()?.let { "$it • SUNO AI STREAM" } ?: "START RADIO FROM AN AI HIT",
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    letterSpacing = 0.05.em,
                                    color = SunoEmerald,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Hot & Most Played",
                                    style = YtmDisplay02,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Surface(
                                onClick = {
                                    quickPicks.firstOrNull()?.let { first ->
                                        PlayerManager.playTrack(first, quickPicks)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = SunoEmerald,
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Play all",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SunoPureBlack,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val colWidth = if (windowState.isWide) 350.dp else (windowState.widthDp - 44).coerceIn(260, 336).dp
                        val columns = remember(quickPicks) { quickPicks.chunked(4) }
                        LazyRow(
                            state = quickPicksCarouselState,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = quickPicksCarouselState),
                            contentPadding = PaddingValues(end = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(columns) { columnTracks ->
                                Column(
                                    modifier = Modifier.width(colWidth)
                                ) {
                                    columnTracks.forEach { track ->
                                        TrackListItem(
                                            track = track,
                                            onTrackClick = { PlayerManager.playTrack(track, quickPicks) },
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

                // 4. Shelf 2 — Featured Suno AI Mixes
                if (featuredMixes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Featured Suno AI Mixes",
                                style = YtmDisplay02,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(featuredMixes) { track ->
                                TrackCardSquare(
                                    track = track,
                                    onClick = { PlayerManager.playTrack(track, featuredMixes) }
                                )
                            }
                        }
                    }
                }

                // 5. Shelf 3 — 32+ Creators Leaderboard
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Creators Leaderboard",
                        style = YtmDisplay02,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(creatorSortOptions) { (key, label) ->
                            SunoFilterChip(
                                label = label,
                                isSelected = selectedCreatorSort == key,
                                onClick = {
                                    if (selectedCreatorSort != key) {
                                        selectedCreatorSort = key
                                        loadCreators(key)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCreatorsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = SunoEmerald,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (topCreators.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(topCreators) { index, creator ->
                                val rank = if (creator.rank > 0) creator.rank else index + 1
                                val medalColor = when (rank) {
                                    1 -> ChartGold
                                    2 -> ChartSilver
                                    3 -> ChartBronze
                                    else -> SunoTinted
                                }
                                val subtitle = buildString {
                                    append("@${creator.cleanHandle}")
                                    val plays = formatCompactNumber(creator.totalPlays)
                                    if (plays.isNotEmpty()) append(" • $plays plays")
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(138.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onArtistClick(
                                                creator.cleanHandle,
                                                creator.bestAvatar,
                                                "${creator.bestName} • ${creator.styleFocus}"
                                            )
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopStart) {
                                        AsyncImage(
                                            model = creator.bestAvatar.ifBlank { creator.topSongs.firstOrNull()?.coverUrl.orEmpty() },
                                            contentDescription = creator.bestName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(128.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (rank <= 3) 2.dp else 1.dp,
                                                    color = if (rank <= 3) medalColor else SunoWhite10,
                                                    shape = CircleShape
                                                )
                                                .background(SunoElevated)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (rank <= 3) medalColor else SunoElevated,
                                            border = BorderStroke(1.dp, SunoPureBlack)
                                        ) {
                                            Text(
                                                text = "#$rank",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rank <= 3) SunoPureBlack else SunoTextPrimary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = creator.bestName,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SunoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = SunoTextSubdued,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Shelf 4 — Top Suno Charts (Adaptive 1-column on phone, 2-column on foldable/tablet)
                if (topChartList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Suno Charts",
                                style = YtmDisplay02
                            )
                            Surface(
                                onClick = {
                                    topChartList.firstOrNull()?.let { first ->
                                        PlayerManager.playTrack(first, topChartList)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, SunoWhite20),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Play all",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SunoTextPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    val colCount = when (windowState.widthClass) {
                        WidthClass.Compact -> 1
                        WidthClass.Medium -> 2
                        WidthClass.Expanded -> if (windowState.widthDp >= 1080) 3 else 2
                    }

                    if (colCount == 1) {
                        itemsIndexed(topChartList) { index, track ->
                            TrackListItem(
                                track = track,
                                rankNumber = index + 1,
                                onTrackClick = { PlayerManager.playTrack(track, topChartList) },
                                isLiked = likedIds.contains(track.id),
                                onLikeClick = { PlayerManager.toggleLike(track.id) },
                                onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                            )
                        }
                    } else {
                        val rows = topChartList.chunked(colCount)
                        itemsIndexed(rows) { rowIdx, rowTracks ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTracks.forEachIndexed { colIdx, track ->
                                    val rank = rowIdx * colCount + colIdx + 1
                                    Box(modifier = Modifier.weight(1f)) {
                                        TrackListItem(
                                            track = track,
                                            rankNumber = rank,
                                            onTrackClick = { PlayerManager.playTrack(track, topChartList) },
                                            isLiked = likedIds.contains(track.id),
                                            onLikeClick = { PlayerManager.toggleLike(track.id) },
                                            onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                                        )
                                    }
                                }
                                repeat(colCount - rowTracks.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            containerColor = SunoBase,
            titleContentColor = SunoTextPrimary,
            textContentColor = SunoTextSubdued,
            title = { Text(text = "Connect to a device", style = YtmTitle03) },
            text = {
                Text(
                    text = "KV Suno audio plays via Media3 background service. Use Android audio output or Bluetooth settings to route playback to speakers or headphones.",
                    style = YtmSubtitle01
                )
            },
            confirmButton = {
                TextButton(onClick = { showCastDialog = false }) {
                    Text("Close", color = SunoEmerald, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun KvSunoHomeToolbar(
    studioTagCount: Int,
    onStudioClick: () -> Unit,
    onCastClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 6.dp)
        ) {
            KvSunoBrandMark(modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KV Suno",
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.04).em,
                    color = SunoTextPrimary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SunoEmeraldMuted
            ) {
                Text(
                    text = "AI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SunoEmerald,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Surface(
                onClick = onStudioClick,
                shape = RoundedCornerShape(16.dp),
                color = if (studioTagCount > 0) SunoEmerald else SunoTinted,
                border = BorderStroke(1.dp, if (studioTagCount > 0) SunoEmerald else SunoWhite10),
                modifier = Modifier.heightIn(min = 30.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = YtmIcons.Spark24,
                        contentDescription = "Prompt Studio",
                        tint = if (studioTagCount > 0) SunoPureBlack else SunoEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (studioTagCount > 0) "Studio ($studioTagCount)" else "Studio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (studioTagCount > 0) SunoPureBlack else SunoTextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            IconButton(
                onClick = onCastClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = YtmIcons.Cast24,
                    contentDescription = "Cast",
                    tint = SunoTextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = YtmIcons.SearchOutline,
                    contentDescription = "Search",
                    tint = SunoTextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick),
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
                    Icon(
                        imageVector = YtmIcons.PersonAudio24,
                        contentDescription = "Settings & Profile",
                        tint = SunoPureBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SunoFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SunoEmerald else SunoWhite10,
        border = if (isSelected) null else BorderStroke(1.dp, SunoWhite08),
        modifier = Modifier.heightIn(min = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SunoPureBlack else SunoTextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
