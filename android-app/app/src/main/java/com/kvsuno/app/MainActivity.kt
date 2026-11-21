package com.kvsuno.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kvsuno.app.player.PlayerManager
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.adaptive.ProvideWindowState
import com.kvsuno.app.ui.components.AppNavigationRail
import com.kvsuno.app.ui.components.BottomNavBar
import com.kvsuno.app.ui.components.MiniPlayer
import com.kvsuno.app.ui.components.NavScreen
import com.kvsuno.app.ui.screens.detail.DetailScreen
import com.kvsuno.app.ui.screens.home.HomeScreen
import com.kvsuno.app.ui.screens.library.LibraryScreen
import com.kvsuno.app.ui.screens.player.FullPlayerScreen
import com.kvsuno.app.ui.screens.search.SearchScreen
import com.kvsuno.app.ui.screens.settings.SettingsScreen
import com.kvsuno.app.ui.screens.studio.PromptStudioScreen
import com.kvsuno.app.ui.theme.KVSunoTheme
import com.kvsuno.app.ui.theme.SunoPureBlack

private data class DetailRouteArgs(
    val id: String,
    val title: String,
    val coverUrl: String,
    val subtitle: String,
    val type: String
)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        PlayerManager.getExoPlayer(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            KVSunoTheme {
                ProvideWindowState {
                    KVSunoMainShell()
                }
            }
        }
    }
}

@Composable
private fun KVSunoMainShell() {
    val windowState = LocalWindowState.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavScreen.Home.route

    val musicRepo = remember { KVSunoApp.instance.musicRepository }
    val authRepo = remember { KVSunoApp.instance.authRepository }

    val studioTags by musicRepo.studioTags.collectAsState()
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val duration by PlayerManager.duration.collectAsState()

    val effectiveDuration = if (duration > 0L) duration else ((currentTrack?.duration ?: 0) * 1000L)
    val progressFraction = if (effectiveDuration > 0L) {
        (currentPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var detailArgs by remember { mutableStateOf<DetailRouteArgs?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val navigateTopLevel: (String) -> Unit = { route ->
        detailArgs = null
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(NavScreen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openArtistDetail: (String, String, String) -> Unit = { name, avatarUrl, subtitle ->
        val cleanHandle = name.removePrefix("@").trim()
        detailArgs = DetailRouteArgs(
            id = cleanHandle,
            title = if (name.startsWith("@")) name else "@$cleanHandle",
            coverUrl = avatarUrl,
            subtitle = subtitle,
            type = "creator"
        )
    }

    val openPlaylistDetail: (String, String, String, String) -> Unit = { id, title, coverUrl, subtitle ->
        detailArgs = DetailRouteArgs(
            id = id,
            title = title,
            coverUrl = coverUrl,
            subtitle = subtitle,
            type = "playlist"
        )
    }

    val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
        Box(modifier = contentModifier) {
            NavHost(
                navController = navController,
                startDestination = NavScreen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NavScreen.Home.route) {
                    HomeScreen(
                        musicRepo = musicRepo,
                        onNavigateSettings = { navigateTopLevel(NavScreen.Settings.route) },
                        onNavigateStudio = { navigateTopLevel(NavScreen.Studio.route) },
                        onArtistClick = openArtistDetail,
                        onNavigateSearch = { navigateTopLevel(NavScreen.Search.route) }
                    )
                }
                composable(NavScreen.Search.route) {
                    SearchScreen(
                        musicRepo = musicRepo,
                        onArtistClick = openArtistDetail,
                        onNavigateStudio = { navigateTopLevel(NavScreen.Studio.route) }
                    )
                }
                composable(NavScreen.Studio.route) {
                    PromptStudioScreen(musicRepo = musicRepo)
                }
                composable(NavScreen.Library.route) {
                    LibraryScreen(
                        authRepo = authRepo,
                        musicRepo = musicRepo,
                        onNavigateSearch = { navigateTopLevel(NavScreen.Search.route) },
                        onNavigateStudio = { navigateTopLevel(NavScreen.Studio.route) },
                        onNavigateSettings = { navigateTopLevel(NavScreen.Settings.route) },
                        onArtistClick = openArtistDetail,
                        onPlaylistClick = openPlaylistDetail
                    )
                }
                composable(NavScreen.Settings.route) {
                    SettingsScreen(
                        authRepo = authRepo,
                        musicRepo = musicRepo
                    )
                }
            }

            AnimatedVisibility(
                visible = detailArgs != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                detailArgs?.let { args ->
                    DetailScreen(
                        id = args.id,
                        title = args.title,
                        coverUrl = args.coverUrl,
                        subtitle = args.subtitle,
                        type = args.type,
                        musicRepo = musicRepo,
                        onBack = { detailArgs = null }
                    )
                }
            }
        }
    }

    BackHandler(enabled = isPlayerExpanded || detailArgs != null) {
        when {
            isPlayerExpanded -> isPlayerExpanded = false
            detailArgs != null -> detailArgs = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SunoPureBlack)
    ) {
        if (windowState.useNavigationRail) {
            // Tablet Landscape, Foldable Unfolded Portrait & Half-Opened Posture Chrome
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavigationRail(
                    currentRoute = currentRoute,
                    onNavigate = navigateTopLevel,
                    studioTagCount = studioTags.size
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    mainContent(
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentTrack != null) 78.dp else 0.dp)
                    )

                    MiniPlayer(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        progressFraction = progressFraction,
                        onTogglePlay = { PlayerManager.togglePlayPause() },
                        onSkipNext = { PlayerManager.playNext() },
                        onClickExpand = { isPlayerExpanded = true },
                        isFloatingWide = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .widthIn(max = 560.dp)
                            .navigationBarsPadding()
                            .padding(end = 8.dp, bottom = 6.dp)
                    )
                }
            }
        } else {
            // Compact Phone Chrome
            Scaffold(
                containerColor = SunoPureBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Column(modifier = Modifier.background(SunoPureBlack)) {
                        MiniPlayer(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progressFraction = progressFraction,
                            onTogglePlay = { PlayerManager.togglePlayPause() },
                            onSkipNext = { PlayerManager.playNext() },
                            onClickExpand = { isPlayerExpanded = true },
                            isFloatingWide = false
                        )
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateTopLevel,
                            studioTagCount = studioTags.size
                        )
                    }
                }
            ) { innerPadding ->
                mainContent(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
        }

        AnimatedVisibility(
            visible = isPlayerExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerScreen(
                musicRepo = musicRepo,
                onCollapse = { isPlayerExpanded = false },
                onNavigateStudio = {
                    isPlayerExpanded = false
                    navigateTopLevel(NavScreen.Studio.route)
                },
                onArtistClick = openArtistDetail
            )
        }
    }
}
