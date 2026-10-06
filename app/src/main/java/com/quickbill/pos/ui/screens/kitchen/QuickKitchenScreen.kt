package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.data.repository.ThemeRepository
import kotlinx.coroutines.delay

@Composable
fun QuickKitchenScreen(
    viewModel: QuickKitchenViewModel,
    themeRepository: ThemeRepository? = null,
    onChangeDeviceMode: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    val currentThemeMode by themeRepository?.themeMode?.collectAsState()
        ?: remember { mutableStateOf(AppThemeMode.SYSTEM) }

    // Live elapsed timer ticking every 1000ms
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showConnectedPosDetailsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Ensure KDS services (WebSocket Server & NSD Advertising) are running when KDS screen is active,
    // and stopped cleanly when leaving KDS mode
    DisposableEffect(Unit) {
        viewModel.startKdsServices()
        onDispose {
            viewModel.stopKdsServices()
        }
    }

    // Screen 3: Kitchen Display Setup (if not completed)
    if (!uiState.isSetupComplete) {
        KitchenSetupScreen(
            initialKitchenName = uiState.kitchenName,
            port = uiState.serverPort,
            onStartKds = { name ->
                viewModel.markSetupComplete(name)
            }
        )
        return
    }

    // Screen 4: Waiting for Connection (initial connection / discovery)
    if (!uiState.hasShownWaitingScreen) {
        WaitingConnectionScreen(
            kitchenName = uiState.kitchenName,
            ipAddress = uiState.serverIp,
            port = uiState.serverPort,
            connectedClientsCount = uiState.connectedClients,
            onConnected = {
                viewModel.markWaitingScreenShown()
            },
            onSkipToDashboard = {
                viewModel.markWaitingScreenShown()
            }
        )
        return
    }

    // Screen 8, 9, 10: Order Details Screen (when an order is clicked)
    val detailOrder = uiState.selectedOrderForDetail
    if (detailOrder != null) {
        OrderDetailsScreen(
            order = detailOrder,
            items = detailOrder.items,
            currentTimeMillis = currentTimeMillis,
            warningThresholdMinutes = uiState.warningThresholdMinutes,
            onBackClick = { viewModel.closeOrderDetail() },
            onAdvanceStatus = { viewModel.advanceOrderStatus(detailOrder) },
            onViewBill = { /* Preview bill */ }
        )
        return
    }

    // Main KDS Views based on Bottom Bar Tab (Orders | History | Settings)
    when (uiState.currentNavTab) {
        1 -> {
            // Screen 11: Order History
            KitchenHistoryScreen(
                orders = uiState.allOrders,
                activeCount = uiState.activeOrdersCount,
                completedCount = uiState.completedOrdersCount,
                currentTimeMillis = currentTimeMillis,
                warningThresholdMinutes = uiState.warningThresholdMinutes,
                onBackClick = { viewModel.selectNavTab(0) },
                onOrderClick = { order -> viewModel.openOrderDetail(order) },
                bottomBar = {
                    QuickKitchenBottomBar(
                        currentTab = 1,
                        activeOrdersCount = uiState.activeOrdersCount,
                        onSelectTab = { viewModel.selectNavTab(it) }
                    )
                }
            )
        }
        2 -> {
            // Screen 12: KDS Settings
            KitchenSettingsScreen(
                settings = uiState.settings,
                isConnected = uiState.connectedClients > 0,
                connectedTerminals = uiState.connectedPosTerminals,
                currentThemeMode = currentThemeMode,
                onThemeChange = { mode -> themeRepository?.setThemeMode(mode) },
                onBackClick = { viewModel.selectNavTab(0) },
                onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
                onChangeDeviceMode = {
                    viewModel.stopKdsServices()
                    onChangeDeviceMode()
                }
            )
        }
        else -> {
            // Screen 5, 6, 7: KDS Main Screen (New / Preparing / Ready Orders)
            Scaffold(
                containerColor = QuickKitchenTheme.Background,
                topBar = {
                    QuickKitchenHeader(
                        kitchenName = uiState.kitchenName,
                        isConnected = uiState.connectedClients > 0,
                        onConnectionClick = { showConnectedPosDetailsDialog = true }
                    )
                },
                bottomBar = {
                    QuickKitchenBottomBar(
                        currentTab = 0,
                        activeOrdersCount = uiState.activeOrdersCount,
                        onSelectTab = { viewModel.selectNavTab(it) }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Status Tabs matching Reference Screen 5, 6, 7
                    QuickKitchenStatusTabs(
                        newCount = uiState.newOrdersCount,
                        preparingCount = uiState.preparingOrdersCount,
                        readyCount = uiState.readyOrdersCount,
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = { viewModel.selectStatus(it) }
                    )

                    // Section Title: "New Orders (4)" / "Preparing Orders (3)" / "Ready Orders (2)"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val titleText = when (uiState.selectedStatus) {
                            OrderStatus.NEW -> "New Orders (${uiState.newOrdersCount})"
                            OrderStatus.PREPARING -> "Preparing Orders (${uiState.preparingOrdersCount})"
                            OrderStatus.READY -> "Ready Orders (${uiState.readyOrdersCount})"
                            else -> "Orders (${uiState.displayedOrders.size})"
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )

                        IconButton(
                            onClick = { /* Sort order toggle */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort Orders",
                                tint = QuickKitchenTheme.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Order Cards Grid matching Reference (2 columns on mobile, adaptive on tablet)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        if (uiState.displayedOrders.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChefHatBadge(
                                        size = 48,
                                        backgroundColor = QuickKitchenTheme.SurfaceVariant,
                                        iconColor = QuickKitchenTheme.TextMuted
                                    )
                                    Text(
                                        text = "No ${uiState.selectedStatus.displayName.lowercase()} orders",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = if (isTabletOrLandscape) GridCells.Adaptive(260.dp) else GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(uiState.displayedOrders, key = { it.orderId }) { order ->
                                    QuickKitchenOrderCard(
                                        order = order,
                                        items = order.items,
                                        currentTimeMillis = currentTimeMillis,
                                        warningThresholdMinutes = uiState.warningThresholdMinutes,
                                        onCardClick = { viewModel.openOrderDetail(order) },
                                        onActionClick = { viewModel.advanceOrderStatus(order) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConnectedPosDetailsDialog) {
        ConnectedPosDetailsDialog(
            kitchenName = uiState.kitchenName,
            serverIp = uiState.serverIp,
            serverPort = uiState.serverPort,
            isConnected = uiState.connectedClients > 0,
            connectedTerminals = uiState.connectedPosTerminals,
            onDismiss = { showConnectedPosDetailsDialog = false }
        )
    }
}
