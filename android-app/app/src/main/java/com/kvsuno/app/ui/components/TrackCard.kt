package com.kvsuno.app.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.theme.ChartBronze
import com.kvsuno.app.ui.theme.ChartGold
import com.kvsuno.app.ui.theme.ChartSilver
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoBlackAt50
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.YtmSubtitle01
import com.kvsuno.app.ui.theme.YtmTitle02
import java.util.Locale

internal fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds <= 0) return ""
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

internal fun formatCompactNumber(value: Long?): String {
    val n = value ?: 0L
    return when {
        n >= 1_000_000L -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
        n >= 1_000L -> String.format(Locale.US, "%.1fK", n / 1_000.0)
        n > 0L -> n.toString()
        else -> ""
    }
}

private fun shareTrack(context: Context, track: Track) {
    val shareUrl = "https://suno.com/song/${track.id}"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(
            Intent.EXTRA_TEXT,
            "Listen to '${track.title}' by ${track.displayArtistHandle} on KV Suno:\n$shareUrl"
        )
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Suno Track"))
}

@Composable
fun TrackListItem(
    track: Track,
    onTrackClick: () -> Unit,
    rankNumber: Int? = null,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onAddStyleToStudio: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    queue: List<Track> = emptyList(),
    onPlayNext: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlayerPlaying by PlayerManager.isPlaying.collectAsState()
    val downloadedIds by PlayerManager.downloadedTrackIds.collectAsState()
    val downloadProgress by PlayerManager.downloadProgress.collectAsState()
    val likedIds by PlayerManager.likedTrackIds.collectAsState()

    val isCurrent = currentTrack?.id == track.id
    val isCurrentPlaying = isCurrent && isPlayerPlaying
    val isDownloaded = downloadedIds.contains(track.id)
    val trackDownloadProgress = downloadProgress[track.id]

    var likedOverride by remember(track.id, isLiked) { mutableStateOf<Boolean?>(null) }
    val effectiveLiked = likedOverride ?: (isLiked || likedIds.contains(track.id))

    var showMenuSheet by remember { mutableStateOf(false) }

    val formattedDuration = formatDuration(track.duration)
    val playsStr = formatCompactNumber(track.viewCount)
    val subtitleText = buildString {
        append(track.displayArtistHandle)
        if (playsStr.isNotEmpty()) {
            append(" • ")
            append(playsStr)
            append(" plays")
        }
        if (formattedDuration.isNotEmpty()) {
            append(" • ")
            append(formattedDuration)
        }
    }

    val toggleLikeAction: () -> Unit = {
        val nextLiked = !effectiveLiked
        if (onLikeClick != null) {
            onLikeClick()
        } else {
            PlayerManager.toggleLike(track.id)
        }
        likedOverride = nextLiked
        Toast.makeText(
            context,
            if (nextLiked) "Saved '${track.title}' to Liked Songs" else "Removed '${track.title}' from Liked Songs",
            Toast.LENGTH_SHORT
        ).show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 66.dp)
            .clickable { onTrackClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rankNumber != null) {
            val rankColor = when (rankNumber) {
                1 -> ChartGold
                2 -> ChartSilver
                3 -> ChartBronze
                else -> SunoTextSubdued
            }
            Text(
                text = "$rankNumber",
                style = YtmTitle02.copy(
                    fontWeight = if (rankNumber <= 3) FontWeight.Bold else FontWeight.Medium
                ),
                color = rankColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SunoBase),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.11f
                        scaleY = 1.11f
                    }
            )
            if (isCurrentPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SunoBlackAt50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = YtmIcons.WaveformMusic18,
                        contentDescription = "Playing",
                        tint = SunoEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 6.dp)
        ) {
            Text(
                text = track.title,
                style = YtmTitle02,
                color = if (isCurrent) SunoEmerald else SunoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trackDownloadProgress != null) {
                    CircularProgressIndicator(
                        color = SunoEmerald,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else if (isDownloaded) {
                    Icon(
                        imageVector = YtmIcons.Downloaded24,
                        contentDescription = "Downloaded",
                        tint = SunoEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = subtitleText,
                    style = YtmSubtitle01,
                    color = SunoTextSubdued,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onAddStyleToStudio != null && track.tags.isNotBlank()) {
            Surface(
                onClick = {
                    onAddStyleToStudio(track.tags)
                    Toast.makeText(context, "Loaded Style DNA into Prompt Studio", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = SunoEmeraldMuted,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Text(
                    text = "+ DNA",
                    color = SunoEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        IconButton(
            onClick = { showMenuSheet = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = YtmIcons.OverflowVertical24,
                contentDescription = "Menu",
                tint = SunoTextSubdued,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showMenuSheet) {
        TrackMenuBottomSheet(
            track = track,
            isLiked = effectiveLiked,
            isDownloaded = isDownloaded,
            downloadProgress = trackDownloadProgress,
            onDismiss = { showMenuSheet = false },
            onPlayNext = {
                if (onPlayNext != null) {
                    onPlayNext()
                } else {
                    val baseQueue = queue.ifEmpty { PlayerManager.queue.value }
                    val nextQueue = if (baseQueue.isEmpty()) {
                        listOf(track)
                    } else {
                        listOf(track) + baseQueue.filterNot { it.id == track.id }
                    }
                    PlayerManager.playTrack(track, nextQueue)
                }
                showMenuSheet = false
            },
            onSendStyleToStudio = if (onAddStyleToStudio != null && track.tags.isNotBlank()) {
                {
                    onAddStyleToStudio(track.tags)
                    Toast.makeText(context, "Style DNA loaded into Prompt Studio", Toast.LENGTH_SHORT).show()
                    showMenuSheet = false
                }
            } else null,
            onToggleLike = toggleLikeAction,
            onToggleDownload = {
                PlayerManager.toggleDownload(context, track)
                showMenuSheet = false
            },
            onShare = {
                shareTrack(context, track)
                showMenuSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackMenuBottomSheet(
    track: Track,
    isLiked: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onSendStyleToStudio: (() -> Unit)?,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SunoElevated,
        contentColor = SunoTextPrimary,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SunoBase)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = YtmTitle02,
                        color = SunoTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.displayArtistHandle,
                        style = YtmSubtitle01,
                        color = SunoTextSubdued,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) YtmIcons.ThumbUpFilled else YtmIcons.ThumbUpOutline,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) SunoEmerald else SunoTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SunoWhite08)
            )

            TrackMenuItem(
                icon = YtmIcons.QueueNext24,
                label = "Play next",
                onClick = onPlayNext
            )
            if (onSendStyleToStudio != null) {
                TrackMenuItem(
                    icon = YtmIcons.Spark24,
                    label = "Load Style DNA into Prompt Studio",
                    iconTint = SunoEmerald,
                    onClick = onSendStyleToStudio
                )
            }
            TrackMenuItem(
                icon = if (isLiked) YtmIcons.CheckCircle24 else YtmIcons.PlaylistAdd24,
                label = if (isLiked) "Saved to Liked Songs" else "Save to Liked Songs",
                onClick = {
                    onToggleLike()
                    onDismiss()
                }
            )
            TrackMenuItem(
                icon = if (isDownloaded) YtmIcons.Downloaded24 else YtmIcons.Download24,
                label = when {
                    downloadProgress != null -> "Downloading ${(downloadProgress * 100).toInt().coerceIn(1, 99)}% (tap to cancel)"
                    isDownloaded -> "Remove offline download"
                    else -> "Save .m4a to phone (Music/KV Suno)"
                },
                onClick = onToggleDownload
            )
            TrackMenuItem(
                icon = YtmIcons.Share24,
                label = "Share Suno link",
                onClick = onShare
            )
        }
    }
}

@Composable
private fun TrackMenuItem(
    icon: ImageVector,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color = SunoTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            style = YtmTitle02,
            color = SunoTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackCardSquare(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlayerPlaying by PlayerManager.isPlaying.collectAsState()
    val isCurrent = currentTrack?.id == track.id
    val isCurrentPlaying = isCurrent && isPlayerPlaying

    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SunoElevated),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.11f
                        scaleY = 1.11f
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SunoEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentPlaying) YtmIcons.Pause24 else YtmIcons.Play24,
                    contentDescription = "Play",
                    tint = SunoPureBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            style = YtmTitle02,
            color = if (isCurrent) SunoEmerald else SunoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.displayArtistHandle,
            style = YtmSubtitle01.copy(fontSize = 12.sp),
            color = SunoTextSubdued,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (track.primaryGenreTag.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SunoElevated
            ) {
                Text(
                    text = track.primaryGenreTag,
                    fontSize = 10.sp,
                    color = SunoEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
