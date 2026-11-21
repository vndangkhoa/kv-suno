package com.kvsuno.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.SunoWhite20
import com.kvsuno.app.ui.theme.YtmCaption03

sealed class NavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val unselectedIcon: ImageVector = icon
) {
    data object Home : NavScreen(
        route = "home",
        title = "Home",
        icon = YtmIcons.HomeFilled,
        unselectedIcon = YtmIcons.HomeOutline
    )

    data object Search : NavScreen(
        route = "search",
        title = "Explore",
        icon = YtmIcons.ExploreFilled,
        unselectedIcon = YtmIcons.ExploreOutline
    )

    data object Studio : NavScreen(
        route = "studio",
        title = "Studio",
        icon = YtmIcons.Spark24,
        unselectedIcon = YtmIcons.Spark24
    )

    data object Library : NavScreen(
        route = "library",
        title = "Library",
        icon = YtmIcons.LibraryFilled,
        unselectedIcon = YtmIcons.LibraryOutline
    )

    data object Settings : NavScreen(
        route = "settings",
        title = "Settings",
        icon = YtmIcons.SettingsGear24,
        unselectedIcon = YtmIcons.SettingsGear24
    )
}

val topLevelNavScreens = listOf(
    NavScreen.Home,
    NavScreen.Search,
    NavScreen.Studio,
    NavScreen.Library,
    NavScreen.Settings
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    studioTagCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        color = SunoBase,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SunoWhite08)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topLevelNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        val tint = if (isSelected) SunoEmerald else SunoTextSubdued
                        val labelColor = if (isSelected) SunoTextPrimary else SunoTextSubdued
                        val tabIcon = if (isSelected) screen.icon else screen.unselectedIcon
                        val interactionSource = remember { MutableInteractionSource() }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .defaultMinSize(minWidth = 48.dp)
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = interactionSource,
                                    indication = rememberRipple(bounded = false, color = SunoWhite20),
                                    role = Role.Tab,
                                    onClick = { onNavigate(screen.route) }
                                )
                                .padding(top = 5.dp, start = 2.dp, end = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SunoEmeraldMuted else Color.Transparent)
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = screen.title,
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                if (screen == NavScreen.Studio && studioTagCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(SunoEmerald),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = studioTagCount.coerceAtMost(99).toString(),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SunoPureBlack
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = screen.title,
                                style = YtmCaption03.copy(
                                    color = labelColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    studioTagCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight()) {
        NavigationRail(
            modifier = Modifier.width(88.dp),
            containerColor = SunoBase,
            contentColor = SunoTextPrimary,
            windowInsets = WindowInsets.systemBars.only(
                WindowInsetsSides.Start + WindowInsetsSides.Top + WindowInsetsSides.Bottom
            ),
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                ) {
                    KvSunoBrandMark(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "kv-suno",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunoEmerald
                    )
                }
            }
        ) {
            topLevelNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val tint = if (isSelected) SunoEmerald else SunoTextSubdued
                val labelColor = if (isSelected) SunoTextPrimary else SunoTextSubdued
                val tabIcon = if (isSelected) screen.icon else screen.unselectedIcon
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .selectable(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            indication = rememberRipple(bounded = false, color = SunoWhite20),
                            role = Role.Tab,
                            onClick = { onNavigate(screen.route) }
                        )
                        .padding(vertical = 6.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) SunoEmeraldMuted else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = screen.title,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (screen == NavScreen.Studio && studioTagCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(SunoEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = studioTagCount.coerceAtMost(99).toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SunoPureBlack
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        VerticalDivider(thickness = 1.dp, color = SunoWhite08)
    }
}
