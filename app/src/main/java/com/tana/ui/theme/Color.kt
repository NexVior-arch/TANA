package com.tana.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Bespoke Human-Crafted Financial Palette (Midnight Wealth — Indigo & Violet)
val PureBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF0A0B14) // Deep Ambient Midnight Ink
val DarkSurface = Color(0xFF141628)    // Elevated Indigo Slate
val DarkSurfaceVariant = Color(0xFF1C1E30)
val DarkCard = Color(0xFF15172A)
val DarkBorder = Color(0xFF2C2F4A)     // Crisp Indigo Edge
val DarkBorderSubtle = Color(0xFF1E2036)

// Translucent Frosted Glass Tokens
val DarkGlassBackground = Color(0xE812132A)
val DarkGlassBorder = Color(0x336366F1)
val DarkGlassHighlight = Color(0x18FFFFFF)

val PureWhite = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF6F6FC) // Warm Organic Periwinkle White
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEDF9)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFDCDCF0)
val LightBorderSubtle = Color(0xFFE9E9F7)

val LightGlassBackground = Color(0xF2FFFFFF)
val LightGlassBorder = Color(0x1E4F46E5)

val TextPrimaryDark = Color(0xFFF3F2FD)
val TextSecondaryDark = Color(0xFFA8A6D6)
val TextMutedDark = Color(0xFF6E6C99)

val TextPrimaryLight = Color(0xFF15142B)
val TextSecondaryLight = Color(0xFF48466E)
val TextMutedLight = Color(0xFF8A88AD)

// Bespoke Brand & Financial Micro-Accents
val BrandPrimary = Color(0xFF6366F1)       // Lush Indigo
val BrandPrimaryBright = Color(0xFF818CF8) // Periwinkle Glow
val BrandPrimaryDark = Color(0xFF4338CA)
val AccentExpense = Color(0xFFF43F5E)       // Crisp Rose Coral (Expense / Deficit)
val AccentIncome = Color(0xFF22C55E)        // Green kept only as income-positive signal
val AccentSavings = Color(0xFF8B5CF6)       // Vibrant Violet (Savings)
val AccentMonochrome = Color(0xFF1C1E30)
val AccentPurple = Color(0xFF8B5CF6)        // Radiant Violet
val AccentAmber = Color(0xFFF59E0B)         // Warm Solar Gold
val AccentCyan = Color(0xFF06B6D4)          // Retained cyan for withdraw/neutral actions

// Atmospheric Ambient Gradient Brushes
val GlassGradientBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x55818CF8),
        Color(0x20FFFFFF),
        Color(0x086366F1)
    )
)

val HeroGradientDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF221D45),
        Color(0xFF161331),
        Color(0xFF0B0A1C)
    )
)

val HeroGradientLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF221D45),
        Color(0xFF191636),
        Color(0xFF0F0D24)
    )
)

val PlatinumCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF27255A),
        Color(0xFF1A1840),
        Color(0xFF100E28)
    )
)

val AmbientAppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1C1740), // Rich Ambient Indigo Tint at top
        Color(0xFF120F28), // Deep Violet Obsidian
        Color(0xFF07060F), // Pure Midnight Base
        Color(0xFF0B0920)  // Soft bottom glow
    )
)

val AmbientBackgroundLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFEAEAFB), // Soft Fresh Periwinkle Top
        Color(0xFFF4F4FC), // Airy Mineral Tone
        Color(0xFFFFFFFF)  // Clean Canvas Bottom
    )
)

val AmbientRadialAura = Brush.radialGradient(
    colors = listOf(
        Color(0x356366F1), // Ambient Indigo Glow
        Color(0x154F46E5),
        Color.Transparent
    )
)

val AiGlowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )
)

// Bespoke Button Gradient Brushes
val PrimaryButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF818CF8), // Periwinkle glow
        Color(0xFF6366F1), // Indigo
        Color(0xFF4338CA)  // Deep indigo
    )
)

val CyanButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF22D3EE), // Bright cyan
        Color(0xFF06B6D4), // Cyan aqua
        Color(0xFF0284C7)  // Deep sky
    )
)

val RoseButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFB7185), // Coral rose
        Color(0xFFF43F5E), // Vivid rose
        Color(0xFFBE123C)  // Crimson
    )
)

val PurpleButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFA78BFA), // Lavender
        Color(0xFF8B5CF6), // Violet
        Color(0xFF6D28D9)  // Deep indigo
    )
)

val SoftBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFC7D2FE), // Soft periwinkle
        Color(0xFFA5B4FC), // Accent blue
        Color(0xFF818CF8)  // Indigo
    )
)

val DarkButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF2D3748), // Slate titanium
        Color(0xFF1F2937), // Dark slate
        Color(0xFF111827)  // Obsidian
    )
)

val DisabledButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF263038),
        Color(0xFF1C242B)
    )
)
