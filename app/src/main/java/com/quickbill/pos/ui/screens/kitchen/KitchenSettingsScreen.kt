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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.data.repository.KdsSettings
import com.quickbill.pos.ui.components.AppearanceDialog

import com.quickbill.pos.data.model.kds.ConnectedPosTerminal

@Composable
fun KitchenSettingsScreen(
    settings: KdsSettings,
    isConnected: Boolean,
    connectedTerminals: List<ConnectedPosTerminal> = emptyList(),
    p2pState: com.quickbill.pos.network.kds.P2pConnectionState = com.quickbill.pos.network.kds.P2pConnectionState(),
    isGroupCreating: Boolean = false,
    p2pLastError: String? = null,
    onStartP2pGroup: () -> Unit = {},
    onStopP2pGroup: () -> Unit = {},
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onBackClick: () -> Unit,
    onUpdateSettings: (KdsSettings) -> Unit,
    onChangeDeviceMode: () -> Unit
) {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it } ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
             ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED)
        if (granted) {
            onStartP2pGroup()
        }
    }

    fun checkHasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showWarningTimeDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
        topBar = {
            Surface(
                color = QuickKitchenTheme.Surface,
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = QuickKitchenTheme.TextPrimary
                        )
                    }

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuickKitchenTheme.TextPrimary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group 1: Kitchen Display
            SettingsGroup(title = "Kitchen Display") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Surface,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditNameDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kitchen Name",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = settings.kitchenName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = QuickKitchenTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Group 2: Order Behaviour
            SettingsGroup(title = "Order Behaviour") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Surface,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
//                        SettingSwitchRow(
//                            icon = Icons.Default.CheckCircle,
//                            title = "Auto Accept Orders",
//                            checked = true,
//                            onCheckedChange = { /* auto-accept is active */ }
//                        )
                        HorizontalDivider(color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingSwitchRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "Sound Notification",
                            checked = settings.soundAlertEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(soundAlertEnabled = it)) }
                        )
                        HorizontalDivider(color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingSwitchRow(
                            icon = Icons.Default.Vibration,
                            title = "Vibration",
                            checked = settings.vibrateAlertEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(vibrateAlertEnabled = it)) }
                        )
                    }
                }
            }

            // Group 3: Long Waiting Alert
            SettingsGroup(title = "Long Waiting Alert") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Surface,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWarningTimeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Warning Time",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${settings.warningThresholdMinutes} minutes",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = QuickKitchenTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Group: Appearance & Theme
            SettingsGroup(title = "Appearance & Theme") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Surface,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppearanceDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (currentThemeMode) {
                                    AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = null,
                                tint = QuickKitchenTheme.GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentThemeMode.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = QuickKitchenTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Group 4: Connection
            SettingsGroup(title = "Connection") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Surface,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoRow(label = "Service Name", value = "QuickKitchen-KDS")
                        InfoRow(label = "Port", value = settings.serverPort.toString())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) QuickKitchenTheme.GreenLight else QuickKitchenTheme.RedPrimary)
                                )
                                Text(
                                    text = if (isConnected) {
                                        if (connectedTerminals.isNotEmpty()) {
                                            "Connected (${connectedTerminals.size} POS)"
                                        } else {
                                            "Connected"
                                        }
                                    } else "Waiting...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isConnected) QuickKitchenTheme.GreenAccent else QuickKitchenTheme.RedAccent
                                    )
                                )
                            }
                        }

                        // Wi-Fi Direct (P2P) Status & Controls
                        HorizontalDivider(
                            color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wi-Fi Direct (P2P)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = QuickKitchenTheme.TextPrimary
                                    )
                                )
                                Text(
                                    text = if (p2pState.isConnected) {
                                        "P2P Group Active (192.168.49.1:${settings.serverPort})"
                                    } else {
                                        "Offline direct mode (no Wi-Fi router needed)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = if (p2pState.isConnected) QuickKitchenTheme.GreenAccent else QuickKitchenTheme.TextSecondary
                                    )
                                )
                            }
                            if (p2pState.isConnected) {
                                OutlinedButton(
                                    onClick = onStopP2pGroup,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuickKitchenTheme.RedAccent),
                                    border = BorderStroke(1.dp, QuickKitchenTheme.RedPrimary.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Stop P2P", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (checkHasPermissions()) {
                                            onStartP2pGroup()
                                        } else {
                                            permissionLauncher.launch(permissionsToRequest)
                                        }
                                    },
                                    enabled = !isGroupCreating,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    if (isGroupCreating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Starting...", fontSize = 11.sp)
                                    } else {
                                        Text("Start P2P", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (!p2pLastError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = QuickKitchenTheme.RedPillBg,
                                border = BorderStroke(1.dp, QuickKitchenTheme.RedPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = QuickKitchenTheme.RedAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = p2pLastError,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            color = QuickKitchenTheme.RedAccent
                                        )
                                    )
                                }
                            }
                        }

                        // Connected POS Terminals Details List
                        if (connectedTerminals.isNotEmpty()) {
                            HorizontalDivider(
                                color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Connected POS Terminals (${connectedTerminals.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = QuickKitchenTheme.TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                connectedTerminals.forEach { terminal ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = QuickKitchenTheme.Background,
                                        border = BorderStroke(0.5.dp, QuickKitchenTheme.BorderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(QuickKitchenTheme.GreenPillBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PointOfSale,
                                                        contentDescription = null,
                                                        tint = QuickKitchenTheme.GreenAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = terminal.name.ifBlank { "POS Terminal" },
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = QuickKitchenTheme.TextPrimary
                                                        )
                                                    )
                                                    if (terminal.deviceModel.isNotBlank()) {
                                                        Text(
                                                            text = terminal.deviceModel,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = QuickKitchenTheme.TextSecondary,
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                    Text(
                                                        text = "${terminal.ipAddress}:${terminal.port}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = QuickKitchenTheme.TextMuted,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(QuickKitchenTheme.GreenLight)
                                                )
                                                Text(
                                                    text = "Synced",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = QuickKitchenTheme.GreenPillText,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isConnected) {
                            HorizontalDivider(
                                color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "1 POS device connected via WebSocket",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = QuickKitchenTheme.TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Red Outlined Button: Change Device Mode matching Reference Screen 12
            OutlinedButton(
                onClick = onChangeDeviceMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, QuickKitchenTheme.RedAccent),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = QuickKitchenTheme.RedAccent
                )
            ) {
                Text(
                    text = "Change Device Mode",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = QuickKitchenTheme.RedAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog for Editing Kitchen Name
    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(settings.kitchenName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Kitchen Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateSettings(settings.copy(kitchenName = tempName.ifBlank { "Main Kitchen" }))
                    showEditNameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Editing Warning Time
    if (showWarningTimeDialog) {
        var tempMinutes by remember { mutableStateOf(settings.warningThresholdMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showWarningTimeDialog = false },
            title = { Text("Warning Threshold (Minutes)") },
            text = {
                OutlinedTextField(
                    value = tempMinutes,
                    onValueChange = { tempMinutes = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = tempMinutes.toIntOrNull() ?: 5
                    onUpdateSettings(settings.copy(warningThresholdMinutes = m))
                    showWarningTimeDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Theme / Appearance Selection
    if (showAppearanceDialog) {
        AppearanceDialog(
            currentThemeMode = currentThemeMode,
            onSelectTheme = { mode ->
                onThemeChange(mode)
                showAppearanceDialog = false
            },
            onDismiss = { showAppearanceDialog = false }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = QuickKitchenTheme.TextPrimary
            )
        )
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = QuickKitchenTheme.BluePillBg,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = QuickKitchenTheme.BluePillText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = QuickKitchenTheme.TextPrimary
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = QuickKitchenTheme.GreenPrimary
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = QuickKitchenTheme.TextSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = QuickKitchenTheme.TextPrimary
            )
        )
    }
}
