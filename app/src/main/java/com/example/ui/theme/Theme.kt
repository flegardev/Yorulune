package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OrbitTheme(
    themeMode: String = "paper", // paper, light, dark, oled, system, dynamic
    accentPreset: String = "terracotta",
    cornerRadiusDp: Int = 22,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = getAccentPalette(accentPreset)
    val customAccent = parseKissatenAccent(accentPreset)
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light", "paper" -> false
        "dark", "oled" -> true
        "dynamic", "system" -> systemDark
        else -> systemDark
    }
    val isOled = themeMode == "oled"
    val isPaper = themeMode == "paper"

    val colorScheme = if (themeMode == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (isDark) {
        // Rainy-Night Lofi Dark Mode & True OLED Black Mode
        // Deep indigo & charcoal blue-gray surfaces with warm window-light ink & soft neon/lantern glow accents
        darkColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.primary.copy(alpha = 0.26f),
            onPrimaryContainer = OrbitWindowWarmLight,
            secondary = accent.secondary,
            onSecondary = Color(0xFF0A0E17),
            secondaryContainer = accent.secondary.copy(alpha = 0.22f),
            onSecondaryContainer = OrbitWindowWarmLight,
            tertiary = accent.tertiary,
            background = if (isOled) OrbitOledBlack else OrbitMidnight,
            onBackground = OrbitWindowWarmLight,
            surface = if (isOled) OrbitOledBlack else OrbitSurfaceDark,
            onSurface = OrbitWindowWarmLight,
            surfaceVariant = if (isOled) OrbitOledCard else OrbitCardDark,
            onSurfaceVariant = OrbitWindowMutedLight,
            outline = if (isOled) Color(0xFF252B3B) else Color(0xFF2E3852)
        )
    } else if (isPaper) {
        // Washi Paper & Japanese Stationery Mode
        lightColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.primary.copy(alpha = 0.16f),
            onPrimaryContainer = OrbitPaperInk,
            secondary = accent.secondary,
            onSecondary = Color.White,
            secondaryContainer = accent.secondary.copy(alpha = 0.16f),
            onSecondaryContainer = OrbitPaperInk,
            tertiary = accent.tertiary,
            background = OrbitPaperBg,
            onBackground = OrbitPaperInk,
            surface = OrbitPaperSurface,
            onSurface = OrbitPaperInk,
            surfaceVariant = OrbitPaperCard,
            onSurfaceVariant = OrbitPaperMuted,
            outline = OrbitPaperBorder
        )
    } else {
        // Soft Sunlight & Pastel Washi Light Mode tailored to each palette
        lightColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.primary.copy(alpha = 0.16f),
            onPrimaryContainer = OrbitPaperInk,
            secondary = accent.secondary,
            onSecondary = Color.White,
            secondaryContainer = accent.secondary.copy(alpha = 0.16f),
            onSecondaryContainer = OrbitPaperInk,
            tertiary = accent.tertiary,
            background = accent.softBgLight,
            onBackground = OrbitPaperInk,
            surface = OrbitPaperSurface,
            onSurface = OrbitPaperInk,
            surfaceVariant = accent.softCardLight,
            onSurfaceVariant = OrbitPaperMuted,
            outline = accent.primary.copy(alpha = 0.22f)
        )
    }

    val radius = cornerRadiusDp.coerceIn(8, 32).dp
    val shapes = Shapes(
        small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(8).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape((cornerRadiusDp + 6).coerceAtMost(36).dp)
    )

    androidx.compose.runtime.SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    MaterialTheme(
        colorScheme = kissatenColors(if (customAccent != null) colorScheme.copy(primary = customAccent) else colorScheme, isOled),
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
