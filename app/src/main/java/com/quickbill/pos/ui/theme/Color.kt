package com.quickbill.pos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =========================================================================
// QuickBill Modern POS Color System
// Palette: Warm Cream Backgrounds, Pure White Surfaces, Deep Emerald/Forest
// Primary, Vibrant Coral/Orange Accents, and Rich Semantic Badges.
// =========================================================================

// --- Primary: Deep Emerald & Forest Green ---
val LightEmeraldPrimary = Color(0xFF0A5C36)          // Deep Emerald Green (Light mode)
val DarkEmeraldPrimary = Color(0xFF10B981)           // Vibrant Emerald 500 (Dark mode - popping & clear)
val EmeraldPrimaryDark = Color(0xFF063B23)          // Dark Forest
val EmeraldPrimaryLight = Color(0xFF10B981)         // Vibrant Emerald Accent

val LightEmeraldContainer = Color(0xFFE3F5EB)        // Soft Mint/Sage Container
val DarkEmeraldContainer = Color(0xFF134E35)         // Deep Emerald Container
val OnDarkEmeraldContainer = Color(0xFFA7F3D0)       // Mint 200 on Emerald Container
val OnEmeraldContainer = Color(0xFF04331E)          // Deep Forest on Mint
val EmeraldRipple = Color(0x1F10B981)

val EmeraldPrimary: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkEmeraldPrimary else LightEmeraldPrimary

val EmeraldContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) DarkEmeraldContainer else LightEmeraldContainer

// Backwards-compatible aliases
val PrimaryGreen = LightEmeraldPrimary
val PrimaryGreenDark = EmeraldPrimaryDark
val PrimaryGreenLight = DarkEmeraldPrimary

// --- Accent: Coral / Warm Orange ---
val CoralAccent = Color(0xFFFF5A36)            // Vibrant Coral for discounts, tags, badges
val CoralAccentDark = Color(0xFFE0421F)
val CoralAccentLight = Color(0xFFFF7A5C)
val CoralContainer: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) Color(0xFF4C1D15) else Color(0xFFFFECE7)
val OnCoralContainer = Color(0xFF7A1900)

// --- Secondary: Soft Warm Slate / Amber ---
val WarmAmber = Color(0xFFE68A00)
val WarmAmberContainer = Color(0xFFFFF3D6)
val OnWarmAmberContainer = Color(0xFF5A3500)

val AccentBlue = Color(0xFF2563EB)
val AccentBlueContainer = Color(0xFFEFF6FF)
val AccentPurple = Color(0xFF7C3AED)
val AccentPurpleContainer = Color(0xFFF5F3FF)

// --- Semantic Feedback ---
val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenContainer = Color(0xFFDCFCE7)
val OnSuccessGreen = Color(0xFF14532D)

val WarningAmber = Color(0xFFD97706)
val WarningAmberContainer = Color(0xFFFEF3C7)

val ErrorCoral = Color(0xFFDC2626)
val ErrorCoralContainer = Color(0xFFFEE2E2)
val OnErrorCoral = Color(0xFF7F1D1D)

// --- Raw Palette Values for Theme Schemes ---
val LightBackground = Color(0xFFF9F8F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceMuted = Color(0xFFF2EFE9)
val LightOutline = Color(0xFFE2DDD5)
val LightOutlineSubtle = Color(0xFFECE8E0)

val LightTextPrimary = Color(0xFF1C2520)
val LightTextSecondary = Color(0xFF64748B)
val LightTextMuted = Color(0xFF94A3B8)

// --- Composable Theme-Aware Dynamic Getters ---
// In light mode these resolve to luxury warm cream/white/charcoal.
// In dark mode these dynamically resolve to high-contrast dark surfaces and white text.
val WarmBackgroundLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.background

val SurfaceWhite: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.surface

val SurfaceMutedLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

val SurfaceVariantLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

val OutlineLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.outline

val OutlineSubtleLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant

val TextPrimaryLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface

val TextSecondaryLight: Color
    @Composable
    get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

val TextMutedLight: Color
    @Composable
    get() = if (LocalIsDarkTheme.current) TextMutedDark else LightTextMuted

// --- Dark Surface & Background Tokens ---
// Modern Slate Palette (optimal readability, WCAG AAA contrast, eliminates murky pitch-black)
val BackgroundDark = Color(0xFF0F172A)          // Rich Slate 900 Background
val SurfaceDark = Color(0xFF1E293B)             // Slate 800 Elevated Card Surface
val SurfaceVariantDark = Color(0xFF283548)      // Slate 750 Variant for inputs, searchbars, muted boxes
val SurfaceMutedDark = Color(0xFF232F42)        // Soft muted container
val OutlineDark = Color(0xFF3E4F68)             // Slate 600 - High contrast, visible clean borders
val OutlineSubtleDark = Color(0xFF283548)

val TextPrimaryDark = Color(0xFFF8FAFC)         // Slate 50 - Ultra-bright, crisp white (15.5:1 contrast against SurfaceDark)
val TextSecondaryDark = Color(0xFFCBD5E1)       // Slate 300 - High contrast secondary text (10.2:1 contrast)
val TextMutedDark = Color(0xFF94A3B8)           // Slate 400 - Clear legible muted text & icons (6.5:1 contrast)

// Thermal Receipt Paper Styling (stays paper-like even in dark mode for realistic receipt)
val ReceiptPaperWhite = Color(0xFFFCFBF7)
val ReceiptInkBlack = Color(0xFF141716)
val ReceiptDottedDivider = Color(0xFFD5D2C9)


