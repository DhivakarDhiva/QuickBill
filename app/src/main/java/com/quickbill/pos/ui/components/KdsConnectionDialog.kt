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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickbill.pos.network.kds.ConnectionStatus
import com.quickbill.pos.network.kds.OrderSyncManager

@Composable
fun KdsConnectionDialog(
    orderSyncManager: OrderSyncManager,
    onDismiss: () -> Unit
) {
    val connectionStatus by orderSyncManager.connectionManager.connectionStatus.collectAsState()
    val connectedAddress by orderSyncManager.connectionManager.connectedServerAddress.collectAsState()
    val connectedScreens by orderSyncManager.connectionManager.connectedKdsScreens.collectAsState()
    val lastErrorMessage by orderSyncManager.connectionManager.lastErrorMessage.collectAsState()
    val discoveredServices by orderSyncManager.discoveryManager.discoveredServices.collectAsState()
    val isDiscovering by orderSyncManager.discoveryManager.isDiscovering.collectAsState()
    val pendingCount by orderSyncManager.outboxManager.pendingCountFlow.collectAsState(initial = 0)

    val p2pManager = orderSyncManager.p2pManager
    val isP2pEnabled by p2pManager.isP2pEnabled.collectAsState()
    val isP2pDiscovering by p2pManager.isDiscovering.collectAsState()
    val discoveredP2pPeers by p2pManager.discoveredPeers.collectAsState()
    val p2pConnectionState by p2pManager.connectionState.collectAsState()
    val p2pLastError by p2pManager.lastError.collectAsState()

    var selectedModeTab by remember { mutableIntStateOf(0) } // 0 = Wi-Fi (NSD), 1 = Wi-Fi Direct (P2P)

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
    var hasP2pPermissions by remember { mutableStateOf(p2pManager.hasRequiredPermissions()) }
    val p2pPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasP2pPermissions = p2pManager.hasRequiredPermissions()
        if (hasP2pPermissions) {
            p2pManager.startPeerDiscovery()
        }
    }

    val lastWifiHost = remember {
        val h = orderSyncManager.connectionManager.getLastConnectedHost()
        if (h.startsWith("192.168.49.")) "" else h
    }
    val lastWifiPort = remember {
        val p = orderSyncManager.connectionManager.getLastConnectedPort().toString()
        val h = orderSyncManager.connectionManager.getLastConnectedHost()
        if (h.startsWith("192.168.49.")) "8080"
        else if (p != "0" && p.isNotBlank()) p else "8080"
    }
    var manualIp by remember { mutableStateOf(lastWifiHost) }
    var manualPort by remember { mutableStateOf(lastWifiPort) }

    LaunchedEffect(orderSyncManager.connectionManager.getLastConnectedHost()) {
        val h = orderSyncManager.connectionManager.getLastConnectedHost()
        if (h.isNotBlank() && !h.startsWith("192.168.49.") && manualIp.isBlank()) {
            manualIp = h
        }
    }

    LaunchedEffect(Unit) {
        orderSyncManager.discoveryManager.startDiscovery()
    }

    LaunchedEffect(selectedModeTab) {
        if (selectedModeTab == 1) {
            hasP2pPermissions = p2pManager.hasRequiredPermissions()
            if (hasP2pPermissions) {
                p2pManager.startPeerDiscovery()
            }
        }
    }

    Dialog(onDismissRequest = {
        orderSyncManager.discoveryManager.stopDiscovery()
        p2pManager.stopPeerDiscovery()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Kitchen Display (KDS)",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Broadcast orders to multiple screens",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    IconButton(onClick = {
                        orderSyncManager.discoveryManager.stopDiscovery()
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Overall Status Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Color(0xFFDCFCE7)
                        ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFFEF9C3)
                        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionStatus) {
                                            ConnectionStatus.CONNECTED -> Color(0xFF16A34A)
                                            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFFCA8A04)
                                            ConnectionStatus.DISCONNECTED -> Color(0xFFDC2626)
                                        }
                                    )
                            )
                            Column {
                                Text(
                                    text = when (connectionStatus) {
                                        ConnectionStatus.CONNECTED -> if (connectedScreens.size > 1) {
                                            "Connected to ${connectedScreens.size} Kitchen Displays"
                                        } else {
                                            "Connected to Kitchen"
                                        }
                                        ConnectionStatus.CONNECTING -> "Connecting..."
                                        ConnectionStatus.RECONNECTING -> "Reconnecting..."
                                        ConnectionStatus.DISCONNECTED -> "Disconnected"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when (connectionStatus) {
                                        ConnectionStatus.CONNECTED -> Color(0xFF166534)
                                        ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> Color(0xFF854D0E)
                                        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = if (connectedScreens.size > 1) {
                                        "Orders are broadcast to all ${connectedScreens.size} screens"
                                    } else if (connectedAddress.isNotBlank()) {
                                        connectedAddress
                                    } else {
                                        "Select or enter IP to connect screens"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (connectionStatus == ConnectionStatus.CONNECTED || p2pConnectionState.isConnected) {
                            TextButton(
                                onClick = { orderSyncManager.disconnectAll() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (orderSyncManager.connectionManager.getLastConnectedHost().isNotBlank()) {
                            Button(
                                onClick = { orderSyncManager.connectionManager.reconnect() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reconnect", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (lastErrorMessage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = lastErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Active Connected Kitchen Screens List (Requirement 1: Multi-KDS Management)
                if (connectedScreens.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Kitchen Screens (${connectedScreens.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Syncing live",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A)
                            )
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 130.dp)
                        ) {
                            items(connectedScreens) { screen ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF16A34A))
                                            )
                                            Column {
                                                Text(
                                                    text = screen.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF166534)
                                                )
                                                Text(
                                                    text = "${screen.host}:${screen.port}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }

                                        // Dedicated Disconnect button for connected screen
                                        OutlinedButton(
                                            onClick = {
                                                orderSyncManager.connectionManager.disconnectFromKds(screen.host, screen.port)
                                                if (screen.host.startsWith("192.168.49.")) {
                                                    p2pManager.removeGroup()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFDC2626)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Connection Mode Tabs: Wi-Fi / Hotspot (NSD) vs Wi-Fi Direct (P2P)
                TabRow(
                    selectedTabIndex = selectedModeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedModeTab == 0,
                        onClick = { selectedModeTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    "Wi-Fi / Hotspot",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedModeTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedModeTab == 1,
                        onClick = {
                            selectedModeTab = 1
                            if (!hasP2pPermissions) {
                                p2pPermissionLauncher.launch(permissionsToRequest)
                            } else {
                                p2pManager.startPeerDiscovery()
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    "Wi-Fi Direct (P2P)",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedModeTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }

                if (selectedModeTab == 0) {
                    // Wi-Fi / Hotspot Mode: Filter out any P2P subnet (192.168.49.x) services
                    val wifiDiscoveredServices = remember(discoveredServices) {
                        discoveredServices.filter { !it.hostIp.startsWith("192.168.49.") }
                    }
                    val availableWifiServices = remember(wifiDiscoveredServices, connectedScreens) {
                        wifiDiscoveredServices.filter { service ->
                            connectedScreens.none { it.host == service.hostIp && it.port == service.port }
                        }
                    }
                    val alreadyConnectedWifiCount = remember(wifiDiscoveredServices, connectedScreens) {
                        wifiDiscoveredServices.count { service ->
                            connectedScreens.any { it.host == service.hostIp && it.port == service.port }
                        }
                    }

                    // Discovered KDS Displays (NSD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discovered Kitchen Devices",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        IconButton(
                            onClick = { orderSyncManager.discoveryManager.startDiscovery() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Devices",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (availableWifiServices.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (alreadyConnectedWifiCount > 0) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (alreadyConnectedWifiCount > 0) BorderStroke(1.dp, Color(0xFFBBF7D0)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (alreadyConnectedWifiCount > 0) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Discovered Wi-Fi screen is connected (see Active Screens above)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF166534)
                                    )
                                } else {
                                    if (isDiscovering) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = if (isDiscovering) "Searching for kitchen screens on local Wi-Fi..." else "No kitchen screens found automatically",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 140.dp)
                        ) {
                            items(availableWifiServices) { service ->
                                val isConnecting = orderSyncManager.connectionManager.isKdsConnecting(service.hostIp, service.port)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tv,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = service.serviceName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${service.hostIp}:${service.port}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                orderSyncManager.connectionManager.connectToKds(service.hostIp, service.port, service.serviceName)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp),
                                            enabled = !isConnecting
                                        ) {
                                            if (isConnecting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Connecting...", fontSize = 11.sp)
                                            } else {
                                                Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Manual IP Fallback / Add Screen via IP
                    val rawLocalIp = remember { orderSyncManager.discoveryManager.getLocalIpAddress() }
                    val isP2pLocal = rawLocalIp.startsWith("192.168.49.")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Or Connect Screen via Manual IP",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isP2pLocal) "Enter the IP address of the KDS device on your Wi-Fi router"
                                   else "This device's IP: $rawLocalIp (port: 8080)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            placeholder = { Text("192.168.1.xxx") },
                            label = { Text("KDS IP") },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    val manualTargetPort = manualPort.toIntOrNull() ?: 8080
                    val isManualTargetConnected = manualIp.isNotBlank() && orderSyncManager.connectionManager.isKdsConnected(manualIp.trim(), manualTargetPort)
                    val isManualTargetConnecting = manualIp.isNotBlank() && orderSyncManager.connectionManager.isKdsConnecting(manualIp.trim(), manualTargetPort)

                    if (isManualTargetConnected) {
                        OutlinedButton(
                            onClick = {
                                orderSyncManager.connectionManager.disconnectFromKds(manualIp.trim(), manualTargetPort)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text("Disconnect $manualIp:$manualTargetPort")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    orderSyncManager.connectionManager.connectToKds(
                                        rawHost = manualIp.trim(),
                                        rawPort = manualTargetPort,
                                        name = "Kitchen Display ${connectedScreens.size + 1}"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = manualIp.isNotBlank() && !isManualTargetConnecting
                        ) {
                            if (isManualTargetConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Text(if (connectedScreens.isNotEmpty()) "Connect Additional Screen (+ Share)" else "Connect via IP")
                            }
                        }
                    }
                } else {
                    // Wi-Fi Direct (P2P) Tab Content
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Info banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Wi-Fi Direct connects devices directly without any Wi-Fi router. Ensure Wi-Fi is ON on both devices and KDS has P2P started.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Wi-Fi Disabled Warning
                        if (!p2pManager.isWifiEnabled()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Wi-Fi is turned OFF. Please turn ON Wi-Fi in Quick Settings to use Wi-Fi Direct.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        // Location Services Disabled Warning (Android < 13)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !p2pManager.isLocationEnabled()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Location (GPS) is OFF. Android requires Location ON to scan and find nearby Wi-Fi devices.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        // Permission request banner if needed
                        if (!hasP2pPermissions) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Permission Required",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "Nearby Wi-Fi Devices permission is needed for P2P direct discovery.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                    Button(
                                        onClick = { p2pPermissionLauncher.launch(permissionsToRequest) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Grant", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Error notification banner if any
                        if (!p2pLastError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Wi-Fi Direct Notice",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = p2pLastError ?: "",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            if (!hasP2pPermissions) {
                                                p2pPermissionLauncher.launch(permissionsToRequest)
                                            } else {
                                                p2pManager.startPeerDiscovery()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Retry", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Wi-Fi Direct Connection Status (if active)
                        if (p2pConnectionState.isConnected) {
                            val groupIp = p2pConnectionState.groupOwnerAddress ?: "192.168.49.1"
                            val isKdsSocketConnected = orderSyncManager.connectionManager.isKdsConnected(groupIp, 8080)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF16A34A))
                                            )
                                            Column {
                                                Text(
                                                    text = "Wi-Fi Direct P2P Active",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF166534)
                                                )
                                                Text(
                                                    text = "Kitchen Screen IP: $groupIp:8080",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                p2pManager.removeGroup()
                                                orderSyncManager.connectionManager.disconnectFromKds(groupIp, 8080)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Disconnect P2P", fontSize = 10.sp)
                                        }
                                    }

                                    if (!isKdsSocketConnected) {
                                        val isConnecting = orderSyncManager.connectionManager.isKdsConnecting(groupIp, 8080)
                                        Button(
                                            onClick = {
                                                orderSyncManager.connectionManager.connectToKds(
                                                    rawHost = groupIp,
                                                    rawPort = 8080,
                                                    name = "Wi-Fi Direct Kitchen"
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            enabled = !isConnecting
                                        ) {
                                            if (isConnecting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Connecting...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            } else {
                                                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Sync Orders with Kitchen Screen", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // P2P Scanning Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nearby P2P Devices (${discoveredP2pPeers.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (!hasP2pPermissions) {
                                            p2pPermissionLauncher.launch(permissionsToRequest)
                                        } else {
                                            p2pManager.startPeerDiscovery()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    if (isP2pDiscovering) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Scanning...", fontSize = 11.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Scan Nearby", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (discoveredP2pPeers.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isP2pDiscovering) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = if (isP2pDiscovering) "Searching for nearby Wi-Fi Direct devices..." else "No P2P devices found. Ensure KDS is waiting or scanning.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.heightIn(max = 160.dp)
                            ) {
                                items(discoveredP2pPeers) { peer ->
                                    val isConnected = peer.status == android.net.wifi.p2p.WifiP2pDevice.CONNECTED ||
                                            (p2pConnectionState.isConnected && peer.deviceAddress == p2pConnectionState.groupOwnerAddress)
                                    val isInvited = peer.status == android.net.wifi.p2p.WifiP2pDevice.INVITED

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isConnected) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isConnected) BorderStroke(1.dp, Color(0xFFBBF7D0)) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = peer.deviceName.ifBlank { "Kitchen Display" },
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isConnected) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = when {
                                                            isConnected -> Color(0xFFDCFCE7)
                                                            isInvited -> Color(0xFFFEF9C3)
                                                            else -> MaterialTheme.colorScheme.surface
                                                        },
                                                        modifier = Modifier.padding(start = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = peer.statusLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            color = when {
                                                                isConnected -> Color(0xFF16A34A)
                                                                isInvited -> Color(0xFFCA8A04)
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = peer.deviceAddress,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }

                                            if (isConnected) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val groupIp = p2pConnectionState.groupOwnerAddress ?: "192.168.49.1"
                                                        p2pManager.removeGroup()
                                                        orderSyncManager.connectionManager.disconnectFromKds(groupIp, 8080)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Disconnect", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        p2pManager.connectToPeer(
                                                            deviceAddress = peer.deviceAddress,
                                                            onSuccess = {},
                                                            onFailure = {}
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    enabled = !isInvited
                                                ) {
                                                    if (isInvited) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Waiting...", fontSize = 11.sp)
                                                    } else {
                                                        Text("Connect", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Note: If prompted on the Kitchen Display, accept the connection prompt.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
