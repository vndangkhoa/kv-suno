package com.kvsuno.app.ui.screens.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.api.DirectSunoClient
import com.kvsuno.app.data.model.CreatorHit
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.adaptive.WidthClass
import com.kvsuno.app.ui.components.TrackCardSquare
import com.kvsuno.app.ui.components.TrackListItem
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.components.formatCompactNumber
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoTinted
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.YouTubeSansFontFamily
import com.kvsuno.app.ui.theme.YtmDisplay02
import com.kvsuno.app.ui.theme.YtmSubtitle01
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ExploreMoodTile(
    val label: String,
    val query: String,
    val stripeColor: Color
)

private val exploreMoodTiles = listOf(
    ExploreMoodTile("V-Pop Ballad", "v-pop", Color(0xFF1ED760)),
    ExploreMoodTile("Folk Trap", "folk trap", Color(0xFFFFB300)),
    ExploreMoodTile("Bolero Trữ Tình", "bolero", Color(0xFF3EA6FF)),
    ExploreMoodTile("Vinahouse Club", "vinahouse", Color(0xFFFF4E45)),
    ExploreMoodTile("Sapa Lo-Fi Chill", "lo-fi", Color(0xFFAB47BC)),
    ExploreMoodTile("Cinematic Epic", "cinematic", Color(0xFF26A69A)),
    ExploreMoodTile("80s Synthwave", "synthwave", Color(0xFFEC407A)),
    ExploreMoodTile("Acoustic Đà Lạt", "acoustic", Color(0xFF66BB6A)),
    ExploreMoodTile("Rap & Drill", "rap", Color(0xFFFF7043)),
    ExploreMoodTile("Đàn Tranh / Sáo", "dan tranh", Color(0xFF5C6BC0))
)

private val searchFilterTabs = listOf("All", "Songs", "Creators", "Style DNA")

@Composable
fun SearchScreen(
    musicRepo: MusicRepository,
    onArtistClick: (name: String, avatarUrl: String, subtitle: String) -> Unit = { _, _, _ -> },
    onNavigateStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowState = LocalWindowState.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val exploreListState = rememberLazyListState()

    var isSearchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var activeFilterTab by remember { mutableStateOf("All") }

    var songs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var creators by remember { mutableStateOf<List<CreatorHit>>(emptyList()) }

    var browseTracks by remember { mutableStateOf(musicRepo.getCachedSongsChart("hot", true)) }
    var browseCreators by remember { mutableStateOf(musicRepo.getCachedCreatorsRanking("hot")) }
    var recentSearches by remember { mutableStateOf(listOf("@futurescall", "Folk Trap", "Bolero", "Sapa Lo-Fi")) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val likedIds by PlayerManager.likedTrackIds.collectAsState()

    LaunchedEffect(Unit) {
        launch {
            val res = musicRepo.getSongsChart("hot", "all", true, 40)
            if (res.isSuccess && res.getOrDefault(emptyList()).isNotEmpty()) {
                browseTracks = res.getOrDefault(emptyList())
            }
        }
        launch {
            val res = musicRepo.getCreatorsRanking("hot")
            if (res.isSuccess && res.getOrDefault(emptyList()).isNotEmpty()) {
                browseCreators = res.getOrDefault(emptyList())
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive && query.isEmpty()) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun performSearch(
        rawQuery: String,
        dismissKeyboard: Boolean = true,
        saveRecent: Boolean = true,
        debounceMs: Long = 0L
    ) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            searchJob?.cancel()
            isLoading = false
            return
        }
        if (dismissKeyboard) focusManager.clearFocus()
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            if (dismissKeyboard || (songs.isEmpty() && creators.isEmpty())) {
                isLoading = true
            }
            try {
                val res = musicRepo.universalSearch(trimmed)
                if (res.isSuccess) {
                    val data = res.getOrNull()
                    if (data != null) {
                        songs = data.songs
                        creators = data.creators
                    }
                }
                if (saveRecent) {
                    recentSearches = (listOf(trimmed) + recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) }).take(8)
                }
            } finally {
                isLoading = false
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                isSearchActive = true
                query = spoken
                performSearch(spoken, dismissKeyboard = true, saveRecent = true)
            }
        }
    }

    fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Suno songs, @creators, or style tags")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice search unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun exitActiveSearch() {
        searchJob?.cancel()
        focusManager.clearFocus()
        isLoading = false
        isSearchActive = false
        query = ""
        songs = emptyList()
        creators = emptyList()
    }

    val showingSearchMode = isSearchActive || query.isNotEmpty()
    val matchingWikiTags = remember(query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) emptyList()
        else DirectSunoClient.soundWikiCatalog.filter {
            it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.promptSnippet.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack)
    ) {
        if (!showingSearchMode) {
            LazyColumn(
                state = exploreListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Explore & Creators",
                            style = TextStyle(
                                fontFamily = YouTubeSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.04).em,
                                color = SunoTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(SunoTinted)
                                .border(1.dp, SunoWhite08, RoundedCornerShape(23.dp))
                                .clickable { isSearchActive = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = YtmIcons.SearchOutline,
                                contentDescription = "Search",
                                tint = SunoEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search tracks, @creators, or Style DNA...",
                                fontSize = 14.sp,
                                color = SunoTextSubdued,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { startVoiceSearch() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = YtmIcons.Mic24,
                                    contentDescription = "Voice search",
                                    tint = SunoTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Navigation Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SunoExploreNavButton(
                            label = "Hot Charts",
                            icon = YtmIcons.Album24,
                            onClick = {
                                coroutineScope.launch { exploreListState.animateScrollToItem(5) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SunoExploreNavButton(
                            label = "32 Creators",
                            icon = YtmIcons.PersonAudio24,
                            onClick = {
                                coroutineScope.launch { exploreListState.animateScrollToItem(3) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SunoExploreNavButton(
                            label = "Studio",
                            icon = YtmIcons.Spark24,
                            onClick = onNavigateStudio,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Recent Searches
                item {
                    if (recentSearches.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Searches",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SunoTextSubdued
                                )
                                Text(
                                    text = "Clear",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SunoEmerald,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { recentSearches = emptyList() }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(recentSearches) { itemQuery ->
                                    Surface(
                                        onClick = {
                                            isSearchActive = true
                                            query = itemQuery
                                            performSearch(itemQuery, dismissKeyboard = true, saveRecent = true)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = SunoElevated,
                                        border = BorderStroke(1.dp, SunoWhite08)
                                    ) {
                                        Text(
                                            text = itemQuery,
                                            fontSize = 13.sp,
                                            color = SunoTextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Community Creators Showcase
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vietnamese & Global Suno Creators",
                        style = YtmDisplay02,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(browseCreators.take(24)) { creator ->
                            Surface(
                                onClick = {
                                    onArtistClick(
                                        creator.cleanHandle,
                                        creator.bestAvatar,
                                        "${creator.bestName} • ${creator.styleFocus}"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = SunoElevated,
                                border = BorderStroke(1.dp, SunoWhite08),
                                modifier = Modifier.width(176.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = creator.bestAvatar.ifBlank { creator.topSongs.firstOrNull()?.coverUrl.orEmpty() },
                                            contentDescription = creator.bestName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(SunoBase)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = creator.bestName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SunoTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "@${creator.cleanHandle}",
                                                fontSize = 11.5.sp,
                                                color = SunoEmerald,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = creator.styleFocus.ifBlank { "Suno AI Producer" },
                                        fontSize = 11.5.sp,
                                        color = SunoTextSubdued,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Moods & Genres Grid (2 columns on phone, 3 columns on tablet/foldable)
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Genres & Acoustic DNA",
                        style = YtmDisplay02,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    val tileCols = when (windowState.widthClass) {
                        WidthClass.Compact -> 2
                        WidthClass.Medium -> 3
                        WidthClass.Expanded -> 4
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        exploreMoodTiles.chunked(tileCols).forEach { rowTiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowTiles.forEach { tile ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SunoElevated)
                                            .clickable {
                                                isSearchActive = true
                                                query = tile.query
                                                activeFilterTab = "All"
                                                performSearch(tile.query, dismissKeyboard = true, saveRecent = true)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .fillMaxHeight()
                                                .background(tile.stripeColor)
                                        )
                                        Text(
                                            text = tile.label,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SunoTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                                repeat(tileCols - rowTiles.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Trending Vietnamese Suno Hits
                if (browseTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(22.dp))
                        Text(
                            text = "Trending Nhạc Việt AI",
                            style = YtmDisplay02,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    val slice = browseTracks.take(15)
                    itemsIndexed(slice) { index, track ->
                        TrackListItem(
                            track = track,
                            rankNumber = index + 1,
                            onTrackClick = { PlayerManager.playTrack(track, slice) },
                            isLiked = likedIds.contains(track.id),
                            onLikeClick = { PlayerManager.toggleLike(track.id) },
                            onAddStyleToStudio = { tags -> musicRepo.loadFullStyle(tags) }
                        )
                    }
                }
            }
        } else {
            // ACTIVE SEARCH STATE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SunoBase)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exitActiveSearch() }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = YtmIcons.ArrowLeft24,
                            contentDescription = "Back",
                            tint = SunoTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search tracks, @creators, or tags",
                                fontSize = 17.sp,
                                color = SunoTextSubdued,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { next ->
                                query = next
                                val trimmed = next.trim()
                                if (trimmed.length >= 2) {
                                    performSearch(next, dismissKeyboard = false, saveRecent = false, debounceMs = 280L)
                                } else {
                                    searchJob?.cancel()
                                    isLoading = false
                                    if (trimmed.isEmpty()) {
                                        songs = emptyList()
                                        creators = emptyList()
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 17.sp, color = SunoTextPrimary),
                            cursorBrush = SolidColor(SunoEmerald),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { performSearch(query, dismissKeyboard = true, saveRecent = true) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchJob?.cancel()
                                isLoading = false
                                query = ""
                                songs = emptyList()
                                creators = emptyList()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = YtmIcons.CloseX24,
                                contentDescription = "Clear search",
                                tint = SunoTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(onClick = { startVoiceSearch() }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = YtmIcons.Mic24,
                            contentDescription = "Voice search",
                            tint = SunoTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = SunoWhite10)
            }

            if (query.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchFilterTabs) { tab ->
                        val isSelected = activeFilterTab == tab
                        Surface(
                            onClick = { activeFilterTab = tab },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SunoEmerald else SunoWhite10,
                            border = if (isSelected) null else BorderStroke(1.dp, SunoWhite08),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                Text(
                                    text = tab,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SunoPureBlack else SunoTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            when {
                query.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(recentSearches) { itemQuery ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        query = itemQuery
                                        performSearch(itemQuery, dismissKeyboard = true, saveRecent = true)
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = YtmIcons.SearchOutline,
                                    contentDescription = null,
                                    tint = SunoTextSubdued,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = itemQuery,
                                    fontSize = 15.sp,
                                    color = SunoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { recentSearches = recentSearches - itemQuery },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = YtmIcons.CloseX24,
                                        contentDescription = "Remove",
                                        tint = SunoTextSubdued,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SunoEmerald, strokeWidth = 2.5.dp)
                    }
                }
                songs.isEmpty() && creators.isEmpty() && matchingWikiTags.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results found for \"$query\". Try searching a @handle like @futurescall or a genre tag.",
                            style = YtmSubtitle01,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        if ((activeFilterTab == "All" || activeFilterTab == "Creators") && creators.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Creators",
                                    style = YtmDisplay02,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                            items(creators) { creator ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onArtistClick(
                                                creator.cleanHandle,
                                                creator.bestAvatar,
                                                "${creator.bestName} • ${creator.styleFocus}"
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = creator.bestAvatar.ifBlank { creator.topSongs.firstOrNull()?.coverUrl.orEmpty() },
                                        contentDescription = creator.bestName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(SunoElevated)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = creator.bestName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SunoTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${creator.cleanHandle} • ${formatCompactNumber(creator.totalPlays)} plays",
                                            fontSize = 13.sp,
                                            color = SunoTextSubdued,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if ((activeFilterTab == "All" || activeFilterTab == "Style DNA") && matchingWikiTags.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Sound Wiki Style DNA",
                                    style = YtmDisplay02,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                            items(matchingWikiTags) { tag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            musicRepo.addStudioTag(tag.promptSnippet)
                                            Toast.makeText(context, "Added '${tag.name}' to Prompt Studio", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tag.name} (${tag.category})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SunoTextPrimary
                                        )
                                        Text(
                                            text = tag.promptSnippet,
                                            fontSize = 12.sp,
                                            color = SunoTextSubdued,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SunoEmeraldMuted
                                    ) {
                                        Text(
                                            text = "+ Studio",
                                            color = SunoEmerald,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if ((activeFilterTab == "All" || activeFilterTab == "Songs") && songs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Songs (${songs.size})",
                                    style = YtmDisplay02,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                            items(songs) { track ->
                                TrackListItem(
                                    track = track,
                                    onTrackClick = { PlayerManager.playTrack(track, songs) },
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
    }
}

@Composable
private fun SunoExploreNavButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = SunoElevated,
        border = BorderStroke(1.dp, SunoWhite08),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SunoEmerald,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SunoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
