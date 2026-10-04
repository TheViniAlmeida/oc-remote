package dev.minios.ocremote.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.minios.ocremote.ui.components.LocalAmoledTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DA3FF),
    onPrimary = Color(0xFF1A1B4B),
    primaryContainer = Color(0xFF2D2F6E),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFCAC3DC),
    onSecondary = Color(0xFF322E41),
    secondaryContainer = Color(0xFF494559),
    onSecondaryContainer = Color(0xFFE7DFF8),
    tertiary = Color(0xFF7DD0E1),
    onTertiary = Color(0xFF003640),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF2B2B35),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainer = Color(0xFF1E1E25),
    surfaceContainerHigh = Color(0xFF262630),
    surfaceContainerHighest = Color(0xFF31313B),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF47464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F52B8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF0C0F6A),
    secondary = Color(0xFF5D5B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3DFF9),
    onSecondaryContainer = Color(0xFF1A182C),
    tertiary = Color(0xFF006879),
    onTertiary = Color(0xFFFFFFFF),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainer = Color(0xFFF3EFF7),
    surfaceContainerHigh = Color(0xFFECE8F1),
    surfaceContainerHighest = Color(0xFFE6E2EB),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC9C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

/**
 * AMOLED dark color scheme — pure black surfaces for OLED battery savings.
 * Uses true black (#000000) for the main surface and very dark tones for containers,
 * ensuring cards/sheets are still visually distinguishable from the background.
 */
private val AmoledDarkColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF1A1A22),
    surfaceContainer = Color(0xFF0D0D12),
    surfaceContainerLow = Color(0xFF080810),
    surfaceContainerLowest = Color.Black,
    surfaceContainerHigh = Color(0xFF141419),
    surfaceContainerHighest = Color(0xFF1C1C24)
)

/**
 * OpenCode Material 3 Theme
 * 
 * Supports:
 * - Light/Dark theme based on system settings
 * - Dynamic color on Android 12+ (Material You)
 * - AMOLED dark mode with pure black surfaces
 * - Edge-to-edge display
 */
@Composable
fun OpenCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledDark: Boolean = false,
    accent: String = "default",
    manageSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && accent == "default" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledDark) {
                scheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1A1A22),
                    surfaceContainer = Color(0xFF0D0D12),
                    surfaceContainerLow = Color(0xFF080810),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerHigh = Color(0xFF141419),
                    surfaceContainerHighest = Color(0xFF1C1C24)
                )
            } else {
                scheme
            }
        }
        darkTheme && amoledDark -> AmoledDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val colorScheme = accentColorScheme(baseColorScheme, accent, darkTheme)

    val view = LocalView.current
    if (manageSystemBars && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use surface color for status bar (less jarring than primary)
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAmoledTheme provides (darkTheme && amoledDark)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Fixed accent pairs use light/dark tones with readable foregrounds; surfaces retain AMOLED. */
internal fun accentColorScheme(
    base: androidx.compose.material3.ColorScheme,
    accent: String,
    dark: Boolean,
): androidx.compose.material3.ColorScheme {
    val tones = when (accent) {
        "red" -> 0xFFB3261E to 0xFFFFB4AB
        "orange" -> 0xFF9C4400 to 0xFFFFB77F
        "amber" -> 0xFF735C00 to 0xFFE9C349
        "green" -> 0xFF286B2A to 0xFF9ED695
        "teal" -> 0xFF006B60 to 0xFF80D5C5
        "blue" -> 0xFF185CA8 to 0xFFA5C8FF
        "violet" -> 0xFF7050A7 to 0xFFD7B9FF
        "pink" -> 0xFF9B3D75 to 0xFFFFADE0
        else -> return base
    }
    val primary = Color(if (dark) tones.second else tones.first)
    return base.copy(
        primary = primary,
        onPrimary = if (dark) Color(0xFF171717) else Color.White,
        primaryContainer = primary.copy(alpha = 1f).let {
            androidx.compose.ui.graphics.lerp(base.surface, it, if (dark) 0.25f else 0.12f)
        },
        onPrimaryContainer = base.onSurface,
        secondary = primary,
        onSecondary = if (dark) Color(0xFF171717) else Color.White,
        secondaryContainer = androidx.compose.ui.graphics.lerp(base.surface, primary, if (dark) 0.18f else 0.08f),
        onSecondaryContainer = base.onSurface,
    )
}
