package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Rainy-Night Lofi Dark & True OLED Palettes (Deep Indigo, Charcoal Blue-Gray & Warm Window Light)
val OrbitMidnight = Color(0xFF0F1422)
val OrbitSurfaceDark = Color(0xFF161D30)
val OrbitCardDark = Color(0xFF1E2740)
val OrbitWindowWarmLight = Color(0xFFF6F1E9)
val OrbitWindowMutedLight = Color(0xFFCBD4E4)
val OrbitOledBlack = Color(0xFF000000)
val OrbitOledCard = Color(0xFF0C101A)

// Washi Paper & Warm Sunlight Palettes (Japanese Stationery & Minimalist Café Vibe)
val OrbitPaperBg = Color(0xFFF7F3EB)
val OrbitPaperSurface = Color(0xFFFCFBF8)
val OrbitPaperCard = Color(0xFFEFEAE0)
val OrbitPaperBorder = Color(0xFFE4DDD0)
val OrbitPaperInk = Color(0xFF221F1B)
val OrbitPaperMuted = Color(0xFF756E64)

val OrbitDaylightBg = Color(0xFFF7F8FC)
val OrbitSurfaceLight = Color(0xFFFFFFFF)
val OrbitCardLight = Color(0xFFEEF2F8)

// Core Semantic & Lofi Accent Tokens
val OrbitIndigo = Color(0xFF5C6BC0)
val OrbitIndigoSecondary = Color(0xFF7986CB)
val OrbitCyan = Color(0xFF26A69A)
val OrbitEmerald = Color(0xFF4CAF7D)
val OrbitAmber = Color(0xFFE59A44)
val OrbitRose = Color(0xFFE06377)
val OrbitViolet = Color(0xFF8E74C8)

// Japanese Lofi & Pastel Signature Colors
val LofiSakuraBlush = Color(0xFFD97B98)
val LofiMatchaMist = Color(0xFF639A7A)
val LofiKyotoSky = Color(0xFF5893C0)
val LofiLanternAmber = Color(0xFFE08A44)
val LofiIndigoNight = Color(0xFF5666C4)
val LofiRainyTokyo = Color(0xFF4C7FA8)
val LofiPlumInk = Color(0xFF8B6299)
val LofiBambooGreen = Color(0xFF4B9676)
val LofiSoftCream = Color(0xFFB68058)
val LofiMoonlightBlue = Color(0xFF5A84C2)

// Editorial & Pastel Palette Tokens (Preserved for 100% Backward Compatibility)
val OrbitTerracotta = Color(0xFFC86D51)
val OrbitSage = Color(0xFF5F8D75)
val OrbitSky = Color(0xFF4A9EE0)
val OrbitLavender = Color(0xFF9378D8)
val OrbitClay = Color(0xFFB07D62)
val OrbitMint = Color(0xFF3CC29A)
val OrbitPeach = Color(0xFFF28B66)
val OrbitSoftRose = Color(0xFFE86A92)
val OrbitOcean = Color(0xFF2884B8)
val OrbitMidnightAccent = Color(0xFF5B6EF5)

// Priority Colors (Restrained & Semantic)
val PriorityP1Red = Color(0xFFD94862)
val PriorityP2Amber = Color(0xFFD9822B)
val PriorityP3Blue = Color(0xFF427AA8)
val PriorityP4Slate = Color(0xFF68768A)

data class AccentPalette(
    val key: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val softBgLight: Color = OrbitPaperBg,
    val softCardLight: Color = OrbitPaperCard,
    val gradientStart: Color = primary,
    val gradientEnd: Color = secondary,
    val isJapaneseLofi: Boolean = false
)

/**
 * 100% Free Theme Studio Palettes:
 * Includes all 10 Japanese Lofi Core Palettes (Sakura Blush, Matcha Mist, Kyoto Sky,
 * Lantern Amber, Indigo Night, Rainy Tokyo, Plum Ink, Bamboo Green, Soft Cream, Moonlight Blue)
 * AND all Pastel & Editorial Palettes (Terracotta, Lavender, Sky Blue, Mint, Peach, Rose, Ocean, Midnight, Sage, Clay, Orbit Indigo).
 */
val OrbitAccentPalettes = listOf(
    // --- Japanese Lofi Core Collection ---
    AccentPalette(
        key = "sakura_blush",
        label = "Sakura Blush",
        primary = LofiSakuraBlush,
        secondary = OrbitLavender,
        tertiary = LofiLanternAmber,
        softBgLight = Color(0xFFFCF5F7),
        softCardLight = Color(0xFFF6E8ED),
        gradientStart = LofiSakuraBlush,
        gradientEnd = OrbitLavender,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "matcha_mist",
        label = "Matcha Mist",
        primary = LofiMatchaMist,
        secondary = LofiBambooGreen,
        tertiary = LofiSoftCream,
        softBgLight = Color(0xFFF4F8F4),
        softCardLight = Color(0xFFE5EFE7),
        gradientStart = LofiMatchaMist,
        gradientEnd = LofiKyotoSky,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "kyoto_sky",
        label = "Kyoto Sky",
        primary = LofiKyotoSky,
        secondary = LofiMoonlightBlue,
        tertiary = LofiLanternAmber,
        softBgLight = Color(0xFFF3F7FB),
        softCardLight = Color(0xFFE4EEF7),
        gradientStart = LofiKyotoSky,
        gradientEnd = LofiSakuraBlush,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "lantern_amber",
        label = "Lantern Amber",
        primary = LofiLanternAmber,
        secondary = OrbitTerracotta,
        tertiary = LofiIndigoNight,
        softBgLight = Color(0xFFFBF6EE),
        softCardLight = Color(0xFFF4EAD9),
        gradientStart = LofiLanternAmber,
        gradientEnd = LofiSakuraBlush,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "indigo_night",
        label = "Indigo Night",
        primary = LofiIndigoNight,
        secondary = LofiMoonlightBlue,
        tertiary = LofiLanternAmber,
        softBgLight = Color(0xFFF3F4FA),
        softCardLight = Color(0xFFE5E8F5),
        gradientStart = LofiIndigoNight,
        gradientEnd = LofiLanternAmber,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "rainy_tokyo",
        label = "Rainy Tokyo",
        primary = LofiRainyTokyo,
        secondary = LofiSakuraBlush,
        tertiary = LofiIndigoNight,
        softBgLight = Color(0xFFF2F6F9),
        softCardLight = Color(0xFFE3ECF3),
        gradientStart = LofiRainyTokyo,
        gradientEnd = LofiSakuraBlush,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "plum_ink",
        label = "Plum Ink",
        primary = LofiPlumInk,
        secondary = LofiSakuraBlush,
        tertiary = LofiSoftCream,
        softBgLight = Color(0xFFF8F4FA),
        softCardLight = Color(0xFFEEE6F3),
        gradientStart = LofiPlumInk,
        gradientEnd = LofiSakuraBlush,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "bamboo_green",
        label = "Bamboo Green",
        primary = LofiBambooGreen,
        secondary = LofiMatchaMist,
        tertiary = LofiKyotoSky,
        softBgLight = Color(0xFFF2FAF5),
        softCardLight = Color(0xFFE2F2E9),
        gradientStart = LofiBambooGreen,
        gradientEnd = LofiKyotoSky,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "soft_cream",
        label = "Soft Cream",
        primary = LofiSoftCream,
        secondary = OrbitTerracotta,
        tertiary = LofiMatchaMist,
        softBgLight = Color(0xFFF9F5ED),
        softCardLight = Color(0xFFF1E9DA),
        gradientStart = LofiSoftCream,
        gradientEnd = LofiLanternAmber,
        isJapaneseLofi = true
    ),
    AccentPalette(
        key = "moonlight_blue",
        label = "Moonlight Blue",
        primary = LofiMoonlightBlue,
        secondary = LofiIndigoNight,
        tertiary = OrbitLavender,
        softBgLight = Color(0xFFF2F6FC),
        softCardLight = Color(0xFFE3ECF9),
        gradientStart = LofiMoonlightBlue,
        gradientEnd = OrbitLavender,
        isJapaneseLofi = true
    ),

    // --- Editorial & Soft Pastel Collection ---
    AccentPalette(
        key = "terracotta",
        label = "Terracotta",
        primary = OrbitTerracotta,
        secondary = OrbitClay,
        tertiary = OrbitSage,
        softBgLight = Color(0xFFF6F2EA),
        softCardLight = Color(0xFFF0EBE1),
        gradientStart = OrbitTerracotta,
        gradientEnd = OrbitAmber
    ),
    AccentPalette(
        key = "lavender",
        label = "Lavender",
        primary = OrbitLavender,
        secondary = OrbitSky,
        tertiary = OrbitSoftRose,
        softBgLight = Color(0xFFF7F4FD),
        softCardLight = Color(0xFFEDE6FA),
        gradientStart = OrbitLavender,
        gradientEnd = OrbitSoftRose
    ),
    AccentPalette(
        key = "sky",
        label = "Sky Blue",
        primary = OrbitSky,
        secondary = OrbitMint,
        tertiary = OrbitLavender,
        softBgLight = Color(0xFFF2F8FE),
        softCardLight = Color(0xFFE3F0FC),
        gradientStart = OrbitSky,
        gradientEnd = OrbitCyan
    ),
    AccentPalette(
        key = "mint",
        label = "Mint",
        primary = OrbitMint,
        secondary = OrbitSky,
        tertiary = OrbitSage,
        softBgLight = Color(0xFFF1FBF7),
        softCardLight = Color(0xFFE0F5EC),
        gradientStart = OrbitMint,
        gradientEnd = OrbitSky
    ),
    AccentPalette(
        key = "peach",
        label = "Peach",
        primary = OrbitPeach,
        secondary = OrbitSoftRose,
        tertiary = OrbitAmber,
        softBgLight = Color(0xFFFFF6F2),
        softCardLight = Color(0xFFFCEAE2),
        gradientStart = OrbitPeach,
        gradientEnd = OrbitAmber
    ),
    AccentPalette(
        key = "rose",
        label = "Rose",
        primary = OrbitSoftRose,
        secondary = OrbitLavender,
        tertiary = OrbitPeach,
        softBgLight = Color(0xFFFFF4F7),
        softCardLight = Color(0xFFFCE4EC),
        gradientStart = OrbitSoftRose,
        gradientEnd = OrbitLavender
    ),
    AccentPalette(
        key = "ocean",
        label = "Ocean",
        primary = OrbitOcean,
        secondary = OrbitCyan,
        tertiary = OrbitMint,
        softBgLight = Color(0xFFF1F8FC),
        softCardLight = Color(0xFFDFF0F8),
        gradientStart = OrbitOcean,
        gradientEnd = OrbitMint
    ),
    AccentPalette(
        key = "midnight",
        label = "Midnight",
        primary = OrbitMidnightAccent,
        secondary = OrbitViolet,
        tertiary = OrbitCyan,
        softBgLight = Color(0xFFF3F5FC),
        softCardLight = Color(0xFFE6EAF8),
        gradientStart = OrbitMidnightAccent,
        gradientEnd = OrbitViolet
    ),
    AccentPalette(
        key = "sage",
        label = "Sage",
        primary = OrbitSage,
        secondary = OrbitMint,
        tertiary = OrbitTerracotta,
        softBgLight = Color(0xFFF3F7F4),
        softCardLight = Color(0xFFE5EFE8),
        gradientStart = OrbitSage,
        gradientEnd = OrbitMint
    ),
    AccentPalette(
        key = "clay",
        label = "Clay",
        primary = OrbitClay,
        secondary = OrbitTerracotta,
        tertiary = OrbitSage,
        softBgLight = Color(0xFFF7F3EE),
        softCardLight = Color(0xFFEDE5DC),
        gradientStart = OrbitClay,
        gradientEnd = OrbitTerracotta
    ),
    AccentPalette(
        key = "orbit_indigo",
        label = "Yorulune Indigo",
        primary = OrbitIndigo,
        secondary = OrbitCyan,
        tertiary = OrbitViolet,
        softBgLight = OrbitDaylightBg,
        softCardLight = OrbitCardLight,
        gradientStart = OrbitIndigo,
        gradientEnd = OrbitCyan
    ),
    AccentPalette(
        key = "cyber_cyan",
        label = "Nebula Cyan",
        primary = OrbitCyan,
        secondary = OrbitIndigo,
        tertiary = OrbitEmerald,
        softBgLight = Color(0xFFF1FAFC),
        softCardLight = Color(0xFFE0F4F8),
        gradientStart = OrbitCyan,
        gradientEnd = OrbitEmerald
    ),
    AccentPalette(
        key = "emerald_zen",
        label = "Emerald Zen",
        primary = OrbitEmerald,
        secondary = OrbitCyan,
        tertiary = OrbitIndigo,
        softBgLight = Color(0xFFF1FBF6),
        softCardLight = Color(0xFFE1F6EC),
        gradientStart = OrbitEmerald,
        gradientEnd = OrbitCyan
    ),
    AccentPalette(
        key = "sunset_amber",
        label = "Solar Flare",
        primary = OrbitAmber,
        secondary = OrbitRose,
        tertiary = OrbitViolet,
        softBgLight = Color(0xFFFFF9F0),
        softCardLight = Color(0xFFFEF0DB),
        gradientStart = OrbitAmber,
        gradientEnd = OrbitRose
    ),
    AccentPalette(
        key = "royal_violet",
        label = "Cosmic Violet",
        primary = OrbitViolet,
        secondary = OrbitIndigo,
        tertiary = OrbitCyan,
        softBgLight = Color(0xFFF6F3FE),
        softCardLight = Color(0xFFEBE4FC),
        gradientStart = OrbitViolet,
        gradientEnd = OrbitIndigo
    )
)

fun getAccentPalette(key: String): AccentPalette {
    parseKissatenAccent(key)?.let { color ->
        return OrbitAccentPalettes.first { it.key == "indigo_night" }.copy(key = key, label = "Custom accent", primary = color, gradientStart = color)
    }
    val normalized = when (key.lowercase()) {
        "sky_blue" -> "sky"
        "rose_quartz" -> "rose"
        else -> key.lowercase()
    }
    return OrbitAccentPalettes.firstOrNull { it.key == normalized }
        ?: OrbitAccentPalettes.first { it.key == "terracotta" }
}
