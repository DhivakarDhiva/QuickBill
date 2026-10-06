package com.quickbill.pos.ui.screens.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.dao.OrderDao
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.model.kds.OrderType
import com.quickbill.pos.data.repository.KdsSettings
import com.quickbill.pos.data.repository.KdsSettingsRepository
import com.quickbill.pos.network.kds.ConnectionManager
import com.quickbill.pos.network.kds.NsdDiscoveryManager
import com.quickbill.pos.network.kds.OrderSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KitchenUiState(
    val orders: List<KitchenOrder> = emptyList(),
    val selectedTab: Int = 0, // 0 = All Active, 1 = New, 2 = Preparing, 3 = Ready, 4 = Completed
    val searchQuery: String = "",
    val serverIp: String = "127.0.0.1",
    val serverPort: Int = 8887,
    val connectedClients: Int = 0,
    val isServerRunning: Boolean = false,
    val warningThresholdMinutes: Int = 5,
    val soundAlertEnabled: Boolean = true,
    val vibrateAlertEnabled: Boolean = true,
    val isSettingsDialogOpen: Boolean = false,
    val isChangeModeDialogOpen: Boolean = false,
    val latestNotificationOrder: KitchenOrder? = null
)

class QuickKitchenViewModel(
    private val orderDao: OrderDao,
    private val orderSyncManager: OrderSyncManager,
    private val connectionManager: ConnectionManager,
    private val discoveryManager: NsdDiscoveryManager,
    private val settingsRepository: KdsSettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _isSettingsDialogOpen = MutableStateFlow(false)
    private val _isChangeModeDialogOpen = MutableStateFlow(false)
    private val _latestNotificationOrder = MutableStateFlow<KitchenOrder?>(null)

    // Flow of all orders converted with their items
    private val _ordersFlow = orderDao.getAllOrdersFlow()

    val uiState: StateFlow<KitchenUiState> = combine(
        _ordersFlow,
        _selectedTab,
        _searchQuery,
        settingsRepository.settings,
        connectionManager.connectedClientsCount,
        connectionManager.isServerRunning,
        _isSettingsDialogOpen,
        _isChangeModeDialogOpen,
        _latestNotificationOrder
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val orderEntities = args[0] as List<com.quickbill.pos.data.local.entity.OrderEntity>
        val tab = args[1] as Int
        val query = args[2] as String
        val settings = args[3] as KdsSettings
        val clients = args[4] as Int
        val isRunning = args[5] as Boolean
        val isSettingsOpen = args[6] as Boolean
        val isChangeModeOpen = args[7] as Boolean
        val notificationOrder = args[8] as KitchenOrder?

        // Load items for orders synchronously in memory mapping (or on demand)
        val kitchenOrders = orderEntities.map { entity ->
            KitchenOrder(
                orderId = entity.orderId,
                orderNumber = entity.orderNumber,
                customerName = entity.customerName,
                tableNumber = entity.tableNumber,
                notes = entity.notes,
                orderType = entity.orderType,
                status = entity.status,
                items = emptyList(), // Populated by item loading or detail view
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                synced = entity.synced
            )
        }

        // Tab filtering
        val filteredByTab = when (tab) {
            0 -> kitchenOrders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
            1 -> kitchenOrders.filter { it.status == OrderStatus.NEW }
            2 -> kitchenOrders.filter { it.status == OrderStatus.PREPARING }
            3 -> kitchenOrders.filter { it.status == OrderStatus.READY }
            4 -> kitchenOrders.filter { it.status == OrderStatus.COMPLETED }
            else -> kitchenOrders
        }

        // Search filtering
        val filtered = if (query.isBlank()) {
            filteredByTab
        } else {
            filteredByTab.filter {
                it.orderNumber.contains(query, ignoreCase = true) ||
                it.customerName.contains(query, ignoreCase = true) ||
                it.tableNumber.contains(query, ignoreCase = true)
            }
        }

        KitchenUiState(
            orders = filtered,
            selectedTab = tab,
            searchQuery = query,
            serverIp = discoveryManager.getLocalIpAddress(),
            serverPort = settings.serverPort,
            connectedClients = clients,
            isServerRunning = isRunning,
            warningThresholdMinutes = settings.warningThresholdMinutes,
            soundAlertEnabled = settings.soundAlertEnabled,
            vibrateAlertEnabled = settings.vibrateAlertEnabled,
            isSettingsDialogOpen = isSettingsOpen,
            isChangeModeDialogOpen = isChangeModeOpen,
            latestNotificationOrder = notificationOrder
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KitchenUiState(serverIp = discoveryManager.getLocalIpAddress())
    )

    init {
        // Start KDS WebSocket server and NSD advertising
        startKdsServices()

        // Collect new order notifications
        viewModelScope.launch {
            orderSyncManager.newOrderNotificationFlow.collect { order ->
                _latestNotificationOrder.value = order
            }
        }
    }

    fun startKdsServices() {
        val settings = settingsRepository.settings.value
        connectionManager.startKdsServer(settings.serverPort)
        discoveryManager.startAdvertising(serviceName = "QuickKitchen-KDS", port = settings.serverPort)
    }

    fun stopKdsServices() {
        connectionManager.stopKdsServer()
        discoveryManager.stopAdvertising()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSettingsDialog() {
        _isSettingsDialogOpen.value = true
    }

    fun closeSettingsDialog() {
        _isSettingsDialogOpen.value = false
    }

    fun openChangeModeDialog() {
        _isChangeModeDialogOpen.value = true
    }

    fun closeChangeModeDialog() {
        _isChangeModeDialogOpen.value = false
    }

    fun dismissNotification() {
        _latestNotificationOrder.value = null
    }

    fun updateSettings(
        kitchenName: String,
        warningMinutes: Int,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        port: Int
    ) {
        val current = settingsRepository.settings.value
        val updated = current.copy(
            kitchenName = kitchenName,
            warningThresholdMinutes = warningMinutes,
            soundAlertEnabled = soundEnabled,
            vibrateAlertEnabled = vibrateEnabled,
            serverPort = port
        )
        settingsRepository.updateSettings(updated)
        // Restart server if port changed
        if (current.serverPort != port) {
            connectionManager.stopKdsServer()
            discoveryManager.stopAdvertising()
            connectionManager.startKdsServer(port)
            discoveryManager.startAdvertising(serviceName = "QuickKitchen-KDS", port = port)
        }
    }

    // Status transitions
    fun advanceOrderStatus(order: KitchenOrder) {
        val next = order.status.nextStatus() ?: return
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(order.orderId, next)
        }
    }

    fun setOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(orderId, newStatus)
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(orderId, OrderStatus.CANCELLED)
        }
    }

    // Fetch order items for a specific order
    fun getOrderItems(orderId: String, onResult: (List<KitchenOrderItem>) -> Unit) {
        viewModelScope.launch {
            val items = orderDao.getItemsForOrder(orderId).map {
                KitchenOrderItem(
                    id = it.id,
                    orderId = it.orderId,
                    productId = it.productId,
                    name = it.name,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    notes = it.notes,
                    isVeg = it.isVeg
                )
            }
            onResult(items)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We do not stop daemon server if running, or can clean up discovery
    }
}
