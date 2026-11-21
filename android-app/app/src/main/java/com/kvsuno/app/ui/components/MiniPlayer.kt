package com.kvsuno.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.SunoWhite15
import com.kvsuno.app.ui.theme.YtmSubtitle01
import com.kvsuno.app.ui.theme.YtmTitle02
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progressFraction: Float,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onClickExpand: () -> Unit,
    isFloatingWide: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (track == null) return@AnimatedVisibility

        var isDragging by remember(track.id) { mutableStateOf(false) }
        var dragOffsetPx by remember(track.id) { mutableFloatStateOf(0f) }
        val animatedOffsetPx by animateFloatAsState(
            targetValue = dragOffsetPx,
            animationSpec = if (isDragging) snap() else tween(durationMillis = 180),
            label = "miniPlayerSwipeOffset"
        )
        val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

        Surface(
            modifier = Modifier
                .then(
                    if (isFloatingWide) {
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onClickExpand() },
            shape = if (isFloatingWide) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp),
            color = SunoElevated,
            border = if (isFloatingWide) BorderStroke(1.dp, SunoWhite10) else null,
            tonalElevation = 0.dp,
            shadowElevation = if (isFloatingWide) 8.dp else 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clipToBounds()
                            .pointerInput(track.id) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        totalDrag = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                        dragOffsetPx = (dragOffsetPx + dragAmount)
                                            .coerceIn(-swipeThresholdPx * 1.4f, swipeThresholdPx * 1.4f)
                                    },
                                    onDragEnd = {
                                        when {
                                            totalDrag <= -swipeThresholdPx -> onSkipNext()
                                            totalDrag >= swipeThresholdPx -> PlayerManager.playPrevious()
                                        }
                                        isDragging = false
                                        dragOffsetPx = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffsetPx = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SunoBase)
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
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    style = YtmTitle02,
                                    color = if (isPlaying) SunoEmerald else SunoTextPrimary,
                                    maxLines = 1,
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

                            if (isFloatingWide && track.primaryGenreTag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SunoEmeraldMuted,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = track.primaryGenreTag,
                                        color = SunoEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = YtmIcons.SkipNext24,
                            contentDescription = "Next track",
                            tint = SunoTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) YtmIcons.Pause24 else YtmIcons.Play24,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = SunoEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(SunoWhite15)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                            .background(SunoEmerald)
                    )
                }
            }
        }
    }
}
