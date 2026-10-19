package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DokanGreen,
    onPrimary = Color.White,
    primaryContainer = DokanGreenContainer,
    onPrimaryContainer = DokanGreenDark,
    secondary = BkashPink,
    onSecondary = Color.White,
    secondaryContainer = BkashPinkContainer,
    onSecondaryContainer = Color(0xFF83003D),
    tertiary = DueAmber,
    onTertiary = Color.White,
    tertiaryContainer = DueAmberContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = LightBg,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LightBorder,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Enforce light theme as requested by user
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
