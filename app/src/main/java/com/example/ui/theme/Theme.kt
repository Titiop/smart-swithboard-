package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

@Immutable
data class SwitchboardColors(
    val brandAccent: Color = BrandGreen,
    val offCard: Color = DarkOffCard,
    val cardBorder: Color = DarkBorder,
    val textPrimary: Color = DarkTextPrimary,
    val textSecondary: Color = DarkTextSecondary,
    val isDark: Boolean = true
)

val LocalSwitchboardColors = staticCompositionLocalOf { SwitchboardColors() }

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color(0xFF0E1013),
    primaryContainer = Color(0xFF144D2D),
    onPrimaryContainer = Color(0xFF98F9BE),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkOffCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = Color(0xFF0E1013)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B8A4C), // High-contrast green for light mode accents
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5D0),
    onPrimaryContainer = Color(0xFF063B1C),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightOffCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ErrorRed,
    onError = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF104426),
    onPrimaryContainer = Color(0xFF98F9BE),
    background = AmoledBackground,
    onBackground = DarkTextPrimary,
    surface = AmoledSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = AmoledOffCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = AmoledBorder,
    error = ErrorRed,
    onError = Color.Black
)

@Composable
fun SmartSwitchboardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        themeMode == ThemeMode.LIGHT -> LightColorScheme
        themeMode == ThemeMode.DARK -> DarkColorScheme
        systemInDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val extraColors = when {
        themeMode == ThemeMode.AMOLED -> SwitchboardColors(
            brandAccent = BrandGreen,
            offCard = AmoledOffCard,
            cardBorder = AmoledBorder,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            isDark = true
        )
        !isDark -> SwitchboardColors(
            brandAccent = BrandGreen,
            offCard = LightOffCard,
            cardBorder = LightBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            isDark = false
        )
        else -> SwitchboardColors(
            brandAccent = BrandGreen,
            offCard = DarkOffCard,
            cardBorder = DarkBorder,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            isDark = true
        )
    }

    CompositionLocalProvider(LocalSwitchboardColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
