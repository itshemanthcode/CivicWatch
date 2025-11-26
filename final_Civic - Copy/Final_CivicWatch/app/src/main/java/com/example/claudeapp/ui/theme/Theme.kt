package com.example.claudeapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CivicBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = CivicBlueLight,

    secondary = CivicGreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = CivicGreenLight,

    tertiary = CivicOrangeLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = CivicOrangeLight,

    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFEF5350),

    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),

    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF3A3A3A)
)

private val LightColorScheme = lightColorScheme(
    primary = CivicBlue,
    onPrimary = Color.White,
    primaryContainer = CivicBlueLight,
    onPrimaryContainer = Color(0xFF0D47A1),

    secondary = CivicGreen,
    onSecondary = Color.White,
    secondaryContainer = CivicGreenLight,
    onSecondaryContainer = Color(0xFF1B5E20),

    tertiary = CivicOrange,
    onTertiary = Color.White,
    tertiaryContainer = CivicOrangeLight,
    onTertiaryContainer = Color(0xFFE65100),

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFEF5350),
    onErrorContainer = Color(0xFFD32F2F),

    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0)
)

@Composable
fun ClaudeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CivicTypography,
        content = content
    )
}
