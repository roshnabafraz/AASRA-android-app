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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LuminousGreen,
    onPrimary = DeepCharcoal,
    secondary = WarningAmberDark,
    onSecondary = DeepCharcoal,
    tertiary = SoftRed,
    background = DeepCharcoal,
    surface = SurfaceGrey,
    error = SoftRed,
    onError = DeepCharcoal,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1)
)

private val LightColorScheme = lightColorScheme(
    primary = PakistanGreen,
    onPrimary = White,
    secondary = WarningAmber,
    onSecondary = White,
    tertiary = UrgentRed,
    background = White,
    surface = White,
    error = UrgentRed,
    onError = White,
    onBackground = CharcoalText,
    onSurface = CharcoalText
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
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}