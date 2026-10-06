package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.ui.graphics.Color

object QuickKitchenTheme {
    // Primary Brand Colors from Reference
    val GreenPrimary = Color(0xFF0F5132)      // Deep emerald green for "Start Preparing", "Completed", tabs
    val GreenLight = Color(0xFF198754)        // Vibrant mid green
    val GreenPillBg = Color(0xFFDCFCE7)       // Light green pill background
    val GreenPillText = Color(0xFF166534)     // Dark green pill text

    val OrangePrimary = Color(0xFFEA580C)     // Rich orange for "Preparing" tab and buttons
    val OrangeLight = Color(0xFFF97316)       // Vibrant orange
    val OrangePillBg = Color(0xFFFFEDD5)      // Light orange pill background
    val OrangePillText = Color(0xFF9A3412)    // Dark orange pill text

    val RedPrimary = Color(0xFFDC2626)        // Red for "Ready" pill / late warning timers
    val RedPillBg = Color(0xFFFEE2E2)         // Light red timer pill background
    val RedPillText = Color(0xFF991B1B)       // Dark red timer text

    val Background = Color(0xFFF8F9FA)        // Clean off-white background
    val Surface = Color(0xFFFFFFFF)           // Crisp white card surface
    val BorderSubtle = Color(0xFFE2E8F0)      // 1dp subtle border for cards

    val TextPrimary = Color(0xFF1E293B)       // Bold slate text for titles, order numbers
    val TextSecondary = Color(0xFF64748B)     // Muted text for dining type, table, notes
    val TextMuted = Color(0xFF94A3B8)         // Lighter text for secondary details
}
