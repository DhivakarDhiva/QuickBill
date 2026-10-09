/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quickbill.pos.ui.theme.LocalIsDarkTheme

object QuickKitchenTheme {
    // Primary Button & Brand Colors (balanced for high readability in both modes)
    val GreenPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF16A34A) else Color(0xFF0F5132)

    val GreenLight: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF22C55E) else Color(0xFF198754)

    val GreenAccent: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF4ADE80) else Color(0xFF0F5132)

    val OrangePrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFEA580C) else Color(0xFFEA580C)

    val OrangeLight: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFB923C) else Color(0xFFF97316)

    val OrangeAccent: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFB923C) else Color(0xFFC2410C)

    val RedPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFDC2626) else Color(0xFFDC2626)

    val RedLight: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF87171) else Color(0xFFEF4444)

    val RedAccent: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF87171) else Color(0xFFDC2626)

    // Dynamic Theme Surfaces & Layouts
    val Background: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0F172A) else Color(0xFFF8F9FA)

    val Surface: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFFFFFFF)

    val SurfaceVariant: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF334155) else Color(0xFFF1F5F9)

    val BorderSubtle: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF334155) else Color(0xFFE2E8F0)

    val TextPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF1E293B)

    val TextSecondary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)

    val TextMuted: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF64748B) else Color(0xFF94A3B8)

    // Status Pills
    val GreenPillBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B) else Color(0xFFDCFCE7)

    val GreenPillText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF86EFAC) else Color(0xFF166534)

    val OrangePillBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF7C2D12) else Color(0xFFFFEDD5)

    val OrangePillText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFDBA74) else Color(0xFF9A3412)

    val RedPillBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)

    val RedPillText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFCA5A5) else Color(0xFF991B1B)

    // Blue & Amber pills (for settings & receipts)
    val BluePillBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)

    val BluePillText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val AmberPillBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF78350F) else Color(0xFFFEF3C7)

    val AmberPillText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFCD34D) else Color(0xFFD97706)

    // Overdue order layout highlights
    val OverdueContainer: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3F131D) else Color(0xFFFFF1F2)

    val OverdueBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF43F5E) else Color(0xFFE11D48)

    val OverdueBannerBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF4C0519) else Color(0xFFFFE4E6)

    val OverdueBannerBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9F1239) else Color(0xFFFDA4AF)

    val OverdueBannerText: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFECDD3) else Color(0xFFBE123C)

    val OverdueScreenBg: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1F1015) else Color(0xFFFFF5F5)
}
