package com.example.petpals.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

// --- פלטת בז'-חום (Light) ---
private val LightColors = lightColorScheme(
    primary = Color(0xFF8B5E34),            // חום
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9C3A3),   // בז' רך
    onPrimaryContainer = Color(0xFF3E2A17),

    secondary = Color(0xFFB08968),          // טאוף/חום בהיר
    onSecondary = Color(0xFF201910),
    secondaryContainer = Color(0xFFE6D3C1),
    onSecondaryContainer = Color(0xFF3A2D22),

    tertiary = Color(0xFFE0A96D),           // כתמתם עדין
    onTertiary = Color(0xFF27190B),

    background = Color(0xFFF7F2EB),         // רקע כללי (בז’ בהיר מאוד)
    onBackground = Color(0xFF2E2620),

    surface = Color(0xFFFFFBF5),            // משטחי כרטיסים/סרגלים
    onSurface = Color(0xFF2E2620),

    surfaceVariant = Color(0xFFFAF5EF),     // וריאנט למשטחים משניים
    onSurfaceVariant = Color(0xFF6D5E54),

    outline = Color(0xFFB8A395),
    outlineVariant = Color(0xFFD8C9BD),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

// --- מצב כהה חם תואם ---
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD2B48C),            // טאן
    onPrimary = Color(0xFF3A2B1C),
    primaryContainer = Color(0xFF6A4E34),
    onPrimaryContainer = Color(0xFFFFEBD2),

    secondary = Color(0xFFD2B48C),
    onSecondary = Color(0xFF3A2B1C),
    secondaryContainer = Color(0xFF5B4633),
    onSecondaryContainer = Color(0xFFFFEBD2),

    tertiary = Color(0xFFE1B07E),
    onTertiary = Color(0xFF3A2B1C),

    background = Color(0xFF1C1917),
    onBackground = Color(0xFFECE0D5),

    surface = Color(0xFF211E1B),
    onSurface = Color(0xFFECE0D5),

    surfaceVariant = Color(0xFF2B2622),
    onSurfaceVariant = Color(0xFFCABBAE),

    outline = Color(0xFF8F7E72),
    outlineVariant = Color(0xFF3F3732),

    error = Color(0xFFFFB4A9),
    onError = Color(0xFF680003),
)

@Composable
fun PetPalsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // סטטוס/ניווט בר בגווני הרקע
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10),
            small = RoundedCornerShape(14),
            medium = RoundedCornerShape(18),
            large = RoundedCornerShape(24),
            extraLarge = RoundedCornerShape(28)
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}
