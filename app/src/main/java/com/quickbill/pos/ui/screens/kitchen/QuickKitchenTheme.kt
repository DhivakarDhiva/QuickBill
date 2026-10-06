package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object QuickKitchenTheme {
    // Primary Brand Colors from Reference (constant across themes)
    val GreenPrimary = Color(0xFF0F5132)      // Deep emerald green
    val GreenLight = Color(0xFF198754)        // Vibrant mid green

    val OrangePrimary = Color(0xFFEA580C)     // Rich orange for "Preparing" tab and buttons
    val OrangeLight = Color(0xFFF97316)       // Vibrant orange

    val RedPrimary = Color(0xFFDC2626)        // Red for "Ready" pill / late warning timers
    val RedLight = Color(0xFFEF4444)

    // Dynamic Theme Colors based on Light / Dark mode
    val Background: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFF8F9FA)

    val Surface: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFFFFFFF)

    val SurfaceVariant: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFF1F5F9)

    val BorderSubtle: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)

    val TextPrimary: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF1E293B)

    val TextSecondary: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)

    val TextMuted: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF64748B) else Color(0xFF94A3B8)

    val GreenPillBg: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF064E3B) else Color(0xFFDCFCE7)

    val GreenPillText: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF86EFAC) else Color(0xFF166534)

    val OrangePillBg: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7C2D12) else Color(0xFFFFEDD5)

    val OrangePillText: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFDBA74) else Color(0xFF9A3412)

    val RedPillBg: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)

    val RedPillText: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFCA5A5) else Color(0xFF991B1B)
}
