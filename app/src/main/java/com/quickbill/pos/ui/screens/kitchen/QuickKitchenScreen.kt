package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.model.kds.OrderType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickKitchenScreen(
    viewModel: QuickKitchenViewModel,
    onChangeDeviceMode: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 720

    // Ticking state for live elapsed timers (re-evaluates every second)
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            KitchenTopAppBar(
                uiState = uiState,
                onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onOpenChangeMode = { viewModel.openChangeModeDialog() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val tabLabels = listOf(
                    "All Active",
                    "New",
                    "Preparing",
                    "Ready",
                    "History"
                )
                tabLabels.forEachIndexed { index, label ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Orders Grid / List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (uiState.orders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "No orders matching '${uiState.searchQuery}'" else "No orders in this category",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Orders created on the POS device will show up here automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    if (isTabletOrLandscape) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.orders, key = { it.orderId }) { order ->
                                KitchenOrderCard(
                                    order = order,
                                    currentTimeMillis = currentTimeMillis,
                                    warningMinutes = uiState.warningThresholdMinutes,
                                    viewModel = viewModel,
                                    onAdvanceStatus = { viewModel.advanceOrderStatus(order) },
                                    onCancel = { viewModel.cancelOrder(order.orderId) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.orders, key = { it.orderId }) { order ->
                                KitchenOrderCard(
                                    order = order,
                                    currentTimeMillis = currentTimeMillis,
                                    warningMinutes = uiState.warningThresholdMinutes,
                                    viewModel = viewModel,
                                    onAdvanceStatus = { viewModel.advanceOrderStatus(order) },
                                    onCancel = { viewModel.cancelOrder(order.orderId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (uiState.isSettingsDialogOpen) {
        KitchenSettingsDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSave = { name, warnMin, sound, vibrate, port ->
                viewModel.updateSettings(name, warnMin, sound, vibrate, port)
                viewModel.closeSettingsDialog()
            },
            onChangeDeviceMode = {
                viewModel.closeSettingsDialog()
                onChangeDeviceMode()
            }
        )
    }

    // Change Mode Confirmation Dialog
    if (uiState.isChangeModeDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeChangeModeDialog() },
            title = { Text("Change Device Mode?") },
            text = { Text("Are you sure you want to exit KDS Mode? You will be returned to the mode selection screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeChangeModeDialog()
                        viewModel.stopKdsServices()
                        onChangeDeviceMode()
                    }
                ) {
                    Text("Change Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeChangeModeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Order Popup Toast / Banner
    uiState.latestNotificationOrder?.let { newOrder ->
        LaunchedEffect(newOrder.orderId) {
            delay(4000L)
            viewModel.dismissNotification()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KitchenTopAppBar(
    uiState: KitchenUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChangeMode: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "QuickKitchen KDS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isServerRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Text(
                                text = "ws://${uiState.serverIp}:${uiState.serverPort} • ${uiState.connectedClients} POS connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Kitchen Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenChangeMode) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Change Device Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search by Order # or Table #...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun KitchenOrderCard(
    order: KitchenOrder,
    currentTimeMillis: Long,
    warningMinutes: Int,
    viewModel: QuickKitchenViewModel,
    onAdvanceStatus: () -> Unit,
    onCancel: () -> Unit
) {
    var items by remember { mutableStateOf<List<KitchenOrderItem>>(emptyList()) }
    LaunchedEffect(order.orderId) {
        viewModel.getOrderItems(order.orderId) { fetched ->
            items = fetched
        }
    }

    // Elapsed calculation
    val elapsedSeconds = ((currentTimeMillis - order.createdAt) / 1000L).coerceAtLeast(0L)
    val elapsedMinutes = elapsedSeconds / 60
    val isOverdue = order.status != OrderStatus.COMPLETED &&
            order.status != OrderStatus.CANCELLED &&
            elapsedMinutes >= warningMinutes

    val formattedTime = String.format(Locale.US, "%02d:%02d", elapsedMinutes, elapsedSeconds % 60)

    val headerBgColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        order.status == OrderStatus.NEW -> MaterialTheme.colorScheme.primaryContainer
        order.status == OrderStatus.PREPARING -> MaterialTheme.colorScheme.tertiaryContainer
        order.status == OrderStatus.READY -> Color(0xFFDCFCE7)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val headerTextColor = when {
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        order.status == OrderStatus.NEW -> MaterialTheme.colorScheme.onPrimaryContainer
        order.status == OrderStatus.PREPARING -> MaterialTheme.colorScheme.onTertiaryContainer
        order.status == OrderStatus.READY -> Color(0xFF166534)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (isOverdue) 2.dp else 1.dp,
            if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 4.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = headerTextColor
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = headerTextColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.orderType.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = headerTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Live Elapsed Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOverdue) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue",
                            tint = headerTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = headerTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isOverdue) "LATE $formattedTime" else formattedTime,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = headerTextColor
                    )
                }
            }

            // Customer & Table info
            if (order.customerName.isNotBlank() || order.tableNumber.isNotBlank() || order.notes.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (order.customerName.isNotBlank()) {
                        Text(
                            text = "Customer: ${order.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (order.tableNumber.isNotBlank()) {
                        Text(
                            text = "Table: ${order.tableNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "Loading items...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    items.forEach { item ->
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
                                // Veg / Non-Veg dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .border(1.dp, if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626), RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626))
                                    )
                                }

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quantity Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "x${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons Footer
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (order.status) {
                    OrderStatus.NEW -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onAdvanceStatus,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start Preparing")
                        }
                    }

                    OrderStatus.PREPARING -> {
                        Button(
                            onClick = onAdvanceStatus,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Ready")
                        }
                    }

                    OrderStatus.READY -> {
                        Button(
                            onClick = onAdvanceStatus,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Complete Order")
                        }
                    }

                    OrderStatus.COMPLETED -> {
                        val completionTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.updatedAt))
                        Text(
                            text = "Completed at $completionTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    OrderStatus.CANCELLED -> {
                        Text(
                            text = "Order Cancelled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KitchenSettingsDialog(
    uiState: KitchenUiState,
    onDismiss: () -> Unit,
    onSave: (name: String, warnMin: Int, sound: Boolean, vibrate: Boolean, port: Int) -> Unit,
    onChangeDeviceMode: () -> Unit
) {
    var kitchenName by remember { mutableStateOf("Main Kitchen") }
    var warningMinutes by remember { mutableStateOf(uiState.warningThresholdMinutes.toString()) }
    var soundEnabled by remember { mutableStateOf(uiState.soundAlertEnabled) }
    var vibrateEnabled by remember { mutableStateOf(uiState.vibrateAlertEnabled) }
    var port by remember { mutableStateOf(uiState.serverPort.toString()) }

    Dialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KDS Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = kitchenName,
                    onValueChange = { kitchenName = it },
                    label = { Text("Kitchen Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = warningMinutes,
                    onValueChange = { warningMinutes = it },
                    label = { Text("Long-Wait Warning (Minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("WebSocket Server Port") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Alert on New Order")
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration Alert on New Order")
                    Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it })
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onChangeDeviceMode,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change Device Mode (POS / KDS)")
                }

                Button(
                    onClick = {
                        val warn = warningMinutes.toIntOrNull() ?: 5
                        val p = port.toIntOrNull() ?: 8887
                        onSave(kitchenName, warn, soundEnabled, vibrateEnabled, p)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings")
                }
            }
        }
    }
}
