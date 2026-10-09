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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import com.quickbill.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBillTopBar(
    title: String,
    currentUser: UserEntity?,
    isOnline: Boolean,
    onMenuClick: () -> Unit,
    onHeldCartsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    heldCartCount: Int = 0,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onKdsClick: () -> Unit = {},
    onChangeDeviceModeClick: () -> Unit = {},
    kdsStatus: com.quickbill.pos.network.kds.ConnectionStatus = com.quickbill.pos.network.kds.ConnectionStatus.DISCONNECTED,
    kdsPendingCount: Int = 0,
    connectedKdsCount: Int = 0
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(QuickBillDimens.topBarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back Button or Hamburger Menu + Brand Logo + App Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryLight
                        )
                    }
                } else {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = TextPrimaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                QuickBillLogoBadge(size = 32.dp, iconSize = 18.dp)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight,
                        fontSize = 18.sp
                    )
                )
            }

            // Right: Online badge, Parked Orders, Avatar pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Held Orders badge
                if (heldCartCount > 0) {
                    Surface(
                        onClick = onHeldCartsClick,
                        shape = RoundedCornerShape(10.dp),
                        color = CoralContainer,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleFilled,
                                contentDescription = "Held Orders",
                                tint = CoralAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$heldCartCount",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CoralAccent
                                )
                            )
                        }
                    }
                }

                // KDS Connection Status Chip
                Surface(
                    onClick = onKdsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = when (kdsStatus) {
                        com.quickbill.pos.network.kds.ConnectionStatus.CONNECTED -> Color(0xFFDCFCE7)
                        com.quickbill.pos.network.kds.ConnectionStatus.CONNECTING,
                        com.quickbill.pos.network.kds.ConnectionStatus.RECONNECTING -> Color(0xFFFEF9C3)
                        com.quickbill.pos.network.kds.ConnectionStatus.DISCONNECTED -> if (kdsPendingCount > 0) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (kdsStatus) {
                                        com.quickbill.pos.network.kds.ConnectionStatus.CONNECTED -> Color(0xFF16A34A)
                                        com.quickbill.pos.network.kds.ConnectionStatus.CONNECTING,
                                        com.quickbill.pos.network.kds.ConnectionStatus.RECONNECTING -> Color(0xFFCA8A04)
                                        com.quickbill.pos.network.kds.ConnectionStatus.DISCONNECTED -> if (kdsPendingCount > 0) Color(0xFFDC2626) else Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (kdsStatus) {
                                com.quickbill.pos.network.kds.ConnectionStatus.CONNECTED -> if (connectedKdsCount > 1) "KDS ($connectedKdsCount)" else "KDS"
                                com.quickbill.pos.network.kds.ConnectionStatus.CONNECTING,
                                com.quickbill.pos.network.kds.ConnectionStatus.RECONNECTING -> "KDS..."
                                com.quickbill.pos.network.kds.ConnectionStatus.DISCONNECTED -> if (kdsPendingCount > 0) "KDS ($kdsPendingCount)" else "KDS"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (kdsStatus) {
                                    com.quickbill.pos.network.kds.ConnectionStatus.CONNECTED -> Color(0xFF166534)
                                    com.quickbill.pos.network.kds.ConnectionStatus.CONNECTING,
                                    com.quickbill.pos.network.kds.ConnectionStatus.RECONNECTING -> Color(0xFF854D0E)
                                    com.quickbill.pos.network.kds.ConnectionStatus.DISCONNECTED -> if (kdsPendingCount > 0) Color(0xFF991B1B) else Color(0xFF475569)
                                }
                            )
                        )
                    }
                }

                // Avatar Circle Button ('A' for Admin, 'C' for Cashier)
                if (currentUser != null) {
                    val roleLetter = if (currentUser.role == UserRole.ADMIN) "A" else "C"
                    val avatarBg = if (currentUser.role == UserRole.ADMIN) CoralAccent else EmeraldPrimary

                    Box {
                        Surface(
                            onClick = { showUserMenu = true },
                            shape = CircleShape,
                            color = avatarBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = roleLetter,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(currentUser.fullName, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${currentUser.role.name} • @${currentUser.username}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                                        )
                                    }
                                },
                                onClick = {},
                                enabled = false
                            )
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("Kitchen Display (KDS)", color = TextPrimaryLight) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showUserMenu = false
                                    onKdsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Device Mode", color = TextPrimaryLight) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showUserMenu = false
                                    onChangeDeviceModeClick()
                                }
                            )
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("Appearance", color = TextPrimaryLight) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showUserMenu = false
                                    onAppearanceClick()
                                }
                            )
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            DropdownMenuItem(

                                text = { Text("Sign Out", color = ErrorCoral) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = ErrorCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showUserMenu = false
                                    onLogoutClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
