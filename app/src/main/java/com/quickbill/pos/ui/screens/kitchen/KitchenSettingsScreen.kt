package com.quickbill.pos.ui.screens.kitchen

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.data.repository.KdsSettings
import com.quickbill.pos.ui.components.AppearanceDialog

import com.quickbill.pos.data.model.kds.ConnectedPosTerminal

@Composable
fun KitchenSettingsScreen(
    settings: KdsSettings,
    isConnected: Boolean,
    connectedTerminals: List<ConnectedPosTerminal> = emptyList(),
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onBackClick: () -> Unit,
    onUpdateSettings: (KdsSettings) -> Unit,
    onChangeDeviceMode: () -> Unit
) {
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
                        SettingSwitchRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Auto Accept Orders",
                            checked = true,
                            onCheckedChange = { /* auto-accept is active */ }
                        )
                        HorizontalDivider(color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingSwitchRow(
                            icon = Icons.Default.VolumeUp,
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
                                        color = if (isConnected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.RedPrimary
                                    )
                                )
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
                                                        tint = QuickKitchenTheme.GreenPrimary,
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
                                                        color = QuickKitchenTheme.GreenPrimary,
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
                border = BorderStroke(1.dp, QuickKitchenTheme.RedPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = QuickKitchenTheme.RedPrimary
                )
            ) {
                Text(
                    text = "Change Device Mode",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = QuickKitchenTheme.RedPrimary
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
                color = Color(0xFFE0F2FE),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
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
