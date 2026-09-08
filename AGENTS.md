# Taste-Skill: Anti-Slop Frontend Framework for AI Agents (Jetpack Compose Edition)
*Adopted from Leonxlnx/taste-skill*

## Core Anti-Slop Directives

### 1. Zero AI Slop & Generic Aesthetics
- **Banned**: Generic rounded pastel cards with zero purpose, neon rainbow gradients without lighting logic, repetitive flat cards with no depth, boring templated layouts, and unstyled cookie-cutter components.
- **Enforced**: Intentional typography, calibrated tactile palette, high-contrast dark/light obsidian and titanium surfaces, specular glass reflection borders (`GlassGradientBorder`), micro-interactions, and hardware-accelerated fluid transitions.

### 2. Typographic Discipline & Visual Scale
- **Display & Monospace**: Strict hierarchy. Large numbers and balances must use bold, heavy, or tabular fonts with animated easing (`AnimatedAmountText`).
- **Hierarchy & Tracking**: Small labels must have uppercase tracking (`letterSpacing = 1.6.sp` to `2.0.sp`) and bold/black weight.
- **Readability**: High contrast against surfaces (`TextPrimaryDark: #F8FAFC`, `TextSecondaryDark: #94A3B8`). Never use uncalibrated gray text on dark surfaces.

### 3. Materiality, Borders & Tactile Surfaces
- **Borders over Shadows**: Elevate elements through crisp 0.5dp–1.5dp specular borders and subtle alpha highlights rather than muddy, oversized drop shadows.
- **Glass & Depth**: Use translucent frosted surfaces (`copy(alpha = 0.85f–0.92f)`) layered over deep backgrounds.
- **Avatar & Icon Customization**: All list items must have dedicated avatar pills/shapes with custom semantic icons and background tints—never naked text or generic bullet points.

### 4. Component Structure & Touch Targets
- **Precision Touch Targets**: Minimum 48dp touch areas with smooth ripple feedback.
- **Floating Controls**: Floating navigation bars and FABs must have floating pill geometry, translucent blur-ready backing, and specular rim lighting.
- **Action Grouping**: Combine related actions into segmented pills or tactile quick-action rows.
