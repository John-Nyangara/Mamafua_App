package com.mamafua.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PremiumTeal,
    onPrimary = Color.White,
    primaryContainer = PremiumTealDark,
    onPrimaryContainer = PremiumTealLight,
    secondary = PremiumOrange,
    onSecondary = Color.White,
    secondaryContainer = PremiumOrangeDark,
    onSecondaryContainer = PremiumOrangeLight,
    tertiary = PremiumBlueLight,
    onTertiary = Color.White,
    tertiaryContainer = PremiumBlueMid,
    onTertiaryContainer = PremiumTealSurface,
    background = PremiumBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = PremiumCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = PremiumSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedDark,
    onErrorContainer = Color.White,
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D)
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumTealDark,
    onPrimary = Color.White,
    primaryContainer = PremiumTealSurface,
    onPrimaryContainer = PremiumBlue,
    secondary = PremiumOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = PremiumOrangeSurface,
    onSecondaryContainer = PremiumOrangeDark,
    tertiary = PremiumBlueMid,
    onTertiary = Color.White,
    tertiaryContainer = PremiumTealLight,
    onTertiaryContainer = PremiumBlue,
    background = PremiumBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = PremiumCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE8EDF2),
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRedDark,
    onError = Color.White,
    errorContainer = ErrorRedSurface,
    onErrorContainer = ErrorRedDark,
    outline = Color(0xFFD0D7DE),
    outlineVariant = Color(0xFFE8EDF2)
)

@Composable
fun MamafuaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}