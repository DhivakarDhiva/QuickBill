package com.quickbill.pos.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quickbill.pos.network.kds.ConnectionManager
import com.quickbill.pos.network.kds.ConnectionStatus
import com.quickbill.pos.network.kds.DiscoveredKdsService
import com.quickbill.pos.network.kds.NsdDiscoveryManager
import com.quickbill.pos.network.kds.OrderSyncManager

@Composable
fun KdsConnectionDialog(
    orderSyncManager: OrderSyncManager,
    onDismiss: () -> Unit
) {
    val connectionStatus by orderSyncManager.connectionManager.connectionStatus.collectAsState()
    val connectedAddress by orderSyncManager.connectionManager.connectedServerAddress.collectAsState()
    val lastErrorMessage by orderSyncManager.connectionManager.lastErrorMessage.collectAsState()
    val discoveredServices by orderSyncManager.discoveryManager.discoveredServices.collectAsState()
    val isDiscovering by orderSyncManager.discoveryManager.isDiscovering.collectAsState()
    val pendingCount by orderSyncManager.outboxManager.pendingCountFlow.collectAsState(initial = 0)

    val lastHost = remember { orderSyncManager.connectionManager.getLastConnectedHost() }
    val lastPort = remember { orderSyncManager.connectionManager.getLastConnectedPort().toString() }
    var manualIp by remember { mutableStateOf(lastHost) }
    var manualPort by remember { mutableStateOf(if (lastPort != "0" && lastPort.isNotBlank()) lastPort else "8080") }

    LaunchedEffect(orderSyncManager.connectionManager.getLastConnectedHost()) {
        val h = orderSyncManager.connectionManager.getLastConnectedHost()
        if (h.isNotBlank() && manualIp.isBlank()) {
            manualIp = h
        }
    }

    LaunchedEffect(Unit) {
        orderSyncManager.discoveryManager.startDiscovery()
    }

    Dialog(onDismissRequest = {
        orderSyncManager.discoveryManager.stopDiscovery()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kitchen Display (KDS)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = {
                        orderSyncManager.discoveryManager.stopDiscovery()
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Current Connection Status Card
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        ConnectionStatus.CONNECTED -> "Connected to Kitchen"
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
                                if (connectedAddress.isNotBlank()) {
                                    Text(
                                        text = connectedAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            TextButton(
                                onClick = { orderSyncManager.connectionManager.disconnectClient() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect")
                            }
                        } else if (orderSyncManager.connectionManager.getLastConnectedHost().isNotBlank()) {
                            Button(
                                onClick = { orderSyncManager.connectionManager.reconnect() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reconnect")
                            }
                        }
                    }
                }

                if (connectionStatus != ConnectionStatus.CONNECTED && lastErrorMessage.isNotBlank()) {
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

                // Outbox Queue Info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Pending Offline Outbox",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$pendingCount order(s) waiting to sync",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (pendingCount > 0) {
                            OutlinedButton(
                                onClick = { orderSyncManager.drainPendingOutbox() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sync Now")
                            }
                        }
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    IconButton(
                        onClick = { orderSyncManager.discoveryManager.startDiscovery() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Devices",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (discoveredServices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isDiscovering) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = if (isDiscovering) "Searching on local Wi-Fi..." else "No KDS device found yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 140.dp)
                    ) {
                        items(discoveredServices) { service ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        orderSyncManager.connectionManager.connectToKds(service.hostIp, service.port)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = service.serviceName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${service.hostIp}:${service.port}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            orderSyncManager.connectionManager.connectToKds(service.hostIp, service.port)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                    }
                }

                // Manual IP Fallback
                val localIp = remember { orderSyncManager.discoveryManager.getLocalIpAddress() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Or Connect via Manual IP",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "This device's IP: $localIp (port: 8080)",
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

                Button(
                    onClick = {
                        val port = manualPort.toIntOrNull() ?: 8080
                        if (manualIp.isNotBlank()) {
                            orderSyncManager.connectionManager.connectToKds(manualIp.trim(), port)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = manualIp.isNotBlank() && connectionStatus != ConnectionStatus.CONNECTING
                ) {
                    if (connectionStatus == ConnectionStatus.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Text("Connect via IP")
                    }
                }
            }
        }
    }
}
