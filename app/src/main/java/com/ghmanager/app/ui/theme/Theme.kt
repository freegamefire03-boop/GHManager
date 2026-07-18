package com.ghmanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme mode preference:
 *  - SYSTEM: follow the phone's default dark/light setting (default)
 *  - DARK:   force dark
 *  - LIGHT:  force light
 */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

// GitHub-inspired professional dark palette.
private val DarkBackground = Color(0xFF0D1117)
private val DarkSurface = Color(0xFF161B22)
private val DarkSurfaceVariant = Color(0xFF21262D)
private val DarkPrimary = Color(0xFF2F81F7)
private val DarkOnPrimary = Color(0xFF0D1117)
private val DarkOnBackground = Color(0xFFE6EDF3)
private val DarkOnSurface = Color(0xFFC9D1D9)
private val DarkOutline = Color(0xFF30363D)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurface,
    outline = DarkOutline,
    error = Color(0xFFF85149),
    onError = Color(0xFF0D1117),
    errorContainer = Color(0xFF3D1518),
    onErrorContainer = Color(0xFFF0A0A4),
    primaryContainer = Color(0xFF1F6FEB),
    onPrimaryContainer = Color(0xFFE6EDF3)
)

// Professional light palette tuned to match.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEAEFF2),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
    error = Color(0xFFCF222E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE9EB),
    onErrorContainer = Color(0xFF82071A),
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0969DA)
)

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
