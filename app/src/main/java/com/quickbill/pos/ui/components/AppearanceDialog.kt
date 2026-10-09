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

package com.quickbill.pos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.ui.theme.*

/**
 * Modern Segmented Row for switching between System, Light, and Dark appearance.
 * Perfect for embedding directly inside the navigation drawer or settings panels.
 */
@Composable
fun AppearanceSegmentedRow(
    currentThemeMode: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(AppThemeMode.SYSTEM, "System", Icons.Default.SettingsBrightness),
        Triple(AppThemeMode.LIGHT, "Light", Icons.Default.LightMode),
        Triple(AppThemeMode.DARK, "Dark", Icons.Default.DarkMode)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceMutedLight,
        border = BorderStroke(1.dp, OutlineLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { (mode, label, icon) ->
                val isSelected = currentThemeMode == mode

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) SurfaceWhite else Color.Transparent,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    label = "pillBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) EmeraldPrimary else TextSecondaryLight,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    label = "pillContent"
                )

                Surface(
                    onClick = { onSelectTheme(mode) },
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor,
                    border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)) else null,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Appearance Dialog allowing the user to select between Dark theme,
 * Light theme, and System theme with visual descriptions and instant preview.
 */
@Composable
fun AppearanceDialog(
    currentThemeMode: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val options = listOf(
        ThemeOptionItem(
            mode = AppThemeMode.SYSTEM,
            title = "System Default",
            subtitle = if (isSystemDark) "Follows device settings (Currently Dark)" else "Follows device settings (Currently Light)",
            icon = Icons.Default.SettingsBrightness,
            accentColor = AccentBlue,
            containerColor = AccentBlueContainer
        ),
        ThemeOptionItem(
            mode = AppThemeMode.LIGHT,
            title = "Light Theme",
            subtitle = "Clean, crisp warm cream surfaces with deep emerald",
            icon = Icons.Default.LightMode,
            accentColor = WarmAmber,
            containerColor = WarmAmberContainer
        ),
        ThemeOptionItem(
            mode = AppThemeMode.DARK,
            title = "Dark Theme",
            subtitle = "Sleek slate dark with ultra-crisp readable contrast",
            icon = Icons.Default.DarkMode,
            accentColor = DarkEmeraldPrimary,
            containerColor = DarkEmeraldContainer
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, OutlineLight),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Display Appearance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Choose your preferred display theme",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Options list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { option ->
                        val isSelected = currentThemeMode == option.mode

                        Surface(
                            onClick = {
                                onSelectTheme(option.mode)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) EmeraldContainer.copy(alpha = 0.45f) else SurfaceMutedLight,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else OutlineLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) EmeraldPrimary else option.containerColor,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else option.accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) EmeraldPrimary else TextPrimaryLight
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondaryLight,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Radio indicator
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (isSelected) EmeraldPrimary else OutlineLight
                                    ),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private data class ThemeOptionItem(
    val mode: AppThemeMode,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color
)
