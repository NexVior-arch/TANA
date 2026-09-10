package com.tana.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Understated Professional Palette (Muted Gold & Warm Charcoal)
// Design intent: neutral charcoal/graphite base (not black, not tinted purple/blue),
// with a single desaturated gold accent used sparingly - closer to how a real fintech
// app (Wise, Monzo, a private-bank app) uses "premium" color: mostly quiet neutrals,
// gold reserved for the one or two things that matter (progress, primary action).
val PureBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF161513)   // Warm near-black charcoal (a hint of brown, not blue/purple)
val DarkSurface = Color(0xFF201E1B)      // Elevated charcoal
val DarkSurfaceVariant = Color(0xFF2A2724)
val DarkCard = Color(0xFF221F1C)
val DarkBorder = Color(0xFF3A3631)       // Soft warm graphite edge
val DarkBorderSubtle = Color(0xFF2C2925)

// Translucent glass tokens (kept subtle - low alpha, neutral tint)
val DarkGlassBackground = Color(0xE8201E1B)
val DarkGlassBorder = Color(0x26C6A15B)
val DarkGlassHighlight = Color(0x14FFFFFF)

val PureWhite = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFAF8F5)  // Warm off-white (paper), not cold blue-white
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1EDE7)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE3DDD3)
val LightBorderSubtle = Color(0xFFEDE8E0)

val LightGlassBackground = Color(0xF2FFFFFF)
val LightGlassBorder = Color(0x1EAD8A4A)

val TextPrimaryDark = Color(0xFFEDE9E3)
val TextSecondaryDark = Color(0xFFA8A199)
val TextMutedDark = Color(0xFF716B62)

val TextPrimaryLight = Color(0xFF23201C)
val TextSecondaryLight = Color(0xFF5C564C)
val TextMutedLight = Color(0xFF938C80)

// Brand accent: a single muted, desaturated gold - not bright/neon yellow-gold.
// Used sparingly (primary actions, progress, small highlights), not as a
// background wash, so the app reads as calm/professional rather than "AI generated".
val BrandPrimary = Color(0xFF94722F)        // Muted Antique Gold (deepened for AA text contrast)
val BrandPrimaryBright = Color(0xFFC6A15B)  // Soft Champagne Gold (slightly lighter, for dark bg)
val BrandPrimaryDark = Color(0xFF8A6C38)    // Deep Bronze
val AccentExpense = Color(0xFFB05C4F)       // Muted Terracotta (Expense / Deficit) - desaturated, not neon red
val AccentIncome = Color(0xFF6B8F71)        // Muted Sage Green (Income-positive signal)
val AccentSavings = Color(0xFF94722F)       // Same muted gold as brand - savings IS the brand accent
val AccentMonochrome = Color(0xFF2A2724)
val AccentPurple = Color(0xFF8A7B99)        // Muted heather (kept only for rare legacy references)
val AccentAmber = Color(0xFFC6A15B)         // Alias of champagne gold, kept for compatibility
val AccentCyan = Color(0xFF6E8B94)          // Muted slate-teal for withdraw/neutral actions

// Ambient gradients - all restrained to near-flat neutrals with only a faint
// gold whisper, instead of the previous saturated indigo/violet glow.
val GlassGradientBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x40C6A15B),
        Color(0x18FFFFFF),
        Color(0x08B08D4E)
    )
)

val HeroGradientDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2A2622),
        Color(0xFF1E1B18),
        Color(0xFF141210)
    )
)

val HeroGradientLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2A2622),
        Color(0xFF211E1A),
        Color(0xFF171512)
    )
)

val PlatinumCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2E2A25),
        Color(0xFF221F1B),
        Color(0xFF17140F)
    )
)

val AmbientAppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF211E1A), // Warm charcoal top - just barely lighter than base
        Color(0xFF1A1815), // Warm graphite mid-tone
        Color(0xFF120F0C), // Near-black warm base
        Color(0xFF16130F)  // Soft bottom
    )
)

val AmbientBackgroundLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF3EEE6), // Soft warm ivory top
        Color(0xFFFAF8F5), // Paper tone
        Color(0xFFFFFFFF)  // Clean canvas bottom
    )
)

val AmbientRadialAura = Brush.radialGradient(
    colors = listOf(
        Color(0x22C6A15B), // Faint gold glow - much lower intensity than before
        Color(0x10B08D4E),
        Color.Transparent
    )
)

val AiGlowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFB08D4E),
        Color(0xFFC6A15B),
        Color(0xFFD4B876)
    )
)

// Button gradients - flattened to near-solid tones (very tight color range)
// instead of high-contrast multi-hue gradients, for a calmer/more professional feel.
val PrimaryButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFC6A15B), // Champagne gold
        Color(0xFF94722F), // Muted gold
        Color(0xFF8A6C38)  // Deep bronze
    )
)

val CyanButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF8AA3AB), // Soft slate-teal
        Color(0xFF6E8B94), // Muted teal
        Color(0xFF52717A)  // Deep slate-teal
    )
)

val RoseButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFC47A6D), // Soft terracotta
        Color(0xFFB05C4F), // Muted terracotta
        Color(0xFF8C4438)  // Deep clay
    )
)

val PurpleButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFA396AD), // Soft heather
        Color(0xFF8A7B99), // Muted heather
        Color(0xFF6B5E7A)  // Deep heather
    )
)

val SoftBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFE3D6B8), // Soft pale gold
        Color(0xFFD4B876), // Light gold
        Color(0xFFC6A15B)  // Champagne gold
    )
)

val DarkButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF33302B), // Warm graphite
        Color(0xFF262320), // Dark graphite
        Color(0xFF19160F)  // Near-black warm base
    )
)

val DisabledButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF302D28),
        Color(0xFF242220)
    )
)
