package com.roshnab.aasra.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary            = MintAccent,
    onPrimary          = DarkBackground,
    primaryContainer   = Color(0xFF1A4A2E),
    onPrimaryContainer = SageGreen,
    secondary          = WarningAmberDark,
    onSecondary        = DarkBackground,
    tertiary           = ErrorRedDark,
    background         = DarkBackground,
    surface            = DarkSurface,
    surfaceVariant     = DarkSurfaceVar,
    error              = ErrorRedDark,
    onError            = DarkBackground,
    onBackground       = DarkText,
    onSurface          = DarkText,
    onSurfaceVariant   = DarkTextMuted,
    outline            = DarkOutline,
    outlineVariant     = Color(0xFF2D2D2D)
)

private val LightColorScheme = lightColorScheme(
    primary            = ForestGreen,
    onPrimary          = White,
    primaryContainer   = SageGreen,
    onPrimaryContainer = ForestGreenDark,
    secondary          = WarningAmber,
    onSecondary        = White,
    tertiary           = ErrorRed,
    background         = LightBackground,
    surface            = LightSurface,
    surfaceVariant     = LightSurfaceVar,
    error              = ErrorRed,
    onError            = White,
    onBackground       = LightText,
    onSurface          = LightText,
    onSurfaceVariant   = LightTextMuted,
    outline            = LightOutline,
    outlineVariant     = Color(0xFFDDE5DF)
)

@Composable
fun AASRATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}