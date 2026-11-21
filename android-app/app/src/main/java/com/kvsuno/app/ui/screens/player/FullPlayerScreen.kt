package com.kvsuno.app.ui.screens.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.LyricLine
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.components.KvSunoBrandMark
import com.kvsuno.app.ui.components.LyricsSheet
import com.kvsuno.app.ui.components.TrackListItem
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.components.formatCompactNumber
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldBright
import com.kvsuno.app.ui.theme.SunoEmeraldDeep
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextMuted
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoTinted
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.SunoWhite15
import com.kvsuno.app.ui.theme.SunoWhite20
import com.kvsuno.app.ui.theme.YtmDisplay02
import com.kvsuno.app.ui.theme.YtmSubtitle01
import com.kvsuno.app.ui.theme.YtmTitle02
import com.kvsuno.app.ui.theme.YtmTitle03
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", min, sec)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullPlayerScreen(
    musicRepo: MusicRepository,
    onCollapse: () -> Unit,
    onNavigateStudio: () -> Unit = {},
    onArtistClick: (name: String, avatarUrl: String, subtitle: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowState = LocalWindowState.current

    val track by PlayerManager.currentTrack.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val currentPosMs by PlayerManager.currentPosition.collectAsState()
    val durationMs by PlayerManager.duration.collectAsState()
    val waveformBars by PlayerManager.waveformBars.collectAsState()
    val isShuffle by PlayerManager.isShuffle.collectAsState()
    val isRepeat by PlayerManager.isRepeat.collectAsState()
    val queue by PlayerManager.queue.collectAsState()
    val queueIndex by PlayerManager.queueIndex.collectAsState()
    val likedIds by PlayerManager.likedTrackIds.collectAsState()
    val downloadedIds by PlayerManager.downloadedTrackIds.collectAsState()
    val downloadProgress by PlayerManager.downloadProgress.collectAsState()
    val lastLoadLatencyMs by PlayerManager.lastLoadLatencyMs.collectAsState()
    val studioTags by musicRepo.studioTags.collectAsState()

    val activeTrack = track ?: return

    var isVisualizerMode by remember { mutableStateOf(false) }
    var selectedDockTab by remember { mutableIntStateOf(1) } // 0=Up Next, 1=Lyrics, 2=DNA & Related
    var showCompactBottomSheet by remember { mutableStateOf(false) }
    var showLyricsModalSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutesLeft by remember { mutableIntStateOf(0) }

    var lyrics by remember(activeTrack.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLoadingLyrics by remember(activeTrack.id) { mutableStateOf(false) }
    var relatedTracks by remember(activeTrack.id) { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingRelated by remember(activeTrack.id) { mutableStateOf(false) }
    var styleExplanation by remember(activeTrack.id) { mutableStateOf<String?>(null) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekSliderFraction by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onCollapse)

    LaunchedEffect(activeTrack.id) {
        isLoadingLyrics = true
        val res = musicRepo.getLyrics(activeTrack.id, activeTrack.title, activeTrack.artist)
        lyrics = res.getOrNull()?.lyrics.orEmpty()
        isLoadingLyrics = false

        isLoadingRelated = true
        relatedTracks = musicRepo.getRecommendations(activeTrack.id).getOrDefault(emptyList())
        isLoadingRelated = false

        if (activeTrack.tags.isNotBlank()) {
            styleExplanation = musicRepo.explainStyle(activeTrack.title, activeTrack.tags, "vi").explanation
        }
    }

    LaunchedEffect(sleepTimerMinutesLeft) {
        if (sleepTimerMinutesLeft > 0) {
            delay(60_000L)
            sleepTimerMinutesLeft -= 1
            if (sleepTimerMinutesLeft == 0 && PlayerManager.isPlaying.value) {
                PlayerManager.togglePlayPause()
                Toast.makeText(context, "Sleep timer paused playback", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalDurationMs = if (durationMs > 0L) durationMs else (activeTrack.duration * 1000L).coerceAtLeast(1_000L)
    val liveFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    val sliderValue = if (isSeeking) seekSliderFraction else liveFraction
    val displayedPosMs = if (isSeeking) (seekSliderFraction * totalDurationMs).toLong() else currentPosMs

    val isLiked = likedIds.contains(activeTrack.id)
    val isDownloaded = downloadedIds.contains(activeTrack.id)
    val dlProgress = downloadProgress[activeTrack.id]

    val shareTrackAction: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                "Listen to '${activeTrack.title}' by ${activeTrack.displayArtistHandle} on KV Suno:\nhttps://suno.com/song/${activeTrack.id}"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Suno Track"))
    }

    val loadFullStyleAction: () -> Unit = {
        if (activeTrack.tags.isNotBlank()) {
            musicRepo.loadFullStyle(activeTrack.tags)
            Toast.makeText(context, "Loaded Style DNA into Prompt Studio", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No style tags on this track", Toast.LENGTH_SHORT).show()
        }
    }

    val artistClickAction: () -> Unit = {
        val cleanHandle = activeTrack.handle.ifBlank { activeTrack.artist }.removePrefix("@").trim()
        if (cleanHandle.isNotBlank()) {
            onCollapse()
            onArtistClick(cleanHandle, activeTrack.coverUrl, activeTrack.primaryGenreTag)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A2A1A),
            SunoBase,
            SunoPureBlack
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            windowState.isTabletop -> {
                val topHeightDp = if (windowState.hingeTopDp > 120f) {
                    (windowState.hingeTopDp - 36f).coerceAtLeast(160f)
                } else 0f

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top pane above fold hinge: Header + Cover/Visualizer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (topHeightDp > 0f) Modifier.height(topHeightDp.dp)
                                else Modifier.weight(0.45f)
                            )
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlayerTopHeader(
                            isVisualizerMode = isVisualizerMode,
                            onToggleVisualizer = { isVisualizerMode = it },
                            onCollapse = onCollapse,
                            showMenu = showOverflowMenu,
                            onDismissMenu = { showOverflowMenu = false },
                            onOpenMenu = { showOverflowMenu = true },
                            onLoadStyle = loadFullStyleAction,
                            onShare = shareTrackAction,
                            onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            isDownloaded = isDownloaded
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerVisualStage(
                                track = activeTrack,
                                isPlaying = isPlaying,
                                isVisualizerMode = isVisualizerMode,
                                waveformBars = waveformBars,
                                loadLatencyMs = lastLoadLatencyMs,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            )
                        }
                    }

                    // Physical fold crease indicator
                    Surface(
                        color = SunoElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(windowState.hingeHeightDp.coerceIn(18f, 36f).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TABLETOP FLEX MODE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = SunoEmerald
                            )
                        }
                    }

                    // Bottom pane below fold hinge: Compact Controls + Docked Queue/Lyrics/DNA
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        TrackMetaHeaderRow(
                            track = activeTrack,
                            isLiked = isLiked,
                            onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                            onArtistClick = artistClickAction,
                            onQuickStudio = {
                                loadFullStyleAction()
                                onCollapse()
                                onNavigateStudio()
                            }
                        )
                        PlayerSeekBar(
                            sliderValue = sliderValue,
                            displayedPosMs = displayedPosMs,
                            totalDurationMs = totalDurationMs,
                            onValueChange = {
                                isSeeking = true
                                seekSliderFraction = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                PlayerManager.seekTo((seekSliderFraction * totalDurationMs).toLong())
                            }
                        )
                        PlayerTransportRow(
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            isRepeat = isRepeat,
                            playButtonSize = 58,
                            onToggleShuffle = { PlayerManager.toggleShuffle() },
                            onPrev = { PlayerManager.playPrevious() },
                            onTogglePlay = { PlayerManager.togglePlayPause() },
                            onNext = { PlayerManager.playNext() },
                            onToggleRepeat = { PlayerManager.toggleRepeat() }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DockedPanelTabsContent(
                            selectedTab = selectedDockTab,
                            onSelectTab = { selectedDockTab = it },
                            track = activeTrack,
                            queue = queue,
                            queueIndex = queueIndex,
                            lyrics = lyrics,
                            isLoadingLyrics = isLoadingLyrics,
                            currentPosMs = currentPosMs,
                            relatedTracks = relatedTracks,
                            isLoadingRelated = isLoadingRelated,
                            styleExplanation = styleExplanation,
                            studioTags = studioTags,
                            onAddStudioTag = { tag ->
                                musicRepo.addStudioTag(tag)
                                Toast.makeText(context, "Added '$tag' to Prompt Studio", Toast.LENGTH_SHORT).show()
                            },
                            onLoadFullStyle = {
                                loadFullStyleAction()
                                onCollapse()
                                onNavigateStudio()
                            },
                            onOpenFullLyrics = { showLyricsModalSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            windowState.isLandscapeTwoPaneWatch -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left pane: Artwork + Transport (honoring hinge offset in Book mode)
                    val leftPaneModifier = if (windowState.isBook && windowState.hingeStartDp > 220f) {
                        Modifier
                            .width(windowState.hingeStartDp.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                    }
                    Column(
                        modifier = leftPaneModifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlayerTopHeader(
                            isVisualizerMode = isVisualizerMode,
                            onToggleVisualizer = { isVisualizerMode = it },
                            onCollapse = onCollapse,
                            showMenu = showOverflowMenu,
                            onDismissMenu = { showOverflowMenu = false },
                            onOpenMenu = { showOverflowMenu = true },
                            onLoadStyle = loadFullStyleAction,
                            onShare = shareTrackAction,
                            onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            isDownloaded = isDownloaded
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlayerVisualStage(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            isVisualizerMode = isVisualizerMode,
                            waveformBars = waveformBars,
                            loadLatencyMs = lastLoadLatencyMs,
                            modifier = Modifier
                                .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                                .fillMaxWidth(0.78f)
                                .aspectRatio(1f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TrackMetaHeaderRow(
                            track = activeTrack,
                            isLiked = isLiked,
                            onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                            onArtistClick = artistClickAction,
                            onQuickStudio = {
                                loadFullStyleAction()
                                onCollapse()
                                onNavigateStudio()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlayerActionPillsRow(
                            isLiked = isLiked,
                            isDownloaded = isDownloaded,
                            dlProgress = dlProgress,
                            sleepTimerMinutes = sleepTimerMinutesLeft,
                            onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                            onLoadStyle = {
                                loadFullStyleAction()
                                onCollapse()
                                onNavigateStudio()
                            },
                            onShowLyrics = { showLyricsModalSheet = true },
                            onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                            onShare = shareTrackAction,
                            onSleepTimer = { showSleepTimerDialog = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlayerSeekBar(
                            sliderValue = sliderValue,
                            displayedPosMs = displayedPosMs,
                            totalDurationMs = totalDurationMs,
                            onValueChange = {
                                isSeeking = true
                                seekSliderFraction = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                PlayerManager.seekTo((seekSliderFraction * totalDurationMs).toLong())
                            }
                        )
                        PlayerTransportRow(
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            isRepeat = isRepeat,
                            playButtonSize = 64,
                            onToggleShuffle = { PlayerManager.toggleShuffle() },
                            onPrev = { PlayerManager.playPrevious() },
                            onTogglePlay = { PlayerManager.togglePlayPause() },
                            onNext = { PlayerManager.playNext() },
                            onToggleRepeat = { PlayerManager.toggleRepeat() }
                        )
                    }

                    if (windowState.isBook && windowState.hingeWidthDp > 0f) {
                        Spacer(modifier = Modifier.width(windowState.hingeWidthDp.dp))
                    } else {
                        VerticalDivider(thickness = 1.dp, color = SunoWhite08)
                    }

                    // Right pane: Permanently docked Queue / Lyrics / Style DNA & Related
                    DockedPanelTabsContent(
                        selectedTab = selectedDockTab,
                        onSelectTab = { selectedDockTab = it },
                        track = activeTrack,
                        queue = queue,
                        queueIndex = queueIndex,
                        lyrics = lyrics,
                        isLoadingLyrics = isLoadingLyrics,
                        currentPosMs = currentPosMs,
                        relatedTracks = relatedTracks,
                        isLoadingRelated = isLoadingRelated,
                        styleExplanation = styleExplanation,
                        studioTags = studioTags,
                        onAddStudioTag = { tag ->
                            musicRepo.addStudioTag(tag)
                            Toast.makeText(context, "Added '$tag' to Prompt Studio", Toast.LENGTH_SHORT).show()
                        },
                        onLoadFullStyle = {
                            loadFullStyleAction()
                            onCollapse()
                            onNavigateStudio()
                        },
                        onOpenFullLyrics = { showLyricsModalSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            windowState.isFoldablePortraitWatch -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    PlayerTopHeader(
                        isVisualizerMode = isVisualizerMode,
                        onToggleVisualizer = { isVisualizerMode = it },
                        onCollapse = onCollapse,
                        showMenu = showOverflowMenu,
                        onDismissMenu = { showOverflowMenu = false },
                        onOpenMenu = { showOverflowMenu = true },
                        onLoadStyle = loadFullStyleAction,
                        onShare = shareTrackAction,
                        onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        isDownloaded = isDownloaded
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerVisualStage(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            isVisualizerMode = isVisualizerMode,
                            waveformBars = waveformBars,
                            loadLatencyMs = lastLoadLatencyMs,
                            modifier = Modifier.size(164.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TrackMetaHeaderRow(
                                track = activeTrack,
                                isLiked = isLiked,
                                onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                                onArtistClick = artistClickAction,
                                onQuickStudio = {
                                    loadFullStyleAction()
                                    onCollapse()
                                    onNavigateStudio()
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PlayerActionPillsRow(
                                isLiked = isLiked,
                                isDownloaded = isDownloaded,
                                dlProgress = dlProgress,
                                sleepTimerMinutes = sleepTimerMinutesLeft,
                                onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                                onLoadStyle = {
                                    loadFullStyleAction()
                                    onCollapse()
                                    onNavigateStudio()
                                },
                                onShowLyrics = { showLyricsModalSheet = true },
                                onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                                onShare = shareTrackAction,
                                onSleepTimer = { showSleepTimerDialog = true }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PlayerSeekBar(
                                sliderValue = sliderValue,
                                displayedPosMs = displayedPosMs,
                                totalDurationMs = totalDurationMs,
                                onValueChange = {
                                    isSeeking = true
                                    seekSliderFraction = it
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    PlayerManager.seekTo((seekSliderFraction * totalDurationMs).toLong())
                                }
                            )
                            PlayerTransportRow(
                                isPlaying = isPlaying,
                                isShuffle = isShuffle,
                                isRepeat = isRepeat,
                                playButtonSize = 60,
                                onToggleShuffle = { PlayerManager.toggleShuffle() },
                                onPrev = { PlayerManager.playPrevious() },
                                onTogglePlay = { PlayerManager.togglePlayPause() },
                                onNext = { PlayerManager.playNext() },
                                onToggleRepeat = { PlayerManager.toggleRepeat() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    DockedPanelTabsContent(
                        selectedTab = selectedDockTab,
                        onSelectTab = { selectedDockTab = it },
                        track = activeTrack,
                        queue = queue,
                        queueIndex = queueIndex,
                        lyrics = lyrics,
                        isLoadingLyrics = isLoadingLyrics,
                        currentPosMs = currentPosMs,
                        relatedTracks = relatedTracks,
                        isLoadingRelated = isLoadingRelated,
                        styleExplanation = styleExplanation,
                        studioTags = studioTags,
                        onAddStudioTag = { tag ->
                            musicRepo.addStudioTag(tag)
                            Toast.makeText(context, "Added '$tag' to Prompt Studio", Toast.LENGTH_SHORT).show()
                        },
                        onLoadFullStyle = {
                            loadFullStyleAction()
                            onCollapse()
                            onNavigateStudio()
                        },
                        onOpenFullLyrics = { showLyricsModalSheet = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            else -> {
                // Compact Phone Fullscreen Player + Swipe-Up Dock Bar
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerTopHeader(
                        isVisualizerMode = isVisualizerMode,
                        onToggleVisualizer = { isVisualizerMode = it },
                        onCollapse = onCollapse,
                        showMenu = showOverflowMenu,
                        onDismissMenu = { showOverflowMenu = false },
                        onOpenMenu = { showOverflowMenu = true },
                        onLoadStyle = loadFullStyleAction,
                        onShare = shareTrackAction,
                        onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        isDownloaded = isDownloaded
                    )

                    Spacer(modifier = Modifier.weight(0.3f))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayerVisualStage(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            isVisualizerMode = isVisualizerMode,
                            waveformBars = waveformBars,
                            loadLatencyMs = lastLoadLatencyMs,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                .sizeIn(maxWidth = 340.dp, maxHeight = 340.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TrackMetaHeaderRow(
                        track = activeTrack,
                        isLiked = isLiked,
                        onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                        onArtistClick = artistClickAction,
                        onQuickStudio = {
                            loadFullStyleAction()
                            onCollapse()
                            onNavigateStudio()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerActionPillsRow(
                        isLiked = isLiked,
                        isDownloaded = isDownloaded,
                        dlProgress = dlProgress,
                        sleepTimerMinutes = sleepTimerMinutesLeft,
                        onToggleLike = { PlayerManager.toggleLike(activeTrack.id) },
                        onLoadStyle = {
                            loadFullStyleAction()
                            onCollapse()
                            onNavigateStudio()
                        },
                        onShowLyrics = {
                            selectedDockTab = 1
                            showCompactBottomSheet = true
                        },
                        onDownload = { PlayerManager.toggleDownload(context, activeTrack) },
                        onShare = shareTrackAction,
                        onSleepTimer = { showSleepTimerDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PlayerSeekBar(
                        sliderValue = sliderValue,
                        displayedPosMs = displayedPosMs,
                        totalDurationMs = totalDurationMs,
                        onValueChange = {
                            isSeeking = true
                            seekSliderFraction = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            PlayerManager.seekTo((seekSliderFraction * totalDurationMs).toLong())
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PlayerTransportRow(
                        isPlaying = isPlaying,
                        isShuffle = isShuffle,
                        isRepeat = isRepeat,
                        playButtonSize = 70,
                        onToggleShuffle = { PlayerManager.toggleShuffle() },
                        onPrev = { PlayerManager.playPrevious() },
                        onTogglePlay = { PlayerManager.togglePlayPause() },
                        onNext = { PlayerManager.playNext() },
                        onToggleRepeat = { PlayerManager.toggleRepeat() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Swipe-up / tap bottom dock bar (UP NEXT | LYRICS | STYLE DNA)
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = SunoElevated,
                        border = BorderStroke(1.dp, SunoWhite08),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -18f) showCompactBottomSheet = true
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SunoWhite20)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("UP NEXT", "LYRICS", "STYLE DNA").forEachIndexed { idx, label ->
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = if (selectedDockTab == idx) SunoEmerald else SunoTextSubdued,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .clickable {
                                                selectedDockTab = idx
                                                showCompactBottomSheet = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompactBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCompactBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SunoBase,
            contentColor = SunoTextPrimary,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            DockedPanelTabsContent(
                selectedTab = selectedDockTab,
                onSelectTab = { selectedDockTab = it },
                track = activeTrack,
                queue = queue,
                queueIndex = queueIndex,
                lyrics = lyrics,
                isLoadingLyrics = isLoadingLyrics,
                currentPosMs = currentPosMs,
                relatedTracks = relatedTracks,
                isLoadingRelated = isLoadingRelated,
                styleExplanation = styleExplanation,
                studioTags = studioTags,
                onAddStudioTag = { tag ->
                    musicRepo.addStudioTag(tag)
                    Toast.makeText(context, "Added '$tag' to Prompt Studio", Toast.LENGTH_SHORT).show()
                },
                onLoadFullStyle = {
                    showCompactBottomSheet = false
                    loadFullStyleAction()
                    onCollapse()
                    onNavigateStudio()
                },
                onOpenFullLyrics = {
                    showCompactBottomSheet = false
                    showLyricsModalSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 640.dp)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            )
        }
    }

    if (showLyricsModalSheet) {
        LyricsSheet(
            lyrics = lyrics,
            currentPositionMs = currentPosMs,
            onDismiss = { showLyricsModalSheet = false }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = SunoElevated,
            titleContentColor = SunoTextPrimary,
            textContentColor = SunoTextSubdued,
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Off", 15 to "15 minutes", 30 to "30 minutes", 60 to "60 minutes").forEach { (mins, label) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sleepTimerMinutesLeft == mins && mins > 0) SunoEmeraldMuted else SunoTinted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sleepTimerMinutesLeft = mins
                                    showSleepTimerDialog = false
                                    Toast.makeText(
                                        context,
                                        if (mins == 0) "Sleep timer off" else "Playback stops in $mins min",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (sleepTimerMinutesLeft == mins && mins > 0) SunoEmerald else SunoTextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = SunoEmerald)
                }
            }
        )
    }
}

@Composable
private fun PlayerTopHeader(
    isVisualizerMode: Boolean,
    onToggleVisualizer: (Boolean) -> Unit,
    onCollapse: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onOpenMenu: () -> Unit,
    onLoadStyle: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    isDownloaded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                imageVector = YtmIcons.ChevronDown24,
                contentDescription = "Collapse Player",
                tint = SunoTextPrimary
            )
        }

        // Song | Visualizer Emerald Capsule Toggle
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SunoTinted,
            border = BorderStroke(1.dp, SunoWhite10)
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                listOf(false to "Song", true to "Visualizer").forEach { (viz, label) ->
                    val active = isVisualizerMode == viz
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (active) SunoEmerald else Color.Transparent)
                            .clickable { onToggleVisualizer(viz) }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) SunoPureBlack else SunoTextSubdued
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = YtmIcons.OverflowVertical24,
                    contentDescription = "More options",
                    tint = SunoTextPrimary
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
                modifier = Modifier.background(SunoElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Load Style DNA into Studio", color = SunoTextPrimary) },
                    onClick = {
                        onDismissMenu()
                        onLoadStyle()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) "Remove Offline Download" else "Download Offline", color = SunoTextPrimary) },
                    onClick = {
                        onDismissMenu()
                        onDownload()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Suno Track Link", color = SunoTextPrimary) },
                    onClick = {
                        onDismissMenu()
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sleep Timer", color = SunoTextPrimary) },
                    onClick = {
                        onDismissMenu()
                        onOpenSleepTimer()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerVisualStage(
    track: Track,
    isPlaying: Boolean,
    isVisualizerMode: Boolean,
    waveformBars: List<Float>,
    loadLatencyMs: Long?,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(durationMillis = 300),
        label = "artworkPlayScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "eqPhase")
    val eqPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eqPhaseValue"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A110D))
    ) {
        if (!isVisualizerMode) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Live Suno Harmonic Visualizer Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SunoEmeraldDeep.copy(alpha = 0.55f), Color(0xFF070C09))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp)) {
                    val bars = if (waveformBars.isNotEmpty()) waveformBars else List(36) { 0.45f }
                    val sampleCount = 28
                    val step = (bars.size / sampleCount).coerceAtLeast(1)
                    val totalW = size.width
                    val barW = totalW / (sampleCount * 1.6f)
                    val gap = (totalW - barW * sampleCount) / (sampleCount - 1).coerceAtLeast(1)
                    val centerY = size.height * 0.56f

                    for (i in 0 until sampleCount) {
                        val baseAmp = bars[(i * step).coerceAtMost(bars.lastIndex)]
                        val wave = if (isPlaying) (0.65f + 0.35f * sin(eqPhase + i * 0.42f)) else 0.45f
                        val h = (size.height * 0.56f * baseAmp * wave).coerceAtLeast(10f)
                        val x = i * (barW + gap)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(SunoEmeraldBright, SunoEmerald, SunoEmeraldDeep)
                            ),
                            topLeft = Offset(x, centerY - h / 2f),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    KvSunoBrandMark(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.primaryGenreTag.uppercase(Locale.US),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SunoEmerald
                    )
                }
            }
        }

        // Top-left SUNO AI HD Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SunoPureBlack.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, SunoEmerald.copy(alpha = 0.45f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            val badge = buildString {
                append("SUNO AI")
                if (track.modelName.isNotBlank()) append(" • ${track.modelName}")
                append(" • HD")
                if (loadLatencyMs != null && loadLatencyMs in 1L..5000L) {
                    append(" (${loadLatencyMs}ms)")
                }
            }
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SunoEmerald,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TrackMetaHeaderRow(
    track: Track,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onArtistClick: () -> Unit,
    onQuickStudio: () -> Unit
) {
    val plays = formatCompactNumber(track.viewCount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.ifBlank { "Untitled Track" },
                style = YtmDisplay02,
                color = SunoTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onArtistClick)
            ) {
                Text(
                    text = track.displayArtistHandle + if (plays.isNotEmpty()) " • $plays plays" else "",
                    style = YtmSubtitle01,
                    color = SunoTextSubdued,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onQuickStudio) {
            Icon(
                imageVector = YtmIcons.Spark24,
                contentDescription = "Open in Prompt Studio",
                tint = SunoEmerald
            )
        }
        IconButton(onClick = onToggleLike) {
            Icon(
                imageVector = if (isLiked) YtmIcons.ThumbUpFilled else YtmIcons.ThumbUpOutline,
                contentDescription = "Like",
                tint = if (isLiked) SunoEmerald else SunoTextPrimary
            )
        }
    }
}

@Composable
private fun PlayerActionPillsRow(
    isLiked: Boolean,
    isDownloaded: Boolean,
    dlProgress: Float?,
    sleepTimerMinutes: Int,
    onToggleLike: () -> Unit,
    onLoadStyle: () -> Unit,
    onShowLyrics: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onSleepTimer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionPill(
            icon = if (isLiked) YtmIcons.ThumbUpFilled else YtmIcons.ThumbUpOutline,
            label = if (isLiked) "Liked" else "Like",
            highlighted = isLiked,
            onClick = onToggleLike
        )
        ActionPill(
            icon = YtmIcons.Spark24,
            label = "+ Studio DNA",
            highlighted = true,
            onClick = onLoadStyle
        )
        ActionPill(
            icon = YtmIcons.LyricsBubble24,
            label = "Lyrics",
            highlighted = false,
            onClick = onShowLyrics
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDownloaded) SunoEmeraldMuted else SunoTinted,
            border = BorderStroke(1.dp, if (isDownloaded) SunoEmerald.copy(alpha = 0.5f) else SunoWhite10),
            modifier = Modifier.clickable(onClick = onDownload)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                val dlFraction = dlProgress?.let { if (it > 1f) it / 100f else it }?.coerceIn(0f, 1f)
                val dlPercent = dlFraction?.let { (it * 100f).toInt() }
                if (dlFraction != null) {
                    CircularProgressIndicator(
                        progress = { dlFraction },
                        color = SunoEmerald,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) YtmIcons.Downloaded24 else YtmIcons.Download24,
                        contentDescription = null,
                        tint = if (isDownloaded) SunoEmerald else SunoTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        dlPercent != null -> "$dlPercent%"
                        isDownloaded -> "Saved"
                        else -> "Save"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDownloaded) SunoEmerald else SunoTextPrimary
                )
            }
        }
        ActionPill(
            icon = YtmIcons.Share24,
            label = "Share",
            highlighted = false,
            onClick = onShare
        )
        ActionPill(
            icon = YtmIcons.SleepTimer24,
            label = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Timer",
            highlighted = sleepTimerMinutes > 0,
            onClick = onSleepTimer
        )
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) SunoEmeraldMuted else SunoTinted,
        border = BorderStroke(1.dp, if (highlighted) SunoEmerald.copy(alpha = 0.5f) else SunoWhite10),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) SunoEmerald else SunoTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (highlighted) SunoEmerald else SunoTextPrimary
            )
        }
    }
}

@Composable
private fun PlayerSeekBar(
    sliderValue: Float,
    displayedPosMs: Long,
    totalDurationMs: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = SunoEmerald,
                activeTrackColor = SunoEmerald,
                inactiveTrackColor = SunoWhite20
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeMs(displayedPosMs),
                fontSize = 11.sp,
                color = SunoTextSubdued
            )
            Text(
                text = formatTimeMs(totalDurationMs),
                fontSize = 11.sp,
                color = SunoTextSubdued
            )
        }
    }
}

@Composable
private fun PlayerTransportRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    playButtonSize: Int,
    onToggleShuffle: () -> Unit,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = YtmIcons.Shuffle24,
                contentDescription = "Shuffle",
                tint = if (isShuffle) SunoEmerald else SunoTextSubdued,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = YtmIcons.SkipPrevious36,
                contentDescription = "Previous",
                tint = SunoTextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        Surface(
            shape = CircleShape,
            color = SunoEmerald,
            modifier = Modifier
                .size(playButtonSize.dp)
                .clickable(onClick = onTogglePlay)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) YtmIcons.Pause36 else YtmIcons.Play36,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = SunoPureBlack,
                    modifier = Modifier.size((playButtonSize / 2).dp)
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = YtmIcons.SkipNext36,
                contentDescription = "Next",
                tint = SunoTextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = if (isRepeat) YtmIcons.RepeatOne24 else YtmIcons.Repeat24,
                contentDescription = "Repeat",
                tint = if (isRepeat) SunoEmerald else SunoTextSubdued,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DockedPanelTabsContent(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    track: Track,
    queue: List<Track>,
    queueIndex: Int,
    lyrics: List<LyricLine>,
    isLoadingLyrics: Boolean,
    currentPosMs: Long,
    relatedTracks: List<Track>,
    isLoadingRelated: Boolean,
    styleExplanation: String?,
    studioTags: List<String>,
    onAddStudioTag: (String) -> Unit,
    onLoadFullStyle: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "UP NEXT (${queue.size})",
                1 to "LYRICS",
                2 to "STYLE DNA"
            ).forEach { (idx, title) ->
                val active = selectedTab == idx
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) SunoEmeraldMuted else SunoTinted,
                    border = BorderStroke(1.dp, if (active) SunoEmerald else SunoWhite08),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(idx) }
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) SunoEmerald else SunoTextSubdued,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = SunoWhite08)

        when (selectedTab) {
            0 -> {
                // Up Next Queue
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(queue, key = { i, t -> "${t.id}_$i" }) { idx, qTrack ->
                        val isActive = idx == queueIndex || qTrack.id == track.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isActive) SunoEmeraldMuted else Color.Transparent, RoundedCornerShape(10.dp))
                        ) {
                            TrackListItem(
                                track = qTrack,
                                rankNumber = idx + 1,
                                onTrackClick = { PlayerManager.playTrack(qTrack, queue) },
                                onAddStyleToStudio = onAddStudioTag,
                                queue = queue
                            )
                        }
                    }
                    if (queue.isEmpty()) {
                        item {
                            Text(
                                text = "Queue is empty. Play a chart or search result to build your queue.",
                                style = YtmSubtitle01,
                                color = SunoTextMuted,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }

            1 -> {
                // Lyrics Stage with [Section Header] formatting + Copy Lyrics
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lyrics.any { it.time > 0.0 }) "Synced Lyrics" else "Suno Song Lyrics",
                            style = YtmTitle03,
                            color = SunoTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SunoTinted,
                                modifier = Modifier.clickable {
                                    val copyText = if (lyrics.isNotEmpty()) {
                                        lyrics.joinToString("\n") { it.text }
                                    } else track.promptLyrics
                                    if (copyText.isNotBlank()) {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Suno Lyrics", copyText))
                                        Toast.makeText(context, "Copied lyrics", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = "Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SunoEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SunoEmeraldMuted,
                                modifier = Modifier.clickable(onClick = onOpenFullLyrics)
                            ) {
                                Text(
                                    text = "Expand",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SunoEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (isLoadingLyrics) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SunoEmerald, modifier = Modifier.size(28.dp))
                        }
                    } else if (lyrics.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp)
                        ) {
                            if (track.promptLyrics.isNotBlank()) {
                                Text(
                                    text = track.promptLyrics,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    color = SunoTextPrimary
                                )
                            } else {
                                Text(
                                    text = "Instrumental / no prompt lyrics published for this Suno generation.",
                                    style = YtmSubtitle01,
                                    color = SunoTextMuted
                                )
                            }
                        }
                    } else {
                        val isSynced = lyrics.any { it.time > 0.0 }
                        val posSec = currentPosMs / 1000.0
                        val activeIdx = if (isSynced) lyrics.indexOfLast { it.time <= posSec } else -1
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(lyrics) { idx, line ->
                                if (line.isSectionHeader) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SunoEmeraldMuted,
                                        border = BorderStroke(1.dp, SunoEmerald.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text(
                                            text = line.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SunoEmerald,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    val highlighted = isSynced && idx == activeIdx
                                    Text(
                                        text = line.text,
                                        fontSize = if (highlighted) 18.sp else 15.sp,
                                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                        lineHeight = 24.sp,
                                        color = when {
                                            highlighted -> SunoEmerald
                                            isSynced && abs(idx - activeIdx) <= 2 -> SunoTextPrimary
                                            isSynced -> SunoTextMuted
                                            else -> SunoTextPrimary
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (line.time > 0.0) {
                                                    Modifier.clickable {
                                                        PlayerManager.seekTo((line.time * 1000.0).toLong())
                                                    }
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Style DNA Inspector + Related Tracks
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SunoElevated,
                            border = BorderStroke(1.dp, SunoWhite08),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Style DNA Inspector",
                                        style = YtmTitle03,
                                        color = SunoTextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SunoEmerald,
                                        modifier = Modifier.clickable(onClick = onLoadFullStyle)
                                    ) {
                                        Text(
                                            text = "+ Load Full Style",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SunoPureBlack,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val tags = track.styleTagList
                                if (tags.isEmpty()) {
                                    Text(
                                        text = "No style descriptors recorded on this track.",
                                        style = YtmSubtitle01,
                                        color = SunoTextMuted
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        tags.forEach { tag ->
                                            val alreadyInStudio = studioTags.any { it.equals(tag, ignoreCase = true) }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (alreadyInStudio) SunoEmeraldMuted else SunoTinted,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (alreadyInStudio) SunoEmerald else SunoWhite15
                                                ),
                                                modifier = Modifier.clickable { onAddStudioTag(tag) }
                                            ) {
                                                Text(
                                                    text = if (alreadyInStudio) "✓ $tag" else "+ $tag",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (alreadyInStudio) SunoEmerald else SunoTextPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!styleExplanation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(thickness = 1.dp, color = SunoWhite08)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = styleExplanation,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = SunoTextSubdued
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Similar Suno AI Tracks",
                            style = YtmTitle03,
                            color = SunoTextPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    if (isLoadingRelated) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SunoEmerald, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        itemsIndexed(relatedTracks, key = { i, t -> "${t.id}_$i" }) { _, rel ->
                            TrackListItem(
                                track = rel,
                                onTrackClick = { PlayerManager.playTrack(rel, relatedTracks) },
                                onAddStyleToStudio = onAddStudioTag,
                                queue = relatedTracks
                            )
                        }
                    }
                }
            }
        }
    }
}
