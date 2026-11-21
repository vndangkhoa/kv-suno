package com.kvsuno.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val SunoDarkColorScheme = darkColorScheme(
    primary = SunoEmerald,
    onPrimary = SunoPureBlack,
    primaryContainer = SunoTinted,
    onPrimaryContainer = SunoTextPrimary,
    secondary = SunoEmeraldBright,
    onSecondary = SunoPureBlack,
    background = SunoPureBlack,
    onBackground = SunoTextPrimary,
    surface = SunoBase,
    onSurface = SunoTextPrimary,
    surfaceVariant = SunoTinted,
    onSurfaceVariant = SunoTextSubdued,
    outline = SunoWhite15
)

@Composable
fun KVSunoTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = SunoBase.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SunoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
